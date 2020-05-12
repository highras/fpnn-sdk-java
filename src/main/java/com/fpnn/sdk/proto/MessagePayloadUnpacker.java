package com.fpnn.sdk.proto;

import org.msgpack.core.ExtensionTypeHeader;
import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ValueType;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by shiwangxing on 2017/12/4.
 */

@SuppressWarnings("unchecked")
public class MessagePayloadUnpacker {

    private MessageUnpacker unpacker;

    public MessagePayloadUnpacker(byte[] contents) {
        unpacker = MessagePack.newDefaultUnpacker(contents);
    }
    public MessagePayloadUnpacker(byte[] contents, int offset, int length) {
        unpacker = MessagePack.newDefaultUnpacker(contents, offset, length);
    }

    private String checkCharset(byte[] bytes, String charsetName) throws CharacterCodingException {
        Charset charset = Charset.forName(charsetName);
        CharsetDecoder decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }

    private Object dispatch() throws IOException {

        if (unpacker.hasNext()) {
            MessageFormat format = unpacker.getNextFormat();
            ValueType type = format.getValueType();
            int length;
            ExtensionTypeHeader extension;
            switch(type) {
                case NIL:
                    unpacker.unpackNil();
                    return null;
                case BOOLEAN:
                    Boolean boolValue = unpacker.unpackBoolean();
                    return boolValue;
                case INTEGER:
                    switch (format) {
                        case UINT64:
                            BigInteger bigIntValue = unpacker.unpackBigInteger();
                            return bigIntValue;
                        case INT64:
                        case UINT32:
                            Long longValue = unpacker.unpackLong();
                            return longValue;
                        default:
                            Integer intValue = unpacker.unpackInt();
                            return intValue;
                    }
                case FLOAT:
                    Double doubleValue = unpacker.unpackDouble();
                    return doubleValue;
                case STRING:
//                    String str = unpacker.unpackString();
//                    return str;
                case BINARY:
                    length = unpacker.unpackBinaryHeader();
                    byte[] binaryValue = new byte[length];
                    unpacker.readPayload(binaryValue);
                    try {
                        return checkCharset(binaryValue,"UTF-8");
                    }
                    catch (CharacterCodingException e) {
                        return binaryValue;
                    }
                case ARRAY:
                    length = unpacker.unpackArrayHeader();
                    List arrayValue = new ArrayList(length);
                    for (int i = 0; i < length; i++) {
                        Object o = dispatch();
                        arrayValue.add(o);
                    }
                    return arrayValue;
                case MAP:
                    Map<Object, Object> mapValue = new TreeMap<>();
                    length = unpacker.unpackMapHeader();
                    for (int i = 0; i < length; i++) {
                        Object key = dispatch();
                        Object value = dispatch();
                        mapValue.put(key, value);
                    }
                    return mapValue;
                case EXTENSION:
                    extension = unpacker.unpackExtensionTypeHeader();
                    byte[] extensionValue = new byte[extension.getLength()];
                    unpacker.readPayload(extensionValue);
                    return extensionValue;
            }
        }

        throw new IOException("No more element, or unsupported format.");
    }

    public Map unpack() throws IOException {

        Object obj = dispatch();
        if (obj instanceof Map) {
            if (unpacker.hasNext()) {
                throw new IOException("Invalid data following payload.");
            }

            return (Map)obj;
        }

        throw new IOException("Invalid payload format.");
    }
}
