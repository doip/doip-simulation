package doip.simulation.standard;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import doip.simulation.EcuBase;
import doip.simulation.EcuConfig;
import doip.simulation.EcuListener;
import doip.simulation.GatewayConfig;
import doip.simulation.api.Gateway;
import doip.simulation.api.ServiceState;
import doip.library.comm.DoipTcpConnection;
import doip.library.comm.DoipTcpConnectionListener;
import doip.library.comm.DoipUdpMessageHandler;
import doip.library.comm.DoipUdpMessageHandlerListener;
import doip.library.exception.DoipException;
import doip.library.exception.IllegalNullArgument;
import doip.library.message.DoipTcpAliveCheckRequest;
import doip.library.message.DoipTcpAliveCheckResponse;
import doip.library.message.DoipTcpDiagnosticMessage;
import doip.library.message.DoipTcpDiagnosticMessageNegAck;
import doip.library.message.DoipTcpDiagnosticMessagePosAck;
import doip.library.message.DoipTcpHeaderNegAck;
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.message.DoipTcpRoutingActivationResponse;
import doip.library.message.DoipUdpDiagnosticPowerModeRequest;
import doip.library.message.DoipUdpDiagnosticPowerModeResponse;
import doip.library.message.DoipUdpEntityStatusRequest;
import doip.library.message.DoipUdpEntityStatusResponse;
import doip.library.message.DoipUdpHeaderNegAck;
import doip.library.message.DoipUdpVehicleAnnouncementMessage;
import doip.library.message.DoipUdpVehicleIdentRequest;
import doip.library.message.DoipUdpVehicleIdentRequestWithEid;
import doip.library.message.DoipUdpVehicleIdentRequestWithVin;
import doip.library.message.UdsMessage;
import doip.library.net.TcpServer;
import doip.library.net.TcpServerListener;
import doip.library.net.TcpServerThread;
import doip.library.timer.Timer;
import doip.library.timer.TimerListener;
import doip.library.timer.TimerThread;
import doip.library.util.Conversion;
import doip.library.util.Helper;

/**
 * Implements a DoIP gateway according to ISO 13400-2. Typically a real 
 * gateway also provides diagnostic functions like a normal ECU. If so,
 * then the diagnostic functions for this gateway needs to be implemented
 * similar to a normal ECU which is behind the gateway.
 */
