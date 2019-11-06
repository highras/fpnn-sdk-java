package com.fpnn.callback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.fpnn.ErrorRecorder;
import com.fpnn.FPConfig;
import com.fpnn.FPData;
import com.fpnn.FPManager;

public class FPCallback {

    public interface ICallback {
        void callback(CallbackData cbd);
    }

    private Map _cbMap = new HashMap();
    private Map _exMap = new HashMap();

    private Object self_locker = new Object();

    public void addCallback(String key, FPCallback.ICallback callback, int timeout) {
        if (key == null || key.isEmpty()) {
            ErrorRecorder.getInstance().recordError(new Exception("callback key is null or empty"));
            return;
        }

        if (callback == null) {
            ErrorRecorder.getInstance().recordError(new Exception("callback is null"));
            return;
        }

        synchronized (self_locker) {
            if (!this._cbMap.containsKey(key)) {
                this._cbMap.put(key, callback);
            }

            if (!this._exMap.containsKey(key)) {
                int ts = timeout <= 0 ? FPConfig.SEND_TIMEOUT : timeout;
                long expire = ts + FPManager.getInstance().getMilliTimestamp();
                this._exMap.put(key, expire);
            }
        }
    }

    public void removeCallback() {
        synchronized (self_locker) {
            this._cbMap.clear();
            this._exMap.clear();
        }
    }

    public void execCallback(String key, FPData data) {
        if (key == null || key.isEmpty()) {
            ErrorRecorder.getInstance().recordError(new Exception("callback key is null or empty"));
            return;
        }

        FPCallback.ICallback callback = null;

        synchronized (self_locker) {
            if (this._cbMap.containsKey(key)) {
                callback = (FPCallback.ICallback)this._cbMap.get(key);
                this._cbMap.remove(key);
            }

            if (this._exMap.containsKey(key)) {
                this._exMap.remove(key);
            }
        }

        if (callback != null) {
            FPManager.getInstance().callbackTask(callback, new CallbackData(data));
        }
    }

    public void execCallback(String key, Exception exception) {
        if (key == null || key.isEmpty()) {
            ErrorRecorder.getInstance().recordError(new Exception("callback key is null or empty"));
            return;
        }

        FPCallback.ICallback callback = null;

        synchronized (self_locker) {
            if (this._cbMap.containsKey(key)) {
                callback = (FPCallback.ICallback)this._cbMap.get(key);
                this._cbMap.remove(key);
            }

            if (this._exMap.containsKey(key)) {
                this._exMap.remove(key);
            }
        }

        if (callback != null) {
            FPManager.getInstance().callbackTask(callback, new CallbackData(exception));
        }
    }

    public void onSecond(long timestamp) {
        List keys = new ArrayList();

        synchronized (self_locker) {
            Iterator itor = this._exMap.entrySet().iterator();

            while (itor.hasNext()) {
                Map.Entry entry = (Map.Entry) itor.next();
                String key = (String) entry.getKey();
                long expire = (long) entry.getValue();

                if (expire > timestamp) {
                    continue;
                }

                keys.add(key);
            }

            itor = keys.iterator();

            while (itor.hasNext()) {
                String key = (String) itor.next();
                this.execCallback(key, new Exception("timeout with expire"));
            }
        }
    }
}
