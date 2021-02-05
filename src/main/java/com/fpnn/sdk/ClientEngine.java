package com.fpnn.sdk;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by shiwangxing on 2017/11/28.
 */

public class ClientEngine {

    public static String SDKVersion = "2.0.4";

    private static ClientEngineCore engineCore = new ClientEngineCore();
    private static boolean stopFuncCalled = false;
    private static boolean autoStop = true;
    private static int questTimeout = 5;
    private static int maxThreadInTaskPool = 4;
    private static int maxPackageLength = 1024 * 1024 * 10;     //-- 10 MB
    private static ExecutorService threadPool = null;

    public static boolean isAutoStop() {
        return autoStop;
    }

    public static void setAutoStop(boolean auto) {
        autoStop = auto;
    }

    public static int getQuestTimeout() {
        return questTimeout;
    }

    public static void setQuestTimeout(int timeout) {
        questTimeout = timeout;
    }

    public static int getMaxThreadInTaskPool() {
        return maxThreadInTaskPool;
    }

    public static void setMaxThreadInTaskPool(int count) {
        maxThreadInTaskPool = count;
    }

    public static int getMaxPackageLength() {
        return maxPackageLength;
    }

    public static void setMaxPackageLength(int length) {
        maxPackageLength = length;
    }

    public static ExecutorService getThreadPool() {
        return threadPool;
    }

    public static void changeChannelInterestedEvent(SocketChannel channel, int ops) {
        engineCore.changeChannelInterestedEvent(channel, ops);
    }

    public static boolean newChannel(TCPConnection connection, int ops) {
        return engineCore.newChannel(connection, ops);
    }

    public static void closeConnection(TCPConnection connection) {
        engineCore.closeConnection(connection);
    }

    public static boolean started() {
        return engineCore.isAlive();
    }

    public static void startEngine() {

        if (!started())
        {
            synchronized (ClientEngineCore.class) {
                if (threadPool == null) {
                    if (autoStop) {
                        threadPool = Executors.newFixedThreadPool(maxThreadInTaskPool,
                                new ThreadFactory() {
                            public Thread newThread(Runnable r) {
                                Thread t = Executors.defaultThreadFactory().newThread(r);
                                t.setDaemon(true);
                                return t;
                            }
                        });
                    }
                    else
                        threadPool = Executors.newFixedThreadPool(maxThreadInTaskPool);
                }

                if (!started())
                    engineCore.start();
            }
        }
    }

    public static void stop() {
        if (stopFuncCalled)
            return;

        synchronized (ClientEngineCore.class) {
            if (stopFuncCalled)
                return;

            if (!started())
                return;

            stopFuncCalled = true;
        }

        engineCore.finish();
        try {
            engineCore.join();
            threadPool.shutdown();
        }
        catch (InterruptedException e)
        {
            ErrorRecorder.record("Join Engine Core thread exception.", e);
        }
    }
}
