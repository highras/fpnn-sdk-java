package com.fpnn;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import com.fpnn.callback.CallbackData;
import com.fpnn.callback.FPCallback;
import com.fpnn.encryptor.FPEncryptor;
import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;

public class FPClient {

    public interface IKeyData {
        FPData getKeyData(FPEncryptor encryptor);
    }

    public interface ICallback {
        void clientConnect(EventData evd);
        void clientClose(EventData evd);
        void clientError(EventData evd);
    }

    class SyncCallbak implements FPCallback.ICallback {
        private CallbackData _cbd;

        @Override
        public void callback(CallbackData cbd) {
            synchronized (this) {
                this._cbd = cbd;
                this.notifyAll();
            }
        }
        public CallbackData getReturn() {
            synchronized (this) {
                return this._cbd;
            }
        }
    }

    public ICallback clientCallback;

    private int _seq = 0;
    private boolean _isClose;

    private FPSocket _sock;
    private IKeyData _keyData = null;

    private FPPackage _pkg;
    private FPEncryptor _cry;
    private FPProcessor _psr;
    private FPCallback _callback;

    private FPEvent.IListener _secondListener = null;

    public FPClient(String endpoint, int connectionTimeout) {
        if (endpoint == null || endpoint.isEmpty()) {
            this.init(null, 0, connectionTimeout);
            return;
        }

        String[] ipport = endpoint.split(":", 2);
        if (ipport.length >=2) {
            this.init(ipport[0], Integer.parseInt(ipport[1]), connectionTimeout);
            return;
        }

        this.init(null, 0, connectionTimeout);
    }

    public FPClient(String host, int port, int connectionTimeout) {
        this.init(host, port, connectionTimeout);
    }

    protected void init(String host, int port, int connectionTimeout) {
        this._pkg = new FPPackage();
        this._cry = new FPEncryptor(_pkg);
        this._psr = new FPProcessor();
        this._callback = new FPCallback();

        if (connectionTimeout <= 0) {
            connectionTimeout = 30 * 1000;
        }

        final FPClient self = this;
        this._secondListener = new FPEvent.IListener() {
            @Override
            public void fpEvent(EventData evd) {
                self.onSecond(evd.getTimestamp());
            }
        };
        FPManager.getInstance().addSecond(this._secondListener);

        this._sock = new FPSocket(new FPSocket.IRecvData() {
            @Override
            public void onData(SocketChannel socket) {
                self.onData(socket);
            }
        }, host, port, connectionTimeout);
        this._sock.socketCallback = new FPSocket.ICallback() {
            @Override
            public void socketConnect(EventData evd) {
                self.sendkey();
            }
            @Override
            public void socketClose(EventData evd) {
                self.onClose(evd);
            }
            @Override
            public void socketError(EventData evd) {
                self.onError(evd);
            }
        };
    }

    public FPProcessor getProcessor() {
        return this._psr;
    }

    public FPPackage getPackage() {
        return this._pkg;
    }

    public FPSocket getSock() {
        return this._sock;
    }

    private Object self_locker = new Object();

    public boolean encryptor(String curve, byte[] peerPublicKey, boolean streamMode, boolean reinforce, boolean bcLib) {
        this._cry.setCurve(curve);
        this._cry.setPeerPublicKey(peerPublicKey);
        this._cry.setStreamMode(streamMode);
        this._cry.setReinforce(reinforce);
        this._cry.setBCLib(bcLib);

        if (this.hasConnect() || this._cry.isCrypto()) {
            ErrorRecorder.getInstance().recordError(new Exception("has connected or enable crypto!"));
            return false;
        }
        return this._cry.encryptor();
    }

    public boolean encryptor() {
        if (this.hasConnect() || this._cry.isCrypto()) {
            ErrorRecorder.getInstance().recordError(new Exception("has connected or enable crypto!"));
            return false;
        }
        return this._cry.encryptor();
    }

