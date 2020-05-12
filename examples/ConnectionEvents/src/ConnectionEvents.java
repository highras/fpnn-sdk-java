import com.fpnn.sdk.*;
import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;

public class ConnectionEvents {

    public static void main(String[] args) {

        TCPClient client = TCPClient.create("52.83.245.22", 13609);

        ConnectionConnectedCallback openCb = (InetSocketAddress peerAddress, boolean connected) -> {
            System.out.println("--- opened ----");
        };

        ConnectionWillCloseCallback willCloseCb = (InetSocketAddress peerAddress, boolean causedByError) -> {
            System.out.println("Connection will be closed by error? " + causedByError);
        };

        ConnectionHasClosedCallback closedCb = (InetSocketAddress peerAddress, boolean causedByError) -> {
            System.out.println("Connection has closed by error? " + causedByError);
        };

        client.setConnectedCallback(openCb);
        client.setWillCloseCallback(willCloseCb);
        client.setHasClosedCallback(closedCb);

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
