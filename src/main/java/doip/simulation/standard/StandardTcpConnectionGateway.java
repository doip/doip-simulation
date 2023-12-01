package doip.simulation.standard;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doip.library.comm.DoipTcpConnection;
import doip.library.message.DoipTcpAliveCheckResponse;
import doip.library.timer.Timer;
import doip.library.timer.TimerListener;
import doip.library.timer.TimerThread;

/**
 * Extends the DoipTcpConnection by implementing a 'registered source address'
 * which will be used to realize the correct behavior of routing activation.
 * 
 * The class is specific for a DoIP gateway, it can not be used to implement a
 * diagnostic tester.
 * 
 * @author Marco Wehnert
 *
 */
public class StandardTcpConnectionGateway extends DoipTcpConnection implements TimerListener {
	
	private static Logger logger = LogManager.getLogger(StandardTcpConnectionGateway.class);

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
	
	private Timer inactivityTimer = null;
	
	private int initialInactivityTime = 0;
	
	private int generalInactivityTime = 0;
	
	/*
	private int timerType = TIMER_TYPE_INITIAL_INACTIVITY;
	
	private static final int TIMER_TYPE_INITIAL_INACTIVITY = 1;
	private static final int TIMER_TYPE_GENERAL_INACTIVITY = 2;
	*/
	
	public StandardTcpConnectionGateway(
				String tcpReceiverThreadName, 
				int maxByteArraySizeLogging, 
				int initialInactivityTime, 
				int generalInactivityTime) {
		super(tcpReceiverThreadName, maxByteArraySizeLogging);
		this.initialInactivityTime = initialInactivityTime;
		this.generalInactivityTime = generalInactivityTime;
	}

	@Override
	public void start(Socket socket) {
		this.state = STATE_SOCKET_INITIALIZED;
		this.registeredSourceAddress = -1;
		super.start(socket);
		inactivityTimer = new TimerThread();
		inactivityTimer.addListener(this);
		inactivityTimer.start(initialInactivityTime, 1);
	}

	@Override
	public void stop() {
		inactivityTimer.stop();
		inactivityTimer.removeListener(this);
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
		this.inactivityTimer.stop();
		this.inactivityTimer.start(this.generalInactivityTime, 1);
		this.registeredSourceAddress = registeredSourceAddress;
		this.state = STATE_REGISTERED_ROUTING_ACTIVE;
	}

	/**
	 * Will be called by initial inactivity timer
	 */
	@Override
	public void onTimerExpired(Timer timer) {
		if (this.isRegistered()) {
			logger.info("Connection will be closed due to general inactivity timer expired. General inactivity time was {} ms.", this.generalInactivityTime);
		} else {
			logger.info("Connection will be closed due to initial inactivity timer expired. Initial inactivity time was {} ms.", this.initialInactivityTime);
		}
		this.stop();
	}
	
	@Override
	public void onDataReceived(byte[] data) {
		if (this.isRegistered()) {
			this.inactivityTimer.stop();
			this.inactivityTimer.start(this.generalInactivityTime, 1);
		}
		super.onDataReceived(data);
	}

	/*
	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}*/
}
