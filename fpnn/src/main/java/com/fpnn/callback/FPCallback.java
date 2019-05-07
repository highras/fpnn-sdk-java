package com.fpnn.callback;

import com.fpnn.FPConfig;
import com.fpnn.FPData;
import com.fpnn.nio.ThreadPool;

import java.util.*;

public class FPCallback {

    public interface ICallback {

        void callback(CallbackData cbd);
    }

    private Map _cbMap = new HashMap();
    private Map _exMap = new HashMap();

    public void addCallback(String key, FPCallback.ICallback callback, int timeout) {

        synchronized (this._cbMap) {

            this._cbMap.put(key, callback);
        }

        synchronized (this._exMap) {

            int ts = timeout <= 0 ? FPConfig.SEND_TIMEOUT : timeout;
            long expire = ts + System.currentTimeMillis();
            this._exMap.put(key, expire);
        }
    }

    public void removeCallback() {

        synchronized (this._cbMap) {

            this._cbMap.clear();
        }
    }

    public void execCallback(String key, FPData data) {

        FPCallback.ICallback cb = (FPCallback.ICallback) this._cbMap.get(key);

        if (cb != null) {

            synchronized (this._cbMap) {

                this._cbMap.remove(key);
            }

            final FPCallback.ICallback fcb = cb;
            final FPData fData = data;

            ThreadPool.getInstance().execute(new Runnable() {

                @Override
                public void run() {

                    fcb.callback(new CallbackData(fData));
                }
            });
        }
    }

    public void execCallback(String key, Exception ex) {

        FPCallback.ICallback cb = (FPCallback.ICallback) this._cbMap.get(key);

        if (cb != null) {

            synchronized (this._cbMap) {

                this._cbMap.remove(key);
            }

            final FPCallback.ICallback fcb = cb;
            final Exception fex = ex;

            ThreadPool.getInstance().execute(new Runnable() {

                @Override
                public void run() {

                    fcb.callback(new CallbackData(fex));
                }
            });
        }
    }

    public void onSecond(long timestamp) {

        synchronized (this._exMap) {

            List keys = new ArrayList();
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

                this._exMap.remove(key);
                this.execCallback(key, new Exception("timeout with expire"));
            }
        }
    }
}
