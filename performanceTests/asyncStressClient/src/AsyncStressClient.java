import com.fpnn.sdk.ClientEngine;

public class AsyncStressClient {

    public static void main(String[] args) {

        String endpoint = "52.83.245.22:13609";
        int clientCount = 100;
        int totalQPS = 5000;

        ClientEngine.setQuestTimeout(10);

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