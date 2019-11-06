package com.fpnn.nio;

import com.fpnn.ErrorRecorder;
import com.fpnn.FPConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadPool {

    public interface IThreadPool {

        void execute(Runnable runnable);
    }

    private int _nThreads = FPConfig.MAX_THREAD_COUNT;

    private ThreadPool() {

        if (this._nThreads <= 0) {

            this._nThreads = Runtime.getRuntime().availableProcessors() * 2;
        }
    }

    private static class Singleton {

        private static final ThreadPool INSTANCE = new ThreadPool();
    }

    public static final ThreadPool getInstance() {

        return ThreadPool.Singleton.INSTANCE;
    }

    public int numThreads() {

        return this._nThreads;
    }

    private boolean _useTimerThread;
    private IThreadPool _threadPool = null;

    public synchronized void setPool(IThreadPool value) throws Exception {

        if (this._threadPool != null) {

            throw new Exception("ThreadPool has been up");
        }

        this._threadPool = value;
    }

    public void execute(Runnable runnable) {

        if (this._threadPool == null) {

            final int nThreads = this._nThreads;

            try{

                this.setPool(new IThreadPool() {

                    ExecutorService executor = Executors.newFixedThreadPool(nThreads);

                    @Override
                    public void execute(Runnable runnable) {

                        executor.execute(runnable);
                    }
                });
            } catch (Exception ex){

                ErrorRecorder.getInstance().recordError(ex);
            }
        }

        this._threadPool.execute(runnable);
    }

    public IThreadPool getThreadPool() {

        return this._threadPool;
    }

    public synchronized void startTimerThread() {

        if (!this._useTimerThread) {

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
