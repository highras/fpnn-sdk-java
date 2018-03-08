package com.fpnn.sdk;

import com.fpnn.sdk.proto.Answer;
import com.fpnn.sdk.proto.Quest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;

public class TCPClient {

    public enum ClientStatus {
        Closed,
        Connecting,
        Connected
    }

    //-----------------[ Properties ]-------------------

    private InetSocketAddress peerAddress;
    private String endpoint = "";
    private int questTimeout = 0;
    private boolean autoReconnect;
    private volatile ClientStatus status;
    private TCPConnection connection;

    private ConnectionConnectedCallback connectedCallback;
    private ConnectionWillCloseCallback connectionWillCloseCallback;

    //-- Server push / Java Reflect
    private Object questProcessor;
    private String questProcessorName;      //-- Require full name. e.g. full package name + class name.

    private KeyGenerator keyGenerator;

    //-----------------[ Constructor Functions ]-------------------

    public TCPClient(String host, int port) {
        this(host, port, true);
    }

    public TCPClient(String host, int port, boolean autoReconnect) {
        peerAddress = new InetSocketAddress(host, port);
        endpoint = host + ":" + port;
        this.autoReconnect = autoReconnect;
        status = ClientStatus.Closed;
        connection = null;

        connectedCallback = null;
        connectionWillCloseCallback = null;

        questProcessor = null;
        questProcessorName = null;

        keyGenerator = null;
    }

    public static TCPClient create(String host, int port) {
        return TCPClient.create(host, port, true);
    }

    public static TCPClient create(String host, int port, boolean autoReconnect) {
        return new TCPClient(host, port, autoReconnect);
    }

    public static TCPClient create(String endpoint) throws IllegalArgumentException {
        return TCPClient.create(endpoint, true);
    }

    public static TCPClient create(String endpoint, boolean autoReconnect) throws IllegalArgumentException {
        String[] endpointInfo = endpoint.split(":");
        if (endpointInfo.length != 2)
            throw new IllegalArgumentException("Endpoint " + endpoint + " is invalid format.");

        int port = Integer.parseInt(endpointInfo[1]);
        if (port <= 0 || port > 65535)
            throw new IllegalArgumentException("Port in endpoint is invalid.");

        return new TCPClient(endpointInfo[0], port, autoReconnect);
    }

    //-----------------[ Properties methods ]-------------------

    public String endpoint() {
        return endpoint;
    }

    public ClientStatus getClientStatus() {
        return status;
    }

    public boolean connected() {
        return status == ClientStatus.Connected;
    }

    public int questTimeout() {
        return questTimeout;
    }

    public void setQuestTimeout(int timeout) {
        questTimeout = timeout;
    }

    public boolean isAautoReconnect() { return autoReconnect; }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    //-----------------[ Configure methods ]-------------------

    public void setConnectedCallback(ConnectionConnectedCallback cb) {
        connectedCallback = cb;
    }

    public void setWillCloseCallback(ConnectionWillCloseCallback cb) {
        connectionWillCloseCallback = cb;
    }

    public void setQuestProcessor(Object questProcessor, String questProcessorFullClassName) {
        if (questProcessor == null || questProcessorFullClassName == null || questProcessorFullClassName.length() == 0)
            return;

        this.questProcessor = questProcessor;
        this.questProcessorName = questProcessorFullClassName;
    }

    private boolean enableEncryptorByDerFile(String curve, String keyFilePath, boolean streamMode, boolean reinforce) {
        try {
            keyGenerator = KeyGenerator.create(curve, keyFilePath, streamMode, reinforce);
            return true;
        } catch (Exception e) {
            keyGenerator = null;
            ErrorRecorder.record("Enable encrypt with curve " + curve + " and key in " + keyFilePath + " failed.", e);
            return false;
        }
    }

    private boolean enableEncryptorByDerData(String curve, byte[] peerPublicKey, boolean streamMode, boolean reinforce) {
        try {
            keyGenerator = new KeyGenerator(curve, peerPublicKey, streamMode, reinforce);
            return true;
        } catch (Exception e) {
            ErrorRecorder.record("Enable encrypt with curve " + curve + " and raw key data failed.", e);
            return false;
        }
    }

    public boolean enableEncryptorByDerFile(String curve, String keyFilePath) {
        return enableEncryptorByDerFile(curve, keyFilePath, false, false);
    }

    public boolean enableEncryptorByDerData(String curve, byte[] peerPublicKey) {
        return enableEncryptorByDerData(curve, peerPublicKey, false, false);
    }

    //-----------------[ Message Methods ]-------------------
    public Answer sendQuest(Quest quest) throws InterruptedException {
        return sendQuest(quest, questTimeout);
    }

    public Answer sendQuest(Quest quest, int timeoutInSeconds) throws InterruptedException {
        SyncAnswerCallback callback = new SyncAnswerCallback();
        sendQuest(quest, callback, timeoutInSeconds);
        return callback.getAnswer();
    }

    public void sendQuest(Quest quest, AnswerCallback callback) {
        sendQuest(quest, callback, questTimeout);
    }

