package com.fpnn.sdk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by shiwangxing on 2017/12/5.
 */

public class PackageReceiver implements PackageReceiverInterface {

    private static int FPNNHeadLength = 12;

    private ByteBuffer headerRecvBuffer;
    private ByteBuffer bodyRecvBuffer;
    private boolean recvHeader;
    private int bodyLength;
    private int receivedLength;

    public PackageReceiver () {
        prepareHeadBuffer();
    }

    private void prepareHeadBuffer() {
        headerRecvBuffer = ByteBuffer.allocate(FPNNHeadLength);
        bodyRecvBuffer = null;
        recvHeader = true;
        bodyLength = 0;
        receivedLength = 0;
    }

    private boolean prepareBodyBuffer(PackageReceivedResult result, InetSocketAddress peerAddress) {
        recvHeader = false;
        headerRecvBuffer.flip();

        if ((headerRecvBuffer.get(0) != 0x46)
                || (headerRecvBuffer.get(1) != 0x50)
                || (headerRecvBuffer.get(2) != 0x4e)
                || (headerRecvBuffer.get(3) != 0x4e)) {

            result.setError(ErrorCode.FPNN_EC_PROTO_INVALID_PACKAGE.value());
            ErrorRecorder.record("Received data magic code mismatched. Connection will be closed. Channel: " + peerAddress.toString());
            return false;
        }

        if ((headerRecvBuffer.get(5) & 0x80) == 0) {
            result.setError(ErrorCode.FPNN_EC_PROTO_PROTO_TYPE.value());
            ErrorRecorder.record("Received data is not encoding by msgpack. Connection will be closed. Channel: " + peerAddress.toString());
            return false;
        }

        bodyLength = (headerRecvBuffer.get(8) & 0xFF)
                | ((headerRecvBuffer.get(9) & 0xFF) << 8)
                | ((headerRecvBuffer.get(10) & 0xFF) << 16)
                | ((headerRecvBuffer.get(11) & 0xFF) << 24);

        if (bodyLength < 1 || bodyLength > ClientEngine.getMaxPackageLength()) {
            result.setError(ErrorCode.FPNN_EC_PROTO_INVALID_PACKAGE.value());
            ErrorRecorder.record("Received invalid package. package payload length: "
                    + bodyLength + ". Connection will be closed. Channel: " + peerAddress.toString());
            return false;
        }

        int mtype = headerRecvBuffer.get(6);
        //if (mtype == 1 || mtype == 2)
        //    bodyLength += 4;

        if (mtype == 2)
            bodyLength += 4;
        else if (mtype == 1) {
            bodyLength += 4 + headerRecvBuffer.get(7);
        }
        else if (mtype == 0)
            bodyLength += headerRecvBuffer.get(7);

        bodyRecvBuffer = ByteBuffer.allocate(bodyLength);
        receivedLength = 0;

        return true;
    }

    public PackageReceivedResult receive(SocketChannel channel, InetSocketAddress peerAddress) {

        PackageReceivedResult result = new PackageReceivedResult();

        int receivedBytes;
        while (true) {

            try {
                if (recvHeader)
                    receivedBytes = channel.read(headerRecvBuffer);
                else
                    receivedBytes = channel.read(bodyRecvBuffer);
            } catch (IOException e) {
                ErrorRecorder.record("Receive data error. Connection will be closed. Channel: " + peerAddress.toString(), e);
                result.setError(ErrorCode.FPNN_EC_CORE_RECV_ERROR.value());
                return result;
            }

            if (receivedBytes > 0) {

                receivedLength += receivedBytes;

                if (recvHeader) {
                    if (receivedLength == FPNNHeadLength) {
                        if (!prepareBodyBuffer(result, peerAddress))
                            return result;
                    }

                } else {
                    if (receivedLength == bodyLength) {
                        bodyRecvBuffer.flip();
                        result.addPackage(headerRecvBuffer, bodyRecvBuffer);
                        prepareHeadBuffer();
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
