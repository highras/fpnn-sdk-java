package com.fpnn.sdk;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public interface PackageReceiverInterface {
    PackageReceivedResult receive(SocketChannel channel, InetSocketAddress peerAddress);
}
