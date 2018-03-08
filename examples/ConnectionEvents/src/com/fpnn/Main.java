package com.fpnn;

import com.fpnn.sdk.ConnectionConnectedCallback;
import com.fpnn.sdk.ConnectionWillCloseCallback;
import com.fpnn.sdk.ErrorCode;
import com.fpnn.sdk.TCPClient;
import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;

public class Main {

    public static void main(String[] args) {

        TCPClient client = TCPClient.create("35.167.185.139", 13011);

        ConnectionConnectedCallback openCb = (InetSocketAddress peerAddress, boolean connected) -> {
            System.out.println("--- opened ----");
        };

        ConnectionWillCloseCallback closeCb = (InetSocketAddress peerAddress, boolean causedByError) -> {
            System.out.println("Connection closed by error? " + causedByError);
        };

        client.setConnectedCallback(openCb);
        client.setWillCloseCallback(closeCb);

        //-- Sync method
        Quest quest = new Quest("two way demo");
        try {
            Answer answer = client.sendQuest(quest);
            System.out.println("Answer is error: " + answer.isErrorAnswer());
            for (Object obj : answer.getPayload().keySet()) {
                Object value = answer.getPayload().get(obj);
                System.out.println("--- key: " + obj + ", value: " + value);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //-- Async method
        quest = new Quest("httpDemo");
        client.sendQuest(quest, (Answer answer, int errorCode) -> {
            System.out.println("Answer is received.");

            if (errorCode == ErrorCode.FPNN_EC_OK.value()) {
                for (Object obj : answer.getPayload().keySet()) {
                    Object value = answer.getPayload().get(obj);
                    System.out.println("--- key: " + obj + ", value: " + value);
                }
            }
            else {
                System.out.println("Answer is error. Error code is " + errorCode);
            }
        });

        //-- Wait for async answer is received.
        try {
            sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        client.close();
        //-- Wait for close event is processed.
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
