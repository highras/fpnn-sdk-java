package com.fpnn;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import com.fpnn.event.EventData;
import com.fpnn.nio.NIOCore;

public class FPSocket {

    public interface IRecvData {
        void onData(SocketChannel socket);
    }

    public interface ICallback {
        void socketConnect(EventData evd);
        void socketClose(EventData evd);
        void socketError(EventData evd);
    }

    class SocketLocker {
        public int status = 0;
    }

    class ConnectingLocker {
        public int status = 0;
        public long timestamp = 0;
    }

    public ICallback socketCallback;

    private int _port;
    private String _host;
    private int _timeout;
    private SocketChannel _socket = null;

    private IRecvData _recvData;

    private boolean _isIPv6 = false;
    private ByteBuffer _sendBuffer = null;

    private SocketLocker socket_locker = new SocketLocker();
    private ConnectingLocker conn_locker = new ConnectingLocker();
    private List<ByteBuffer> _sendQueue = new ArrayList<ByteBuffer>();

    public FPSocket(IRecvData recvData, String host, int port, int timeout) {
        this._host = host;
        this._port = port;
        this._timeout = timeout;
        this._recvData = recvData;
    }

    public void open() {
        if (this._host == null || this._host.isEmpty()) {
            this.onError(new Exception("Cannot open null host"));
            return;
        }

        if (this._port <= 0) {
            this.onError(new Exception("Cannot open without port"));
            return;
        }

        synchronized (socket_locker) {
            if (this._socket != null) {
                return;
            }

            socket_locker.status = 0;
        }

        synchronized (conn_locker) {
            if (conn_locker.status != 0) {
                return;
            }

            conn_locker.status = 1;
            conn_locker.timestamp = FPManager.getInstance().getMilliTimestamp();
        }

        final FPSocket self = this;
        FPManager.getInstance().asyncTask(new FPManager.ITask() {
            @Override
            public void task(Object state) {
                self.asyncConnect(state);
            }
        }, null);
    }

