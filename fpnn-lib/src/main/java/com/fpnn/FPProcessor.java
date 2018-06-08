package com.fpnn;

import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;

public class FPProcessor {

    public interface IAnswer {

        void sendAnswer(Object payload, boolean exception);
    }

    public interface IProcessor {

        void service(FPData data, IAnswer answer);

        void onSecond(long timestamp);
    }

    private IProcessor _processor;
    private FPEvent _event;

    public FPProcessor() {

        this._event = new FPEvent();
    }

    public FPEvent getEvent() {

        return this._event;
    }

    public void setProcessor(IProcessor processor) {

        this._processor = processor;
    }

    public void service(FPData data, IAnswer answer) {

        if (this._processor == null) {

            final FPProcessor self = this;

            this._processor = new IProcessor() {

                @Override
                public void service(FPData data, IAnswer answer) {

                    if (data.getFlag() == 0) {

                        self._event.fireEvent(new EventData(this, data.getMethod(), data.jsonPayload()));
                    }

                    if (data.getFlag() == 1) {

                        self._event.fireEvent(new EventData(this, data.getMethod(), data.msgpackPayload()));
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
