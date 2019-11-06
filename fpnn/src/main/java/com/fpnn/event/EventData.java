package com.fpnn.event;

import java.nio.channels.SocketChannel;
import java.util.EventObject;
import com.fpnn.FPData;

public class EventData extends EventObject {

    private String _type;

    public String getType() {
        return this._type;
    }

    public EventData(Object source, String type) {
        super(source);
        this._type = type;
    }


    private FPData _data = null;

    public FPData getData() {
        return this._data;
    }

    public EventData(Object source, String type, FPData data) {
        super(source);
        this._type = type;
        this._data = data;
    }


    private SocketChannel _socket = null;

    public SocketChannel getSocket() {
        return this._socket;
    }

    public EventData(Object source, String type, SocketChannel socket) {
        super(source);
        this._type = type;
        this._socket = socket;
    }


    private Exception _exception = null;

    public Exception getException() {
        return this._exception;
    }

    public EventData(Object source, String type, Exception ex) {
        super(source);
        this._type = type;
        this._exception = ex;
    }


    private long _timestamp = 0;

    public long getTimestamp() {
        return this._timestamp;
    }

    public EventData(Object source, String type, long timestamp) {
        super(source);
        this._type = type;
        this._timestamp = timestamp;
    }


    private Object _payload;

    public Object getPayload() {
        return this._payload;
    }

    public EventData(Object source, String type, Object payload) {
        super(source);
        this._type = type;
        this._payload = payload;
    }

    private boolean _retry;

    public boolean hasRetry() {
        return this._retry;
    }

    public EventData(Object source, String type, boolean retry) {
        super(source);
        this._type = type;
        this._retry = retry;
    }
}
