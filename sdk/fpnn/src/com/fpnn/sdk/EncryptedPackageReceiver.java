package com.fpnn.sdk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

public class EncryptedPackageReceiver implements PackageReceiverInterface {

    private static int FPNNHeadLength = 12;
    private static int packageModeEncryptedPackageLength = 4;

    private ByteBuffer lengthRecvBuffer;
    private ByteBuffer packageRecvBuffer;
    private int packageLength;
    private int receivedLength;

    private KeyGenerator.EncryptionKit encryptKit;

    public EncryptedPackageReceiver(KeyGenerator.EncryptionKit kit) {
        encryptKit = kit;
        prepareLengthBuffer();
    }

    private void prepareLengthBuffer() {
        lengthRecvBuffer = ByteBuffer.allocate(packageModeEncryptedPackageLength);
        packageRecvBuffer = null;
        packageLength = 0;
        receivedLength = 0;
    }

    private void preparePackageBuffer() {
        lengthRecvBuffer.flip();

        packageLength = (lengthRecvBuffer.get(0) & 0xFF)
                | ((lengthRecvBuffer.get(1) & 0xFF) << 8)
                | ((lengthRecvBuffer.get(2) & 0xFF) << 16)
                | ((lengthRecvBuffer.get(3) & 0xFF) << 24);

        packageRecvBuffer = ByteBuffer.allocate(packageLength);
        receivedLength = 0;
    }

    private boolean processPackageData(PackageReceivedResult result, InetSocketAddress peerAddress) {

        byte[] plaintext;
        packageRecvBuffer.flip();

        try {
            plaintext = encryptKit.decryptor.doFinal(packageRecvBuffer.array());
        } catch (GeneralSecurityException e) {
            ErrorRecorder.record("Decode received package in package mode failed. Connection will be closed. Channel: "
                    + peerAddress.toString(), e);
            result.setError(ErrorCode.FPNN_EC_CORE_DECODING.value());
            return false;
        }

        ByteBuffer headerBuffer = ByteBuffer.allocate(FPNNHeadLength);
        headerBuffer.clear();
        headerBuffer.put(plaintext, 0, FPNNHeadLength);
        headerBuffer.flip();

        ByteBuffer bodyBuffer = ByteBuffer.allocate(plaintext.length - FPNNHeadLength);
        bodyBuffer.clear();
        bodyBuffer.put(plaintext, FPNNHeadLength, plaintext.length - FPNNHeadLength);
        bodyBuffer.flip();

        result.addPackage(headerBuffer, bodyBuffer);

        return true;
    }

    public PackageReceivedResult receive(SocketChannel channel, InetSocketAddress peerAddress) {

        PackageReceivedResult result = new PackageReceivedResult();

        int receivedBytes;
        while (true) {

            try {
                if (packageLength == 0)
                    receivedBytes = channel.read(lengthRecvBuffer);
                else
                    receivedBytes = channel.read(packageRecvBuffer);
            } catch (IOException e) {
                ErrorRecorder.record("Receive data error. Connection will be closed. Channel: " + peerAddress.toString(), e);
                result.setError(ErrorCode.FPNN_EC_CORE_RECV_ERROR.value());
                return result;
            }

            if (receivedBytes > 0) {

                receivedLength += receivedBytes;

                if (packageLength == 0) {
                    if (receivedLength == packageModeEncryptedPackageLength) {
                        preparePackageBuffer();
                    }

                } else {
                    if (receivedLength == packageLength) {
                        if (!processPackageData(result, peerAddress))
                            return result;

                        prepareLengthBuffer();
                    }
                }

            } else {
                if (receivedBytes == -1)
                    result.setError(ErrorCode.FPNN_EC_CORE_CONNECTION_CLOSED.value());

                return result;
            }
        }
    }
}