public class StandardGateway
		implements Gateway, TcpServerListener, DoipTcpConnectionListener, EcuListener, DoipUdpMessageHandlerListener, TimerListener {

	private static Logger logger = LogManager.getLogger(StandardGateway.class);

	/** Log4j marker for function entry */
	private static Marker enter = MarkerManager.getMarker("ENTER");
	
	/** Log4j marker for function exit */
	private static Marker exit = MarkerManager.getMarker("EXIT");

	/**
	 * Contains the configuration for this gateway
	 */
	private GatewayConfig config = null;

	/**
	 * UDP socket for this gateway
	 */
	private MulticastSocket udpSocket = null;

	/**
	 * TCP server socket on which the gateway will listen for new
	 * TCP connections
	 */
	private ServerSocket tcpServerSocket = null;

	private DoipUdpMessageHandler doipUdpMessageHandler = null;

	/**
	 * The server thread which is waiting for incoming TCP connections
	 */
	private TcpServerThread tcpServerThread = null;

	/**
	 * ConnectionManager who is responsible to handle 
	 * routing activation messages 
	 */
	private ConnectionManager connectionManager;

	/**
	 * List of ECUs which are behind this gateway. To this ECUs the UDS messages 
	 * will be send and the ECUs will send back their responses.
	 */
	private List<EcuBase> ecus = new LinkedList<EcuBase>();

	/**
	 * Every TCP connection (which will be running in a thread) 
	 * gets an own number which will be used for logging the thread name.
	 * This will be helpful in the log output where you can see the thread name.
	 * 
	 */
	private int connectionInstanceCounter = 0;
	
	private Timer timerVam = null;
	
	private ServiceState serviceState = ServiceState.STOPPED;

	public StandardGateway(GatewayConfig config) {
		String method = "public StandardGateway(GatewayConfig config)";

		// Check config for invalid values
		if (config.getName() == null) {
			throw new IllegalArgumentException("The name of the gateway is null");
		}
		if (config.getLocalPort() <= 0) {
			throw new IllegalArgumentException("The local port for DoIP is invalid, it must be greater than 0");
		}
		if (config.getMaxByteArraySizeLogging() < 0) {
			throw new IllegalArgumentException("The value of 'maxByteArraySizeLogging' is negative, it must be greater or equal than 0");
		}
		if (config.getMaxByteArraySizeLookup() < 0) {
			throw new IllegalArgumentException("The value of 'maxByteArraySizeLookup' in class GatewayConfig is negative, it must be greater or equal than 0");
		}
		if (config.getVin() == null) {
			throw new IllegalArgumentException("The VIN isn't defined in the gateway configuration");
		}
		if (config.getVin().length != 17) {
			throw new IllegalArgumentException("The value of 'vin' in the configuration of DoIP server isn't 17 bytes long");
		}
		if (config.getEid() == null) throw logger.throwing(new IllegalNullArgument("config.eid", method));
		if (config.getGid() == null) throw logger.throwing(new IllegalNullArgument("config.gid", method));
		
		if (config.getMaxNumberOfRegisteredConnections() < 1) {
			throw new IllegalArgumentException("The value of 'maxNumberOfRegisteredConnections' is lower than 1");
		}
		
		this.config = config;
		connectionManager = createConnectionManager();
		logger.debug("Prepare ECUs");
		this.prepareEcus();
	}

	@Override
	public ServiceState getState() {
		return serviceState;
	}

	@Override
	public String getName() {
		return config.getName();
	}

	@Override
	public doip.simulation.api.Ecu getEcuByName(String name) {
		for (EcuBase ecu : ecus) {
			if (name.equals(ecu.getName())) {
				return ecu;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<doip.simulation.api.Ecu> getEcus() {
		return (List<doip.simulation.api.Ecu>)(List<?>)ecus;
	}

	public ConnectionManager createConnectionManager() {
		ConnectionManager connectionManager = new ConnectionManager(config.getMaxNumberOfRegisteredConnections());	
		return connectionManager;
	}

	/**
	 * Creates a new StandardConnectionThread. This function can be overridden to
	 * create a different type of connection.
	 * 
	 * @return A StandardConnectionThread
	 */
	public StandardTcpConnectionGateway createConnection() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> StandardConnection createStandardConnection()");
		}
		this.connectionInstanceCounter++;
		StandardTcpConnectionGateway standardConnection = new StandardTcpConnectionGateway(
				config.getName() + ":TCP-RECV-" + this.connectionInstanceCounter, config.getMaxByteArraySizeLogging(), config.getInitialInactivityTime(), config.getGeneralInactivityTime());

		if (logger.isTraceEnabled()) {
			logger.trace("<<< StandardConnection createConnection()");
		}
		return standardConnection;
	}

	/**
	 * Will be called when a new TCP connection has been established. It creates a
	 * new connection thread, adds itself as listener and start the thread.
	 */
	@Override
	public void onConnectionAccepted(TcpServer tcpServer, Socket socket) {
		logger.trace(enter, ">>> void onConnectionAccepted(Socket socket)");
		
		try {
			socket.setTcpNoDelay(true);
		} catch (SocketException e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		
		StandardTcpConnectionGateway standardConnection = createConnection();
		standardConnection.addListener(this);
		connectionManager.addConnection(standardConnection);
		
		standardConnection.start(socket);
		logger.trace(exit, "<<< void onConnectionAccepted(Socket socket)");
	}

	@Override
	public void onConnectionClosed(DoipTcpConnection doipTcpConnection) {
		logger.trace(enter, ">>> public void onConnectionClosed(DoipTcpConnection doipTcpConnection)");
		doipTcpConnection.removeListener(this);
		connectionManager.removeConnection((StandardTcpConnectionGateway) doipTcpConnection);
		//this.standardConnectionList.remove(doipTcpConnection);
		logger.trace(exit, "<<< public void onConnectionClosed(DoipTcpConnection doipTcpConnection)");
	}

	@Override
	public void onDoipTcpAliveCheckRequest(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckRequest doipMessage) {
		logger.trace(enter, 
				">>> public void onDoipTcpAliveCheckRequest(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckRequest doipMessage)");
		logger.warn("No implementation");
		// TODO
		logger.trace(exit,
				"<<< public void onDoipTcpAliveCheckRequest(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckRequest doipMessage)");
	}

	@Override
	public void onDoipTcpAliveCheckResponse(DoipTcpConnection doipTcpConnection,
			DoipTcpAliveCheckResponse doipMessage) {
		logger.trace(enter,
				">>> public void onDoipTcpAliveCheckResponse(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckResponse doipMessage)");
		logger.warn("No implementation");

		logger.trace(exit,
				"<<< public void onDoipTcpAliveCheckResponse(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckResponse doipMessage)");
	}

	/**
	 * Will be called when a DoIP diagnostic message had been received
	 */
	@Override
	public void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage) {
		if (logger.isTraceEnabled()) {
			logger.trace(enter, ">>> void onDoipTcpDiagnosticMessage(" + "DoipTcpConnection doipTcpConnection, "
					+ "DoipTcpDiagnosticMessage doipMessage)");
		}
		// The parameter doipTcpConnection is instance of StandardConnection,
		// because the StandardConnection had been created here or
		// in a lower class. Therefore a type cast is always possible.
		StandardTcpConnectionGateway standardConnection = (StandardTcpConnectionGateway) doipTcpConnection;

		// Get the data out from the parameter "doipMessage".
		int source = doipMessage.getSourceAddress();
		int target = doipMessage.getTargetAddress();
		byte[] diagnosticMessage = doipMessage.getDiagnosticMessage();

		if (logger.isInfoEnabled()) {
			logger.info("UDS-RECV: Source = " + source + ", target = " + target + ", data = "
					+ Conversion.byteArrayToHexString(diagnosticMessage));
		}

		// [DoIP-070] If source address is not activated on the current socket
		// send a negative acknowledgement with code 0x02 and close the socket
		if (source != standardConnection.getRegisteredSourceAddress()) {
			logger.warn("Received a diagnostic message which is not registered at this socket.");
			DoipTcpDiagnosticMessageNegAck negAck = new DoipTcpDiagnosticMessageNegAck(target,
					source, DoipTcpDiagnosticMessageNegAck.NACK_CODE_INVALID_SOURCE_ADDRESS, new byte[] {});
			doipTcpConnection.send(negAck);
			doipTcpConnection.stop();
			if (logger.isTraceEnabled()) {
				logger.trace(
						"<<< void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage)");
			}
			return;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("Source address matches the registered source address");
			logger.debug("Search for ECU which corresponding target address");
		}

		// Iterate over all ECUs and find ECUs which have a 
		// physical or functional address like target address.
		LinkedList<EcuBase> targetEcus = new LinkedList<EcuBase>();
		for (EcuBase tmpEcu : this.ecus) {
			if ((tmpEcu.getConfig().getPhysicalAddress() == target) ||
				 (tmpEcu.getConfig().getFunctionalAddress() == target)) {
				targetEcus.add(tmpEcu);
			}
		}

		// [DoIP-071] If target address is unknown then send negative acknowledgement
		// with code 0x03.
		if (targetEcus.size() == 0) {
			logger.warn("Could not find a ECU with target address " + target);
			DoipTcpDiagnosticMessageNegAck negAck = new DoipTcpDiagnosticMessageNegAck(target,
					source, DoipTcpDiagnosticMessageNegAck.NACK_CODE_UNKNOWN_TARGET_ADDRESS, new byte[] {});
			doipTcpConnection.send(negAck);
			
			if (logger.isTraceEnabled()) {
				logger.trace(
						"<<< void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage)");
			}
			
			return;
		}

		// A target ECU could be found; send the positive acknowledgement
		DoipTcpDiagnosticMessagePosAck posAck = new DoipTcpDiagnosticMessagePosAck(target,
				source, 0x00, new byte[] {});
		doipTcpConnection.send(posAck);
		
		// Send UDS message to ECU
		for (EcuBase tmpEcu : targetEcus) {
			if (tmpEcu.getConfig().getPhysicalAddress() == target) {
				UdsMessage request = new UdsMessage(source, target, UdsMessage.PHYSICAL, diagnosticMessage);
				tmpEcu.putRequest(request);
			} else if (tmpEcu.getConfig().getFunctionalAddress() == target) {
				UdsMessage request = new UdsMessage(source, target, UdsMessage.FUNCTIONAL, diagnosticMessage);
				tmpEcu.putRequest(request);
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace(exit,
					"<<< void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage)");
		}
	}

	@Override
	public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessageNegAck doipMessage) {
		logger.trace(enter,
				">>> public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessageNegAck doipMessage)");
		logger.info("Received DoIP diagnostic message negative acknowledgement, no further action required");
		logger.trace(exit,
				"<<< public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessageNegAck doipMessage)");
	}

	@Override
	public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessagePosAck doipMessage) {
		logger.trace(enter,
				">>> public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessagePosAck doipMessage)");
		logger.info("Received DoIP diagnostic message positive acknowledgement, no further action required");
		logger.trace(exit,
				"<<< public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessagePosAck doipMessage)");
	}

	/**
	 * [DoIP-039] Ignore DoIP header negative acknowledge message.
	 */
	@Override
	public void onDoipTcpHeaderNegAck(DoipTcpConnection doipTcpConnection, DoipTcpHeaderNegAck doipMessage) {
		logger.trace(enter, ">>> public void onDoipHeaderNegAck("
				+ "DoipTcpConnection doipTcpConnection, DoipHeaderNegAck doipMessage)");
		
		logger.warn("Received generic DoIP header negative acknowledge, no further action required.");
		
		logger.trace(exit, "<<< public void onDoipHeaderNegAck("
				+ "DoipTcpConnection doipTcpConnection, DoipHeaderNegAck doipMessage)");
	}

	/**
	 * [DoIP-100] The DoIP entity shall process routing activation as specified
	 * in figure 9.
	 * Implements the routing activation handler according to figure 22 on page 64 of
	 * the ISO 13400-2:2019.
	 */
	@Override
	public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationRequest doipMessage) {
		if (logger.isTraceEnabled()) {
			logger.trace(enter,
					">>> public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationRequest doipMessage)");
		}

		StandardTcpConnectionGateway standardConnection = (StandardTcpConnectionGateway) doipTcpConnection;
		int source = doipMessage.getSourceAddress();
		
		// TODO: Figure 9, first box with "Check if source address is know":
		// What is a known source address?
		
		
		// Figure 9, second box:
		// Check if routing activation type is supported
		int activationType = doipMessage.getActivationType();
		if (logger.isDebugEnabled()) {
			logger.debug("Check activation type");
		}
		if (activationType != 0x00 && activationType != 0x01) {
			if (logger.isDebugEnabled()) {
				logger.debug("Routing activation type is not supported");
			}
			DoipTcpRoutingActivationResponse doipResponse = 
					new DoipTcpRoutingActivationResponse(source, this.config.getLogicalAddress(), 0x06, -1);
			standardConnection.send(doipResponse);
			standardConnection.stop();
			if (logger.isTraceEnabled()) {
				logger.trace(
						"<<< public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationRequest doipMessage)");
			}
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Routing activation type is supported");
		}
		
		// Call socket handler which returns the response code.
		// This can be positive result (0x10 = routing activation accepted) 
		// or a negative result.
		//int responseCode = routingActivationSocketHandler(standardConnection, doipMessage);
		// TODO: Call the routing activation socket handler in a background thread
		// to get the reader thread free for other DoIP messages while
		// connection manager performs a alive check
		int responseCode = connectionManager.routingActivationSocketHandler(standardConnection, doipMessage);
		logger.debug(String.format("Response code from socket handler is %02X", responseCode));

		DoipTcpRoutingActivationResponse doipResponse = new DoipTcpRoutingActivationResponse(source,
				this.config.getLogicalAddress(), responseCode, 0);
		doipTcpConnection.send(doipResponse);

		if (responseCode != 0x10 && responseCode != 0x11) {
			if (logger.isDebugEnabled()) {
				logger.debug("Close socket because result from socket handler was not 0x10");
			}
			doipTcpConnection.stop();
			// No need to remove socket from list of connections
			// because that will be done on callback onConnectionClosed
		}

		if (logger.isTraceEnabled()) {
			logger.trace(exit,
					"<<< public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationRequest doipMessage)");
		}
	}

	/**
	 * Implements the routing activation socket handler as figure 26 from ISO 13400-2:2019.
	 * It is NOT the routing activation handler as described in figure 22 from ISO 13400-2:2019.
	 * @param connection
	 * @param routingActivationRequest
	 * @return
	 */
	/*
	private int routingActivationSocketHandler(StandardTcpConnectionGateway connection,
			DoipTcpRoutingActivationRequest routingActivationRequest) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> private int routingActivationSocketHandler(StandardTcpConnection connection, DoipTcpRoutingActivationRequest routingActivationRequest)");
		}
		
		
		int source = routingActivationRequest.getSourceAddress();
		int count = getNumberOfRegisteredSockets();
		if (count == 0) {
			connection.setRegisteredSourceAddress(source);
			//connection.setState(StandardTcpConnectionGateway.STATE_REGISTERED_ROUTING_ACTIVE);	
			if (logger.isTraceEnabled()) {
				logger.trace("<<< private int routingActivationSocketHandler(StandardTcpConnection connection, DoipTcpRoutingActivationRequest routingActivationRequest)");
			}
			return 0x10;
		}

		if (connection.isRegistered()) {
			if (source == connection.getRegisteredSourceAddress()) {
				if (logger.isTraceEnabled()) {
					logger.trace("<<< private int routingActivationSocketHandler(StandardTcpConnection connection, DoipTcpRoutingActivationRequest routingActivationRequest)");
				}
				return 0x10;
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("<<< private int routingActivationSocketHandler(StandardTcpConnection connection, DoipTcpRoutingActivationRequest routingActivationRequest)");
				}
				return 0x02;
			}
		}

		StandardTcpConnectionGateway alreadyRegisteredConnection = getRegisteredConnection(source);
		if (alreadyRegisteredConnection != connection) {
			// TODO: perform alive check
			logger.error("Alive check not implemented");

			if (logger.isTraceEnabled()) {
				logger.trace("<<< private int routingActivationSocketHandler(StandardTcpConnection connection, DoipTcpRoutingActivationRequest routingActivationRequest)");
			}
			return 0x03;
		}

		// TODO: Check if we can accept more connections
		
		// Store source address
		connection.setRegisteredSourceAddress(source);
		
		// Currently no authentication and confirmation is required
		//connection.setState(StandardTcpConnectionGateway.STATE_REGISTERED_ROUTING_ACTIVE);
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< private int routingActivationSocketHandler(StandardTcpConnection connection, DoipTcpRoutingActivationRequest routingActivationRequest)");
		}
		return 0x10;
	}
	*/

	/**
	 * Returns the connection on which the source address given by parameter
	 * 'source' is registered.
	 * 
	 * @param source
	 * @return
	 */
	/*
	public StandardTcpConnectionGateway getRegisteredConnection(int source) {
		Iterator<StandardTcpConnectionGateway> iter = this.standardConnectionList.iterator();
		while (iter.hasNext()) {
			StandardTcpConnectionGateway connection = iter.next();
			if (connection.getRegisteredSourceAddress() == source) {
				return connection;
			}
		}
		return null;
	}*/

	/**
	 * Returns the number of registered sockets
	 * 
	 * @return The number of registered sockets
	 */
	/*
	public int getNumberOfRegisteredSockets() {
		int count = 0;
		Iterator<StandardTcpConnectionGateway> iter = this.standardConnectionList.iterator();
		while (iter.hasNext()) {
			StandardTcpConnectionGateway connection = iter.next();
			if (connection.isRegistered()) {
				count++;
			}
		}
		return count;
	}*/

	@Override
	public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationResponse doipMessage) {
		logger.trace(enter,
				">>> public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationResponse doipMessage)");
		logger.warn("No implementation");
		logger.trace(exit,
				"<<< public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationResponse doipMessage)");
	}

	@Override
	public void onDoipUdpDiagnosticPowerModeRequest(DoipUdpDiagnosticPowerModeRequest doipMessage,
			DatagramPacket packet) {
		logger.trace(enter,
				">>> void processDoipUdpDiagnosticPowerModeRequest(DoipUdpDiagnosticPowerModeRequest doipMessage, DatagramPacket packet)");
		DoipUdpDiagnosticPowerModeResponse doipResponse = new DoipUdpDiagnosticPowerModeResponse(0);
		byte[] response = doipResponse.getMessage();
		try {
			this.sendDatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
		} catch (IOException e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		logger.trace(exit,
				"<<< void processDoipUdpDiagnosticPowerModeRequest(DoipUdpDiagnosticPowerModeRequest doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpDiagnosticPowerModeResponse(DoipUdpDiagnosticPowerModeResponse doipMessage,
			DatagramPacket packet) {
		logger.trace(enter,
				">>> void processDoipUdpDiagnosticPowerModeResponse(DoipUdpDiagnosticPowerModeResponse doipMessage, DatagramPacket packet)");
		logger.warn("Received unexpected diagnostic power mode response");
		logger.trace(exit,
				"<<< void processDoipUdpDiagnosticPowerModeResponse(DoipUdpDiagnosticPowerModeResponse doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpEntityStatusRequest(DoipUdpEntityStatusRequest doipMessage, DatagramPacket packet) {
		logger.trace(enter,
				">>> void processDoipUdpEntityStatusRequest(DoipUdpEntityStatusRequest doipMessage, DatagramPacket packet)");
		DoipUdpEntityStatusResponse doipResponse = new DoipUdpEntityStatusResponse(0, 255,
				this.connectionManager.getNumberOfCurrentConnections() , 65536);
		byte[] response = doipResponse.getMessage();
		try {
			this.sendDatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
		} catch (IOException e) {
			logger.error(Helper.getExceptionAsString(e));
		}

		logger.trace(exit,
				"<<< void processDoipUdpEntityStatusRequest(DoipUdpEntityStatusRequest doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpEntityStatusResponse(DoipUdpEntityStatusResponse doipMessage, DatagramPacket packet) {
		logger.trace(enter,
				">>> void processDoipUdpEntityStatusResponse(DoipUdpEntityStatusResponse doipMessage, DatagramPacket packet)");
		// Nothing to do
		logger.trace(exit,
				"<<< void processDoipUdpEntityStatusResponse(DoipUdpEntityStatusResponse doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpHeaderNegAck(DoipUdpHeaderNegAck doipMessage, DatagramPacket packet) {
		logger.trace(enter, ">>> void processDoipHeaderNegAck(DoipHeaderNegAck doipMessage, DatagramPacket packet)");
		if (logger.isDebugEnabled()) {
			logger.debug("Received DoIP header negative acknowledge, message will be discarged");
		}
		logger.trace(exit, "<<< void processDoipHeaderNegAck(DoipHeaderNegAck doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpVehicleAnnouncementMessage(DoipUdpVehicleAnnouncementMessage doipMessage,
			DatagramPacket packet) {
		logger.trace(enter,
				">>> void processDoipUdpVehicleAnnouncementMessage(DoipUdpVehicleAnnouncementMessage doipMessage, DatagramPacket packet)");
		if (logger.isDebugEnabled()) {
			logger.debug("Received DoIP vehicle announcement message, message will be discarged");
		}
		logger.trace(exit,
				"<<< void processDoipUdpVehicleAnnouncementMessage(DoipUdpVehicleAnnouncementMessage doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpVehicleIdentRequest(DoipUdpVehicleIdentRequest doipMessage, DatagramPacket packet) {
		logger.trace(enter,
				">>> public void onDoipUdpVehicleIdentRequest(DoipUdpVehicleIdentRequest doipMessage, DatagramPacket packet)");
		
		logger.info("Received DoIP UDP vehicle identification request -> will send DoIP UDP vehicle identification response");
		DoipUdpVehicleAnnouncementMessage response = new DoipUdpVehicleAnnouncementMessage(config.getVin(),
				config.getLogicalAddress(), config.getEid(), config.getGid(), 0, 0);
		byte[] message = response.getMessage();
		try {
			this.sendDatagramPacket(message, message.length, packet.getAddress(), packet.getPort());
		} catch (IOException e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		logger.trace(exit,
				"<<< public void onDoipUdpVehicleIdentRequest(DoipUdpVehicleIdentRequest doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpVehicleIdentRequestWithEid(DoipUdpVehicleIdentRequestWithEid doipMessage,
			DatagramPacket packet) {
		logger.trace(enter,
				">>> public void onDoipUdpVehicleIdentRequestWithEid(DoipUdpVehicleIdentRequestWithEid doipMessage, DatagramPacket packet)");
		
		logger.info("Received DoIP UDP vehicle identification request with EID -> will check EID");
		byte[] eid = doipMessage.getEid();
		byte[] ownEid = this.config.getEid();
		String eidAsString = Conversion.byteArrayToHexString(eid);
		String ownEidAsString = Conversion.byteArrayToHexString(ownEid);
		logger.debug("Received EID = " + eidAsString);
		logger.debug("Own EID      = " + ownEidAsString);
		if (Arrays.equals(eid, ownEid)) {
			logger.info("EID matched -> will send DoIP UDP vehicle identification response");
			DoipUdpVehicleAnnouncementMessage response = new DoipUdpVehicleAnnouncementMessage(config.getVin(),
					config.getLogicalAddress(), config.getEid(), config.getGid(), 0, 0);
			byte[] message = response.getMessage();
			try {
				this.sendDatagramPacket(message, message.length, packet.getAddress(), packet.getPort());
			} catch (IOException e) {
				logger.error(Helper.getExceptionAsString(e));
			}
		} else {
			logger.info("EID didn't match -> will not send a DoIP UDP vehicle identification response");
		}
			

		logger.trace(exit,
				"<<< public void onDoipUdpVehicleIdentRequestWithEid(DoipUdpVehicleIdentRequestWithEid doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpVehicleIdentRequestWithVin(DoipUdpVehicleIdentRequestWithVin doipMessage,
			DatagramPacket packet) {
		logger.trace(enter,
				">>> public void onDoipUdpVehicleIdentRequestWithVin(DoipUdpVehicleIdentRequestWithVin doipMessage, DatagramPacket packet)");
		
		logger.info("Received DoIP UDP vehicle identification request with VIN -> will check VIN");
		byte[] vin = doipMessage.getVin();
		byte[] ownVin = this.config.getVin();
		String receivedVinAsString = Conversion.byteArrayToHexString(vin);
		String ownVinAsString = Conversion.byteArrayToHexString(ownVin);
		logger.debug("Received VIN = " + receivedVinAsString);
		logger.debug("Own VIN      = " + ownVinAsString);
		if (Arrays.equals(vin, ownVin)) {
			logger.info("VIN matched -> will send DoIP UDP vehicle identification response");
			DoipUdpVehicleAnnouncementMessage response = new DoipUdpVehicleAnnouncementMessage(config.getVin(),
					config.getLogicalAddress(), config.getEid(), config.getGid(), 0, 0);
			byte[] message = response.getMessage();
			try {
				this.sendDatagramPacket(message, message.length, packet.getAddress(), packet.getPort());
			} catch (IOException e) {
				logger.error(Helper.getExceptionAsString(e));
			}
		} else {
			logger.info("VIN didn't match -> will not send a DoIP UDP vehicle identification response");
		}

		logger.trace(exit,
				"<<< public void onDoipUdpVehicleIdentRequestWithVin(DoipUdpVehicleIdentRequestWithVin doipMessage, DatagramPacket packet)");
	}

	/**
	 * Loads all ECU configurations and create for each configuration a new instance
	 * of an ECU.
	 */
	public void prepareEcus() {
		logger.trace(enter, ">>> public void prepareEcus()");
		LinkedList<EcuConfig> ecuConfigList = this.config.getEcuConfigList();
		Iterator<EcuConfig> iter = ecuConfigList.iterator();
		while (iter.hasNext()) {
			EcuConfig ecuConfig = iter.next();
			EcuBase ecu = this.createEcu(ecuConfig);
			ecu.addListener(this);
			this.ecus.add(ecu);
		}
		logger.trace(exit, "<<< public void prepareEcus()");
	}

	/**
	 * Creates a new StandardEcu. This function can be overridden if a different
	 * implementation of an ECU shall be used.
	 * 
	 * @param config The configuration of the ECU
	 * @return A new instance of the ECU
	 */
	public EcuBase createEcu(EcuConfig config) {
		if (logger.isTraceEnabled()) {
			logger.trace(enter, ">>> public StandardEcu createEcu(EcuConfig config)");
		}

		StandardEcu ecu = new StandardEcu(config);

		if (logger.isTraceEnabled()) {
			logger.trace(exit, "<<< public StandardEcu createEcu(EcuConfig config)");
		}
		return ecu;
	}

	/**
	 * Does nothing. It is supposed to be overwritten in a child class.
	 * 
	 * @param packet The datagram which shall be processed.
	 * @return Returns true if the datagram had been handled.
	 */
	public boolean processDatagramByFunction(DatagramPacket packet) {
		return false;
	}

	/**
	 * Sends a UDP datagram.
	 * 
	 * @param data
	 * @param length
	 * @param target
	 * @param port
	 * @throws IOException
	 */
	public void sendDatagramPacket(byte[] data, int length, InetAddress target, int port) throws IOException {
		logger.trace(">>> void sendDatagramPacket(byte[] data, int length, InetAddress target, int port)");

		DatagramPacket packet = new DatagramPacket(data, length, target, port);
		try {
			logger.info("UDP-SEND: Target = " + target.getHostAddress() + ":" + port + ", Data = "
					+ Conversion.byteArrayToHexString(data));
			this.udpSocket.send(packet);
		} catch (IOException e) {
			logger.error(Helper.getExceptionAsString(e));
			logger.trace(
					"<<< void sendDatagramPacket(byte[] data, int length, InetAddress target, int port) return with IOException");
			throw e;
		}
		logger.trace("<<< void sendDatagramPacket(byte[] data, int length, InetAddress target, int port)");
	}

	@Override
	public void onSendUdsMessage(UdsMessage message) {
		logger.trace(">>> public void sendUdsMessage(UdsMessage message)");

		// Get data out of the parameter "message".
		int source = message.getSourceAdrress();
		int target = message.getTargetAddress();
		byte[] diagnosticMessage = message.getMessage();

		// Find the "StandardConnection" on which the message need to
		// get send out.
		StandardTcpConnectionGateway targetConnection = connectionManager.getConnectionBySourceAddress(target); 
		if (targetConnection != null) {
			DoipTcpDiagnosticMessage doipMessage = new DoipTcpDiagnosticMessage(source, target, diagnosticMessage);
			if (logger.isInfoEnabled()) {
				logger.info("UDS-SEND: Source = " + message.getSourceAdrress() + ", target = "
						+ message.getTargetAddress() + ", data = "
						+ Conversion.byteArrayToHexString(diagnosticMessage));
			}
			targetConnection.send(doipMessage);
		} else {
			if (logger.isErrorEnabled()) {
				logger.error("Could not find TCP connection to send out DoIP message "
						+ "because target address of the diagnostic message does not "
						+ "match and source address in the available TCP connections.");
			}
		}

		/*
		while (iter.hasNext()) {
			StandardTcpConnectionGateway standardConnection = iter.next();
			if (standardConnection.getRegisteredSourceAddress() == target) {
				found = true;
				DoipTcpDiagnosticMessage doipMessage = new DoipTcpDiagnosticMessage(source, target, diagnosticMessage);
				if (logger.isInfoEnabled()) {
					logger.info("UDS-SEND: Source = " + message.getSourceAdrress() + ", target = "
							+ message.getTargetAddress() + ", data = "
							+ Conversion.byteArrayToHexString(diagnosticMessage));
				}
				standardConnection.send(doipMessage);
				break;
			}
		}
		
		if (!found) {
		}*/

		logger.trace("<<< public void sendUdsMessage(UdsMessage message)");
	}

	public DoipUdpMessageHandler createDoipUdpMessageHandler(String name) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public DoipUdpMessageHandler createDoipUdpMessageHandler(String name)");
		}

		DoipUdpMessageHandler handler = new DoipUdpMessageHandler(name, null);

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public DoipUdpMessageHandler createDoipUdpMessageHandler(String name)");
		}
		return handler;
	}

	/**
	 * Starts the gateway thread.
	 */
	public void start() throws DoipException {
		try {
			logger.trace(">>> public void start()");
			this.serviceState = ServiceState.STOPPED;

			logger.debug("Create UDP socket");
			this.udpSocket = Helper.createUdpSocket(config.getLocalAddress(), config.getLocalPort(),
					config.getMulticastAddress());
			this.udpSocket.setBroadcast(true);
			logger.debug("Create TCP server socket");
			this.tcpServerSocket = Helper.createTcpServerSocket(config.getLocalAddress(), config.getLocalPort());

			logger.debug("Pepare UDP message handler");
			this.doipUdpMessageHandler = createDoipUdpMessageHandler(config.getName() + ":UDP-RECV");
			this.doipUdpMessageHandler.addListener(this);

			logger.debug("Prepare TCP server thread");
			this.tcpServerThread = new TcpServerThread(config.getName() + ":TCP-SERV");
			this.tcpServerThread.addListener(this);

			logger.debug("Start ECUs");
			this.startEcus();

			logger.debug("Start UDP interpreter thread");
			this.doipUdpMessageHandler.start(this.udpSocket);

			logger.debug("Start TCP receiver thread");
			this.tcpServerThread.start(this.tcpServerSocket);
			
			if (this.config.getBroadcastEnable() == true) {
				this.timerVam = new TimerThread();
				this.timerVam.addListener(this);
				this.timerVam.start(500, 3);
			}
			
			this.serviceState = ServiceState.RUNNING;
		} catch (IOException e) {
			this.serviceState = ServiceState.ERROR;
			throw logger.throwing(new DoipException(e));
		} finally {
			logger.trace(enter, "<<< public void start()");
		}
	}

	public void startEcus() {
		logger.trace(">>> public void startEcus()");
		Iterator<EcuBase> iter = this.ecus.iterator();
		while (iter.hasNext()) {
			EcuBase ecu = iter.next();
			ecu.start();
		}
		logger.trace("<<< public void startEcus()");
	}

	/**
	 * Stops the gateway thread
	 */
	public void stop() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void stop()");
		}

		
		if (this.doipUdpMessageHandler != null) {
			logger.debug("Stop UDP interpreter");
			this.doipUdpMessageHandler.stop();
		}
		

		if (this.tcpServerThread != null) {
			logger.debug("Stop TCP server thread");
			this.tcpServerThread.stop();
		}

		logger.debug("Stop TCP connections");
		this.stopConnections();

		logger.debug("Stop ECUs");
		this.stopEcus();

		//logger.debug("Unprepare ECUs");
		//this.unprepareEcus();

		if (this.tcpServerThread != null) {
			logger.debug("Unprepare TCP server thread");
			this.tcpServerThread.removeListener(this);
			this.tcpServerThread = null;
		}

		if (this.doipUdpMessageHandler != null) {
			logger.debug("Unprepare UDP receiver thread");
			this.doipUdpMessageHandler.removeListener(this);
			this.doipUdpMessageHandler = null;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void stop()");
		}
	}

	public void stopConnections() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void stopConnections()");
		}
		connectionManager.stopAllConnections();
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void stopConnections()");
		}
	}

	public void stopEcus() {
		logger.trace(">>> public void stopEcus()");
		Iterator<EcuBase> iter = this.ecus.iterator();
		while (iter.hasNext()) {
			EcuBase ecu = iter.next();
			ecu.stop();
		}
		logger.trace("<<< public void stopEcus()");
	}

	/*
	public void unprepareEcus() {
		logger.trace(">>> public void unprepareEcus()");
		Iterator<EcuBase> iter = this.ecus.iterator();
		while (iter.hasNext()) {
			EcuBase ecu = iter.next();
			ecu.removeListener(this);
		}
		this.ecus.clear();
		logger.trace("<<< public void unprepareEcus()");
	}*/

	@Override
	public void onTimerExpired(Timer timer) {
		// The only timer which will be used is the VAM timer.
		// Send VAM
		String method = "public void onTimerExpired(Timer timer)";
		logger.trace(enter, ">>> " + method);
		InetAddress broadcast = config.getBroadcastAddress();
		DoipUdpVehicleAnnouncementMessage response = new DoipUdpVehicleAnnouncementMessage(config.getVin(),
				config.getLogicalAddress(), config.getEid(), config.getGid(), 0, 0);
		byte[] message = response.getMessage();
		try {
			this.sendDatagramPacket(message, message.length, broadcast, 13400);
		} catch (IOException e) {
			logger.error("IOException occured when trying to send vehicle announcement message to broadcast address");
			logger.error(Helper.getExceptionAsString(e));
		};
		logger.trace(exit, "<<< " + method);
	}

	
//-----------------------------------------------------------------------------
// Getter & setter
//-----------------------------------------------------------------------------
	
	/**
	 * Getter for member 'standardConnectionList' 
	 * @return
	 */
	/*public LinkedList<StandardTcpConnectionGateway> getStandardConnectionList() {
		return standardConnectionList;
	}*/
	

	/**
	 * Getter for member 'config'
	 * @return
	 */
	public GatewayConfig getConfig() {
		return this.config;
	}

}
