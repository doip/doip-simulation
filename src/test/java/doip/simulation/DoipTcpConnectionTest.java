package doip.simulation;

import java.net.Socket;
import java.util.LinkedList;

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
import doip.logging.LogManager;
import doip.logging.Logger;

/**
 * Helper class to test DoipTcpConnection
 */
public class DoipTcpConnectionTest implements DoipTcpConnectionListener {
	
	private static Logger logger = LogManager.getLogger(DoipTcpConnectionTest.class);
	
	private DoipTcpConnection doipTcpConnection = null;
	
	private LinkedList<DoipTcpConnectionTestListener> listeners = new LinkedList<DoipTcpConnectionTestListener>();
	
	private int onConnectionClosedCounter = 0;
	
	private int onDoipTcpDiagnosticMessageCounter = 0;

	private int onDoipTcpDiagnosticMessagePosAckCounter = 0;
	
	private int onDoipTcpDiagnosticMessageNegAckCounter = 0;
	
	private int onDoipTcpRoutingActivationRequestCounter = 0;

	private int onDoipTcpRoutingActivationResponseCounter = 0;
	
	private DoipTcpDiagnosticMessage lastDoipTcpDiagnosticMessage = null;
	
	private DoipTcpDiagnosticMessagePosAck lastDoipTcpDiagnosticMessagePosAck = null;
	
	private DoipTcpDiagnosticMessageNegAck lastDoipTcpDiagnosticMessageNegAck = null;
	
	private DoipTcpRoutingActivationRequest lastDoipTcpRoutingActivationRequest = null;
	
	private DoipTcpRoutingActivationResponse lastDoipTcpRoutingActivationResponse = null;
	

	public void start(Socket socket) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void start(Socket socket)");
		}
		doipTcpConnection = new DoipTcpConnection("TEST-TCP", 256);
		doipTcpConnection.addListener(this);
		doipTcpConnection.start(socket);
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void start(Socket socket)");
		}
	}
	
	public void stop() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void stop()");
		}
		doipTcpConnection.stop();
		doipTcpConnection.removeListener(this);
		doipTcpConnection = null;
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void stop()");
		}
	}
	
	public void reset() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void reset()");
		}
		
		this.onConnectionClosedCounter = 0;
		
		this.onDoipTcpDiagnosticMessageCounter = 0;
		this.onDoipTcpDiagnosticMessagePosAckCounter = 0;
		this.onDoipTcpDiagnosticMessageNegAckCounter = 0;
		
		this.onDoipTcpRoutingActivationRequestCounter = 0;
		this.onDoipTcpRoutingActivationResponseCounter = 0;

		this.lastDoipTcpDiagnosticMessage = null;
		this.lastDoipTcpDiagnosticMessagePosAck = null;
		this.lastDoipTcpDiagnosticMessageNegAck = null;
		this.lastDoipTcpRoutingActivationRequest = null;
		this.lastDoipTcpRoutingActivationResponse = null;

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void reset()");
		}
		
	}
	
	public void addListener(DoipTcpConnectionTestListener listener) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void addListener(DoipTcpConnectionTestListener listener)");
		}
		
		this.listeners.add(listener);
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void addListener(DoipTcpConnectionTestListener listener)");
		}
	}
	
	public void removeListener(DoipTcpConnectionTestListener listener) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void removeListener(DoipTcpConnectionTestListener listener)");
		}
		
		this.listeners.remove(listener);
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void removeListener(DoipTcpConnectionTestListener listener)");
		}
	}
	
	public DoipTcpConnection getDoipTcpConnection() {
		return this.doipTcpConnection;
	}

	@Override
	public void onConnectionClosed(DoipTcpConnection doipTcpConnection) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onConnectionClosed(DoipTcpConnection doipTcpConnection)");
		}
		
		this.onConnectionClosedCounter++;
		for (DoipTcpConnectionTestListener listener : this.listeners) {
			listener.onConnectionClosed();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void onConnectionClosed(DoipTcpConnection doipTcpConnection)");
		}

	}

	@Override
	public void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage)");
		}
		
		this.onDoipTcpDiagnosticMessageCounter++;
		this.lastDoipTcpDiagnosticMessage = doipMessage;
		this.onDoipTcpMessageReceived();
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage)");
		}
		
	}

	@Override
	public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessageNegAck doipMessage) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection,	DoipTcpDiagnosticMessageNegAck doipMessage)");
		}
		
		this.onDoipTcpDiagnosticMessageNegAckCounter++;
		this.lastDoipTcpDiagnosticMessageNegAck = doipMessage;
		this.onDoipTcpMessageReceived();
		
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection,	DoipTcpDiagnosticMessageNegAck doipMessage)");
		}
	}

	@Override
	public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessagePosAck doipMessage) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessagePosAck doipMessage)");
		}
		
		this.onDoipTcpDiagnosticMessagePosAckCounter++;
		this.lastDoipTcpDiagnosticMessagePosAck = doipMessage;
		this.onDoipTcpMessageReceived();
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessagePosAck doipMessage)");
		}
	}

	@Override
	public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationRequest doipMessage) {
			
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationRequest doipMessage)");
		}
		
		this.onDoipTcpRoutingActivationRequestCounter++;
		this.lastDoipTcpRoutingActivationRequest = doipMessage;
		this.onDoipTcpMessageReceived();
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationRequest doipMessage)");
		}
	}

	@Override
	public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationResponse doipMessage) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationResponse doipMessage)");
		}
		
		this.onDoipTcpRoutingActivationResponseCounter++;
		this.lastDoipTcpRoutingActivationResponse = doipMessage;
		this.onDoipTcpMessageReceived();
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationResponse doipMessage)");
		}
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
	
	/**
	 * Will be called when any DoIP TCP message had been received.
	 * The function just calls the listeners of this class.
	 */
	private void onDoipTcpMessageReceived() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> private void onDoipTcpMessageReceived()");
		}
		
		for (DoipTcpConnectionTestListener listener : this.listeners) {
			listener.onDoipTcpMessageReceived();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("<<< private void onDoipTcpMessageReceived()");
		}
 	}
	
	public int getOnConnectionClosedCounter() {
		return this.onConnectionClosedCounter;
	}
	
	public int getOnDoipTcpDiagnosticMessageCounter() {
		return onDoipTcpDiagnosticMessageCounter;
	}
	
	public int getOnDoipTcpDiagnosticMessagePosAckCounter() {
		return onDoipTcpDiagnosticMessagePosAckCounter;
	}
	
	public int getOnDoipTcpDiagnosticMessageNegAckCounter() {
		return onDoipTcpDiagnosticMessageNegAckCounter;
	}
	
	public int getOnDoipTcpRoutingActivationRequestCounter() {
		return this.onDoipTcpRoutingActivationRequestCounter;
	}
	
	public int getOnDoipTcpRoutingActivationResponseCounter() {
		return this.onDoipTcpRoutingActivationResponseCounter;
	}
	
	public DoipTcpDiagnosticMessage getLastDoipTcpDiagnosticMessage() {
		return this.lastDoipTcpDiagnosticMessage;
	}
	
	public DoipTcpDiagnosticMessageNegAck getLastDoipTcpDiagnosticMessageNegAck() {
		return this.lastDoipTcpDiagnosticMessageNegAck;
	}
	
	public DoipTcpDiagnosticMessagePosAck getLastDoipTcpDiagnosticMessagePosAck() {
		return this.lastDoipTcpDiagnosticMessagePosAck;
	}
	
	public DoipTcpRoutingActivationResponse getLastDoipTcpRoutingActivationResponse() {
		return this.lastDoipTcpRoutingActivationResponse;
	}
}