    private void asyncConnect(Object state) {
        synchronized (conn_locker) {
            if (conn_locker.status != 1) {
                return;
            }

            conn_locker.status = 2;
        }

        boolean isClose = false;

        try {
            synchronized (socket_locker) {
                if (this._socket != null) {
                    return;
                }

                this._socket = this.initConnect();
                InetSocketAddress addr = new InetSocketAddress(this._host, this._port);
                this._isIPv6 = addr.getAddress() instanceof Inet6Address;

                if (this._socket.connect(addr)) {
                    isClose = true;
                    this.close(new Exception("wrong connector!"));
                }

                if (!isClose) {
                    isClose = (socket_locker.status != 0);
                }
            }

            synchronized (conn_locker) {
                conn_locker.status = 0;
            }

            if (isClose) {
                this.delayClose(null);
                return;
            }

            synchronized (socket_locker) {
                NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_CONNECT);
            }
        } catch (Exception ex) {
            synchronized (conn_locker) {
                conn_locker.status = 0;
            }

            this.close(ex);
        }
    }

    public boolean isIPv6() {
        synchronized (socket_locker) {
            return this._isIPv6;
        }
    }

    public boolean isOpen() {
        synchronized (socket_locker) {
            if (this._socket != null) {
                return this._socket.isConnected();
            }

            return false;
        }
    }

    public boolean isConnecting() {
        synchronized (conn_locker) {
            return conn_locker.status != 0;
        }
    }

    public void onSecond(long timestamp) {
        if (this._timeout <= 0) {
            return;
        }

        boolean isTimeout = false;

        synchronized (conn_locker) {
            if (conn_locker.status != 0) {
                if (timestamp - conn_locker.timestamp >= this._timeout) {
                    isTimeout = true;
                    conn_locker.status = 0;
                }
            }
        }

        if (isTimeout) {
            this.close(new Exception("Connect Timeout"));
        }
    }

    public void close(Exception ex) {
        boolean firstClose = false;

        try {
            synchronized (socket_locker) {
                if (socket_locker.status == 0) {
                    firstClose = true;
                    socket_locker.status = 1;

                    if (ex != null) {
                        this.onError(ex);
                    }

                    if (this.isConnecting()) {
                        return;
                    }
                }

                this.tryClose();
            }

            if (firstClose) {
                final FPSocket self = this;
                FPManager.getInstance().delayTask(200, new FPManager.ITask() {
                    @Override
                    public void task(Object state) {
                        self.delayClose(state);
                    }
                }, null);
            }
        } catch (Exception e) {
            ErrorRecorder.getInstance().recordError(e);
        }
    }

    private void delayClose(Object state) {
        synchronized (socket_locker) {
            if (socket_locker.status != 3) {
                this.socketClose();
            }
        }
    }

    private void tryClose() {
        if (socket_locker.status == 3) {
            return;
        }

        try {
            this.socketClose();
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        }
    }

    private void socketClose() {
        NIOCore.getInstance().closeSocket(this._socket);
        socket_locker.status = 3;
        this.onClose();
    }

    private void onClose() {
        try {
            if (this.socketCallback != null) {
                this.socketCallback.socketClose(new EventData(this, "close"));
            }
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        }

        this.destroy();
    }

    private void destroy() {
        this._recvData = null;
        this.socketCallback = null;
    }

    private Object self_locker = new Object();

    public void write(ByteBuffer buf) {
        if (buf == null) {
            return;
        }

        synchronized (self_locker) {
            this._sendQueue.add(buf);
        }

        synchronized (socket_locker) {
            if (socket_locker.status != 0) {
                return;
            }

            NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        }
    }

    public String getHost() {
        return this._host;
    }

    public int getPort() {
        return this._port;
    }

    public int getTimeout() {
        return this._timeout;
    }

    private void onConnect() {
        try {
            if (this.socketCallback != null) {
                this.socketCallback.socketConnect(new EventData(this, "connect"));
            }
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        }
    }

    public void onRead(SelectionKey key) {
        synchronized (socket_locker) {
            if (this._recvData != null) {
                this._recvData.onData(this._socket);
            }
        }
    }

    private void onError(Exception ex) {
        try {
            if (this.socketCallback != null) {
                this.socketCallback.socketError(new EventData(this, "error", ex));
            }
        } catch (Exception e) {
            ErrorRecorder.getInstance().recordError(e);
        }
    }

    public void onWrite(SelectionKey key) throws IOException {
        boolean isEmptyQueue = false;

        synchronized (self_locker) {
            if (this._sendBuffer == null && !this._sendQueue.isEmpty()) {
                this._sendBuffer = this._sendQueue.remove(0);
            }

            if (this._sendBuffer != null) {
                this._sendBuffer.flip();
            }

            if (this._sendBuffer == null) {
                synchronized (socket_locker) {
                    NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_READ);
                }

                return;
            }

            try {
                synchronized (socket_locker) {
                    int i = this._socket.write(this._sendBuffer);
                }
            } catch (IOException ex) {
                this.onError(ex);
            } catch (Exception ex) {
                this.close(ex);
                return;
            }

            if (this._sendBuffer.hasRemaining()) {
                synchronized (socket_locker) {
                    NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                }

                return;
            }

            this._sendBuffer.clear();
            this._sendBuffer = null;
            isEmptyQueue = this._sendQueue.isEmpty();
        }

        synchronized (socket_locker) {
            if (isEmptyQueue) {
                NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_READ);
            } else {
                NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            }
        }
    }

    public void finishConnection(SelectionKey key) {
        boolean flag = false;

        try {
            synchronized (socket_locker) {
                flag = this._socket.finishConnect();
            }
        } catch (Exception ex) {
            this.close(ex);
        }

        if (flag) {
            synchronized (socket_locker) {
                NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }

            this.onConnect();
        }
    }

    private SocketChannel initConnect() throws IOException {
        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);
        return socket;
    }
}
