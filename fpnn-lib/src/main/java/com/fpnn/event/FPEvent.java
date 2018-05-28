package com.fpnn.event;

import com.fpnn.nio.ThreadPool;

import java.util.*;

public class FPEvent {

    public interface IListener extends EventListener {

        void fpEvent(EventData event);
    }


    private Map _listeners = new HashMap();

    public void addListener(String type, IListener lisr) {

        synchronized (this._listeners) {

            List queue = (List) this._listeners.get(type);

            if (queue == null) {

                queue = new ArrayList();
                this._listeners.put(type, queue);
            }

            queue.add(lisr);
        }
    }

    public void fireEvent(EventData event) {

        List queue;

        synchronized (this._listeners) {

            queue = (List) this._listeners.get(event.getType());
        }

        if (queue != null && queue.size() > 0) {

            final List fQueue = queue;
            final EventData fEvent = event;

            ThreadPool.getInstance().execute(new Runnable() {

                @Override
                public void run() {

                    Iterator<IListener> iterator = fQueue.iterator();

                    while (iterator.hasNext()) {

                        IListener lisr = iterator.next();
                        lisr.fpEvent(fEvent);
                    }
                }
            });
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

        synchronized (this._listeners) {

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
}