    public void sendQuest(Quest quest, AnswerCallback callback, int timeoutInSeconds) {
        TCPConnection conn = null;
        boolean needConnect = false;

        synchronized (this) {
            if (status == ClientStatus.Closed) {
                if (!autoReconnect) {
                    TCPConnection.runCallback(callback, ErrorCode.FPNN_EC_CORE_INVALID_CONNECTION.value());
                    return;
                }
                else
                    needConnect = true;
            }
            else
                conn = connection;
        }

        if (needConnect) {
            try {
                needConnect = !connect(false);
            } catch (InterruptedException e) {
                ErrorRecorder.record("Reconnect for send quest action failed. Peer: " + peerAddress.toString(), e);
            }
        }

        if (conn == null && !needConnect) {
            synchronized (this) {
                if (status != ClientStatus.Closed)
                    conn = connection;
            }
        }

        if (conn != null)
            conn.sendQuest(quest, callback, (timeoutInSeconds != 0) ? timeoutInSeconds : questTimeout);
        else
            TCPConnection.runCallback(callback, ErrorCode.FPNN_EC_CORE_INVALID_CONNECTION.value());
    }

    public void sendQuest(Quest quest, FunctionalAnswerCallback callback) {
        sendQuest(quest, callback, questTimeout);
    }


    public void sendQuest(Quest quest, FunctionalAnswerCallback callback, int timeoutInSeconds) {
        AnswerCallback standardCallback = new AnswerCallback() {
            @Override
            public void onAnswer(Answer answer) {
            }

            @Override
            public void onException(Answer answer, int errorCode) {
            }
        };

        standardCallback.setFuncationalAnswerCallback(callback);
        sendQuest(quest, standardCallback, timeoutInSeconds);
    }

    //-- Async & Advanced Answer.
    public void sendAnswer(Answer answer) {
        synchronized (this) {
            if (connection != null)
                connection.sendAnswer(answer);
        }
    }

    //-----------------[ Internal Callbacks ]-------------------

    class ClientConnectedCallback implements ConnectionConnectedCallback {

        private ConnectionConnectedCallback userCallback;
        private TCPClient client;
        private int hashCode;

        ClientConnectedCallback(TCPClient client, int hashCode, ConnectionConnectedCallback callback) {
            this.client = client;
            userCallback = callback;
            this.hashCode = hashCode;
        }

        public void connectResult(InetSocketAddress peerAddress, boolean connected) {

            if (userCallback != null) {
                try {
                    userCallback.connectResult(peerAddress, connected);
                } catch (Exception e) {
                    ErrorRecorder.record("Connection connected callback exception.", e);
                }
            }

            client.connectionConnectResult(connected, hashCode);
        }
    }

    class ClientConnectionClosedCallback implements ConnectionWillCloseCallback {

        private ConnectionWillCloseCallback userCallback;
        private TCPClient client;
        private int hashCode;

        ClientConnectionClosedCallback(TCPClient client,int hashCode,  ConnectionWillCloseCallback callback) {
            this.client = client;
            userCallback = callback;
            this.hashCode = hashCode;
        }

        public void  connectionWillClose(InetSocketAddress peerAddress, boolean causedByError) {

            if (userCallback != null) {
                try {
                    userCallback.connectionWillClose(peerAddress, causedByError);
                } catch (Exception e) {
                    ErrorRecorder.record("Connection will close callback exception.", e);
                }
            }

            client.connectionDisconnected(hashCode);
        }
    }

    //-----------------[ Optional Methods ]-------------------

    private void connectionConnectResult(boolean connected, int hashCode) {
        synchronized (this) {
            if (connected) {
                if (connection == null || hashCode != connection.hashCode())
                    return;

                status = ClientStatus.Connected;
            }
            else {
                connection = null;
                status = ClientStatus.Closed;
            }

            this.notifyAll();
        }
    }

    private void connectionDisconnected(int hashCode) {
        synchronized (this) {
            if (connection == null || hashCode != connection.hashCode())
                return;

            status = ClientStatus.Closed;
            this.notifyAll();
        }
    }

    public boolean connect(boolean synchronous) throws InterruptedException {

        ClientEngine.startEngine();

        synchronized (this) {
            if (status == ClientStatus.Connected)
                return true;

            KeyGenerator.EncryptionKit encKit = null;
            if (keyGenerator != null) {
                try {
                    encKit = keyGenerator.gen();
                } catch (GeneralSecurityException e) {
                    ErrorRecorder.record("Init encryption modules failed.", e);
                    return false;
                }
            }

            if (status == ClientStatus.Closed) {
                connection = new TCPConnection(peerAddress);

                ClientConnectedCallback openCb = new ClientConnectedCallback(this, connection.hashCode(), connectedCallback);
                ClientConnectionClosedCallback closeCb = new ClientConnectionClosedCallback(this, connection.hashCode(), connectionWillCloseCallback);

                if (encKit != null)
                    connection.setEncryptionKit(encKit);

                connection.setQuestTimeout(questTimeout);
                connection.setConnectedCallback(openCb);
                connection.setWillCloseCallback(closeCb);
                connection.setQuestProcessor(questProcessor, questProcessorName);

                boolean connStatus;
                try {
                    connStatus = connection.connect();
                } catch (IOException e) {
                    connStatus = false;
                    ErrorRecorder.record("Connection open channel failed. Peer: " + peerAddress.toString(), e);
                }

                if (connStatus) {
                    status = ClientStatus.Connecting;
                } else {
                    connection = null;
                    status = ClientStatus.Closed;
                    return false;
                }
            }

            if (!synchronous)
                return true;

            while (status == ClientStatus.Connecting)
                this.wait();

            return status == ClientStatus.Connected;
        }
    }

    public boolean reconnect(boolean synchronous) throws InterruptedException {
        close();
        return connect(synchronous);
    }

    public void close() {
        synchronized (this) {
            if (status == ClientStatus.Closed)
                return;

            else {
                connection.closeByUser();
                connection = null;
                status = ClientStatus.Closed;

                this.notifyAll();
            }
        }
    }
}
