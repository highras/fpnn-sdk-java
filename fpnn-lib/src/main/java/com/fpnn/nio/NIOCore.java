package com.fpnn.nio;

import com.fpnn.FPSocket;
import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NIOCore implements Runnable {

    private NIOCore() {

        try {

            this._event = new FPEvent();
            this._selector = this.initSelector();
            ThreadPool.getInstance().execute(this);
        } catch (IOException ex) {

            ex.printStackTrace();
        }
    }

    private static class Singleton {

        private static final NIOCore INSTANCE = new NIOCore();
    }

    public static final NIOCore getInstance() {

        return Singleton.INSTANCE;
    }

    private FPEvent _event;
    private Selector _selector;

    private Map _fpsockData = new HashMap();
    private List _pendingChanges = new LinkedList();

    public FPEvent getEvent() {

        return this._event;
    }

    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    public void wakeupSelector() {

        this._selector.wakeup();
    }

    public void closeSocket(SocketChannel socket) {

        if (socket == null) {
            return;
        }

        synchronized (this._fpsockData) {

            this._fpsockData.remove(socket);
        }

        SelectionKey key = socket.keyFor(this._selector);

        try {

            socket.close();
        } catch (IOException ex) {

            ex.printStackTrace();
        }

        if (key != null) {

            key.cancel();
        }
    }

    public void closeSocket() {

        synchronized (this._fpsockData) {

            Iterator itor = this._fpsockData.entrySet().iterator();

            while (itor.hasNext()) {

                Map.Entry entry = (Map.Entry) itor.next();
                SocketChannel socket = (SocketChannel) entry.getKey();
                this.closeSocket(socket);
            }
        }
    }

    public void changeOps(SocketChannel socket, int op) {

        synchronized (this._pendingChanges) {

            this._pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, op));
        }

        this.wakeupSelector();
    }

    public void register(FPSocket sock, SocketChannel socket, int op) {

        if (!this._fpsockData.containsKey(socket)) {

            synchronized (this._fpsockData) {

                this._fpsockData.put(socket, sock);
            }
        }

        synchronized (this._pendingChanges) {

            this._pendingChanges.add(new ChangeRequest(socket, ChangeRequest.REGISTER, op));
        }

        this.wakeupSelector();
    }

    @Override
    public void run() {

        while (true) {

            try {

                synchronized (this._pendingChanges) {

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

                int i = this._selector.select();

                Set<SelectionKey> keys = this._selector.selectedKeys();

                if (!keys.isEmpty()) {

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
            } catch (Exception ex) {

                this._pendingChanges.clear();
                ex.printStackTrace();
            }
        }
    }

    private void onRead(SelectionKey key) {

        SocketChannel socket = (SocketChannel) key.channel();

        synchronized (this._fpsockData) {

            FPSocket sock = (FPSocket) this._fpsockData.get(socket);

            if (sock != null) {

                sock.onRead(key);
            }
        }
    }

    private void onWrite(SelectionKey key) throws IOException {

        SocketChannel socket = (SocketChannel) key.channel();

        synchronized (this._fpsockData) {

            FPSocket sock = (FPSocket) this._fpsockData.get(socket);

            if (sock != null) {

                sock.onWrite(key);
            }
        }
    }

    private void finishConnection(SelectionKey key) {

        SocketChannel socket = (SocketChannel) key.channel();

        synchronized (this._fpsockData) {

            FPSocket sock = (FPSocket) this._fpsockData.get(socket);

            if (sock != null) {

                sock.finishConnection(key);
            }
        }
    }

    private Selector initSelector() throws IOException {

        return SelectorProvider.provider().openSelector();
    }

    public void checkSecond() {

        synchronized (this._fpsockData) {

            long ts = this.getTimestamp();
            this._event.fireEvent(new EventData(this, "second", ts));

            Iterator<FPSocket> iterator = this._fpsockData.values().iterator();
            while (iterator.hasNext()) {

                FPSocket sock = iterator.next();
                if (sock != null) {

                    sock.onSecond(ts);
                }
            }
        }
    }
}

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
