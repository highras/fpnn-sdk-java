package com.fpnn;

import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;

public class QuestProcessor {
    public Answer duplexQuest(Quest quest, InetSocketAddress peer) {

        System.out.println("Recv server push from "  + peer.toString());
        Main.client.sendAnswer(new Answer(quest));

        System.out.println("Answer is sent, processor will do other thing, and function will not return.");
        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("processor function will return.");
        return null;
    }
}
