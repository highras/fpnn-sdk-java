package com.fpnn.sdk;

import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Created by shiwangxing on 2017/11/29.
 */

class TCPConnection {

    private volatile boolean connected;   //-- Pls Note: this filed can be visited without synchronized block in same case, but in other case MUST be visited in synchronized block.
    private SocketChannel channel;
    private InetSocketAddress peerAddress;
    private boolean connectionClosed;
    private boolean connectedCallbackCalled;
    private ConnectionConnectedCallback connectedCallback;
    private ConnectionWillCloseCallback connectionWillCloseCallback;
    private ConnectionHasClosedCallback connectionHasClosedCallback;

    //-- Server push / Java Reflect
    private Object questProcessor;
    private String questProcessorName;      //-- Require full name. e.g. full package name + class name.
    private HashMap<String, Method> questProcessorMethodsMap;

    //-- Quest callbacks Maps
    private TreeMap<Long, Set<AnswerCallback>> callbackTimeoutMap;
    private TreeMap<Integer, AnswerCallback> callbackSeqNumMap;

    //-- IO Operations &Operators
    private LinkedList<ByteBuffer> sendQueue;
    private PackageReceiverInterface receiver;
    private int cachedErrorCode;

    private int questTimeout;
    private boolean keyExchanged;
    private ByteBuffer currentSendingBuffer;
    private KeyGenerator.EncryptionKit encryptionKit;

    //-----------------[ Constructor Functions ]-------------------

    public TCPConnection(InetSocketAddress remote) {
        connected = false;
        channel = null;
        peerAddress = remote;

        connectionClosed = false;
        connectedCallbackCalled = false;

        connectedCallback = null;
        connectionWillCloseCallback = null;
        connectionHasClosedCallback = null;

        questProcessor = null;
        questProcessorName = null;
        questProcessorMethodsMap = null;

        callbackTimeoutMap = new TreeMap<>();
        callbackSeqNumMap = new TreeMap<>();
        sendQueue = new LinkedList<>();
        receiver = new PackageReceiver();
        cachedErrorCode = ErrorCode.FPNN_EC_CORE_CONNECTION_CLOSED.value();

        questTimeout = 0;
        keyExchanged = false;
        currentSendingBuffer = null;
        encryptionKit = null;
    }

    //-----------------[ Properties methods ]-------------------

    public SocketChannel getChannel() {
        return channel;
    }

    public void setConnectedCallback(ConnectionConnectedCallback cb) {
        connectedCallback = cb;
    }

    public void setWillCloseCallback(ConnectionWillCloseCallback cb) {
        connectionWillCloseCallback = cb;
    }

    public void setHasClosedCallback(ConnectionHasClosedCallback cb) {
        connectionHasClosedCallback = cb;
    }

    public void setQuestProcessor(Object questProcessor, String questProcessorFullClassName) {
        if (questProcessor == null || questProcessorFullClassName == null || questProcessorFullClassName.length() == 0)
            return;

        this.questProcessor = questProcessor;
        this.questProcessorName = questProcessorFullClassName;

        if (questProcessorMethodsMap == null)
            questProcessorMethodsMap = new HashMap<>();
    }

    public void setQuestTimeout(int timeout) {
        questTimeout = timeout;
    }

    public void setEncryptionKit(KeyGenerator.EncryptionKit kit) {
        encryptionKit = kit;

        if (!kit.streamMode)
            receiver = new EncryptedPackageReceiver(kit);
        else
            throw new RuntimeException("Current FPNN don't support stream encrypt & decrypt in Java.");
    }

    public long getNextTimeoutMillis() {
        synchronized (this) {
            if (callbackTimeoutMap.size() == 0)
                return 0;

            return callbackTimeoutMap.firstKey();
        }
    }

    //-----------------[ Static Run Answer Callback ]-------------------

    static void runCallback(AnswerCallback callback, int errorCode) {
        ClientEngine.getThreadPool().execute(
                new Runnable() {
                    @Override
                    public void run() {
                        callback.fillResult(null, errorCode);
                    }
                });
    }

