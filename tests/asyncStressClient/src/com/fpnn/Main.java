package com.fpnn;

public class Main {

    public static void main(String[] args) {

        String endpoint = "35.167.185.139:13011";
        int clientCount = 100;
        int totalQPS = 10000;

        /*
        System.out.println("Usage:");
        System.out.println("\tasyncStressClient [endpoint] [clientCount] [totalQPS]");
        System.out.println("\tdefault: endpoint: <please change code>, clientCount = 100, totalQPS = 10000.");
        */

        AsyncStressTester tester = new AsyncStressTester(endpoint, clientCount, totalQPS);
        tester.launch();
        try {
            tester.showStatistics();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
