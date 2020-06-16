# FPNN Java SDK

[TOC]

## Depends

* [msgpack-java](https://github.com/msgpack/msgpack-java)

### Language Level:

Java 8

## Usage

### For Maven Users:

	<dependency>
	  <groupId>com.github.highras</groupId>
	  <artifactId>fpnn</artifactId>
	  <version>2.0.3-RELEASE</version>
	</dependency>

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

* **\<fpnn-sdk-java\>/src**

	Maven Project.

	+ **\<fpnn-sdk-java\>/src/main/java**

		Codes of SDK.

* **\<fpnn-sdk-java\>/examples**

	Example modules for using this SDK.  
	All modules are normal module with IDEA, **NOT MAVEN PROJECT** and **NOT MAVEN MODULE**.  
	Testing server is \<fpnn\>/core/test/serverTest. Refer: [Cpp codes of serverTest](https://github.com/highras/fpnn/blob/master/core/test/serverTest.cpp)

* **\<fpnn-sdk-java\>/performanceTests**

	+ **\<fpnn-sdk-java\>/performanceTests/asyncStressClient**

		Stress & Concurrent testing codes for SDK.  
		This is normal module with IDEA, **NOT MAVEN PROJECT** and **NOT MAVEN MODULE**.  
		Testing server is <fpnn>/core/test/serverTest. Refer: [Cpp codes of serverTest](https://github.com/highras/fpnn/blob/master/core/test/serverTest.cpp)

	+ **\<fpnn-sdk-java\>/performanceTests/singleClientConcurrentTest**

		Stability testing codes for SDK.  
		This is normal module with IDEA, **NOT MAVEN PROJECT** and **NOT MAVEN MODULE**.  
		Testing server is <fpnn>/core/test/serverTest. Refer: [Cpp codes of serverTest](https://github.com/highras/fpnn/blob/master/core/test/serverTest.cpp)
