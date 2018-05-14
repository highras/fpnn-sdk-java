package com.fpnn;

import java.nio.ByteBuffer;

public class FPData {

    private byte[] _magic = FPConfig.TCP_MAGIC;

    public byte[] getMagic() {

        return this._magic;
    }

    public void setMagic(byte[] value) {

        this._magic = value;
    }


    private int _version = 1;

    public int getVersion() {

        return this._version;
    }

    public void setVersion(int value) {

        this._version = value;
    }


    private int _flag = 1;

    public int getFlag() {

        return this._flag;
    }

    public void setFlag(int value) {

        this._flag = value;
    }


    private int _mtype = 1;

    public int getMtype() {

        return this._mtype;
    }

    public void setMtype(int value) {

        this._mtype = value;
    }


    private int _ss = 0;

    public int getSS() {

        return this._ss;
    }

    public void setSS(int value) {

        this._ss = value;
    }


    private String _method = null;

    public String getMethod() {

        return this._method;
    }

    public void setMethod(String value) {

        this._method = value;

        if (this._method != null) {

            try {

                this.setSS(this._method.getBytes("utf-8").length);
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        }
    }


    private int _seq = 0;

    public int getSeq() {

        return this._seq;
    }

    public void setSeq(int value) {

        this._seq = value;
    }


    private byte[] _msgpack_data = null;

    public byte[] msgpackPayload() {

        return this._msgpack_data;
    }

    public void setPayload(byte[] value) {

        this._msgpack_data = value;

        if (this._msgpack_data != null) {

            this._psize = this._msgpack_data.length;
        }
    }


    private String _json_data = null;

    public String jsonPayload() {

        return this._json_data;
    }

    public void setPayload(String value) {

        this._json_data = value;

        if (this._json_data != null) {

            try {

                this._psize = this._json_data.getBytes("utf-8").length;
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        }
    }


    private int _psize = 0;

    public int getPsize() {

        return this._psize;
    }

    public void setPsize(int value) {

        this._psize = value;
    }


    private int _pkgLen = 0;

    public int getPkgLen() {

        return this._pkgLen;
    }

    public void setPkgLen(int value) {

        this._pkgLen = value;
        this.buffer = ByteBuffer.allocate(this._pkgLen);
    }

    public ByteBuffer buffer;
}
