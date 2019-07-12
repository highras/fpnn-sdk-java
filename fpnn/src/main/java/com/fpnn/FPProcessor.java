package com.fpnn;

import com.fpnn.event.FPEvent;
import com.fpnn.nio.ThreadPool;

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
        FPEvent getEvent();
    }

    private IProcessor _processor;

    public FPProcessor() {}

    public FPEvent getEvent() {

        if (this._processor != null) {

            return this._processor.getEvent();
        }

        return null;
    }

    public void setProcessor(IProcessor processor) {

        this._processor = processor;
    }

    private boolean _serviceAble;

    private void startServiceThread() {

        if (this._serviceAble) {

            return;
        }

        this._serviceAble = true;

        final FPProcessor self = this;

        ThreadPool.getInstance().execute(new Runnable() {

            @Override
            public void run() {

                try {

                    while(self._serviceAble) {

                        List<BaseService> list;

                        synchronized (self.service_lock) {

                            self.service_lock.wait();

                            list = self._serviceCache;
                            self._serviceCache = new ArrayList<BaseService>();
                        }

                        self.callService(list);
                    }

                } catch (Exception ex) {

                    ErrorRecorder.getInstance().recordError(ex);
                }
            }
        });
    }

    private void callService(List<BaseService> list) {

        for(BaseService bs : list) {

            if (bs != null) {

                bs.service(this._processor);
            }
        }
    }

    private void stopServiceThread() {

        synchronized (this.service_lock) {

            this.service_lock.notify();
        }

        this._serviceAble = false;
    }

    private List<BaseService> _serviceCache = new ArrayList<BaseService>();
    private Object service_lock = new Object();

    public void service(FPData data, IAnswer answer) {

        if (this._processor == null) {

            this._processor = new BaseProcessor();
        }

        if (!this._processor.hasPushService(data.getMethod())) {

            if (data.getMethod() != "ping") {

                return;
            }
        }

        synchronized (this.service_lock) {

            this._serviceCache.add(new BaseService(data, answer));

            if (this._serviceCache.size() >= 100) {

                this._serviceCache.clear();
            }

            if (!this._serviceAble) {

                this.startServiceThread();
            }

            this.service_lock.notify();
        }
    }

    public void onSecond(long timestamp) {

        if (this._processor != null) {

            this._processor.onSecond(timestamp);
        }
    }

    public void destroy() {

        this.stopServiceThread();
    }
}

class BaseService {

    private FPData _data;
    private FPProcessor.IAnswer _answer;

    public BaseService(FPData data, FPProcessor.IAnswer answer) {

        this._data = data;
        this._answer = answer;
    }

    public void service(FPProcessor.IProcessor processor) {

        if (processor != null) {

            processor.service(this._data, this._answer);
        }
    }
}

class BaseProcessor implements FPProcessor.IProcessor {

    FPEvent event = new FPEvent();

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
    public FPEvent getEvent() {

        return this.event;
    }

    @Override
    public void onSecond(long timestamp) {}
}
