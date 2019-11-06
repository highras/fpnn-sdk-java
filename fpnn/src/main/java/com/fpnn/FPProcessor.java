package com.fpnn;

import java.util.List;
import java.util.ArrayList;

public class FPProcessor {

    public interface IAnswer {
        void sendAnswer(Object payload, boolean exception);
    }

    public interface IProcessor {
        void service(FPData data, IAnswer answer);
        void onSecond(long timestamp);
        boolean hasPushService(String name);
    }

    class ServiceLocker {
        public int status = 0;
    }

    class BaseProcessor implements FPProcessor.IProcessor {
        @Override
        public void service(FPData data, FPProcessor.IAnswer answer) {
            // TODO
            if (data.getFlag() == 0) {}
            if (data.getFlag() == 1) {}
        }
        @Override
        public boolean hasPushService(String name) {
            return false;
        }
        @Override
        public void onSecond(long timestamp) {}
    }

    private boolean _destroyed;
    private IProcessor _processor;
    private Object self_locker = new Object();

    public void setProcessor(IProcessor processor) {
        synchronized (self_locker) {
            this._processor = processor;
        }
    }

    private Thread _serviceThread = null;
    private ServiceLocker service_locker = new ServiceLocker();

    private void startServiceThread() {
        synchronized (self_locker) {
            if (this._destroyed) {
                return;
            }
        }

        synchronized (service_locker) {
            if (service_locker.status != 0) {
                return;
            }
            service_locker.status = 1;

            try {
                final FPProcessor self = this;
                this._serviceThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        self.serviceThread();
                    }
                });

                try {
                    this._serviceThread.setName("FPNN-PUSH");
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
                List<FPManager.IService> list;
                synchronized (service_locker) {
                    service_locker.wait();

                    if (service_locker.status == 0) {
                        return;
                    }

                    list = this._serviceCache;
                    this._serviceCache = new ArrayList<FPManager.IService>();
                }
                this.callService(list);
            }
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        } finally {
            this.stopServiceThread();
        }
    }

    private void callService(List<FPManager.IService> list) {
        if (list == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            FPManager.IService is = list.get(i);
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
                service_locker.status = 2;

                try {
                    service_locker.notify();
                } catch (Exception ex) {
                    ErrorRecorder.getInstance().recordError(ex);
                }

                final FPProcessor self = this;
                FPManager.getInstance().delayTask(100, new FPManager.ITask() {
                    @Override
                    public void task(Object state) {
                        synchronized (service_locker) {
                            service_locker.status = 0;
                            self._serviceCache.clear();
                        }
                    }
                }, null);
            }
        }
    }

    private List<FPManager.IService> _serviceCache = new ArrayList<FPManager.IService>();

    public void service(final FPData data, IAnswer answer) {
        String method = null;

        if (data != null) {
            method = data.getMethod();
        }

        if (method == null || method.isEmpty()) {
            return;
        }

        IProcessor psr = null;
        synchronized (self_locker){
            if (this._destroyed) {
                return;
            }

            if (this._processor == null) {
                this._processor = new BaseProcessor();
            }

            psr = this._processor;
            if (!psr.hasPushService(method)) {
                if (method != "ping") {
                    return;
                }
            }
        }

        final IProcessor fpsr = psr;
        final IAnswer fanswer = answer;
        this.addService(new FPManager.IService() {
            @Override
            public void service() {
                synchronized (self_locker) {
                    if (fpsr != null) {
                        fpsr.service(data, fanswer);
                    }
                }
            }
        });
    }

    private void addService(FPManager.IService service) {
        synchronized (self_locker) {
            if (this._destroyed) {
                return;
            }
        }

        if (service == null) {
            return;
        }
        this.startServiceThread();

        synchronized (service_locker) {
            if (this._serviceCache.size() < 10000) {
                this._serviceCache.add(service);
            }

            if (this._serviceCache.size() == 9998) {
                ErrorRecorder.getInstance().recordError(new Exception("Push Calls Limit!"));
            }

            try {
                service_locker.notify();
            } catch (Exception ex) {
                ErrorRecorder.getInstance().recordError(ex);
            }
        }
    }

    public void onSecond(long timestamp) {
        try {
            synchronized (self_locker) {
                if (this._processor != null) {
                    this._processor.onSecond(timestamp);
                }
            }
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        }
    }

    public void destroy() {
        synchronized (self_locker) {
            if (this._destroyed) {
                return;
            }
            this._destroyed = true;
        }
        this.stopServiceThread();
    }
}
