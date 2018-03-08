package com.fpnn;

import com.fpnn.sdk.ErrorCode;
import com.fpnn.sdk.TCPClient;
import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import java.util.LinkedList;
import java.util.TreeMap;

public class TestHolder {

    static void showSignDesc()
    {
        System.out.println("Sign:");
        System.out.println("    +: establish connection");
        System.out.println("    ~: close connection");
        System.out.println("    #: connection error");

        System.out.println("    *: send sync quest");
        System.out.println("    &: send async quest");

        System.out.println("    ^: sync answer Ok");
        System.out.println("    ?: sync answer exception");
        System.out.println("    |: sync answer exception by connection closed");
        System.out.println("    (: sync operation fpnn exception");
        System.out.println("    ): sync operation unknown exception");

        System.out.println("    $: async answer Ok");
        System.out.println("    @: async answer exception");
        System.out.println("    ;: async answer exception by connection closed");
        System.out.println("    {: async operation fpnn exception");
        System.out.println("    }: async operation unknown exception");

        System.out.println("    !: close operation");
        System.out.println("    [: close operation fpnn exception");
        System.out.println("    ]: close operation unknown exception");
    }

    private LinkedList<Tester> threads;

    TestHolder(TCPClient client, int threadCount, int perThreadQuestCount) {
        threads = new LinkedList<>();

        for (int i = 0; i < threadCount; i++) {
            Tester test = new Tester(client, perThreadQuestCount);
            threads.add(test);
        }
    }

    void launch() {
        for (Tester test : threads) {
            test.start();
        }
    }

    void stop() throws InterruptedException {
        for (Tester test : threads) {
            test.join();
        }
    }

    private class Tester extends Thread {

        private TCPClient client;
        private int questCount;

        Tester(TCPClient client, int questCount) {
            this.client = client;
            this.questCount = questCount;
        }

        private Quest buildStandardTestQuest() {
            LinkedList<Object> array = new LinkedList<>();
            array.add("first_vec");
            array.add(4);

            TreeMap<String, Object> map = new TreeMap<>();
            map.put("map1","first_map");
            map.put("map2",true);
            map.put("map3",5);
            map.put("map4",5.7);
            map.put("map5","中文");

            Quest quest = new Quest("two way demo");
            quest.param("quest", "one");
            quest.param("int", 2);
            quest.param("double", 3.3);
            quest.param("boolean", true);
            quest.param("ARRAY", array);
            quest.param("MAP", map);

            return quest;
        }

        @Override
        public void run() {
            int act = 0;
            int k = 0;
            for (int i = 0; i < questCount; i++, k++)
            {
                if (k == 20) {
                    System.out.println();
                    k = 0;
                }
                long index = (System.currentTimeMillis() + i) % 64;
                if (i >= 10)
                {
                    if (index < 6)
                        act = 2;	//-- close operation
                    else if (index < 32)
                        act = 1;	//-- async quest
                    else
                        act = 0;	//-- sync quest
                }
                else
                    act = (int)index & 0x1;

                try
                {
                    switch (act)
                    {
                        case 0:
                        {
                            System.out.print('*');
                            Answer answer = client.sendQuest(buildStandardTestQuest());
                            if (answer != null)
                            {
                                if (!answer.isErrorAnswer())
                                    System.out.print('^');
                                else
                                {
                                    if (answer.getErrorCode() == ErrorCode.FPNN_EC_CORE_CONNECTION_CLOSED.value()
                                            || answer.getErrorCode() == ErrorCode.FPNN_EC_CORE_INVALID_CONNECTION.value())
                                        System.out.print('|');
                                    else
                                        System.out.print('?');
                                }
                            }
                            else
                                System.out.print('?');

                            break;
                        }
                        case 1:
                        {
                            System.out.print('&');
                            client.sendQuest(buildStandardTestQuest(), (Answer answer, int errorCode) -> {
                                if (errorCode == ErrorCode.FPNN_EC_OK.value())
                                    System.out.print('$');
                                else if (errorCode == ErrorCode.FPNN_EC_CORE_CONNECTION_CLOSED.value()
                                        || errorCode == ErrorCode.FPNN_EC_CORE_INVALID_CONNECTION.value())
                                    System.out.print(';');
                                else
                                    System.out.print('@');
                            });

                            break;
                        }
                        case 2:
                        {
                            System.out.print('!');
                            client.close();
                            break;
                        }
                    }
                }
                catch (InterruptedException e){
                    switch (act)
                    {
                        case 0: System.out.print('('); break;
                        case 1: System.out.print('{'); break;
                        case 2: System.out.print('['); break;
                    }
                }
                catch (Exception e) {
                    switch (act)
                    {
                        case 0: System.out.print(')'); break;
                        case 1: System.out.print('}'); break;
                        case 2: System.out.print(']'); break;
                    }
                }
            }
        }
    }
}