    public void connect() {
        synchronized (self_locker) {
            if (this._isClose) {
                return;
            }

            this._sock.open();
        }
    }

    public void connect(IKeyData keyData) {
        synchronized (self_locker) {
            this._keyData = keyData;
        }
        this.connect();
    }

    public void close() {
        synchronized (self_locker) {
            if (this._isClose) {
                return;
            }

            this.socketClose(null);
        }
    }

    public void close(Exception ex) {
        synchronized (self_locker) {
            if (this._isClose) {
                return;
            }

            this.socketClose(ex);
        }
    }

    private void socketClose(Exception ex) {
        this._isClose = true;
        if (this._secondListener != null) {
            FPManager.getInstance().removeSecond(this._secondListener);
            this._secondListener = null;
        }

        this._psr.destroy();
        this._sock.close(ex);
    }

    private void destroy() {
        if (this.clientCallback != null) {
            this.clientCallback = null;
        }
    }

    public void sendQuest(FPData data) {
        this.sendQuest(data, null, 0);
    }

    public CallbackData sendQuest(FPData data, int timeout) throws InterruptedException {
        CallbackData cbd = null;
        SyncCallbak syncCallbak = new SyncCallbak();
        this.sendQuest(data, syncCallbak, timeout);
        synchronized (syncCallbak) {
            while (syncCallbak.getReturn() == null) {
                syncCallbak.wait();
            }
            cbd = syncCallbak.getReturn();
        }
        return cbd;
    }

    public void sendQuest(FPData data, FPCallback.ICallback callback) {
        this.sendQuest(data, callback, 0);
    }

    public void sendQuest(FPData data, FPCallback.ICallback callback, int timeout) {
        if (data != null && data.getSeq() == 0) {
            data.setSeq(this.addSeq());
        }

        ByteBuffer buf = null;
        try {
            buf = this._pkg.enCode(data);
            buf = this._cry.enCode(buf);
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
            return;
        }

        if (callback != null) {
            this._callback.addCallback(this._pkg.getKeyCallback(data), callback, timeout);
        }

        if (buf != null) {
            this._sock.write(buf);
        }
    }

    public void sendNotify(FPData data) {
        if (data.getMtype() != 0x0) {
            data.setMtype(0x0);
        }

        ByteBuffer buf = null;
        try {
            buf = this._pkg.enCode(data);
            buf = this._cry.enCode(buf);
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
            return;
        }

        if (buf != null) {
            this._sock.write(buf);
        }
    }

    public boolean isIPv6() {
        return this._sock.isIPv6();
    }

    public boolean isOpen() {
        return this._sock.isOpen();
    }

    public boolean hasConnect() {
        return this._sock.isOpen() || this._sock.isConnecting();
    }

    private void sendkey() {
        final FPClient self = this;
        if (this._cry.isCrypto()) {
            FPData data = this._keyData.getKeyData(this._cry);
            data.setMagic(FPConfig.TCP_MAGIC);
            data.setFlag(0x1);
            data.setMtype(0x1);
            data.setMethod("*key");

            this.sendQuest(data, new FPCallback.ICallback() {
                @Override
                public void callback(CallbackData cbd) {
                    self.onSendKey(cbd.getData());
                }
            }, 30 * 1000);

            this._cry.setCryptoed(true);
            return;
        }

        this.onConnect();
    }

    private void onSendKey(FPData data) {
        if (data.getFlag() == 0) {
            if (!data.jsonPayload().trim().equals("{}")) {
                this._cry.setCryptoed(false);
                ErrorRecorder.getInstance().recordError(new Exception("wrong cryptor!"));
                return;
            }
        }

        if (data.getFlag() == 1) {
            if (data.msgpackPayload().length != 1) {
                this._cry.setCryptoed(false);
                ErrorRecorder.getInstance().recordError(new Exception("wrong cryptor!"));
                return;
            }
        }

        this.onConnect();
    }

