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

    public void service(FPData data, IAnswer answer) {

        if (this._processor == null) {

            final FPProcessor self = this;

            this._processor = new IProcessor() {

                FPEvent event = new FPEvent();

                @Override
                public void service(FPData data, IAnswer answer) {

                    if (data.getFlag() == 0) {

                        this.event .fireEvent(new EventData(this, data.getMethod(), data.jsonPayload()));
                    }

                    if (data.getFlag() == 1) {

                        this.event .fireEvent(new EventData(this, data.getMethod(), data.msgpackPayload()));
                    }
                }

                @Override
                public FPEvent getEvent() {

                    return this.event;
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

    public void destroy() {

        FPEvent event = this.getEvent();

//        if (event != null) {
//
//            event.removeListener();
//        }
    }
}
