package com.fpnn;

import com.fpnn.sdk.ClientEngine;
import com.fpnn.sdk.TCPClient;
import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;

public class QuestProcessor {

    TCPClient client;

    public Answer duplexQuest(Quest quest, InetSocketAddress peer) {

        System.out.println("Recv server push from "  + peer.toString());

        ClientEngine.getThreadPool().execute(() -> {

            System.out.println("Answer will send after second.");

            try{
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Will answer server push.");
            Main.client.sendAnswer(new Answer(quest));
        });

        System.out.println("Quest processor return.");
        return null;
    }
}
