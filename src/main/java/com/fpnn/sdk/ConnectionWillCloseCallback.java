package com.fpnn.sdk;

import java.net.InetSocketAddress;

/**
 * Created by shiwangxing on 2017/11/30.
 */

public interface ConnectionWillCloseCallback {
    void connectionWillClose(InetSocketAddress peerAddress, boolean causedByError);
}
