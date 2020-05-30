package com.kapok;

public class ConnectionPool {

    private int poolMaximumActiveConnections = 10;
    private int poolMaximumIdleConnections = 5;
    private int poolMaximumCheckoutTime = 20000;
    private int poolTimeToWait = 20000;
    private int poolMaximumLocalBadConnectionTolerance = 3;

    private ConnectionFactory originalConnectionFactory;

    private PoolState state = new PoolState(this);

    public ConnectionPool(ConnectionFactory originalConnectionFactory) {
        this.originalConnectionFactory = originalConnectionFactory;
    }

    public PooledConnection popConnection() {
        boolean countedWait = false;
        PooledConnection conn = null;
        int localBadConnectionCount = 0;

        while (conn == null) {
            synchronized (state) {
                if (!state.idleConnections.isEmpty()) {
                    // Pool has available connection
                    conn = state.idleConnections.remove(0);
                } else {
                    // Pool does not have available connection
                    if (state.activeConnections.size() < poolMaximumActiveConnections) {
                        // Can create new connection
                        conn = new PooledConnection(originalConnectionFactory.createConnection(), this);
                    } else {
                        // Cannot create new connection
                        PooledConnection oldestActiveConnection = state.activeConnections.get(0);
                        long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
                        if (longestCheckoutTime > poolMaximumCheckoutTime) {
                            // Can claim overdue connection
                            state.activeConnections.remove(oldestActiveConnection);
                            conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
                            conn.setCreatedTimestamp(oldestActiveConnection.getCreatedTimestamp());
                            conn.setLastUsedTimestamp(oldestActiveConnection.getLastUsedTimestamp());
                            oldestActiveConnection.invalidate();
                        } else {
                            // Must wait
                            try {
                                if (!countedWait) {
                                    countedWait = true;
                                }
                                state.wait(poolTimeToWait);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                }
                if (conn != null) {
                    if (conn.isValid()) {
                        conn.setCheckoutTimestamp(System.currentTimeMillis());
                        conn.setLastUsedTimestamp(System.currentTimeMillis());
                        state.activeConnections.add(conn);
                    } else {
                        localBadConnectionCount++;
                        conn = null;
                        if (localBadConnectionCount > (poolMaximumIdleConnections + poolMaximumLocalBadConnectionTolerance)) {
                            throw new RuntimeException("PooledDataSource: Could not get a good connection to the database.");
                        }
                    }
                }
            }

        }
        if (conn == null) {
            throw new RuntimeException("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
        }
        return conn;
    }

    public void pushConnection(PooledConnection conn) {
        synchronized (state) {
            state.activeConnections.remove(conn);
            if (conn.isValid()) {
                if (state.idleConnections.size() < poolMaximumIdleConnections) {
                    PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
                    state.idleConnections.add(newConn);
                    newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
                    newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
                    conn.invalidate();
                    state.notifyAll();
                } else {
                    conn.getRealConnection().close();
                    conn.invalidate();
                }
            }
        }
    }

    public void forceCloseAll() {
        synchronized (state) {
            for (int i = state.activeConnections.size(); i > 0; i--) {
                try {
                    PooledConnection conn = state.activeConnections.remove(i - 1);
                    conn.invalidate();
                    Connection realConn = conn.getRealConnection();
                    realConn.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            for (int i = state.idleConnections.size(); i > 0; i--) {
                try {
                    PooledConnection conn = state.idleConnections.remove(i - 1);
                    conn.invalidate();
                    Connection realConn = conn.getRealConnection();
                    realConn.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        forceCloseAll();
        super.finalize();
    }
}
