package doip.simulation;

import java.net.Socket;

import doip.library.comm.DoipTcpConnection;
import doip.library.comm.DoipTcpConnectionListener;
import doip.library.message.DoipTcpAliveCheckRequest;
import doip.library.message.DoipTcpAliveCheckResponse;
import doip.library.message.DoipTcpDiagnosticMessage;
import doip.library.message.DoipTcpDiagnosticMessageNegAck;
import doip.library.message.DoipTcpDiagnosticMessagePosAck;
import doip.library.message.DoipTcpHeaderNegAck;
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.message.DoipTcpRoutingActivationResponse;

/**
 * Helper class to test DoipTcpConnection
 */
public class DoipTcpConnectionTest implements DoipTcpConnectionListener {
	
	private DoipTcpConnection doipTcpConnection = null;
	
	public void start(Socket socket) {
		doipTcpConnection = new DoipTcpConnection("TEST-TCP", 256);
		doipTcpConnection.addListener(this);
		doipTcpConnection.start(socket);
	}
	
	public void stop() {
		doipTcpConnection.stop();
		doipTcpConnection.removeListener(this);
		doipTcpConnection = null;
	}
	
	public void reset() {
		
	}
	
	public DoipTcpConnection getDoipTcpConnection() {
		return this.doipTcpConnection;
	}

	@Override
	public void onConnectionClosed(DoipTcpConnection doipTcpConnection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessageNegAck doipMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessagePosAck doipMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationRequest doipMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationResponse doipMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipTcpAliveCheckRequest(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckRequest doipMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipTcpAliveCheckResponse(DoipTcpConnection doipTcpConnection,
			DoipTcpAliveCheckResponse doipMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipTcpHeaderNegAck(DoipTcpConnection doipTcpConnection, DoipTcpHeaderNegAck doipMessage) {
		// TODO Auto-generated method stub
		
	}
}
