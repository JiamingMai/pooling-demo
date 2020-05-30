package com.kapok;

import org.junit.jupiter.api.Test;

public class TestConnectionPool {

    private final int CONNECTION_NUM = 10;

    @Test
    public void testConnectionPool() {
        ConnectionFactory connectionFactory = ConnectionFactory.getInstance();
        for (int i = 0; i < CONNECTION_NUM; i++) {
            Connection connection = connectionFactory.createConnection();
            connection.connect();
            connection.close();
        }

        ConnectionPool connectionPool = new ConnectionPool(connectionFactory);
        for (int i = 0; i < CONNECTION_NUM; i++) {
            Connection connection = connectionPool.popConnection();
            connection.connect();
        }
        connectionPool.forceCloseAll();
    }

}
