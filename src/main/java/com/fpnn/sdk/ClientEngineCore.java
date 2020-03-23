package com.fpnn.sdk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by shiwangxing on 2017/11/28.
 */

class ClientEngineCore extends Thread {

    private Selector selector;
    private Map<SocketChannel, Integer> channelEvents;
    private Map<SocketChannel, TCPConnection> connectionMap;
    private Set<SocketChannel> closedChannels;
    private Set<TCPConnection> userClosedTCPConnection;

    private volatile boolean running;
    private boolean keyCancelled;


    public ClientEngineCore() {
        try {
            selector = SelectorProvider.provider().openSelector();
        }
        catch (IOException e) {
            ErrorRecorder.record("Create NIO selector failed.");
            selector = null;
            running = false;
        }

        channelEvents = new HashMap<>();        //-- Maybe change to TreeMap
        connectionMap = new HashMap<>();        //-- Maybe change to TreeMap
        closedChannels = new HashSet<>();
        userClosedTCPConnection = new HashSet<>();

        keyCancelled = false;
        running = true;
        setDaemon(true);
    }

    public void closeConnection(TCPConnection connection) {
        synchronized (userClosedTCPConnection) {
            userClosedTCPConnection.add(connection);
        }
        selector.wakeup();
    }

    public void changeChannelInterestedEvent(SocketChannel channel, int ops) {
        synchronized (channelEvents) {
            channelEvents.put(channel, ops);
        }
        selector.wakeup();
    }

