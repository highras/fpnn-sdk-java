package com.fpnn.nio;

import com.fpnn.FPConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadPool {

    public interface IThreadPool {

        void execute(Runnable runnable);
    }

    private ThreadPool() {
    }

    private static class Singleton {

        private static final ThreadPool INSTANCE = new ThreadPool();
    }

    public static final ThreadPool getInstance() {

        return ThreadPool.Singleton.INSTANCE;
    }

    private boolean _useTimerThread;
    private IThreadPool _threadPool = null;

    public void setPool(IThreadPool value) throws Exception {

        synchronized (this) {
            if (this._threadPool != null) {

                throw new Exception("ThreadPool has been up");
            }

            this._threadPool = value;
        }
    }

    public void execute(Runnable runnable) {

        if (this._threadPool == null) {

            try{

                this.setPool(new IThreadPool() {

                    ExecutorService executor = Executors.newFixedThreadPool(FPConfig.MAX_THREAD_COUNT);

                    @Override
                    public void execute(Runnable runnable) {

                        executor.execute(runnable);
                    }
                });
            } catch (Exception ex){

                ex.printStackTrace();
            }
        }

        this._threadPool.execute(runnable);
    }

    public IThreadPool threadPool() {

        return this._threadPool;
    }

    public void startTimerThread() {

        synchronized (this) {

            if (this._useTimerThread) {

                return;
            }

            this._useTimerThread = true;
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

            executor.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {

                    NIOCore.getInstance().checkSecond();
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }
    }
}
