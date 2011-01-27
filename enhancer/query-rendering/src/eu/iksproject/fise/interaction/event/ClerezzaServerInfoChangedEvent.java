package org.apache.stanbol.enhancer.interaction.event;

public class ClerezzaServerInfoChangedEvent extends Event {

    private final String serverHost;
    private final int serverPort;
    private final String username;
    private final String password;

    public ClerezzaServerInfoChangedEvent (String serverHost, int serverPort, String username, String password) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.username = username;
        this.password = password;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword () {
        return password;
    }

}
