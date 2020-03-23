
import com.fpnn.sdk.ErrorCode;
import com.fpnn.sdk.TCPClient;
import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import static java.lang.Thread.sleep;

public class EncryptedCommunication {

    public static void main(String[] args) {
        TCPClient client = TCPClient.create("52.83.245.22", 13609);

        if (!client.enableEncryptorByDerFile("secp256k1",
                "/Users/shiwangxing/Documents/Development/TestPlayground/keys/test-secp256k1-public.der")) {
            System.out.println("Enable encrypt failed.");
            return;
        }

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
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
