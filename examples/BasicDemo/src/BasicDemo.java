import com.fpnn.sdk.*;
import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import java.net.InetSocketAddress;
import com.example.*;

public class BasicDemo {

    public static void main(String[] args) {

        //-- Optional
        ClientEngine.setAutoStop(false);

        TCPClient client = TCPClient.create("52.83.245.22", 9876);

        ConnectionConnectedCallback openCb = (InetSocketAddress peerAddress, boolean connected) -> {
            System.out.println("--- opened ----");
        };

        ConnectionWillCloseCallback closeCb = (InetSocketAddress peerAddress, boolean causedByError) -> {
            System.out.println("Connection closed by error? " + causedByError);
        };

        client.setConnectedCallback(openCb);
        client.setWillCloseCallback(closeCb);

        client.setQuestProcessor(new QuestProcessor(), "com.example.QuestProcessor");

/*        if (!client.enableEncryptorByDerFile("secp256k1",
                "/Users/shiwangxing/Documents/Development/TestingPlayground/keys/test-secp256k1-public.der"))
            System.out.println("Enable encrypt failed.");
*/
        Quest oneWayDemo = new Quest("one way demo", true);
        try{
            System.out.println("send one way: ");
            client.sendQuest(oneWayDemo);
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }

        Quest quest = new Quest("two way demo");
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

        quest = new Quest("httpDemo");
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

        quest = new Quest("duplex demo");
        quest.param("duplex method", "duplexQuest");
        try {
            Answer answer = client.sendQuest(quest);
            System.out.println("Answer: is error answer: " + answer.getErrorCode());
            for (Object obj : answer.getPayload().keySet()) {
                Object value = answer.getPayload().get(obj);
                System.out.println("--- key: " + obj + ", value: " + value);
            }
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //-- Optional
        ErrorRecorder recorder = (ErrorRecorder)ErrorRecorder.getInstance();
        recorder.println();

        //-- Optional: Only when ClientEngine.setAutoStop(true);
        ClientEngine.stop();
    }
}
