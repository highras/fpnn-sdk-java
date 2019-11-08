package com.fpnn.event;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.fpnn.ErrorRecorder;
import com.fpnn.FPManager;

public class FPEvent {

    public interface IListener extends EventListener {
        void fpEvent(EventData evd);
    }

    private Map _listeners = new HashMap();
    private Object self_locker = new Object();

    public void addListener(String type, IListener lisr) {
        if (type == null || type.isEmpty()) {
            ErrorRecorder.getInstance().recordError(new Exception("event type is null or empty"));
            return;
        }

        if (lisr == null) {
            ErrorRecorder.getInstance().recordError(new Exception("IListener is null"));
            return;
        }

        List queue = null;

        synchronized (self_locker) {
            if (!this._listeners.containsKey(type)) {
                this._listeners.put(type, new ArrayList());
            }

            queue = (List) this._listeners.get(type);

            if (queue.indexOf(lisr) == -1) {
                queue.add(lisr);
            }
        }
    }

    public void removeListener() {
        synchronized (self_locker) {
            this._listeners.clear();
        }
    }

    public void removeListener(String type) {
        if (type == null || type.isEmpty()) {
            ErrorRecorder.getInstance().recordError(new Exception("event type is null or empty"));
            return;
        }

        synchronized (self_locker) {
            this._listeners.remove(type);
        }
    }

    public void removeListener(String type, IListener lisr) {
        if (type == null || type.isEmpty()) {
            ErrorRecorder.getInstance().recordError(new Exception("event type is null or empty"));
            return;
        }

        if (lisr == null) {
            ErrorRecorder.getInstance().recordError(new Exception("IListener is null"));
            return;
        }

        List queue = null;

        synchronized (self_locker) {
            if (!this._listeners.containsKey(type)) {
                return;
            }

            queue = (List) this._listeners.get(type);
            int index = queue.indexOf(lisr);

            if (index != -1) {
                queue.remove(index);
            }
        }
    }

    public void fireEvent(EventData evd) {
        if (evd == null) {
            ErrorRecorder.getInstance().recordError(new Exception("IListener is null"));
            return;
        }

        List queue = null;
        String type = evd.getType();

        if (type == null || type.isEmpty()) {
            ErrorRecorder.getInstance().recordError(new Exception("event type is null or empty"));
            return;
        }

        synchronized (self_locker) {
            if (!this._listeners.containsKey(type)) {
                return;
            }

            queue = (List) this._listeners.get(type);
            Iterator<IListener> iterator = queue.iterator();

            while (iterator.hasNext()) {
                IListener lisr = iterator.next();

                if (lisr != null) {
                    FPManager.getInstance().eventTask(lisr, evd);
                }
            }
        }
    }
}
