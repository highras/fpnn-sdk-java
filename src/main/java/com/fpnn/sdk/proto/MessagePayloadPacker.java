package com.fpnn.sdk.proto;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

/**
 * Created by shiwangxing on 2017/12/1.
 */

@SuppressWarnings("unchecked")
public class MessagePayloadPacker {

    private MessageBufferPacker packer;

    public MessagePayloadPacker() {
        packer = MessagePack.newDefaultBufferPacker();
    }

    private void dispatch(Object obj) throws IOException {
        if (obj instanceof String) {
            packer.packString((String)obj);
        } else if (obj instanceof Integer) {
            packer.packInt((int)obj);
        } else if (obj instanceof Long) {
            packer.packLong((long)obj);
        } else if (obj instanceof Short) {
            packer.packShort((short)obj);
        } else if (obj instanceof Byte) {
            packer.packByte((byte)obj);
        } else if (obj instanceof Double) {
            packer.packDouble((double)obj);
        } else if (obj instanceof Float) {
            packer.packFloat((float)obj);
        } else if (obj instanceof Boolean) {
            packer.packBoolean((boolean)obj);
        } else if (obj instanceof Character) {
            packer.packString(obj.toString());
        } else if (obj instanceof StringBuffer) {
            packer.packString(obj.toString());
        } else if (obj instanceof StringBuilder) {
            packer.packString(obj.toString());
        } else if (obj instanceof Map) {
            packMap((Map)obj);
        } else if (obj instanceof Set) {
            Set s = (Set)obj;
            packer.packArrayHeader(s.size());
            for (Object o: s) {
                dispatch(o);
            }
        } else if (obj instanceof List) {
            List s = (List)obj;
            packer.packArrayHeader(s.size());
            for (Object o: s) {
                dispatch(o);
            }
        } else if (obj instanceof Vector) {
            Vector s = (Vector)obj;
            packer.packArrayHeader(s.size());
            for (Object o: s) {
                dispatch(o);
            }
        } else if (obj instanceof Dictionary) {
            packDictionary((Dictionary)obj);
        } else if (obj instanceof Queue) {
            Queue s = (Queue)obj;
            packer.packArrayHeader(s.size());
            for (Object o: s) {
                dispatch(o);
            }
        } else if (obj == null) {
            packer.packNil();
        } else if (obj instanceof BigInteger) {
            packer.packBigInteger((BigInteger)obj);
        } else if (obj instanceof String[]) {
            String[] stringArray = (String[])obj;
            packer.packArrayHeader(stringArray.length);
            for (String str: stringArray) {
                packer.packString(str);
            }
        } else if (obj instanceof Integer[]) {
            Integer[] intArray = (Integer[])obj;
            packer.packArrayHeader(intArray.length);
            for (Integer value: intArray) {
                packer.packInt(value);
            }
        } else if (obj instanceof int[]) {
            int[] intArray = (int[])obj;
            packer.packArrayHeader(intArray.length);
            for (int value: intArray) {
                packer.packInt(value);
            }
        } else if (obj instanceof Long[]) {
            Long[] longArray = (Long[])obj;
            packer.packArrayHeader(longArray.length);
            for (Long value: longArray) {
                packer.packLong(value);
            }
        } else if (obj instanceof long[]) {
            long[] longArray = (long[])obj;
            packer.packArrayHeader(longArray.length);
            for (long value: longArray) {
                packer.packLong(value);
            }
        } else if (obj instanceof Short[]) {
            Short[] shortArray = (Short[])obj;
            packer.packArrayHeader(shortArray.length);
            for (Short value: shortArray) {
                packer.packShort(value);
            }
        } else if (obj instanceof short[]) {
            short[] shortArray = (short[])obj;
            packer.packArrayHeader(shortArray.length);
            for (short value: shortArray) {
                packer.packShort(value);
            }
        } else if (obj instanceof Byte[]) {
            Byte[] bytes = (Byte[])obj;
            if (bytes.length > 0)
            {
                byte[] bytes2 = new byte[bytes.length];
                for (int i = 0; i < bytes.length; i++)
                    bytes2[i] = bytes[i];

                packer.packBinaryHeader(bytes2.length);
                packer.writePayload(bytes2);
            }
            else
                packer.packBinaryHeader(0);

        } else if (obj instanceof byte[]) {
            byte[] bytes = (byte[])obj;
            packer.packBinaryHeader(bytes.length);
            packer.writePayload(bytes);
        } else if (obj instanceof Double[]) {
            Double[] doubleArray = (Double[])obj;
            packer.packArrayHeader(doubleArray.length);
            for (Double value: doubleArray) {
                packer.packDouble(value);
            }
        } else if (obj instanceof double[]) {
            double[] doubleArray = (double[])obj;
            packer.packArrayHeader(doubleArray.length);
            for (double value: doubleArray) {
                packer.packDouble(value);
            }
        } else if (obj instanceof Float[]) {
            Float[] floatArray = (Float[])obj;
            packer.packArrayHeader(floatArray.length);
            for (Float value: floatArray) {
                packer.packFloat(value);
            }
        } else if (obj instanceof float[]) {
            float[] floatArray = (float[])obj;
            packer.packArrayHeader(floatArray.length);
            for (float value: floatArray) {
                packer.packFloat(value);
            }
        } else if (obj instanceof Boolean[]) {
            Boolean[] boolArray = (Boolean[])obj;
            packer.packArrayHeader(boolArray.length);
            for (Boolean value: boolArray) {
                packer.packBoolean(value);
            }
        } else if (obj instanceof boolean[]) {
            boolean[] boolArray = (boolean[])obj;
            packer.packArrayHeader(boolArray.length);
            for (boolean value: boolArray) {
                packer.packBoolean(value);
            }
        } else if (obj instanceof BigInteger[]) {
            BigInteger[] bigIntArray = (BigInteger[])obj;
            packer.packArrayHeader(bigIntArray.length);
            for (BigInteger value: bigIntArray) {
                packer.packBigInteger(value);
            }
        } else if (obj instanceof Character[]) {
            Character[] charPackedArray = (Character[])obj;
            if (charPackedArray.length > 0)
            {
                char[] charArray = new char[charPackedArray.length];
                for (int i = 0; i < charPackedArray.length; i++)
                    charArray[i] = charPackedArray[i];

                packer.packString(new String(charArray));
            }
            else
                packer.packString("");

        } else if (obj instanceof char[]) {
            packer.packString(new String((char[])obj));
        } else if (obj instanceof StringBuffer[]) {
            StringBuffer[] sbArray = (StringBuffer[])obj;
            packer.packArrayHeader(sbArray.length);
            for (StringBuffer value: sbArray) {
                packer.packString(value.toString());
            }
        } else if (obj instanceof StringBuilder[]) {
            StringBuilder[] sbArray = (StringBuilder[])obj;
            packer.packArrayHeader(sbArray.length);
            for (StringBuilder value: sbArray) {
                packer.packString(value.toString());
            }
        } else if (obj instanceof Map[]) {
            Map[] mapArray = (Map[])obj;
            packer.packArrayHeader(mapArray.length);
            for (Map value: mapArray) {
                dispatch(value);
            }
        } else if (obj instanceof Set[]) {
            Set[] setArray = (Set[])obj;
            packer.packArrayHeader(setArray.length);
            for (Set value: setArray) {
                dispatch(value);
            }
        } else if (obj instanceof List[]) {
            List[] listArray = (List[])obj;
            packer.packArrayHeader(listArray.length);
            for (List value: listArray) {
                dispatch(value);
            }
        } else if (obj instanceof Vector[]) {
            Vector[] listArray = (Vector[])obj;
            packer.packArrayHeader(listArray.length);
            for (Vector value: listArray) {
                dispatch(value);
            }
        } else if (obj instanceof Dictionary[]) {
            Dictionary[] mapArray = (Dictionary[])obj;
            packer.packArrayHeader(mapArray.length);
            for (Dictionary value: mapArray) {
                dispatch(value);
            }
        } else if (obj instanceof Queue[]) {
            Queue[] listArray = (Queue[])obj;
            packer.packArrayHeader(listArray.length);
            for (Queue value: listArray) {
                dispatch(value);
            }
        } else if (obj instanceof Object[]) {
            Object[] listArray = (Object[])obj;
            packer.packArrayHeader(listArray.length);
            for (Object value: listArray) {
                dispatch(value);
            }
        } else {
            throw new IOException("Unsupported data type.");
        }
    }

    private void packMap(Map payload) throws IOException {
        int size = payload.size();
        packer.packMapHeader(size);

        Iterator<Map.Entry> entries = payload.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = entries.next();
            Object key = entry.getKey();
            Object value = entry.getValue();

            dispatch(key);
            dispatch(value);
        }
    }

    private void packDictionary(Dictionary dict) throws IOException {
        int size = dict.size();
        packer.packMapHeader(size);

        Enumeration keys = dict.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = dict.get(key);

            dispatch(key);
            dispatch(value);
        }
    }

    public void pack(Map payload) throws IOException {
        packMap(payload);
    }

    public byte[] toByteArray() throws IOException {
        packer.close();
        return packer.toByteArray();
    }
}
