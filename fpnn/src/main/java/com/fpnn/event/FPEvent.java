package com.fpnn.event;

import com.fpnn.ErrorRecorder;
import com.fpnn.nio.ThreadPool;

import java.util.*;

public class FPEvent {

    public interface IListener extends EventListener {

        void fpEvent(EventData evd);
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

    public void fireEvent(EventData evd) {

        List queue = (List) this._listeners.get(evd.getType());

        if (queue != null) {

            synchronized (queue) {

                final EventData fevd = evd;
                Iterator<IListener> iterator = queue.iterator();

                while (iterator.hasNext()) {

                    final IListener fLisr = iterator.next();

                    if (fLisr != null) {

                        ThreadPool.getInstance().execute(new Runnable() {

                            @Override
                            public void run() {

                                try {

                                    fLisr.fpEvent(fevd);
                                } catch(Exception ex) {

                                    ErrorRecorder.getInstance().recordError(ex);
                                }
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
