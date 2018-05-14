package com.fpnn.encryptor;

import com.fpnn.FPConfig;
import com.fpnn.FPData;
import com.fpnn.FPPackage;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

public class FPEncryptor {

    private String _curve;
    private byte[] _peerPublicKey;
    private boolean _streamMode;
    private boolean _reinforce;
    private boolean _bcLib;

    private FPPackage _pkg;

    private boolean _cryptoed;

    private KeyGenerator _keyGenerator;
    private KeyGenerator.EncryptionKit _encKit;

    public FPEncryptor(FPPackage pkg) {

        this._pkg = pkg;
    }

    public FPEncryptor(FPPackage pkg, String curve, byte[] peerPublicKey, boolean streamMode, boolean reinforce, boolean bcLib) {

        this._curve = curve;
        this._peerPublicKey = peerPublicKey;
        this._streamMode = streamMode;
        this._reinforce = reinforce;
        this._bcLib = bcLib;

        this._pkg = pkg;
    }

    public void setCurve(String value) {

        this._curve = value;
    }

    public void setPeerPublicKey(byte[] value) {

        this._peerPublicKey = value;
    }

    public void setStreamMode(boolean value) {

        this._streamMode = value;
    }

    public void setReinforce(boolean value) {

        this._reinforce = value;
    }

    public void setBCLib(boolean value) {

        this._bcLib = value;
    }

    public void clear() {

        this._cryptoed = false;
        this._encKit = null;
        this._keyGenerator = null;
    }

    public boolean encryptor() {

        try {

            this._keyGenerator = KeyGenerator.create(this._curve, this._peerPublicKey, this._streamMode, this._reinforce, this._bcLib);
        } catch (Exception ex) {

            this._keyGenerator = null;
            ex.printStackTrace();
        }

        if (this._keyGenerator != null) {

            try {

                this._encKit = this._keyGenerator.gen();
            } catch (GeneralSecurityException ex) {

                this._encKit = null;
                ex.printStackTrace();
            }
        }

        return this.isCrypto();
    }

    public KeyGenerator.EncryptionKit cryptoInfo() {

        return this._encKit;
    }

    public boolean streamMode() {

        if (this._encKit != null) {

            return this._encKit.streamMode;
        }

        return false;
    }

    public boolean isCrypto() {

        if (this._encKit == null) {

            return false;
        }

        if (this._encKit.decryptor == null) {

            return false;
        }

        if (this._encKit.encryptor == null) {

            return false;
        }

        return true;
    }

    public boolean cryptoed() {

        return this._cryptoed;
    }

    public void setCryptoed(boolean value) {

        this._cryptoed = value;
    }

    public ByteBuffer deCode(byte[] bytes) {

        if (this._cryptoed && !this.streamMode()) {

            return this.cryptoDecode(bytes);
        }

        return ByteBuffer.wrap(bytes);
    }

    private ByteBuffer cryptoDecode(byte[] bytes) {

        try {

            bytes = this._encKit.decryptor.doFinal(bytes);
        } catch (GeneralSecurityException ex) {

            ex.printStackTrace();
        }

        return ByteBuffer.wrap(bytes);
    }

    private ByteBuffer streamDecode(ByteBuffer buf) {

        //TODO
        return buf;
    }

    public ByteBuffer enCode(ByteBuffer buf) {

        if (this._cryptoed && !this.streamMode()) {

            return this.cryptoEncode(buf);
        }

        return buf;
    }

    private ByteBuffer cryptoEncode(ByteBuffer buf) {

        byte[] bytes = new byte[0];

        try {

            bytes = this._encKit.encryptor.doFinal(buf.array());
        } catch (GeneralSecurityException ex) {

            ex.printStackTrace();
        }

        ByteBuffer rbuf = ByteBuffer.allocate(bytes.length + 4);

        rbuf.put(this._pkg.Uint32BEToLEByte(bytes.length));
        rbuf.put(bytes);

        return rbuf;
    }

    private ByteBuffer streamEncode(ByteBuffer buf) {

        //TODO
        return buf;
    }

    public FPData peekHead(byte[] bytes) {

        if (!this.cryptoed()) {

            return this.commonPeekHead(bytes);
        }

        if (this.streamMode()) {

            return this.streamPeekHead(bytes);
        }

        return this.cryptoPeekHead(bytes);
    }

    public FPData peekHead(FPData peek) {

        if (this._cryptoed) {

            FPData data = this._pkg.peekHead(peek.buffer.array());
            data.buffer = peek.buffer;

            return data;
        }

        return peek;
    }

    private FPData commonPeekHead(byte[] bytes) {

        if (bytes.length == 12) {

            FPData data = this._pkg.peekHead(bytes);

            if (!this.checkHead(data)) {

                return null;
            }

            if (this._pkg.isOneWay(data)) {

                data.setPkgLen(12 + data.getSS() + data.getPsize());
            }

            if (this._pkg.isTwoWay(data)) {

                data.setPkgLen(16 + data.getSS() + data.getPsize());
            }

            if (this._pkg.isAnswer(data)) {

                data.setPkgLen(16 + data.getPsize());
            }

            data.buffer.put(bytes);
            return data;
        }

        return null;
    }

    private FPData cryptoPeekHead(byte[] bytes) {

        if (bytes.length >= 4) {

            FPData data = new FPData();

            byte[] bs = new byte[4];
            System.arraycopy(bytes, 0, bs, 0, bs.length);

            data.setPkgLen(this._pkg.LEByteToUint32BE(bs));

            data.buffer.put(bytes, bs.length, bytes.length - bs.length);

            if (data.getPkgLen() > 8 * 1024 * 1024) {

                return null;
            }

            return data;
        }

        return null;
    }

    private FPData streamPeekHead(byte[] bytes) {

        //TODO
        return null;
    }

    private boolean checkHead(FPData data) {

        if (!this._pkg.isTcp(data) && !this._pkg.isHttp(data)) {

            return false;
        }

        if (data.getVersion() < 0 || data.getVersion() >= FPConfig.FPNN_VERSION.length) {

            return false;
        }

        if (!this._pkg.checkVersion(data)) {

            return false;
        }

        if (!this._pkg.isMsgPack(data) && !this._pkg.isJson(data)) {

            return false;
        }

        if (!this._pkg.isOneWay(data) && !this._pkg.isTwoWay(data) && !this._pkg.isAnswer(data)) {

            return false;
        }

        return true;
    }
}
