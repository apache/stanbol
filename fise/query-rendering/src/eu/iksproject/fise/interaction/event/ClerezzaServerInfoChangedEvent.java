package eu.iksproject.fise.interaction.event;

public class ClerezzaServerInfoChangedEvent extends Event {

	private String serverHost;
	private int serverPort;
	private String username;
	private String password;

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
