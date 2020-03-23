# FPNN Java SDK

[TOC]

## Depends

* [msgpack-java](https://github.com/msgpack/msgpack-java)

### Language Level:

Java 8

## Usage

### Import package

	import com.fpnn.sdk.*;
	import com.fpnn.sdk.proto.Answer;
	import com.fpnn.sdk.proto.Quest;


### Create TCPClient

	TCPClient client = new TCPClient(String host, int port);
	TCPClient client = new TCPClient(String host, int port, boolean autoConnect);

### Configure (Optional)

#### Set Duplex Mode (Server Push) (Optional)

	client.setQuestProcessor(Object questProcessor, String questProcessorFullClassName);

#### Set connection events' callbacks (Optional)

	client.setConnectedCallback(ConnectionConnectedCallback cb);
	client.setWillCloseCallback(ConnectionWillCloseCallback cb);

	public interface ConnectionConnectedCallback {
	    void connectResult(InetSocketAddress peerAddress, boolean connected);
	}

	public interface ConnectionWillCloseCallback {
	    void connectionWillClose(InetSocketAddress peerAddress, boolean causedByError);
	}

#### Set Encryption (Optional)

	public boolean client.enableEncryptorByDerFile(String curve, String keyFilePath);
	public boolean client.enableEncryptorByDerData(String curve, byte[] peerPublicKey);

### Send Quest

	//-- Sync method
	public Answer client.sendQuest(Quest quest) throws InterruptedException;
	public Answer client.sendQuest(Quest quest, int timeoutInSeconds) throws InterruptedException;

	//-- Async methods
	public void client.sendQuest(Quest quest, AnswerCallback callback);
	public void client.sendQuest(Quest quest, AnswerCallback callback, int timeoutInSeconds);
	public void client.sendQuest(Quest quest, FunctionalAnswerCallback callback);
	public void client.sendQuest(Quest quest, FunctionalAnswerCallback callback, int timeoutInSeconds);

	public interface FunctionalAnswerCallback {
	    void onAnswer(Answer answer, int errorCode);
	}


### Close (Optional)

	client.close();


### SDK Version

	System.out.println(ClientEngine.SDKVersion);

## API docs

Please refer: [API docs](API.md)


## Directory structure

* **\<fpnn-sdk-java\>/sdk**

	Codes of SDK.

* **\<fpnn-sdk-java\>/examples**

	Examples codes for using this SDK.  
	Testing server is \<fpnn\>/core/test/serverTest. Refer: [Cpp codes of serverTest](https://github.com/highras/fpnn/blob/master/core/test/serverTest.cpp)

* **\<fpnn-sdk-java\>/tests**

	+ **\<fpnn-sdk-java\>/tests/asyncStressClient**

		Stress & Concurrent testing codes for SDK.  
		Testing server is <fpnn>/core/test/serverTest. Refer: [Cpp codes of serverTest](https://github.com/highras/fpnn/blob/master/core/test/serverTest.cpp)

	+ **\<fpnn-sdk-java\>/tests/singleClientConcurrentTest**

		Stability testing codes for SDK.  
		Testing server is <fpnn>/core/test/serverTest. Refer: [Cpp codes of serverTest](https://github.com/highras/fpnn/blob/master/core/test/serverTest.cpp)