    private void registerChannelEvents() {
        synchronized (channelEvents) {
            Iterator iterator = channelEvents.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                SocketChannel channel = (SocketChannel) entry.getKey();
                Integer ops = (Integer) entry.getValue();

                try {
                    channel.register(selector, ops);
                }
                catch (IOException | CancelledKeyException e)
                {
                    String peer = "<Get server address failed>";
                    try {
                        InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
                        peer = address.getHostName() + ':' + address.getPort();
                        channel.close();
                    }
                    catch (IOException e2) {
                    }

                    ErrorRecorder.record("Register channel event ops " + ops + " failed. Peer: " + peer, e);
                }
            }

            channelEvents.clear();
        }
    }

    public boolean newChannel(TCPConnection connection, int ops) {
        if (!running)
            return false;

        synchronized (connectionMap) {
            connectionMap.put(connection.getChannel(), connection);

            //-- Prevent new channel created in the running flag checking gap.
            if (!running) {
                connectionMap.remove(connection.getChannel());
                return false;
            }
        }

        changeChannelInterestedEvent(connection.getChannel(), ops);
        return true;
    }

    private long getNextTimeoutInterval() {

        Set<TCPConnection> connSet = new HashSet<>();

        synchronized (connectionMap) {
            if (connectionMap.values().size() == 0) {
                if (keyCancelled) {
                    keyCancelled = false;
                    return -1;
                }
                return 0;
            }

            connSet.addAll(connectionMap.values());
        }

        long value = 5 * 1000;
        long current = System.currentTimeMillis();
        for (TCPConnection connection : connSet) {
            long nextValue = connection.getNextTimeoutMillis();

            if (nextValue == 0)
                continue;

            if (nextValue > 0)
                nextValue -= current;

            if (nextValue < 0)
                return nextValue;

            if (nextValue == 0)
                return -1;

            if (nextValue < value)
                value = nextValue;
        }

        return value;
    }

    private void checkTimeoutCallbacks() {

        Set<TCPConnection> connSet = new HashSet<>();

        synchronized (connectionMap) {
            connSet.addAll(connectionMap.values());
        }

        for (TCPConnection connection : connSet) {
            connection.checkTimeoutCallbacks();
        }
    }

    private void processConnectedEvent(Set<TCPConnection> connectedSet) {
        for (TCPConnection conn: connectedSet) {
            boolean succeed = true;
            SocketChannel channel = conn.getChannel();
            try {
                channel.finishConnect();
            }
            catch (IOException e) {
                succeed = false;
                closedChannels.add(channel);

                String peer = "<Get server address failed>";
                try {
                    InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
                    peer = address.getHostName() + ':' + address.getPort();
                    channel.close();
                }
                catch (IOException e2) {
                }

                ErrorRecorder.record("Finish connect action failed. Peer: " + peer, e);
            }
            conn.processConnectedEvent(succeed);
        }
    }

    private void processIOEvent(Map<TCPConnection, Integer> connMap) {
        Iterator iterator = connMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            TCPConnection conn = (TCPConnection) entry.getKey();
            Integer ops = (Integer) entry.getValue();

            if (!conn.processIOEvent(ops)) {
                closedChannels.add(conn.getChannel());
            }
        }
    }

    private void processChannelEvents() {
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

        Set<TCPConnection> connectedSet = new HashSet<>();
        Map<TCPConnection, Integer> ioMap = new HashMap<>();
        Set<TCPConnection> invalidConnectionedSet = new HashSet<>();

        if (keyIterator.hasNext()) {
            synchronized (connectionMap) {
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    SocketChannel channel = (SocketChannel) key.channel();
                    TCPConnection conn = connectionMap.get(channel);

                    if (conn == null) {
                        key.cancel();
                        keyIterator.remove();
                        keyCancelled = true;
                        continue;
                    }

                    if (key.isValid()) {
                        if (key.isConnectable()) {
                            connectedSet.add(conn);
                        }
                        else {
                            int ops = key.readyOps();
                            ioMap.put(conn, ops);
                        }
                    } else {
                        invalidConnectionedSet.add(conn);
                    }

                    keyIterator.remove();
                }
            }
        }

        synchronized (userClosedTCPConnection) {
            userClosedTCPConnection.addAll(invalidConnectionedSet);
        }

        processConnectedEvent(connectedSet);
        processIOEvent(ioMap);
    }

    private void processInvalidChannels() {

        HashSet<SocketChannel> invalidChannels = new HashSet<>();
        HashSet<TCPConnection> invalidConnections = new HashSet<>();

        synchronized (userClosedTCPConnection) {
            for (TCPConnection conn : userClosedTCPConnection) {
                closedChannels.add(conn.getChannel());
            }
            userClosedTCPConnection.clear();
        }

        Set<SelectionKey> keys = selector.keys();
        synchronized (connectionMap) {
            for (SelectionKey key: keys) {
                SocketChannel channel = (SocketChannel) key.channel();
                if (closedChannels.contains(channel) || !key.isValid()) {
                    key.cancel();
                    keyCancelled = true;

                    TCPConnection conn = connectionMap.get(channel);
                    if (conn != null)
                        invalidConnections.add(conn);

                    connectionMap.remove(channel);
                    invalidChannels.add(channel);
                }
            }
        }

        synchronized (channelEvents) {
            for (SocketChannel channel : invalidChannels)
                channelEvents.remove(channel);
        }

        for (TCPConnection connection : invalidConnections)
            connection.closedByCachedError();

        closedChannels.clear();
    }

    @Override
    public void run() {

        if (selector == null)
        {
            ErrorRecorder.record("NIO selector is null.");
            return;
        }

        while (running) {
            registerChannelEvents();

            long milliseconds = getNextTimeoutInterval();
            try {
                if (milliseconds == 0)
                    selector.select();
                else if (milliseconds > 0)
                    selector.select(milliseconds);
                else
                    selector.selectNow();
            }
            catch (IOException e)
            {
                ErrorRecorder.record("NIO select() exception. milliseconds is " + milliseconds + ".", e);
            }

            if (running) {
                processChannelEvents();
                processInvalidChannels();
                checkTimeoutCallbacks();
            }
        }

        clean();
    }

    private void clean() {
        try {
            selector.close();
        }
        catch (IOException e)
        {
            ErrorRecorder.record("Close selector exception.", e);
        }

        synchronized (connectionMap) {
            for (TCPConnection connection : connectionMap.values()) {
                connection.closeBySelector();
            }
        }
    }

    public void finish() {
        running = false;
        selector.wakeup();
    }
}
