package com.fpnn;

import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import java.net.InetSocketAddress;

public class QuestProcessor {
    public Answer duplexQuest(Quest quest, InetSocketAddress peer) {

        System.out.println("Recv server push from "  + peer.toString());
        return null;
    }
}
