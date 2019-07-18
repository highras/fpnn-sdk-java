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

        synchronized (this._listeners) {

            List queue = (List) this._listeners.get(type);

            if (queue == null) {

                queue = new ArrayList();
                this._listeners.put(type, queue);
            }

            queue.add(lisr);
        }
    }

    public void fireEvent(EventData evd) {

        synchronized (this._listeners) {

            List queue = (List) this._listeners.get(evd.getType());

            if (queue != null) {

                final EventData fevd = evd;
                Iterator<IListener> iterator = queue.iterator();

                while (iterator.hasNext()) {

                    final IListener fLisr = iterator.next();

                    if (fLisr == null) {

                        continue;
                    }

                    ThreadPool.getInstance().execute(new Runnable() {

                        @Override
                        public void run() {

                            try {

                                if (fLisr != null) {

                                    fLisr.fpEvent(fevd);
                                }
                            } catch(Exception ex) {

                                ErrorRecorder.getInstance().recordError(ex);
                            }
                        }
                    });
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
