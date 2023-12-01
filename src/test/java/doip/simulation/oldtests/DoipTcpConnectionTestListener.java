package doip.simulation.oldtests;

public interface DoipTcpConnectionTestListener {

	public void onDoipTcpMessageReceived();
	
	public void onConnectionClosed();
}
