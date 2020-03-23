# FPNN Java SDK API Docs

# Index

[TOC]

## Current Version

	public static String ClientEngine.SDKVersion = "2.0.0";

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
`autoConnect` is NOT `keep alive`, just calling `connect()` method automatically when sending quest without established connection.

### Config & Properties Methods

### Connect & Close Methods

### Event Methods

### Send Quest & Answer Methods


## FPNN Message

### Constructors

### Methods



## FPNN Quest

### Constructors

### Methods



## FPNN Answer

### Constructors

### Methods



## AnswerCallback

### Constructors

### Methods


## FunctionalAnswerCallback






