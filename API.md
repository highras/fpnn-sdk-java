# FPNN Java SDK API Docs

# Index

[TOC]

## Current Version

	public static String ClientEngine.SDKVersion = "2.0.1";

## Global Config & ClientEngine (Optional)

### Auto Cleanup

	public static void ClientEngine.setAutoStop(boolean auto);
	public static boolean ClientEngine.isAutoStop();

Config the SDK can be cleaned up automatically.

`true` is the default action.

If the automatic action is disabled, **`ClientEngine.stop();`** MUST be called for cleaning up.

### Quest Timeout

	public static void ClientEngine.setQuestTimeout(int timeout);
	public static int ClientEngine.getQuestTimeout();

Config the default timeout in seconds for global quest. Default is 5 seconds.

### Max Limitation for Thread Pool

	public static void ClientEngine.setMaxThreadInTaskPool(int count);
	public static int ClientEngine.getMaxThreadInTaskPool();

Default max limitation is 4 threads.

### Max Package Length

	public static void ClientEngine.setMaxPackageLength(int length);
	public static int ClientEngine.getMaxPackageLength();

The max length for FPNN Quest & FPNN Answer packages.

Default is 10MB.

### Start & Stop

	public static void ClientEngine.startEngine();
	public static boolean ClientEngine.started();
	public static void ClientEngine.stop();

Start & stop ClientEngine.

Explicitly call `ClientEngine.startEngine()` is not necessary, because it will be called automatically when client connecting.

If  `ClientEngine.setAutoStop` set to `false`, MUST call `ClientEngine.stop()`  explicitly. In other cases, calling is optional.


## TCPClient

### Constructors & Create Functions

	TCPClient client = new TCPClient(String host, int port);
	TCPClient client = new TCPClient(String host, int port, boolean autoConnect);

	TCPClient client = TCPClient.create(String host, int port);
	TCPClient client = TCPClient.create(String host, int port, boolean autoConnect);

	TCPClient client = TCPClient.create(String endpoint);
	TCPClient client = TCPClient.create(String endpoint, boolean autoConnect);

**endpoint** format: `"hostname/ip" + ":" + "port"`.  
e.g. `"localhost:8000"`

**Note**:  
`autoConnect` is **NOT** `keep alive`, just calling `connect()` method automatically when sending quest without established connection.

### Config & Properties Methods

#### Endpoint

	public String endpoint();

Return current endpoint of client.

#### Client Status

	public ClientStatus getClientStatus();

Return client current status.

Values:

+ TCPClient.ClientStatus.Closed
+ TCPClient.ClientStatus.Connecting
+ TCPClient.ClientStatus.Connected

#### Connected Check

	public boolean connected();

Return client current is connected or not.

#### Quest Timeout

	public int questTimeout();
	public void setQuestTimeout(int timeout);

Get/set the quest timeout in seconds for this client instance.

Default `0`, means using global settings.

#### Auto Connect

	public boolean isAutoConnect();
	public void setAutoConnect(boolean autoConnect);

Get/set the auto connecting behavior.

**Note**:  
**Auto Connect** is **NOT** **keep alive**, just calling `connect()` method automatically when sending quest without established connection.

#### Encryption

	public boolean enableEncryptorByDerFile(String curve, String keyFilePath);
	public boolean enableEncryptorByDerData(String curve, byte[] peerPublicKey);

Enable encrypted connection.

**curve** is the curve name of the der key for ECC encryption.  
Curve `secp256k1` is recommended.

### Connect & Close Methods

#### Connect

	public boolean connect(boolean synchronous) throws InterruptedException;
	public boolean reconnect(boolean synchronous) throws InterruptedException;

**synchronous**: connect/reconnect is in synchronous mode or asynchronous mode.

#### Close

	public void close();

Close current connection. Calling without available connection or client connecting are safe.

### Event Methods

#### Connected Event
	
	public void setConnectedCallback(ConnectionConnectedCallback cb);

Set the connected evnet. Prototype:

	public interface ConnectionConnectedCallback {
	    void connectResult(InetSocketAddress peerAddress, boolean connected);
	}

#### Close Event

	public void setWillCloseCallback(ConnectionWillCloseCallback cb);
	public void setHasClosedCallback(ConnectionHasClosedCallback cb);

Set the will close evnet & has closed event. Prototype:

+ Will closing callback:

		public interface ConnectionWillCloseCallback {
		    void connectionWillClose(InetSocketAddress peerAddress, boolean causedByError);
		}

+ Has closed callback:

		public interface ConnectionHasClosedCallback {
		    void connectionHasClosed(InetSocketAddress peerAddress, boolean causedByError);
		}

#### Server Push

	public void setQuestProcessor(Object questProcessor, String questProcessorFullClassName);

+ `questProcessor`:

	The server pushed quest processor.

	Each quest processing method has the same declaration:

		public Answer questMethodName(Quest quest, InetSocketAddress peer);

	`questMethodName` is the real interface name, same with the `method` in quest.


+ `questProcessorFullClassName`:

	The full class name of `questProcessor`, which includes the package name.

### Send Quest & Answer Methods

