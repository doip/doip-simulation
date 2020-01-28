package doip.tester;

public interface DoipTcpConnectionTestListener {

	public void onDoipTcpMessageReceived();
	
	public void onConnectionClosed();
}
