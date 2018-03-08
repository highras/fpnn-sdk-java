package com.fpnn;

import com.fpnn.sdk.TCPClient;

import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;

public class Main {

    private static long connectSuccess = 0;
    private static long connectFailed = 0;
    private static long connectionClosed = 0;

    private static void incConnectSuccess() {
        synchronized (Main.class) {
            connectSuccess += 1;
        }
    }

    private static void incConnectFailed() {
        synchronized (Main.class) {
            connectFailed += 1;
        }
    }

    private static void incConnectionClosed() {
        synchronized (Main.class) {
            connectionClosed += 1;
        }
    }

    private static void showStatic() {
        System.out.println("connectSuccess: " + connectSuccess);
        System.out.println("connectFailed: " + connectFailed);
        System.out.println("connectionClosed: " + connectionClosed);
    }

    public static void main(String[] args) {

        //String endpoint = "35.167.185.139:13011";
        String endpoint ="localhost:13011";


        TCPClient client = TCPClient.create(endpoint);
        client.setConnectedCallback((InetSocketAddress peerAddress, boolean connected) -> {
            if (connected)
                incConnectSuccess();
            else
                incConnectFailed();

            System.out.print(connected ? '+' : '#');
        });

        client.setWillCloseCallback((InetSocketAddress peerAddress, boolean causedByError) -> {
            incConnectionClosed();
            if (causedByError)
                System.out.print('#');
            else
                System.out.print('~');
        });

        TestHolder.showSignDesc();

        try {
            final int questCount = 30000;
            for (int i = 10; i <= 60; i += 10) {
                System.out.println("\n\n-- Test case begin: " + i + " threads, " + questCount + " quest per thread.");
                TestHolder holder = new TestHolder(client, i, questCount);
                holder.launch();
                holder.stop();
                System.out.println("\n\n-- Test case end: " + i + " threads, " + questCount + " quest per thread.");
                showStatic();
            }

        } catch (InterruptedException e) {
            e. printStackTrace();
        }

        System.out.println("=============== down ====================");

        int i = 1;
        while (connectSuccess != connectionClosed) {
            try {
                sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("=============== wait " + (i * 5) + " seconds ====================");
            showStatic();
        }

        //ClientEngine.stop();

        //System.out.println("\n\n-----------------[ Error Info ]------------------------");
        //ErrorRecorder recorder = (ErrorRecorder)ErrorRecorder.getInstance();
        //recorder.println();

        showStatic();
    }
}
