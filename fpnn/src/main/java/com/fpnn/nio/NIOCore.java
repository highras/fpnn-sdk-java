package com.fpnn.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fpnn.ErrorRecorder;
import com.fpnn.FPManager;
import com.fpnn.FPSocket;

public class NIOCore {

    class ChangeRequest {
        public static final int REGISTER = 1;
        public static final int CHANGEOPS = 2;

        public int ops;
        public int type;
        public SocketChannel socket;

        public ChangeRequest(SocketChannel socket, int type, int ops) {
            this.ops = ops;
            this.type = type;
            this.socket = socket;
        }
    }

    class ServiceLocker {
        public int status = 0;
    }

    private NIOCore() {
        try {
            this._selector = this.initSelector();
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        }

        this.startServiceThread();
    }

    private static class Singleton {
        private static final NIOCore INSTANCE = new NIOCore();
    }

    public static final NIOCore getInstance() {
        return Singleton.INSTANCE;
    }

    private Selector _selector;

    private Map _fpsockData = new HashMap();
    private List _pendingChanges = new LinkedList();

    public void wakeupSelector() {
        if (this._selector != null) {
            this._selector.wakeup();
        }
    }

    private Object self_locker = new Object();

    public void closeSocket(SocketChannel socket) {
        if (socket == null) {
            return;
        }

        synchronized (self_locker) {
            if (this._fpsockData.containsKey(socket)) {
                this._fpsockData.remove(socket);
            }
        }

        SelectionKey key = null;

        if (this._selector != null) {
            key = socket.keyFor(this._selector);
        }

        try {
            socket.close();
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        }

        if (key != null) {
            key.cancel();
        }
    }

    public void closeSocket() {
        List<SocketChannel> list = new ArrayList<SocketChannel>();

        synchronized (self_locker) {
            Iterator itor = this._fpsockData.entrySet().iterator();

            while (itor.hasNext()) {
                Map.Entry entry = (Map.Entry) itor.next();
                SocketChannel socket = (SocketChannel) entry.getKey();
                list.add(socket);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            SocketChannel sc = list.get(i);

            if (sc != null) {
                this.closeSocket(sc);
            }
        }
    }

    public void changeOps(SocketChannel socket, int op) {
        synchronized (self_locker) {
            this._pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, op));
        }

        this.wakeupSelector();
    }

    public void register(FPSocket sock, SocketChannel socket, int op) {
        this.startServiceThread();

        synchronized (self_locker) {
            if (!this._fpsockData.containsKey(socket)) {
                this._fpsockData.put(socket, sock);
            }

            this._pendingChanges.add(new ChangeRequest(socket, ChangeRequest.REGISTER, op));
        }

        this.wakeupSelector();
    }

    private void onRead(SelectionKey key) {
        FPSocket sock = null;
        SocketChannel socket = (SocketChannel) key.channel();

        synchronized (self_locker) {
            sock = (FPSocket) this._fpsockData.get(socket);
        }

        if (sock != null) {
            sock.onRead(key);
        }
    }

    private void onWrite(SelectionKey key) throws IOException {
        FPSocket sock = null;
        SocketChannel socket = (SocketChannel) key.channel();

        synchronized (self_locker) {
            sock = (FPSocket) this._fpsockData.get(socket);
        }

        if (sock != null) {
            sock.onWrite(key);
        }
    }

    private void finishConnection(SelectionKey key) {
        FPSocket sock = null;
        SocketChannel socket = (SocketChannel) key.channel();

        synchronized (self_locker) {
            sock = (FPSocket) this._fpsockData.get(socket);
        }

        if (sock != null) {
            sock.finishConnection(key);
        }
    }

    private Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    public void checkSecond() {
        List<FPSocket> list = new ArrayList<FPSocket>();
        long ts = FPManager.getInstance().getMilliTimestamp();

        synchronized (self_locker) {
            Iterator<FPSocket> iterator = this._fpsockData.values().iterator();

            while (iterator.hasNext()) {
                FPSocket sock = iterator.next();
                list.add(sock);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            FPSocket sc = list.get(i);

            if (sc != null) {
                sc.onSecond(ts);
            }
        }
    }

    private Thread _serviceThread = null;
    private ServiceLocker service_locker = new ServiceLocker();

    private void startServiceThread() {
        synchronized (service_locker) {
            if (service_locker.status != 0) {
                return;
            }

            service_locker.status = 1;

            try {
                final NIOCore self = this;
                this._serviceThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        self.serviceThread();
                    }
                });

                try {
                    this._serviceThread.setName("FPNN-NIOCORE");
                } catch (Exception e) {}

                this._serviceThread.start();
            } catch (Exception ex) {
                ErrorRecorder.getInstance().recordError(ex);
            }
        }
    }

    private void serviceThread() {
        try {
            while (true) {
                synchronized (service_locker) {
                    if (service_locker.status == 0) {
                        return;
                    }
                }

                synchronized (self_locker) {
                    if (!this._pendingChanges.isEmpty()) {
                        Iterator changes = this._pendingChanges.iterator();

                        while (changes.hasNext()) {
                            ChangeRequest change = (ChangeRequest) changes.next();

                            if (change == null || change.socket == null) {
                                continue;
                            }

                            SelectionKey key;

                            switch (change.type) {
                                case ChangeRequest.CHANGEOPS:
                                    key = change.socket.keyFor(this._selector);

                                    if (key != null && key.isValid()) {
                                        key.interestOps(key.interestOps() | change.ops);
                                    }

                                    break;

                                case ChangeRequest.REGISTER:
                                    change.socket.register(this._selector, change.ops);
                                    break;
                            }
                        }

                        this._pendingChanges.clear();
                    }
                }

                Set<SelectionKey> keys = null;

                if (this._selector != null) {
                    int i = this._selector.select();
                    keys = this._selector.selectedKeys();
                }

                if (keys != null && !keys.isEmpty()) {
                    Iterator selectedKeys = keys.iterator();

                    while (selectedKeys.hasNext()) {
                        SelectionKey key = (SelectionKey) selectedKeys.next();
                        selectedKeys.remove();

                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isConnectable()) {
                            this.finishConnection(key);
                            continue;
                        }

                        if (key.isWritable()) {
                            this.onWrite(key);
                        }

                        if (key.isReadable()) {
                            this.onRead(key);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
        } finally {
            this.stopServiceThread();
        }
    }

    private void stopServiceThread() {
        synchronized (self_locker) {
            this._pendingChanges.clear();
        }

        synchronized (service_locker) {
            if (service_locker.status == 1) {
                service_locker.status = 0;
            }
        }
    }
}
