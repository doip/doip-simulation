package doip.simulation.standard;

import java.io.IOException;
import java.net.Socket;

import doip.library.comm.DoipTcpConnection;
import doip.library.message.DoipTcpAliveCheckResponse;

/**
 * Extends the DoipTcpConnection by implementing a 'registered source address'
 * which will be used to realize the correct behavior of routing activation.
 * 
 * It also stores the last received alive check response which will be needed to
 * check if a diagnostic tester is still alive.
 * 
 * The class is specific for a DoIP gateway, it can not be used to implement a
 * diagnostic tester.
 * 
 * @author Marco Wehnert
 *
 */
public class StandardTcpConnection extends DoipTcpConnection {

	public static final int STATE_SOCKET_NOT_ASSIGNED = 0;
	public static final int STATE_SOCKET_INITIALIZED = 1;
	public static final int STATE_REGISTERED_PENDING_FOR_AUTHENTICATION = 2;
	public static final int STATE_REGISTERED_PENDING_FOR_CONFIRMATION = 3;
	public static final int STATE_REGISTERED_ROUTING_ACTIVE = 4;
	public static final int STATE_SOCKET_CLOSED = 5;

	private int state = STATE_SOCKET_NOT_ASSIGNED;

	/**
	 * The registered source address. The value -1 means that there is no source
	 * address registered.
	 */
	private int registeredSourceAddress = -1;

	private DoipTcpAliveCheckResponse lastDoipTcpAliveCheckResponse = null;

	public StandardTcpConnection(String tcpReceiverThreadName, int maxByteArraySizeLogging) {
		super(tcpReceiverThreadName, maxByteArraySizeLogging);
	}

	@Override
	public void start(Socket socket) {
		this.state = STATE_SOCKET_INITIALIZED;
		this.registeredSourceAddress = -1;
		super.start(socket);
	}

	@Override
	public void stop() {
		this.state = STATE_SOCKET_CLOSED;
		this.registeredSourceAddress = -1;
		super.stop();
	}

	/**
	 * Returns if the socket is registered, that means a source address has been assigned to it.
	 * @return True if the socket is registered.
	 */
	public boolean isRegistered() {
		if (this.state == STATE_REGISTERED_PENDING_FOR_AUTHENTICATION
				|| this.state == STATE_REGISTERED_PENDING_FOR_CONFIRMATION
				|| this.state == STATE_REGISTERED_ROUTING_ACTIVE) {
			return true;
		}
		return false;
	}

	public int getRegisteredSourceAddress() {
		return registeredSourceAddress;
	}

	public void setRegisteredSourceAddress(int registeredSourceAddress) {
		this.registeredSourceAddress = registeredSourceAddress;
	}

	public DoipTcpAliveCheckResponse getLastDoipTcpAliveCheckResponse() {
		return lastDoipTcpAliveCheckResponse;
	}

	public void setLastDoipTcpAliveCheckResponse(DoipTcpAliveCheckResponse lastDoipTcpAliveCheckResponse) {
		this.lastDoipTcpAliveCheckResponse = lastDoipTcpAliveCheckResponse;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}
}
