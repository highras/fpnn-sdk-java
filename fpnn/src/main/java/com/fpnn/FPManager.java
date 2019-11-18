package com.fpnn;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.fpnn.callback.CallbackData;
import com.fpnn.callback.FPCallback;
import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;
import com.fpnn.nio.NIOCore;

public class FPManager {

    public interface IService {
        void service();
    }

    public interface ITask {
        void task(Object state);
    }

    class TimerLocker {
        public int status = 0;
    }

    class ServiceLocker {
        public int status = 0;
    }

    private FPManager() {}

    private static class Singleton {
        private static final FPManager INSTANCE = new FPManager();
    }

    public static final FPManager getInstance() {
        return Singleton.INSTANCE;
    }

    public void init() {
        this.startTaskTimer();
        this.startTimerThread();
        this.startServiceThread();
    }

    private List<FPEvent.IListener> _secondCalls = new ArrayList<FPEvent.IListener>();

    public void addSecond(FPEvent.IListener callback) {
        if (callback == null) {
            return;
        }

        synchronized (timer_locker) {
            this._secondCalls.add(callback);
        }

        this.startTimerThread();
    }

    public void removeSecond(FPEvent.IListener callback) {
        if (callback == null) {
            return;
        }

        synchronized (timer_locker) {
            int index = this._secondCalls.indexOf(callback);

            if (index != -1) {
                this._secondCalls.remove(index);
            }
        }
    }

