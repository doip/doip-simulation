package doip.simulation;

public interface DoipTcpConnectionTestListener {

	public void onDoipTcpMessageReceived();
	
	public void onConnectionClosed();
}