    private void onConnect() {
        try {
            if (this.clientCallback != null) {
                this.clientCallback.clientConnect(new EventData(this, "connect"));
            }
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        }
    }

    private void onClose(EventData evd) {
        synchronized (self_locker) {
            this._peekData = null;
            this._buffer.clear();
            this._callback.removeCallback();
            this._cry.clear();
        }

        synchronized (seq_locker) {
            this._seq = 0;
        }

        try {
            if (this.clientCallback != null) {
                this.clientCallback.clientClose(evd);
            }
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        }

        this.destroy();
    }

    private FPData _peekData = null;
    private ByteBuffer _buffer = ByteBuffer.allocate(FPConfig.READ_BUFFER_LEN);

    public void onData(SocketChannel socket) {
        synchronized (self_locker) {
            if (this._buffer.hasRemaining()) {
                int num = this.readHead(socket);
                if (num == -1) {
                    return;
                }
            }

            if (!this._buffer.hasRemaining()) {
                boolean res = this.buildHead();
                if (!res) {
                    return;
                }
            }

            if (this._peekData.buffer.hasRemaining()) {
                int num = this.readBody(socket);
                if (num == -1) {
                    return;
                }
            }

            if (!this._peekData.buffer.hasRemaining()) {
                this.buildData();
            }
        }
    }

    private int readHead(SocketChannel socket) {
        return this.readSocket(socket, this._buffer);
    }

    private boolean buildHead() {
        this._peekData = this._cry.peekHead(this._buffer.array());
        this._buffer.clear();

        if (this._peekData == null) {
            this._sock.close(new Exception("worng package head!"));
            return false;
        }
        return true;
    }

    private int readBody(SocketChannel socket) {
        return this.readSocket(socket, this._peekData.buffer);
    }

    private void buildData() {
        this._peekData.buffer = this._cry.deCode(this._peekData.buffer.array());
        this._peekData = this._cry.peekHead(this._peekData);
        if (!this._pkg.deCode(this._peekData)) {
            this._sock.close(new Exception("worng package body!"));
            return;
        }

        if (this._pkg.isAnswer(this._peekData)) {
            this.execCallback(this._peekData);
        }

        if (this._pkg.isQuest(this._peekData)) {
            this.pushService(this._peekData);
        }
        this._peekData = null;
    }

    private int readSocket(SocketChannel socket, ByteBuffer buf) {
        int numRead = -1;
        try {
            numRead = socket.read(buf);
        } catch (Exception ex) {
            this.close(ex);
        }
        return numRead;
    }

    private void onError(EventData evd) {
        try {
            if (this.clientCallback != null) {
                this.clientCallback.clientError(evd);
            }
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        }
    }

    private void onSecond(long timestamp) {
        this._psr.onSecond(timestamp);
        this._callback.onSecond(timestamp);
    }

    private void pushService(FPData quest) {
        final FPClient self = this;
        final FPData fQuest = quest;
        this._psr.service(quest, new FPProcessor.IAnswer() {
            @Override
            public void sendAnswer(Object payload, boolean exception) {
                FPData data = new FPData();
                data.setFlag(fQuest.getFlag());
                data.setMtype(0x2);
                data.setSeq(fQuest.getSeq());
                data.setSS(exception ? 1 : 0);

                if (fQuest.getFlag() == 0) {
                    data.setPayload((String) payload);
                }

                if (fQuest.getFlag() == 1) {
                    data.setPayload((byte[]) payload);
                }
                self.sendQuest(data);
            }
        });
    }

    private void execCallback(FPData answer) {
        String key = this._pkg.getKeyCallback(answer);
        if (key != null && !key.isEmpty()) {
            this._callback.execCallback(key, answer);
        }
    }

    private Object seq_locker = new Object();

    private int addSeq() {
        synchronized (seq_locker) {
            return ++this._seq;
        }
    }
}
