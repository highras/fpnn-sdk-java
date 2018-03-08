package com.fpnn.sdk;

import java.net.InetSocketAddress;

/**
 * Created by shiwangxing on 2017/11/30.
 */

public interface ConnectionConnectedCallback {
    void connectResult(InetSocketAddress peerAddress, boolean connected);
}
