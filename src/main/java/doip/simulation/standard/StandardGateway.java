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

import doip.logging.LogManager;
import doip.logging.Logger;
import doip.simulation.nodes.Ecu;
import doip.simulation.nodes.EcuConfig;
import doip.simulation.nodes.EcuListener;
import doip.simulation.nodes.Gateway;
import doip.simulation.nodes.GatewayConfig;
import doip.library.comm.DoipTcpConnection;
import doip.library.comm.DoipTcpConnectionListener;
import doip.library.comm.DoipUdpMessageHandler;
import doip.library.comm.DoipUdpMessageHandlerListener;
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
import doip.library.util.Conversion;
import doip.library.util.Helper;

public class StandardGateway
		implements Gateway, TcpServerListener, DoipTcpConnectionListener, EcuListener, DoipUdpMessageHandlerListener {

	private static Logger logger = LogManager.getLogger(StandardGateway.class);

	private GatewayConfig config = null;

	private MulticastSocket udpSocket = null;

	private ServerSocket tcpSocket = null;

	private DoipUdpMessageHandler doipUdpMessageHandler = null;

	private TcpServerThread tcpServerThread = null;

	private LinkedList<StandardTcpConnectionGateway> standardConnectionList = new LinkedList<StandardTcpConnectionGateway>();

	private LinkedList<Ecu> standardEcuList = new LinkedList<Ecu>();

	private int connectionInstanceCounter = 0;

	public StandardGateway(GatewayConfig config) {
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
		
		this.config = config;
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
				config.getName() + ":TCP-RECV-" + this.connectionInstanceCounter, config.getMaxByteArraySizeLogging());

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
		logger.trace(">>> void onConnectionAccepted(Socket socket)");
		
		try {
			socket.setTcpNoDelay(true);
		} catch (SocketException e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		
		StandardTcpConnectionGateway standardConnection = createConnection();
		standardConnection.addListener(this);
		this.standardConnectionList.add(standardConnection);
		standardConnection.start(socket);
		logger.trace("<<< void onConnectionAccepted(Socket socket)");
	}

	@Override
	public void onConnectionClosed(DoipTcpConnection doipTcpConnection) {
		logger.trace(">>> public void onConnectionClosed(DoipTcpConnection doipTcpConnection)");
		this.standardConnectionList.remove(doipTcpConnection);
		logger.trace("<<< public void onConnectionClosed(DoipTcpConnection doipTcpConnection)");
	}

	@Override
	public void onDoipTcpAliveCheckRequest(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckRequest doipMessage) {
		logger.trace(
				">>> public void onDoipTcpAliveCheckRequest(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckRequest doipMessage)");
		logger.warn("No implementation");
		// TODO
		logger.trace(
				"<<< public void onDoipTcpAliveCheckRequest(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckRequest doipMessage)");
	}

	@Override
	public void onDoipTcpAliveCheckResponse(DoipTcpConnection doipTcpConnection,
			DoipTcpAliveCheckResponse doipMessage) {
		logger.trace(
				">>> public void onDoipTcpAliveCheckResponse(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckResponse doipMessage)");
		logger.warn("No implementation");

		StandardTcpConnectionGateway standardConnection = (StandardTcpConnectionGateway) doipTcpConnection;
		standardConnection.setLastDoipTcpAliveCheckResponse(doipMessage);

		logger.trace(
				"<<< public void onDoipTcpAliveCheckResponse(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckResponse doipMessage)");
	}

	/**
	 * Will be called when a DoIP diagnostic message had been received
	 */
	@Override
	public void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> void onDoipTcpDiagnosticMessage(" + "DoipTcpConnection doipTcpConnection, "
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

		// Iterate over all ECUs and find the ECU by the given
		// target address.
		Ecu targetEcu = null;
		Iterator<Ecu> iter = this.standardEcuList.iterator();
		while (iter.hasNext()) {
			Ecu tmpEcu = iter.next();
			if (tmpEcu.getConfig().getPhysicalAddress() == target) {
				if (logger.isInfoEnabled()) {
					logger.info("Found ECU to handle diagnostic message");
				}
				targetEcu = tmpEcu;
				break;
			}
		}

		// [DoIP-071] If target address is unknown then send negative acknowledgement
		// with code 0x03.
		if (targetEcu == null) {
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
		UdsMessage request = new UdsMessage(source, target, UdsMessage.PHYSICAL, diagnosticMessage);
		targetEcu.putRequest(request);

		if (logger.isTraceEnabled()) {
			logger.trace(
					"<<< void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage)");
		}
	}

	@Override
	public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessageNegAck doipMessage) {
		logger.trace(
				">>> public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessageNegAck doipMessage)");
		// TODO
		logger.error("onDoipTcpDiagnosticMessageNegAck not implemented");
		logger.trace(
				"<<< public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessageNegAck doipMessage)");
	}

	@Override
	public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessagePosAck doipMessage) {
		logger.trace(
				">>> public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessagePosAck doipMessage)");
		// TODO
		logger.error("onDoipTcpDiagnosticMessagePosAck not implemented");
		logger.trace(
				"<<< public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessagePosAck doipMessage)");
	}

	@Override
	public void onDoipTcpHeaderNegAck(DoipTcpConnection doipTcpConnection, DoipTcpHeaderNegAck doipMessage) {
		logger.trace(">>> public void onDoipHeaderNegAck("
				+ "DoipTcpConnection doipTcpConnection, DoipHeaderNegAck doipMessage)");
		// TODO
		logger.warn("No implementation");
		logger.trace("<<< public void onDoipHeaderNegAck("
				+ "DoipTcpConnection doipTcpConnection, DoipHeaderNegAck doipMessage)");
	}

	/**
	 * [DoIP-100] The DoIP entity shall process routing activation as specified
	 * in figure 9.
	 * Implements the routing activation handler according to figure 9 on page 31 of
	 * the ISO 13400-2:2012.
	 */
	@Override
	public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationRequest doipMessage) {
		if (logger.isTraceEnabled()) {
			logger.trace(
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
		int responseCode = routingActivationSocketHandler(standardConnection, doipMessage);

		if (logger.isDebugEnabled()) {
			logger.debug("Result from routing activation socket handler was " + responseCode);
		}

		DoipTcpRoutingActivationResponse doipResponse = new DoipTcpRoutingActivationResponse(source,
				this.config.getLogicalAddress(), responseCode, 0);
		doipTcpConnection.send(doipResponse);

		if (responseCode != 0x10) {
			if (logger.isDebugEnabled()) {
				logger.debug("Close socket because result from socket handler was not 0x10");
			}
			doipTcpConnection.stop();
		}

		if (logger.isTraceEnabled()) {
			logger.trace(
					"<<< public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationRequest doipMessage)");
		}
	}

	/**
	 * @param connection
	 * @param routingActivationRequest
	 * @return
	 */
	private int routingActivationSocketHandler(StandardTcpConnectionGateway connection,
			DoipTcpRoutingActivationRequest routingActivationRequest) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> private int routingActivationSocketHandler(StandardTcpConnection connection, DoipTcpRoutingActivationRequest routingActivationRequest)");
		}
		
		
		int source = routingActivationRequest.getSourceAddress();
		int count = getNumberOfRegisteredSockets();
		if (count == 0) {
			connection.setRegisteredSourceAddress(source);
			connection.setState(StandardTcpConnectionGateway.STATE_REGISTERED_ROUTING_ACTIVE);	
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
		connection.setState(StandardTcpConnectionGateway.STATE_REGISTERED_ROUTING_ACTIVE);
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< private int routingActivationSocketHandler(StandardTcpConnection connection, DoipTcpRoutingActivationRequest routingActivationRequest)");
		}
		return 0x10;
	}

	/**
	 * Returns the connection on which the source address given by parameter
	 * 'source' is registered.
	 * 
	 * @param source
	 * @return
	 */
	public StandardTcpConnectionGateway getRegisteredConnection(int source) {
		Iterator<StandardTcpConnectionGateway> iter = this.standardConnectionList.iterator();
		while (iter.hasNext()) {
			StandardTcpConnectionGateway connection = iter.next();
			if (connection.getRegisteredSourceAddress() == source) {
				return connection;
			}
		}
		return null;
	}

	/**
	 * Returns the number of registered sockets
	 * 
	 * @return The number of registered sockets
	 */
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
	}

	@Override
	public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationResponse doipMessage) {
		logger.trace(
				">>> public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationResponse doipMessage)");
		logger.warn("No implementation");
		logger.trace(
				"<<< public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection, DoipTcpRoutingActivationResponse doipMessage)");
	}

	@Override
	public void onDoipUdpDiagnosticPowerModeRequest(DoipUdpDiagnosticPowerModeRequest doipMessage,
			DatagramPacket packet) {
		logger.trace(
				">>> void processDoipUdpDiagnosticPowerModeRequest(DoipUdpDiagnosticPowerModeRequest doipMessage, DatagramPacket packet)");
		DoipUdpDiagnosticPowerModeResponse doipResponse = new DoipUdpDiagnosticPowerModeResponse(0);
		byte[] response = doipResponse.getMessage();
		try {
			this.sendDatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
		} catch (IOException e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		logger.trace(
				"<<< void processDoipUdpDiagnosticPowerModeRequest(DoipUdpDiagnosticPowerModeRequest doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpDiagnosticPowerModeResponse(DoipUdpDiagnosticPowerModeResponse doipMessage,
			DatagramPacket packet) {
		logger.trace(
				">>> void processDoipUdpDiagnosticPowerModeResponse(DoipUdpDiagnosticPowerModeResponse doipMessage, DatagramPacket packet)");
		logger.warn("Received unexpected diagnostic power mode response");
		logger.trace(
				"<<< void processDoipUdpDiagnosticPowerModeResponse(DoipUdpDiagnosticPowerModeResponse doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpEntityStatusRequest(DoipUdpEntityStatusRequest doipMessage, DatagramPacket packet) {
		logger.trace(
				">>> void processDoipUdpEntityStatusRequest(DoipUdpEntityStatusRequest doipMessage, DatagramPacket packet)");
		DoipUdpEntityStatusResponse doipResponse = new DoipUdpEntityStatusResponse(0, 255,
				this.standardConnectionList.size(), 65536);
		byte[] response = doipResponse.getMessage();
		try {
			this.sendDatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
		} catch (IOException e) {
			logger.error(Helper.getExceptionAsString(e));
		}

		logger.trace(
				"<<< void processDoipUdpEntityStatusRequest(DoipUdpEntityStatusRequest doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpEntityStatusResponse(DoipUdpEntityStatusResponse doipMessage, DatagramPacket packet) {
		logger.trace(
				">>> void processDoipUdpEntityStatusResponse(DoipUdpEntityStatusResponse doipMessage, DatagramPacket packet)");
		// Nothing to do
		logger.trace(
				"<<< void processDoipUdpEntityStatusResponse(DoipUdpEntityStatusResponse doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpHeaderNegAck(DoipUdpHeaderNegAck doipMessage, DatagramPacket packet) {
		logger.trace(">>> void processDoipHeaderNegAck(DoipHeaderNegAck doipMessage, DatagramPacket packet)");
		if (logger.isDebugEnabled()) {
			logger.debug("Received DoIP header negative acknowledge, message will be discarged");
		}
		logger.trace("<<< void processDoipHeaderNegAck(DoipHeaderNegAck doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpVehicleAnnouncementMessage(DoipUdpVehicleAnnouncementMessage doipMessage,
			DatagramPacket packet) {
		logger.trace(
				">>> void processDoipUdpVehicleAnnouncementMessage(DoipUdpVehicleAnnouncementMessage doipMessage, DatagramPacket packet)");
		if (logger.isDebugEnabled()) {
			logger.debug("Received DoIP vehicle announcement message, message will be discarged");
		}
		logger.trace(
				"<<< void processDoipUdpVehicleAnnouncementMessage(DoipUdpVehicleAnnouncementMessage doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpVehicleIdentRequest(DoipUdpVehicleIdentRequest doipMessage, DatagramPacket packet) {
		logger.trace(
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
		logger.trace(
				"<<< public void onDoipUdpVehicleIdentRequest(DoipUdpVehicleIdentRequest doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpVehicleIdentRequestWithEid(DoipUdpVehicleIdentRequestWithEid doipMessage,
			DatagramPacket packet) {
		logger.trace(
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
			

		logger.trace(
				"<<< public void onDoipUdpVehicleIdentRequestWithEid(DoipUdpVehicleIdentRequestWithEid doipMessage, DatagramPacket packet)");
	}

	@Override
	public void onDoipUdpVehicleIdentRequestWithVin(DoipUdpVehicleIdentRequestWithVin doipMessage,
			DatagramPacket packet) {
		logger.trace(
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

		logger.trace(
				"<<< public void onDoipUdpVehicleIdentRequestWithVin(DoipUdpVehicleIdentRequestWithVin doipMessage, DatagramPacket packet)");
	}

	/**
	 * Loads all ECU configurations and create for each configuration a new instance
	 * of an ECU.
	 */
	public void prepareEcus() {
		logger.trace(">>> public void prepareEcus()");
		LinkedList<EcuConfig> ecuConfigList = this.config.getEcuConfigList();
		Iterator<EcuConfig> iter = ecuConfigList.iterator();
		while (iter.hasNext()) {
			EcuConfig ecuConfig = iter.next();
			Ecu ecu = this.createEcu(ecuConfig);
			ecu.addListener(this);
			this.standardEcuList.add(ecu);
		}
		logger.trace("<<< public void prepareEcus()");
	}

	/**
	 * Creates a new StandardEcu. This function can be overridden if a different
	 * implementation of an ECU shall be used.
	 * 
	 * @param config The configuration of the ECU
	 * @return A new instance of the ECU
	 */
	public Ecu createEcu(EcuConfig config) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public StandardEcu createEcu(EcuConfig config)");
		}

		StandardEcu ecu = new StandardEcu(config);

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public StandardEcu createEcu(EcuConfig config)");
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
		boolean found = false;
		Iterator<StandardTcpConnectionGateway> iter = this.standardConnectionList.iterator();
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
			if (logger.isErrorEnabled()) {
				logger.error("Could not find TCP connection to send out DoIP message "
						+ "because target address of the diagnostic message does not "
						+ "match and source address in the available TCP connections.");
			}
		}

		logger.trace("<<< public void sendUdsMessage(UdsMessage message)");
	}

	public DoipUdpMessageHandler createDoipUdpMessageHandler(String name) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public DoipUdpMessageHandler createDoipUdpMessageHandler(String name)");
		}

		DoipUdpMessageHandler handler = new DoipUdpMessageHandler(name, config.getUdpLookupTable());

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public DoipUdpMessageHandler createDoipUdpMessageHandler(String name)");
		}
		return handler;
	}

	/**
	 * Starts the gateway thread.
	 */
	public void start() throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void start()");
		}
		try {
			logger.debug("Create UDP socket");
			this.udpSocket = Helper.createUdpSocket(config.getLocalAddress(), config.getLocalPort(),
					config.getMulticastAddress());
			logger.debug("Create TCP server socket");
			this.tcpSocket = Helper.createTcpServerSocket(config.getLocalAddress(), config.getLocalPort());

			logger.debug("Pepare UDP message handler");
			this.doipUdpMessageHandler = createDoipUdpMessageHandler(config.getName() + ":UDP-RECV");
			this.doipUdpMessageHandler.addListener(this);

			logger.debug("Prepare TCP server thread");
			this.tcpServerThread = new TcpServerThread(config.getName() + ":TCP-SERV");
			this.tcpServerThread.addListener(this);

			logger.debug("Prepare ECUs");
			this.prepareEcus();
			logger.debug("Start ECUs");
			this.startEcus();

			logger.debug("Start UDP interpreter thread");
			this.doipUdpMessageHandler.start(this.udpSocket);

			logger.debug("Start TCP receiver thread");
			this.tcpServerThread.start(this.tcpSocket);
			// Thread.sleep(1);

		} catch (IOException e) {
			logger.error(Helper.getExceptionAsString(e));
			throw e;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void start()");
		}
	}

	public void startEcus() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void startEcus()");
		}
		Iterator<Ecu> iter = this.standardEcuList.iterator();
		while (iter.hasNext()) {
			Ecu ecu = iter.next();
			ecu.start();
		}
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void startEcus()");
		}
	}

	/**
	 * Stops the gateway thread
	 */
	public void stop() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> void stop()");
		}

		logger.debug("Stop UDP interpreter");
		this.doipUdpMessageHandler.stop();

		logger.debug("Stop TCP server thread");
		this.tcpServerThread.stop();

		logger.debug("Stop TCP connections");
		this.stopConnections();

		logger.debug("Stop ECUs");
		this.stopEcus();

		logger.debug("Unprepare ECUs");
		this.unprepareEcus();

		logger.debug("Unprepare TCP server thread");
		this.tcpServerThread.removeListener(this);
		this.tcpServerThread = null;

		logger.debug("Unprepare UDP receiver thread");
		this.doipUdpMessageHandler.removeListener(this);
		this.doipUdpMessageHandler = null;

		if (logger.isTraceEnabled()) {
			logger.trace("<<< void stop()");
		}
	}

	public void stopConnections() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void stopConnections()");
		}
		Iterator<StandardTcpConnectionGateway> iter = this.standardConnectionList.iterator();
		while (iter.hasNext()) {
			StandardTcpConnectionGateway connection = iter.next();
			connection.stop();
		}
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void stopConnections()");
		}
	}

	public void stopEcus() {
		logger.trace(">>> public void stopEcus()");
		Iterator<Ecu> iter = this.standardEcuList.iterator();
		while (iter.hasNext()) {
			Ecu ecu = iter.next();
			ecu.stop();
		}
		logger.trace("<<< public void stopEcus()");
	}

	public void unprepareEcus() {
		logger.trace(">>> public void unprepareEcus()");
		Iterator<Ecu> iter = this.standardEcuList.iterator();
		while (iter.hasNext()) {
			Ecu ecu = iter.next();
			ecu.removeListener(this);
		}
		this.standardEcuList.clear();
		logger.trace("<<< public void unprepareEcus()");
	}
}
