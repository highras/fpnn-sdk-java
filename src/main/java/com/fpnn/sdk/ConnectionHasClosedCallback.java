package com.fpnn.sdk;

import java.net.InetSocketAddress;

public interface ConnectionHasClosedCallback {
    void connectionHasClosed(InetSocketAddress peerAddress, boolean causedByError);
}
