package com.fpnn;

import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;
import com.fpnn.nio.NIOCore;
import com.fpnn.nio.ThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class FPSocket {

    public interface IRecvData {

        void onData(SocketChannel socket);
    }

    private int _port;
    private String _host;
    private int _timeout;
    private SocketAddress _addr;
    private SocketChannel _socket = null;

    private IRecvData _recvData;

    private long _expire = 0;
    private boolean _isClosed = true;
    private FPEvent _event;

    private ByteBuffer _sendBuffer = null;
    private List _sendQueue = new ArrayList();

    public FPSocket(IRecvData recvData, String host, int port, int timeout) {

        this._host = host;
        this._port = port;
        this._timeout = timeout;
        this._event = new FPEvent();
        this._recvData = recvData;
    }

    public FPEvent getEvent() {

        return this._event;
    }

    public void open() {

        if (this._socket != null && (this._socket.isConnected() || this._socket.isConnectionPending())) {

            this.onError(new Exception("has been connect!"));
            return;
        }

        this._isClosed = false;

        try {

            this._socket = this.initConnect();
        } catch (IOException ex) {

            this.onError(ex);
        }

        final FPSocket self = this;
        final SocketChannel socket = this._socket;

        ThreadPool.getInstance().execute(new Runnable() {

            @Override
            public void run() {
                try {

                    SocketAddress addr = self.getAddr();
                    if (socket.connect(addr)) {

                        self.close(new Exception("wrong connector!"));
                        return;
                    }

                    NIOCore.getInstance().register(self, socket, SelectionKey.OP_CONNECT);
                } catch (IOException ex) {

                    self.close(ex);
                }
            }
        });
    }

    public void write(ByteBuffer buf) {

        synchronized (this._sendQueue) {

            this._sendQueue.add(buf);

            if (this.isOpen()) {

                NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            }
        }
    }

    public void close(Exception ex) {

        if (!this._isClosed) {

            this._isClosed = true;
            NIOCore.getInstance().closeSocket(this._socket);
            this.onClose(ex);
        }
    }

    public String getHost() {

        return this._host;
    }

    public int getPort() {

        return this._port;
    }

    public SocketAddress getAddr() {

        if (this._addr == null) {

            this._addr = new InetSocketAddress(this._host, this._port);
        }

        return this._addr;
    }

    public int getTimeout() {

        return this._timeout;
    }

    public boolean isOpen() {

        if (this._socket != null) {

            return this._socket.isConnected();
        }

        return false;
    }

    public boolean isConnecting() {

        if (this._socket != null) {

            return this._socket.isConnectionPending();
        }

        return false;
    }

    public void onRead(SelectionKey key) {

        this.onData(this._socket);
    }

    public void onWrite(SelectionKey key) throws IOException {

        synchronized (this._sendQueue) {

            if (this._sendBuffer == null && !this._sendQueue.isEmpty()) {

                this._sendBuffer = (ByteBuffer) this._sendQueue.get(0);
                this._sendBuffer.flip();
                this._sendQueue.remove(0);
            }

            if (this._sendBuffer == null) {

                NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_READ);
                return;
            }

            try {

                int i = this._socket.write(this._sendBuffer);
            } catch (Exception ex) {

                if (ex instanceof IOException) {

                    throw (ex);
                } else {

                    this.close(ex);
                    return;
                }
            }

            if (this._sendBuffer.hasRemaining()) {

                NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                return;
            }

            this._sendBuffer.clear();
            this._sendBuffer = null;

            if (this._sendQueue.isEmpty()) {

                NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_READ);
            }else{

                NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            }
        }
    }

    public void finishConnection(SelectionKey key) {

        boolean flag = false;

        try {

            flag = this._socket.finishConnect();
        } catch (Exception ex) {

            this.close(ex);
        }

        if (flag) {

            NIOCore.getInstance().register(this, this._socket, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            this.onConnect();
        }
    }

    private SocketChannel initConnect() throws IOException {

        if (this._timeout > 0) {

            this._expire = NIOCore.getInstance().getTimestamp() + this._timeout;
        }

        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);

        return socket;
    }

    private void onConnect() {

        this._expire = 0;
        this._event.fireEvent(new EventData(this, "connect"));
    }

    private void onClose(Exception ex) {

        this._expire = 0;
        this._socket = null;

        if (ex != null) {

            this.onError(ex);
        }

        this._event.fireEvent(new EventData(this, "close"));
    }

    private void onData(SocketChannel socket) {

        this._recvData.onData(socket);
    }

    private void onError(Exception ex) {

        this._event.fireEvent(new EventData(this, "error", ex));
    }

    public void onSecond(long timestamp) {

        if (this._expire > 0) {

            if (timestamp > this._expire) {

                this.close(new Exception("connect time out!"));
            }
        }
    }
}