    private static void runCallback(AnswerCallback callback, Answer answer) {
        ClientEngine.getThreadPool().execute(
                new Runnable() {
                    @Override
                    public void run() {
                        callback.fillResult(answer, answer.getErrorCode());
                    }
                });
    }

    //-----------------[ Process timeout quests methods ]-------------------

    public void checkTimeoutCallbacks() {

        ArrayList<Long> timeoutKeys = new ArrayList<>();
        ArrayList<Set<AnswerCallback>> timeoutAnswerSets = new ArrayList<>();

        long current = System.currentTimeMillis();
        synchronized (this) {
            for (Long timeout : callbackTimeoutMap.keySet()) {
                if (timeout <= current) {
                    timeoutKeys.add(timeout);
                    timeoutAnswerSets.add(callbackTimeoutMap.get(timeout));
                }
            }

            for (Long timeout : timeoutKeys) {
                callbackTimeoutMap.remove(timeout);
            }

            for (Set<AnswerCallback> answerSet: timeoutAnswerSets) {
                for (AnswerCallback callback : answerSet) {
                    callbackSeqNumMap.remove(callback.getSeqNum());
                }
            }
        }

        for (Set<AnswerCallback> answerSet: timeoutAnswerSets) {
            for (AnswerCallback callback : answerSet) {
                runCallback(callback, ErrorCode.FPNN_EC_CORE_TIMEOUT.value());
            }
        }
    }

    //-----------------[ Connection events methods ]-------------------

    public boolean connect() throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(false);

