package com.fpnn.sdk.proto;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 * Created by shiwangxing on 2017/11/29.
 */

@SuppressWarnings("unchecked")
public class Message {

    protected Map payload;

    //-----------------[ Constructor Functions ]-------------------
    public Message() {
        payload = new TreeMap<String, Object>();
    }

    public Message(Map body) {
        payload = body;
    }

    //-----------------[ Properties Functions ]-------------------

    public Map getPayload() {
        return payload;
    }

    public void setPayload(Map p) {
        payload = p;
    }

    //-----------------[ Data Accessing Functions ]-------------------

    public void param(String key, Object value) {
        payload.put(key, value);
    }

    public Object get(String key) {
        return payload.get(key);
    }

    public Object get(String key, Object def) {
        Object o = payload.get(key);
        return (o != null) ? o : def;
    }

    public int getInt(String key, int defaultValue) {
        Object obj = payload.get(key);
        if (obj == null)
            return defaultValue;

        if (obj instanceof Integer)
            return (Integer) obj;
        else if (obj instanceof Long)
            return ((Long) obj).intValue();
        else if (obj instanceof BigInteger)
            return ((BigInteger) obj).intValue();
        else if (obj instanceof Short)
            return ((Short) obj).intValue();
        else if (obj instanceof Byte)
            return ((Byte) obj).intValue();
        else
            return defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        Object obj = payload.get(key);
        if (obj == null)
            return defaultValue;

        if (obj instanceof Integer)
            return ((Integer) obj).longValue();
        else if (obj instanceof Long)
            return (Long) obj;
        else if (obj instanceof BigInteger)
            return ((BigInteger) obj).longValue();
        else if (obj instanceof Short)
            return ((Short) obj).longValue();
        else if (obj instanceof Byte)
            return ((Byte) obj).longValue();
        else
            return defaultValue;
    }

    public Object want(String key) throws NoSuchElementException {
        Object o = payload.get(key);
        if (o == null)
            throw new NoSuchElementException("Cannot found object for key: " + key);

        return o;
    }

    public int wantInt(String key) throws ClassCastException, NoSuchElementException {
        Object obj = want(key);
        if (obj instanceof Integer)
            return (Integer) obj;
        else if (obj instanceof Long)
            return ((Long) obj).intValue();
        else if (obj instanceof BigInteger)
            return ((BigInteger) obj).intValue();
        else if (obj instanceof Short)
            return ((Short) obj).intValue();
        else if (obj instanceof Byte)
            return ((Byte) obj).intValue();
        else
            throw new ClassCastException("Convert " + obj.getClass().getTypeName() + " to Integer failed.");
    }

    public long wantLong(String key) throws ClassCastException, NoSuchElementException {
        Object obj = want(key);
        if (obj instanceof Integer)
            return ((Integer) obj).longValue();
        else if (obj instanceof Long)
            return (Long) obj;
        else if (obj instanceof BigInteger)
            return ((BigInteger) obj).longValue();
        else if (obj instanceof Short)
            return ((Short) obj).longValue();
        else if (obj instanceof Byte)
            return ((Byte) obj).longValue();
        else
            throw new ClassCastException("Convert " + obj.getClass().getTypeName() + " to Long failed.");
    }

    //-----------------[ To Bytes Array Functions ]-------------------
    public byte[] toByteArray() throws IOException {
        MessagePayloadPacker packer = new MessagePayloadPacker();
        packer.pack(payload);
        return packer.toByteArray();
    }

    public byte[] raw() throws IOException {
        return toByteArray();
    }
}
