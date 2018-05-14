package com.fpnn;

import com.fpnn.callback.FPCallback;
import com.fpnn.callback.FPCallbackManager;
import com.fpnn.encryptor.FPEncryptor;
import com.fpnn.event.FPEvent;
import com.fpnn.event.FPEventManager;
import com.fpnn.nio.NIOCore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class FPClient {

    public interface IAnswer {

        void sendAnswer(Object payload, boolean exception);
    }

    public interface IKeyData {

        FPData getKeyData(FPEncryptor encryptor);
    }

    private int _seq = 0;
    private int _timeout = 30 * 1000;
    private boolean _autoReconnect;

    private FPSocket _sock;
    private FPData _peekData = null;
    private IKeyData _keyData = null;
    private ByteBuffer _buffer = ByteBuffer.allocate(FPConfig.READ_BUFFER_LEN);

    private FPPackage _pkg = new FPPackage();
    private FPEncryptor _cyr = new FPEncryptor(_pkg);
    private FPEventManager _event = new FPEventManager();
    private FPProcessor _psr = new FPProcessor();
    private FPCallbackManager _callback = new FPCallbackManager();

    private long _intervalID = 0;

    public FPClient(String endpoint, boolean autoReconnect, int connectionTimeout) {

        String[] ipport = endpoint.split(":", 2);
        this.init(ipport[0], Integer.parseInt(ipport[1]), autoReconnect, connectionTimeout);
    }

    public FPClient(String host, int port, boolean autoReconnect, int connectionTimeout) {

        this.init(host, port, autoReconnect, connectionTimeout);
    }

    protected void init(String host, int port, boolean autoReconnect, int connectionTimeout) {

        if (connectionTimeout > 0) {

            this._timeout = connectionTimeout;
        }

        this._autoReconnect = autoReconnect;


        final FPClient self = this;
        NIOCore.getInstance().getEvent().addListener("second", new FPEvent.IListener() {

            @Override
            public void fpEvent(FPEvent event) {

                self.onSecond(event.getTimestamp());
            }
        });

        this._sock = new FPSocket(new FPSocket.IRecvData() {

            @Override
            public void onData(SocketChannel socket) {

                self.onData(socket);
            }
        }, host, port, this._timeout);

        FPEvent.IListener listener = new FPEvent.IListener() {

            @Override
            public void fpEvent(FPEvent event) {

                switch (event.getType()) {
                    case "connect":
                        self.onConnect();
                        break;
                    case "close":
                        self.onClose();
                        break;
                    case "error":
                        self.onError(event.getException());
                        break;
                }
            }
        };

        this._sock.getEvent().addListener("connect", listener);
        this._sock.getEvent().addListener("close", listener);
        this._sock.getEvent().addListener("error", listener);
    }

    public FPEventManager getEvent() {

        return this._event;
    }

    public FPProcessor getProcessor() {

        return this._psr;
    }

    public FPSocket sock() {

        return this._sock;
    }

    public boolean encryptor(String curve, byte[] peerPublicKey, boolean streamMode, boolean reinforce, boolean bcLib) {

        this._cyr.setCurve(curve);
        this._cyr.setPeerPublicKey(peerPublicKey);
        this._cyr.setStreamMode(streamMode);
        this._cyr.setReinforce(reinforce);
        this._cyr.setBCLib(bcLib);

        if (this.hasConnect() || this._cyr.isCrypto()) {

            this.onError(new Exception("has connected or enable crypto!'"));
            return false;
        }

        return this._cyr.encryptor();
    }

    public boolean encryptor() {

        if (this.hasConnect() || this._cyr.isCrypto()) {

            this.onError(new Exception("has connected or enable crypto!'"));
            return false;
        }

        return this._cyr.encryptor();
    }

    public void connect() {

        this._sock.open();
    }

    public void connect(IKeyData keyData) {

        this._keyData = keyData;
        this.connect();
    }

    public void close() {

        this._sock.close(null);
    }

    public void close(Exception ex) {

        this._sock.close(ex);
    }

    public void sendQuest(FPData data) {

        this.sendQuest(data, null, 0);
    }

    public FPCallback sendQuest(FPData data, int timeout) throws InterruptedException {

        FPCallback fpcb = null;

        SyncCallbak syncCallbak = new SyncCallbak();
        this.sendQuest(data, syncCallbak, timeout);

        synchronized (syncCallbak) {

            while (syncCallbak.getReturn() == null) {

                syncCallbak.wait();
            }

            fpcb = syncCallbak.getReturn();
        }

        return fpcb;
    }

    public void sendQuest(FPData data, FPCallback.ICallback callback) {

        this.sendQuest(data, callback, 0);
    }

    public void sendQuest(FPData data, FPCallback.ICallback callback, int timeout) {

        if (data.getSeq() == 0) {

            data.setSeq(this.addSeq());
        }

        ByteBuffer buf = null;

        try {

            buf = this._pkg.enCode(data);
            buf = this._cyr.enCode(buf);
        } catch (IOException ex) {

            this.onError(ex);
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
            buf = this._cyr.enCode(buf);
        } catch (IOException ex) {

            this.onError(ex);
        }

        if (buf != null) {

            this._sock.write(buf);
        }
    }

    public boolean isOpen() {

        return this._sock.isOpen();
    }

    public boolean hasConnect() {

        return this._sock.isOpen() || this._sock.isConnecting();
    }

    private void sendkey() {

        final FPClient self = this;
        if (this._cyr.isCrypto()) {

            FPData data = this._keyData.getKeyData(this._cyr);
            data.setMagic(FPConfig.TCP_MAGIC);
            data.setFlag(0x1);
            data.setMtype(0x1);
            data.setMethod("*key");

            this.sendQuest(data, new FPCallback.ICallback() {

                @Override
                public void callback(FPCallback fpcb) {

                    self.onSendKey(fpcb.getData());
                }
            }, this._timeout);

            this._cyr.setCryptoed(true);
            return;
        }

        this._intervalID = 0;
        this._event.fireEvent(new FPEvent(this, "connect"));
    }

    private void onSendKey(FPData data) {

        if (data.getFlag() == 0) {

            if (!data.jsonPayload().trim().equals("{}")) {

                this._cyr.setCryptoed(false);
                this._event.fireEvent(new FPEvent(this, "error", new Exception("wrong cryptor!")));
                return;
            }
        }

        if (data.getFlag() == 1) {

            if (data.msgpackPayload().length != 1) {

                this._cyr.setCryptoed(false);
                this._event.fireEvent(new FPEvent(this, "error", new Exception("wrong cryptor!")));
                return;
            }
        }

        this._intervalID = 0;
        this._event.fireEvent(new FPEvent(this, "connect"));
    }

    private void onConnect() {

        this.sendkey();
    }

    private void onClose() {

        this._seq = 0;
        this._peekData = null;
        this._buffer.clear();

        this._callback.removeCallback();
        this._cyr.clear();

        this._event.fireEvent(new FPEvent(this, "close"));

        if (this._autoReconnect) {

            this.reConnect();
        }
    }

    public void onData(SocketChannel socket) {

        int numRead = this.readSocket(socket);

        if (numRead == -1) {

            this._sock.close(null);
            return;
        }

        if (!this._buffer.hasRemaining()) {

            this._peekData = this._cyr.peekHead(this._buffer.array());

            this._buffer.clear();

            if (this._peekData == null) {

                this._sock.close(new Exception("worng package head!"));
                return;
            }
        }

        if (this._peekData != null && !this._peekData.buffer.hasRemaining()) {

            this._peekData.buffer = this._cyr.deCode(this._peekData.buffer.array());

            this._peekData = this._cyr.peekHead(this._peekData);

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
    }

    private int readSocket(SocketChannel socket) {

        int numRead = -1;

        try {

            if (this._peekData == null) {

                numRead = socket.read(this._buffer);
            } else {

                numRead = socket.read(this._peekData.buffer);
            }
        } catch (IOException ex) {

            this.onError(ex);
        }

        return numRead;
    }

    private void pushService(FPData quest) {

        final FPClient self = this;
        final FPData fQuest = quest;
        this._psr.service(quest, new IAnswer() {

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

        if (key != null) {

            this._callback.execCallback(key, answer);
        }
    }

    private void onError(Exception ex) {

        ex.printStackTrace();
        this._event.fireEvent(new FPEvent(this, "error", ex));
    }

    private void onSecond(long timestamp) {

        this._psr.onSecond(timestamp);
        this._callback.onSecond(timestamp);

        if (this._intervalID == 0) {

            return;
        }


        if (this.hasConnect()) {

            return;
        }

        if (timestamp - this._intervalID < 100) {

            return;
        }

        this._intervalID = timestamp;

        if (this._cyr.isCrypto()) {

            if (this.encryptor()) {

                this.connect(this._keyData);
                return;
            }
        }

        this.connect();
    }

    private synchronized int addSeq() {

        return ++this._seq;
    }

    private void reConnect() {

        if (this._intervalID == 0) {

            this._intervalID = NIOCore.getInstance().getTimestamp();
        }
    }
}

class SyncCallbak implements FPCallback.ICallback {

    private FPCallback _fpcb;

    @Override
    public void callback(FPCallback fpcb) {

        synchronized (this) {

            this._fpcb = fpcb;
            this.notifyAll();
        }
    }

    public FPCallback getReturn() {

        return this._fpcb;
    }
}