        if (channel.connect(peerAddress)) {
            if (ClientEngine.newChannel(this, SelectionKey.OP_READ)) {
                connectionConnected(true);
                return true;
            }
            else
                return false;
        }
        else {
            return ClientEngine.newChannel(this, SelectionKey.OP_CONNECT);
        }
    }

    public void sendKeyExchangeQuest() {

        Quest quest = new Quest("*key");
        quest.param("publicKey", encryptionKit.selfPublicKey);
        quest.param("streamMode", encryptionKit.streamMode);
        quest.param("bits", encryptionKit.keyLength);

        AnswerCallback callback = new AnswerCallback() {
            @Override
            public void onAnswer(Answer answer) {
                afterKeyExchanged(true);
            }

            @Override
            public void onException(Answer answer, int errorCode) {
                afterKeyExchanged(false);
            }
        };

        sendQuest(quest, callback, questTimeout, true);
    }

    private void afterKeyExchanged(boolean succeed) {

        synchronized (this) {
            if (connectionClosed)
                return;

            connected = succeed;
            connectedCallbackCalled = true;
        }

        if (succeed) {
            if (connectedCallback != null)
                connectedCallback.connectResult(peerAddress,true);

            int interestEvents = SelectionKey.OP_READ;
            synchronized (this) {
                if (sendQueue.size() > 0)
                    interestEvents |= SelectionKey.OP_WRITE;
            }

            ClientEngine.changeChannelInterestedEvent(channel, interestEvents);
        }
        else {
            if (connectedCallback != null)
                connectedCallback.connectResult(peerAddress,false);

            synchronized (this) {
                clearAllCallback(ErrorCode.FPNN_EC_CORE_INVALID_CONNECTION.value());
                sendQueue.clear();
            }
        }
    }

    private void connectionConnected(boolean succeed) {

        if (succeed) {

            if (encryptionKit == null)
                afterKeyExchanged(true);
            else
                sendKeyExchangeQuest();
        }
        else {
            afterKeyExchanged(false);
        }
    }

    public void processConnectedEvent(boolean succeed) {
        final boolean status = succeed;
        ClientEngine.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                connectionConnected(status);
            }
        });
    }

    private void connectionWillClose() {

        boolean callCloseCallback;
        synchronized (this) {
            callCloseCallback = connectedCallbackCalled && connected;
        }
        if (callCloseCallback && connectionWillCloseCallback != null)
            connectionWillCloseCallback.connectionWillClose(peerAddress, false);

        try {
            channel.close();
        } catch (IOException e) {
            ErrorRecorder.record("Close channel exception. Channel: " + peerAddress.toString(), e);
        }

        if (callCloseCallback && connectionHasClosedCallback != null)
            connectionHasClosedCallback.connectionHasClosed(peerAddress, false);
    }

    private void processDisconnectedEvent(int errorCode) {

        synchronized (this) {
            connectionClosed = true;
            clearAllCallback(errorCode);
            sendQueue.clear();
        }

        ClientEngine.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                connectionWillClose();
            }
        });
    }

    //-----------------[ I/O Processing methods ]-------------------

    /* Only be called by sendData(). */
    private boolean prepareSendingBuffer() {
        if (encryptionKit.streamMode) {
            byte[] ciphertext = encryptionKit.encryptor.update(currentSendingBuffer.array());
            if (ciphertext == null) {
                ErrorRecorder.record("Prepare sending buffer in stream mode failed. encryptor.update() return null.");
                return false;
            }

            ByteBuffer buffer = ByteBuffer.allocate(ciphertext.length);
            buffer.clear();

            buffer.put(ciphertext);
            buffer.flip();
            currentSendingBuffer = buffer;
            return true;
        }
        else {
            byte[] ciphertext;
            try {
                ciphertext = encryptionKit.encryptor.doFinal(currentSendingBuffer.array());
                if (ciphertext == null) {
                    ErrorRecorder.record("Prepare sending buffer in package mode failed. encryptor.doFinal() return null.");
                    return false;
                }
            } catch (GeneralSecurityException e) {
                ErrorRecorder.record("Prepare sending buffer in package mode failed.", e);
                return false;
            }

            byte[] packageHeader = new byte[4];

            packageHeader[0] = (byte) (ciphertext.length & 0xFF);
            packageHeader[1] = (byte) ((ciphertext.length >> 8) & 0xFF);
            packageHeader[2] = (byte) ((ciphertext.length >> 16) & 0xFF);
            packageHeader[3] = (byte) ((ciphertext.length >> 24) & 0xFF);

            ByteBuffer buffer = ByteBuffer.allocate(4 + ciphertext.length);
            buffer.clear();

            buffer.put(packageHeader);
            buffer.put(ciphertext);
            buffer.flip();
            currentSendingBuffer = buffer;
            return true;
        }
    }
    /* Only be called by processIOEvent(). */
    private boolean sendData() {

        while (true) {
            if (currentSendingBuffer == null || !currentSendingBuffer.hasRemaining()) {
                synchronized (this) {
                    if (sendQueue.size() == 0) {
                        currentSendingBuffer = null;
                        ClientEngine.changeChannelInterestedEvent(channel, SelectionKey.OP_READ);
                        return true;
                    }

                    currentSendingBuffer = sendQueue.getFirst();
                    sendQueue.remove();
                }

                if (encryptionKit != null)
                    if (!prepareSendingBuffer())
                        return false;
            }

            try {
                channel.write(currentSendingBuffer);
            }
            catch (IOException e) {
                ErrorRecorder.record("Send data error. Connection will be closed. Channel: " + peerAddress.toString(), e);
                cachedErrorCode = ErrorCode.FPNN_EC_CORE_SEND_ERROR.value();
                return false;
            }

            if (currentSendingBuffer.hasRemaining())
                return true;
            else {
                currentSendingBuffer = null;
                if (!keyExchanged) {
                    keyExchanged = true;
                    //-- Stop send until connected event is called.
                    ClientEngine.changeChannelInterestedEvent(channel, SelectionKey.OP_READ);
                    return true;
                }
            }
        }
    }

    /* Only be called by processIOEvent(). */
    private boolean recvData() {
        PackageReceivedResult result = receiver.receive(channel, peerAddress);
        result.processPackage();

        LinkedList<Answer> answerList = result.getAnswerList();

        if (answerList != null) {
            synchronized (this) {
                for (Answer answer: answerList) {
                    int key = answer.getSeqNum();
                    AnswerCallback callback = callbackSeqNumMap.get(key);
                    if (callback != null) {
                        callbackSeqNumMap.remove(key);

                        long callbackTimeoutTime = callback.getTimeoutTime();
                        Set<AnswerCallback> answerSet = callbackTimeoutMap.get(callbackTimeoutTime);
                        if (answerSet != null) {
                            answerSet.remove(callback);
                            if (answerSet.size() == 0)
                                callbackTimeoutMap.remove(callbackTimeoutTime);
                        }

                        runCallback(callback, answer);
                    }
                    else
                        ErrorRecorder.record("Cannot find callback for answer. SeqNum is " + answer.getSeqNum());
                }
            }
        }

        if (result.errorCode != ErrorCode.FPNN_EC_OK.value()) {
            cachedErrorCode = result.errorCode;
            return false;
        }

        if  (result.success) {
            LinkedList<Quest> questList = result.getQuestList();

            if (questList != null) {
                for (Quest quest: questList)
                    processQuest(quest);
            }

            return true;
        }
        else {
            cachedErrorCode = ErrorCode.FPNN_EC_CORE_UNKNOWN_ERROR.value();
            return false;
        }
    }

    //-----------------[ Server push methods ]-------------------

    private Answer buildErrorAnswerAndRecordError(Quest quest, String recordMessage, int errorCode, String ex, Exception e) {
        if (e == null)
            ErrorRecorder.record(recordMessage);
        else
            ErrorRecorder.record(recordMessage, e);

        Answer answer = null;
        if (quest.isTwoWay()) {
            answer = new Answer(quest);
            answer.fillErrorInfo(errorCode, ex);
        }
        return answer;
    }

    private void runQuestProcessor(Method method, Quest quest) {
        ClientEngine.getThreadPool().execute(
                new Runnable() {
                    @Override
                    public void run() {
                        Answer answer;
                        try {
                            answer = (Answer) method.invoke(questProcessor, quest, peerAddress);
                        } catch (ReflectiveOperationException e) {

                            answer = buildErrorAnswerAndRecordError(quest,
                                    "Process quest(method: " + quest.method() + ") exception.",
                                    ErrorCode.FPNN_EC_CORE_UNKNOWN_ERROR.value(), "Quest method exception.", e);
                        }

                        if (answer != null)
                            sendAnswer(answer);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    /* Only be called In NIO Selector thread. */
    private void processQuest(Quest quest) {

        Answer answer = null;
        if (questProcessor != null) {

            Method method = questProcessorMethodsMap.get(quest.method());
            if (method == null) {
                try {
                    Class processorClass = Class.forName(questProcessorName);
                    method = processorClass.getMethod(quest.method(), Quest.class, InetSocketAddress.class);

                    if (method != null)
                        questProcessorMethodsMap.put(quest.method(), method);
                    else
                        throw new NoSuchMethodException();

                } catch (ReflectiveOperationException e) {
                    answer = buildErrorAnswerAndRecordError(quest,
                            "Cannot find method " + quest.method() + " in class " + questProcessorName,
                            ErrorCode.FPNN_EC_CORE_UNKNOWN_METHOD.value(), "Unknown method: " + quest.method(), e);
                }
            }

            if (method != null) {
                runQuestProcessor(method, quest);
                return;
            }
        }
        else {
            answer = buildErrorAnswerAndRecordError(quest,
                    "Client received a quest package but without quest processor. Quest method: " + quest.method(),
                    ErrorCode.FPNN_EC_CORE_INVALID_PACKAGE.value(), "Client without quest processor.", null);
        }

        if (answer != null)
            sendAnswer(answer);
    }

    //-----------------[ I/O Processing methods ]-------------------

    /* Only be called by ClientEngineCore. */
    public boolean processIOEvent(int ops) {

        if ((ops & SelectionKey.OP_WRITE) != 0) {
            if (!sendData())
                return false;
        }
        else if ((ops & SelectionKey.OP_READ) != 0) {
            if (!recvData())
                return false;
        }
        return true;
    }

    //-----------------[ Message Methods ]-------------------

    private void sendQuest(Quest quest, AnswerCallback callback, int timeoutInSeconds, boolean keyExchangedQuest) {

        if (quest == null) {
            if (callback != null)
                runCallback(callback, ErrorCode.FPNN_EC_CORE_INVALID_PACKAGE.value());

            return;
        }

        ByteBuffer buf;
        try {
            buf = quest.rawData();
        } catch (IOException e) {

            ErrorRecorder.record("Encoding quest exception. method: " + quest.method(), e);

            if (callback != null)
                runCallback(callback, ErrorCode.FPNN_EC_CORE_ENCODING.value());

            return;
        }

        if (timeoutInSeconds == 0)
            timeoutInSeconds = ClientEngine.getQuestTimeout();

        if (callback != null) {
            callback.setSeqNum(quest.getSeqNum());
            callback.setTimeout(timeoutInSeconds);
            callback.setSentTime();
        }

        synchronized (this) {

            if (connectionClosed) {
                if (!keyExchangedQuest)
                    ErrorRecorder.record("Call sendQuest() after connection closed.");

                if (callback != null)
                    runCallback(callback, ErrorCode.FPNN_EC_CORE_CONNECTION_CLOSED.value());
                return;
            }

            if (callback != null) {
                long timeout = callback.getTimeoutTime();
                Set<AnswerCallback> answerSet = callbackTimeoutMap.get(timeout);
                if (answerSet == null)
                    answerSet = new HashSet<>();

                answerSet.add(callback);
                callbackTimeoutMap.put(timeout, answerSet);
                callbackSeqNumMap.put(quest.getSeqNum(), callback);
            }

            if (!keyExchangedQuest) {
                sendQueue.add(buf);
            }
        }

        if (!keyExchangedQuest) {
            if (connected) {
                int interestEvents = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
                ClientEngine.changeChannelInterestedEvent(channel, interestEvents);
            }
        }
        else {
            currentSendingBuffer = buf;

            int interestEvents = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
            ClientEngine.changeChannelInterestedEvent(channel, interestEvents);
        }
    }

    public void sendQuest(Quest quest, AnswerCallback callback, int timeoutInSeconds) {
        sendQuest(quest, callback, timeoutInSeconds, false);
    }

    public void sendAnswer(Answer answer) {

        if (!connected || answer == null)
            return;

        ByteBuffer buf;
        try {
            buf = answer.rawData();
        } catch (IOException e) {
            ErrorRecorder.record("Encoding answer exception.", e);
            return;
        }

        synchronized (this) {
            if (connectionClosed) {
                ErrorRecorder.record("Call sendAnswer() after connection closed.");
                return;
            }

            sendQueue.add(buf);
        }

        int interestEvents = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        ClientEngine.changeChannelInterestedEvent(channel, interestEvents);
    }

    //-----------------[ Close & Clear Methods ]-------------------

    //-- MUST call synchronized (this) outside.
    @SuppressWarnings("unchecked")
    private void clearAllCallback(int errorCode) {
        Iterator iterator = callbackTimeoutMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Set<AnswerCallback> answerCallbackSet = (Set<AnswerCallback>) entry.getValue();

            for (AnswerCallback callback : answerCallbackSet) {
                runCallback(callback, errorCode);
            }
        }

        callbackSeqNumMap.clear();
    }
    private void close(int errorCode) {
        processDisconnectedEvent(errorCode);
    }
    void closedByCachedError() {
        close(cachedErrorCode);
    }
    void closeBySelector() {
        close(ErrorCode.FPNN_EC_CORE_CONNECTION_CLOSED.value());
    }

    public void closeByUser() {
        ClientEngine.closeConnection(this);
    }
}
