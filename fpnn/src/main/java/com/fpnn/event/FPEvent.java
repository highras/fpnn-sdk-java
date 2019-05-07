package com.fpnn.event;

import com.fpnn.nio.ThreadPool;

import java.util.*;

public class FPEvent {

    public interface IListener extends EventListener {

        void fpEvent(EventData event);
    }


    private Map _listeners = new HashMap();

    public void addListener(String type, IListener lisr) {

        List queue = (List) this._listeners.get(type);

        if (queue == null) {

            queue = new ArrayList();

            synchronized (this._listeners) {

                this._listeners.put(type, queue);
            }
        }

        queue.add(lisr);
    }

    public void fireEvent(EventData event) {

        List queue = (List) this._listeners.get(event.getType());

        if (queue != null) {

            synchronized (queue) {

                final EventData fEvent = event;
                Iterator<IListener> iterator = queue.iterator();

                while (iterator.hasNext()) {

                    final IListener fLisr = iterator.next();

                    if (fLisr != null) {

                        ThreadPool.getInstance().execute(new Runnable() {

                            @Override
                            public void run() {

                                fLisr.fpEvent(fEvent);
                            }
                        });
                    }
                }
            }
        }
    }

    public void removeListener() {

        synchronized (this._listeners) {

            this._listeners.clear();
        }
    }

    public void removeListener(String type) {

        synchronized (this._listeners) {

            this._listeners.remove(type);
        }
    }

    public void removeListener(String type, IListener lisr) {

        List queue = (List) this._listeners.get(type);

        if (queue == null) {

            return;
        }

        int index = queue.indexOf(lisr);

        if (index != -1) {

            queue.remove(index);
        }
    }
}
