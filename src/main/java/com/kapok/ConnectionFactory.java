package com.kapok;

public class ConnectionFactory {

    private static volatile ConnectionFactory connectionFactory;

    public static ConnectionFactory getInstance() {
        if (null == connectionFactory) {
            synchronized (ConnectionFactory.class) {
                if (null == connectionFactory) {
                    connectionFactory = new ConnectionFactory();
                }
            }
        }
        return connectionFactory;
    }

    public Connection createConnection() {
        return new Connection();
    }

}