#### Send Quest

	//-- synchronous
	public Answer sendQuest(Quest quest) throws InterruptedException;
	public Answer sendQuest(Quest quest, int timeoutInSeconds) throws InterruptedException;

	//-- asynchronus
	public void sendQuest(Quest quest, AnswerCallback callback);
	public void sendQuest(Quest quest, AnswerCallback callback, int timeoutInSeconds);

	public void sendQuest(Quest quest, FunctionalAnswerCallback callback);
	public void sendQuest(Quest quest, FunctionalAnswerCallback callback, int timeoutInSeconds);

Send quest to server.

+ `AnswerCallback`:
	
	Prototype:

		public abstract class AnswerCallback {
		    public abstract void onAnswer(Answer answer);
		    public abstract void onException(Answer answer, int errorCode);

		    public long getSentTime();
		    public long getAnsweredTime();
		}

	When quest successed, `onAnswer()` will be called; else `onException()` will be called.  
	The `answer` in `onException()` maybe `null`.

+ `FunctionalAnswerCallback`:

	Prototype:

		public interface FunctionalAnswerCallback {
		    void onAnswer(Answer answer, int errorCode);
		}

	When the `errorCode` beyond 0 or `com.fpnn.sdk.ErrorCode.FPNN_EC_OK.value()` (value of `FPNN_EC_OK` is `0`), the `answer` maybe `null`.

#### Send Answer

	public void sendAnswer(Answer answer);

Send answer in unforeseen case.


## FPNN Message

FPNN message is the parent class of the FPNN Quest & FPNN Answer.  
It offers the data access capability for FPNN Quest & FPNN Answer.

### Constructors

	public Message();
	public Message(Map body);

### Methods

#### Raw Data Map

	public Map getPayload();
	public void setPayload(Map p);

Get/set the raw data map.

#### Add Data

	public void param(String key, Object value);

Add data to FPNN Message.

#### Get Data

	public Object get(String key);
	public Object get(String key, Object def);

Fetch data from FPNN Messsage. If data is not exist, the `null` or `def` will be returned.

	public int getInt(String key, int defaultValue);
	public long getLong(String key, long defaultValue);

Fetch int type or long type data from FPNN Messsage. If data is not exist, the `defaultValue` will be returned.

#### Want Data

	public Object want(String key) throws NoSuchElementException;

Fetch data from FPNN Messsage. If data is not exist, a `NoSuchElementException` exception will be thrown.

	public int wantInt(String key) throws ClassCastException, NoSuchElementException;
	public long wantLong(String key) throws ClassCastException, NoSuchElementException;

Fetch int type or long type data from FPNN Messsage. If data is not exist, a `NoSuchElementException` exception will be thrown. If data type dose not match, a `ClassCastException` exception will be thrown.

#### Serialize

	public byte[] toByteArray() throws IOException;
	public byte[] raw() throws IOException;

Serialize the FPNN Message instance to the byte[] data in msgpack SPEC.


## FPNN Quest

### Constructors

	public Quest(String method);
	public Quest(String method, boolean isOneWay);
	public Quest(String method, int seqNum, boolean isOneWay, Map payload);

* method:

	The requested interface/method name.

* isOneWay:

	Create the new FPNN Quest as an one way quest or not.

* seqNum:

	The sequence number for FPNN package (Quest & Answer).

* payload:

	The quest data in FPNN Message.

The first constructor will create a two way quest with an empty data map.

### Methods

#### SeqNum

	public int getSeqNum();

Fetch the sequence number of this quest.

#### Method

	public String method();

Fetch the quest requested interface/method.

#### IsOneWay

	public boolean isOneWay();

Check the quest is one way quest or not.

#### IsTwoWay

	public boolean isTwoWay();

Check the quest is two way quest or not.

#### Raw Data

	public ByteBuffer rawData();

Serialize this quest instance into a ByteBuffer instance in FPNN Packet Protocol.



## FPNN Answer

### Constructors

	public Answer(Quest quest);
	public Answer(int seqNum);
	public Answer(int seqNum, boolean error, Map payload);

* quest:

	The quest will be answered by this answer.

* seqNum:

	The seqNum of the quest will be answered by this answer.

* error:

	Create this answer as FPNN Standard Error Answer or not. 

* payload:

	The answer data map in FPNN Message.

### Methods

#### SeqNum

	public int getSeqNum();

Fetch the sequence number of this answer.

#### FPNN Standard Error Answer Check

	public boolean isErrorAnswer();

Check this answer is the FPNN Standard Error Answer or not.

#### Error Code

	public int getErrorCode();

The error code for FPNN Standard Error Answer.

#### Error Message

	public String getErrorMessage();

Fetch the error message for FPNN Standard Error Answer.

#### Fill Error Code

	public void fillErrorCode(int errorCode);
	
Fill the error code to the FPNN Standard Error Answer.

#### Fill Error Info

	public void fillErrorInfo(int errorCode);
	public void fillErrorInfo(int errorCode, String message);

Fill the error code and error message to the FPNN Standard Error Answer.

#### Raw Data

	public ByteBuffer rawData() throws IOException;

Serialize this answer instance into a ByteBuffer instance in FPNN Packet Protocol.





