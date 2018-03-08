package com.fpnn;

import com.fpnn.sdk.TCPClient;
import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

public class Main {

    public static void main(String[] args) {

        TCPClient client = TCPClient.create("35.167.185.139", 13011);

        client.setQuestProcessor(new QuestProcessor(), "com.fpnn.QuestProcessor");

        Quest quest = new Quest("duplex demo");
        quest.param("duplex method", "duplexQuest");
        try {
            Answer answer = client.sendQuest(quest);
            System.out.println("Answer: is error answer: " + answer.getErrorCode());
            for (Object obj : answer.getPayload().keySet()) {
                Object value = answer.getPayload().get(obj);
                System.out.println("--- key: " + obj + ", value: " + value);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
