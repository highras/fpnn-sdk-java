package com.fpnn;

import com.fpnn.event.FPEvent;
import com.fpnn.event.FPEventManager;

public class FPProcessor {

    public interface IProcessor {

        void service(FPData data, FPClient.IAnswer answer);

        void onSecond(long timestamp);
    }

    private IProcessor _processor;
    private FPEventManager _event;

    public FPProcessor() {

        this._event = new FPEventManager();
    }

    public FPEventManager getEvent() {

        return this._event;
    }

    public void setProcessor(IProcessor processor) {

        this._processor = processor;
    }

    public void service(FPData data, FPClient.IAnswer answer) {

        if (this._processor == null) {

            final FPProcessor self = this;

            this._processor = new IProcessor() {

                @Override
                public void service(FPData data, FPClient.IAnswer answer) {

                    if (data.getFlag() == 0) {

                        self._event.fireEvent(new FPEvent(this, data.getMethod(), data.jsonPayload()));
                    }

                    if (data.getFlag() == 1) {

                        self._event.fireEvent(new FPEvent(this, data.getMethod(), data.msgpackPayload()));
                    }
                }

                @Override
                public void onSecond(long timestamp) {

                }
            };
        }

        this._processor.service(data, answer);
    }

    public void onSecond(long timestamp) {

        if (this._processor != null) {

            this._processor.onSecond(timestamp);
        }
    }
}
