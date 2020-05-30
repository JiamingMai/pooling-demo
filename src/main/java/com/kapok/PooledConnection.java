package com.kapok;

import lombok.Data;

@Data
public class PooledConnection extends Connection {

    private Connection realConnection;

    private ConnectionPool pool;

    private long checkoutTimestamp;
    private long createdTimestamp;
    private long lastUsedTimestamp;
    private int connectionTypeCode;
    private boolean valid;

    public PooledConnection(Connection realConnection, ConnectionPool pool) {
        this.realConnection = realConnection;
        this.pool = pool;
        this.createdTimestamp = System.currentTimeMillis();
        this.lastUsedTimestamp = System.currentTimeMillis();
        this.valid = true;
    }

    public long getCheckoutTime() {
        return System.currentTimeMillis() - checkoutTimestamp;
    }

    public Connection getRealConnection() {
        return realConnection;
    }

    public void invalidate() {
        valid = false;
    }

    @Override
    public void connect() {
        if (valid) {
            setLastUsedTimestamp(System.currentTimeMillis());
            super.connect();
        }
    }

    @Override
    public void close() {
        pool.pushConnection(this);
    }
}