    private ScheduledFuture _timerFuture;
    private TimerLocker timer_locker = new TimerLocker();
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public void startTimerThread() {
        synchronized (timer_locker) {
            if (timer_locker.status != 0) {
                return;
            }

            timer_locker.status = 1;

            if (this._timerFuture == null) {
                final FPManager self = this;
                this._timerFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        self.onSecond();
                    }
                }, 1000, 1000, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void onSecond() {
        synchronized (timer_locker) {
            NIOCore.getInstance().checkSecond();
            this.callSecond(this._secondCalls);
        }
    }

    private void callSecond(List<FPEvent.IListener> list) {
        for (int i = 0; i < list.size(); i++) {
            FPEvent.IListener cb = list.get(i);

            if (cb != null) {
                try {
                    cb.fpEvent(new EventData(this, "second", this.getMilliTimestamp()));
                } catch (Exception ex) {
                    ErrorRecorder.getInstance().recordError(ex);
                }
            }
        }
    }

    public void stopTimerThread() {
        synchronized (timer_locker) {
            timer_locker.status = 0;

            if (this._timerFuture != null) {
                try {
                    this._timerFuture.cancel(true);
                } catch (Exception ex) {
                    ErrorRecorder.getInstance().recordError(ex);
                }

                this._timerFuture = null;
            }
        }
    }

    private Thread _serviceThread = null;
    private ServiceLocker service_locker = new ServiceLocker();

    private void startServiceThread() {
        synchronized (service_locker) {
            if (service_locker.status != 0) {
                return;
            }

            service_locker.status = 1;

            try {
                final FPManager self = this;
                this._serviceThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        self.serviceThread();
                    }
                });

                try {
                    this._serviceThread.setName("FPNN-SERVICE");
                } catch (Exception e) {}

                this._serviceThread.start();
            } catch (Exception ex) {
                ErrorRecorder.getInstance().recordError(ex);
            }
        }
    }

    private void serviceThread() {
        try {
            while (true) {
                List<IService> list;

                synchronized (service_locker) {
                    if (service_locker.status == 0) {
                        return;
                    }

                    list = this._serviceCache;
                    this._serviceCache = new ArrayList<IService>();
                }
                this.callService(list);
                synchronized (service_locker) {
                    if (service_locker.status == 0) {
                        return;
                    }
                    service_locker.wait();
                }
            }
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        } finally {
            this.stopServiceThread();
        }
    }

    private void callService(List<IService> list) {
        if (list == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            IService is = list.get(i);

            if (is != null) {
                try {
                    is.service();
                } catch (Exception ex) {
                    ErrorRecorder.getInstance().recordError(ex);
                }
            }
        }
    }

    private void stopServiceThread() {
        synchronized (service_locker) {
            if (service_locker.status == 1) {
                service_locker.status = 0;
                try {
                    service_locker.notify();
                } catch (Exception ex) {
                    ErrorRecorder.getInstance().recordError(ex);
                }
                this._serviceCache.clear();
            }
        }
    }

    private List<IService> _serviceCache = new ArrayList<IService>();

    public void eventTask(FPEvent.IListener callback, EventData evd) {
        if (callback == null) {
            return;
        }

        final FPEvent.IListener fcb = callback;
        final EventData fevd = evd;
        this.addService(new IService() {
            @Override
            public void service() {
                if (fcb != null) {
                    fcb.fpEvent(fevd);
                }
            }
        });
    }

    public void callbackTask(FPCallback.ICallback callback, CallbackData cbd) {
        if (callback == null) {
            return;
        }

        final FPCallback.ICallback fcb = callback;
        final CallbackData fcbd = cbd;
        this.addService(new IService() {
            @Override
            public void service() {
                if (fcb != null) {
                    fcb.callback(fcbd);
                }
            }
        });
    }

    public void asyncTask(ITask task, Object state) {
        if (task == null) {
            return;
        }

        final ITask ftask = task;
        final Object fstate = state;
        this.addService(new IService() {
            @Override
            public void service() {
                if (ftask != null) {
                    ftask.task(fstate);
                }
            }
        });
    }

    private void addService(IService is) {
        if (is == null) {
            return;
        }

        this.startServiceThread();

        synchronized (service_locker) {
            if (this._serviceCache.size() < 10000) {
                this._serviceCache.add(is);
            }

            if (this._serviceCache.size() == 9998) {
                ErrorRecorder.getInstance().recordError(new Exception("Service Calls Limit!"));
            }

            try {
                service_locker.notify();
            } catch (Exception ex) {
                ErrorRecorder.getInstance().recordError(ex);
            }
        }
    }

    public void delayTask(int milliSecond, ITask task, Object state) {
        if (milliSecond <= 0) {
            this.asyncTask(task, state);
            return;
        }

        if (task == null) {
            return;
        }

        final FPManager self = this;
        final ITask ftask = task;
        final Object fstate = state;
        this.addTimerTask(new TimerTask() {
            @Override
            public void run() {
                self.asyncTask(ftask, fstate);
            }
        }, milliSecond);
    }

    private void addTimerTask(TimerTask task, long delay) {
        if (task == null) {
            return;
        }

        this.startTaskTimer();

        synchronized (task_locker) {
            this._taskTimer.schedule(task, delay);
        }
    }

    private Timer _taskTimer;
    private TimerLocker task_locker = new TimerLocker();

    private void startTaskTimer() {
        synchronized (task_locker) {
            if (task_locker.status != 0) {
                return;
            }

            task_locker.status = 1;

            if (this._taskTimer == null) {
                this._taskTimer = new Timer();
            }
        }
    }

    public void stopTaskTimer() {
        synchronized (task_locker) {
            task_locker.status = 0;

            if (this._taskTimer != null) {
                try {
                    this._taskTimer.purge();
                } catch (Exception ex) {
                    ErrorRecorder.getInstance().recordError(ex);
                }

                this._taskTimer = null;
            }
        }
    }

    public long getMilliTimestamp() {
        return System.currentTimeMillis();
    }

    public int getTimestamp() {
        return (int) Math.floor(System.currentTimeMillis() / 1000);
    }

    public String md5(byte[] bytes) {
        byte[] md5Binary = new byte[0];

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            md5Binary = md5.digest();
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
            return null;
        }

        return this.bytesToHexString(md5Binary, false);
    }

    public String md5(String str) {
        byte[] md5Binary = new byte[0];

        try {
            byte[] bytes = str.getBytes("UTF-8");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            md5Binary = md5.digest();
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
            return null;
        }

        return this.bytesToHexString(md5Binary, false);
    }

    public String bytesToHexString(byte[] bytes, boolean isLowerCase) {
        String from = isLowerCase ? "%02x" : "%02X";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        Formatter formatter = new Formatter(sb);

        for (byte b : bytes) {
            formatter.format(from, b);
        }

        return sb.toString();
    }
}
