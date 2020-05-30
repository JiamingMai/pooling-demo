package com.kapok;

import java.util.ArrayList;
import java.util.List;

public class PoolState {

    protected final List<PooledConnection> idleConnections = new ArrayList<>();

    protected final List<PooledConnection> activeConnections = new ArrayList<>();

    // TODO: we can also add some statistics variable to record the state of the pool here

    protected ConnectionPool connectionPool;

    public PoolState(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

}
