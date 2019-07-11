package com.test;

import com.fpnn.ErrorRecorder;
import com.fpnn.FPClient;
import com.fpnn.FPData;
import com.fpnn.callback.CallbackData;
import com.fpnn.callback.FPCallback;
import com.fpnn.encryptor.FPEncryptor;
import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;
import com.fpnn.nio.ThreadPool;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;

public class TestCase {

    private FPClient _client;

    public TestCase(byte[] derKey) {

        ThreadPool.getInstance().startTimerThread();

        _client = new FPClient("52.83.245.22", 13013, true, 5 * 1000);

        final TestCase self = this;

        _client.getEvent().addListener("connect", new FPEvent.IListener() {

            @Override
            public void fpEvent(EventData evd) {

                self.onConnect();
            }
        });

        _client.getEvent().addListener("close", new FPEvent.IListener() {

            @Override
            public void fpEvent(EventData evd) {

                self.onClose(evd.hasRetry());
            }
        });

        _client.getEvent().addListener("error", new FPEvent.IListener() {

            @Override
            public void fpEvent(EventData evd) {

                self.onError(evd.getException());
            }
        });

        if (derKey != null && derKey.length > 0) {

            FPClient.IKeyData keyData = new FPClient.IKeyData() {

                @Override
                public FPData getKeyData(FPEncryptor encryptor) {

                    MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

                    try {

                        packer.packMapHeader(3);
                        packer.packString("publicKey");
                        packer.packBinaryHeader(encryptor.cryptoInfo().selfPublicKey.length);
                        packer.writePayload(encryptor.cryptoInfo().selfPublicKey);
                        packer.packString("streamMode");
                        packer.packBoolean(encryptor.cryptoInfo().streamMode);
                        packer.packString("bits");
                        packer.packInt(encryptor.cryptoInfo().keyLength);
                        packer.close();
                    } catch (IOException ex) {

                        System.err.println(ex);
                    }

                    FPData data = new FPData();
                    data.setPayload(packer.toByteArray());

                    return data;
                }
            };

            if (_client.encryptor("secp256k1", derKey, false, false, true)) {

                System.out.println("try to connect with ecdh!");

                _client.connect(keyData);
                return;
            }
        }

        _client.connect();
    }

    public FPClient getClient() {

        return this._client;
    }

    private void onConnect() {

        System.out.println(new String("Connected!"));

        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

        try {

            packer.packMapHeader(4);

            packer.packString("pid");
            packer.packInt(1017);

            packer.packString("uid");
            packer.packInt(654321);

            packer.packString("what");
            packer.packString("rtmGated");

            packer.packString("addrType");
            packer.packString("ipv4");

            packer.close();
        } catch (IOException ex) {

            System.err.println(ex);
        }

        FPData data = new FPData();
        data.setFlag((byte) 0x1);
        data.setMtype((byte) 0x1);
        data.setMethod("httpDemo");

        data.setPayload(packer.toByteArray());

        _client.sendQuest(data, new FPCallback.ICallback() {
            @Override
            public void callback(CallbackData cbd) {

                FPData data = cbd.getData();

                if (data != null) {

                    System.out.println(data.msgpackPayload());
                } else {

                    System.err.println(cbd.getException());
                }
            }
        });
    }

    private void onClose(boolean hasRetry) {

        System.out.println(new String("Closed! retry? " + hasRetry));
    }

    private void onError(Exception ex) {

        System.err.println(ex);
    }
}
