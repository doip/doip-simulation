package doip.simulation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import doip.junit.Assert;
import doip.library.comm.DoipTcpConnection;
import doip.library.message.DoipMessage;
import doip.library.message.DoipTcpDiagnosticMessage;
import doip.library.message.DoipTcpDiagnosticMessageNegAck;
import doip.library.message.DoipTcpDiagnosticMessagePosAck;
import doip.library.message.DoipTcpMessage;
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.message.DoipTcpRoutingActivationResponse;
import doip.library.message.DoipUdpMessage;
import doip.library.message.DoipUdpVehicleAnnouncementMessage;
import doip.library.message.DoipUdpVehicleIdentRequest;
import doip.library.message.DoipUdpVehicleIdentRequestWithEid;
import doip.library.message.DoipUdpVehicleIdentRequestWithVin;
import doip.library.util.Conversion;
import doip.library.util.Helper;
import doip.logging.LogManager;
import doip.logging.Logger;
import doip.simulation.nodes.Gateway;
import doip.simulation.nodes.GatewayConfig;
import doip.simulation.standard.StandardGateway;

public class TestSimulation implements DoipTcpConnectionTestListener, DoipUdpMessageHandlerTestListener {
	
	private static Logger logger = LogManager.getLogger(TestSimulation.class);
	
	private static Gateway gateway = null;
	
	private static GatewayConfig gatewayConfig = null;
	
	private DatagramSocket udpSocket = null;
	
	private Socket tcpSocket = null;
	
	private DoipUdpMessageHandlerTest doipUdpMessageHandlerTest = null;
	
	private DoipTcpConnectionTest doipTcpConnectionTest = null;
	
	private static InetAddress localhost = null;
	
	/**
	 * Will be set to the current thread which is running the unit tests.
	 * This variable will be used to interrupt the sleep method. 
	 */
	private Thread testThread = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public static void setUpBeforeClass()");

		localhost = InetAddress.getLocalHost();
		
		gatewayConfig = new GatewayConfig();
		gatewayConfig.loadFromFile("src/test/resources/gateway.properties");
		
		gateway = new StandardGateway(gatewayConfig);
		gateway.start();
		
		logger.info("<<< public static void setUpBeforeClass()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public static void tearDownAfterClass()");
		
		if (gateway != null) {
			gateway.stop();
		}
		gateway = null;
		
		logger.info("<<< public static void tearDownAfterClass()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@Before
	public void setUp() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void setUp()");
		
	this.testThread = Thread.currentThread();
		
		udpSocket = new DatagramSocket(13401);
		doipUdpMessageHandlerTest = new DoipUdpMessageHandlerTest();
		doipUdpMessageHandlerTest.addListener(this);
		doipUdpMessageHandlerTest.start(udpSocket);
		
		tcpSocket = new Socket(localhost, 13400);
		doipTcpConnectionTest = new DoipTcpConnectionTest();
		doipTcpConnectionTest.addListener(this);
		doipTcpConnectionTest.start(tcpSocket);
		
		logger.info("<<< public void setUp()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@After
	public void tearDown() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void tearDown()");
		
		if (doipUdpMessageHandlerTest != null) {
			doipUdpMessageHandlerTest.stop();
			doipUdpMessageHandlerTest.removeListener(this);
			doipUdpMessageHandlerTest = null;
		}
		
		if (doipTcpConnectionTest != null) {
			doipTcpConnectionTest.stop();
			doipTcpConnectionTest.removeListener(this);
			doipTcpConnectionTest = null;
		}
		
		logger.info("<<< public void tearDown()");
		logger.info("-----------------------------------------------------------------------------");
	}
	
	/**
	 * [DoIP-051] Test that ECU needs to send a vehicle identification response
	 * in case of reception of a vehicle identification request message
	 * 
	 * @throws IOException
	 */
	@Test
	public void testUdpVehicleIdentRequest() throws IOException {
		logger.info("#############################################################################");
		logger.info(">>> public void testVehicleIdentRequest()");
		
		DoipUdpVehicleIdentRequest doipMessage = new DoipUdpVehicleIdentRequest();
		testUdpVehicleIdentRequest(doipMessage);
		
		logger.info("<<< public void testVehicleIdentRequest()");
		logger.info("#############################################################################");
	}
	
	
	/**
	 * [DoIP-52] Test that ECU needs to send a vehicle identification response
	 * in case of reception of a vehicle identification request message with VIN
	 * 
	 * @throws IOException
	 */
	@Test
	public void testUdpVehicleIdentRequestWithVin() throws IOException {
		logger.info("#############################################################################");
		logger.info(">>> public void testVehicleIdentRequestWithVin()");
		
		byte[] vin = Conversion.asciiStringToByteArray("12345678901234567");
		DoipUdpVehicleIdentRequestWithVin doipMessage = new DoipUdpVehicleIdentRequestWithVin(vin);
		testUdpVehicleIdentRequest(doipMessage);
		
		logger.info("<<< public void testVehicleIdentRequestWithVin()");
		logger.info("#############################################################################");
	}
	
	/**
	 * [DoIP-53] Test that ECU needs to send a vehicle identification response
	 * in case of reception of a vehicle identification request message with EID
	 * 
	 * @throws IOException
	 */
	@Test
	public void testUdpVehicleIdentRequestWithEid() throws IOException {
		logger.info("#############################################################################");
		logger.info(">>> public void testVehicleIdentRequestWithEid()");
	
		byte[] eid = new byte[] {(byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6 };
		DoipUdpVehicleIdentRequestWithEid doipMessage = new DoipUdpVehicleIdentRequestWithEid(eid);
		testUdpVehicleIdentRequest(doipMessage);
		
		logger.info("<<< public void testVehicleIdentRequestWithEid()");
		logger.info("#############################################################################");
	}
	
	@Test
	public void testUdpVehicleIdentRequestWithNotMatchingEid() throws IOException {
		logger.info("#############################################################################");
		logger.info(">>> public void testUdpVehicleIdentRequestWithNotMatchingEid()");

		byte[] eid = new byte[] {(byte) 0x00, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6 };
		DoipUdpVehicleIdentRequestWithEid doipMessage = new DoipUdpVehicleIdentRequestWithEid(eid);
		sendUdpMessage(doipMessage);
		sleep(550);
		
		Assert.assertEquals(0, doipUdpMessageHandlerTest.getOnDoipUdpVehicleAnnouncementMessageCounter());
		DoipUdpVehicleAnnouncementMessage vam = doipUdpMessageHandlerTest.getLastDoipUdpVehicleAnnouncementMessage();
		Assert.assertNull(vam);

		logger.info("<<< public void testUdpVehicleIdentRequestWithNotMatchingEid()");
		logger.info("#############################################################################");
	}
	
	/** 
	 * Helper function to test the vehicle announcement message for all three
	 * vehicle identification request messages.
	 * 
	 * @param doipUdpMessage The vehicle identification request message
	 * @throws IOException Will be thrown if message could not be sent to gateway
	 */
	private void testUdpVehicleIdentRequest(DoipUdpMessage doipUdpMessage) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> private void testUdpVehicleIdentRequest(DoipUdpMessage doipUdpMessage)");
		}
		
		sendUdpMessage(doipUdpMessage);
		sleep(100);
	
		Assert.assertEquals(1, doipUdpMessageHandlerTest.getOnDoipUdpVehicleAnnouncementMessageCounter());
		DoipUdpVehicleAnnouncementMessage vam = doipUdpMessageHandlerTest.getLastDoipUdpVehicleAnnouncementMessage();
		Assert.assertNotNull(vam);
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< private void testUdpVehicleIdentRequest(DoipUdpMessage doipUdpMessage)");
		}
	}
	
	
	/**
	 * [DoIP-70] If source address is not activated on the socket send negative
	 * acknowledgement 0x02 and close socket.
	 * @throws IOException
	 */
	@Test
	public void testTcpNotActivatedSourceAddress() throws IOException {
		logger.info("#############################################################################");
		logger.info(">>> testTcpNotActivatedSourceAddress()");
		
		DoipTcpDiagnosticMessage doipTcpDiagnosticMessage = new DoipTcpDiagnosticMessage(0xF0, 0, new byte[] { 0x10, 0x03 });
		doipTcpConnectionTest.getDoipTcpConnection().send(doipTcpDiagnosticMessage);
		
		sleep(100);
		
		Assert.assertEquals(1, doipTcpConnectionTest.getOnDoipTcpDiagnosticMessageNegAckCounter());
		DoipTcpDiagnosticMessageNegAck negAck = doipTcpConnectionTest.getLastDoipTcpDiagnosticMessageNegAck();
		Assert.assertEquals(DoipTcpDiagnosticMessageNegAck.NACK_CODE_INVALID_SOURCE_ADDRESS, negAck.getAckCode());
		
		if (doipTcpConnectionTest.getOnConnectionClosedCounter() == 0) {
			sleep(100);
		}
		
		Assert.assertEquals(1, doipTcpConnectionTest.getOnConnectionClosedCounter());
		
		logger.info("<<< testTcpNotActivatedSourceAddress()");
		logger.info("#############################################################################");
	}
	
	@Test
	public void testTcpDiagnosticCommunication() {
		logger.info("#############################################################################");
		logger.info(">>> public void testTcpDiagnosticCommunication()");
		
		DoipTcpConnection conn = doipTcpConnectionTest.getDoipTcpConnection();
		
		DoipTcpRoutingActivationRequest doipTcpRoutingActivationRequest = new DoipTcpRoutingActivationRequest(0xF1, 0x00, -1);
		conn.send(doipTcpRoutingActivationRequest);
		sleep(100);
		Assert.assertEquals(1,  doipTcpConnectionTest.getOnDoipTcpRoutingActivationResponseCounter());
		DoipTcpRoutingActivationResponse doipTcpRoutingActivationResponse = 
				doipTcpConnectionTest.getLastDoipTcpRoutingActivationResponse();
		Assert.assertNotNull(doipTcpRoutingActivationResponse);
		Assert.assertEquals(0x10, doipTcpRoutingActivationResponse.getResponseCode());
		
		doipTcpConnectionTest.reset();
	
		DoipTcpDiagnosticMessage doipTcpDiagnosticMessage = new DoipTcpDiagnosticMessage(0xF1, 4711, new byte[] {0x10,0x03});
		conn.send(doipTcpDiagnosticMessage);
		
		sleep(100);
		
		Assert.assertEquals(1, doipTcpConnectionTest.getOnDoipTcpDiagnosticMessagePosAckCounter());
		DoipTcpDiagnosticMessagePosAck doipTcpDiagnosticMessagePosAck = 
				doipTcpConnectionTest.getLastDoipTcpDiagnosticMessagePosAck();
		Assert.assertEquals(4711, doipTcpDiagnosticMessagePosAck.getSourceAddress());
		Assert.assertEquals(0xF1, doipTcpDiagnosticMessagePosAck.getTargetAddress() );
		
		sleep(100);
		Assert.assertEquals(1, doipTcpConnectionTest.getOnDoipTcpDiagnosticMessageCounter());
		doipTcpDiagnosticMessage = doipTcpConnectionTest.getLastDoipTcpDiagnosticMessage();
		Assert.assertEquals(4711, doipTcpDiagnosticMessage.getSourceAddress());
		Assert.assertEquals(0xF1, doipTcpDiagnosticMessage.getTargetAddress());
		byte[] response = doipTcpDiagnosticMessage.getDiagnosticMessage();
		Assert.assertArrayEquals(new byte[] {0x50,  0x03, 0x00, 0x32, (byte) 0x01, (byte) 0xF4 }, response);
		
		logger.info("<<< public void testTcpDiagnosticCommunication()");
		logger.info("#############################################################################");
	}
	
	
	@Test
	public void testRoutingActivation() {
		logger.info("#############################################################################");
		logger.info(">>> public void testRoutingActivation()");
		
		performRoutingActivation();
		
		logger.info("<<< public void testRoutingActivation()");
		logger.info("#############################################################################");
	}
	
	/**
	 * Performing a routing activation will be needed at several tests, so this function
	 * had been extracted from the test function "testRoutingActivation" 
	 */
	public void performRoutingActivation() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void performRoutingActivation()");
		}
		DoipTcpRoutingActivationRequest doipMessage = new DoipTcpRoutingActivationRequest(0xE0, 0x00, 0);
		doipTcpConnectionTest.getDoipTcpConnection().send(doipMessage);
		sleep(100);
		Assert.assertEquals(1, doipTcpConnectionTest.getOnDoipTcpRoutingActivationResponseCounter());
		DoipTcpRoutingActivationResponse doipResponse = doipTcpConnectionTest.getLastDoipTcpRoutingActivationResponse();
		Assert.assertNotNull(doipResponse);
		Assert.assertEquals(0x10, doipResponse.getResponseCode());

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void performRoutingActivation()");
		}
	}
	
	/**
	 * [DoIP-149] If source address is different than the already registered source
	 * address then a response with code 0x02 shall be send. 
	 */
	@Test
	public void testRoutingActivationTwice() {
		logger.info("#############################################################################");
		logger.info(">>> public void testRoutingActivationTwice()");
		
		performRoutingActivation();
		
		doipTcpConnectionTest.reset();
		
		DoipTcpRoutingActivationRequest doipMessage = new DoipTcpRoutingActivationRequest(0xE1, 0X00, 0);
		doipTcpConnectionTest.getDoipTcpConnection().send(doipMessage);
		sleep(100);
		Assert.assertEquals(1, doipTcpConnectionTest.getOnDoipTcpRoutingActivationResponseCounter());
		DoipTcpRoutingActivationResponse doipResponse = doipTcpConnectionTest.getLastDoipTcpRoutingActivationResponse();
		Assert.assertNotNull(doipResponse);
		Assert.assertEquals(0x02, doipResponse.getResponseCode());

	
		logger.info("<<< public void testRoutingActivationTwice()");
		logger.info("#############################################################################");
	}
	
	/**
	 * Sends a UDP message to localhost to port 13400
	 * 
	 * @param doipUdpMessage The DoIP UDP message to send
	 * @throws IOException Will be thrown if UDP message could not be send
	 */
	private void sendUdpMessage(DoipUdpMessage doipUdpMessage) throws IOException {
		byte[] message = doipUdpMessage.getMessage();
		int length = message.length;
		DatagramPacket packet = new DatagramPacket(message, length, localhost, 13400);
		udpSocket.send(packet);
	}
	
	/**
	 * It's just a wrapper function for Thread.sleep(millis)
	 * 
	 * @param millis Time to sleep in milliseconds
	 */
	private void sleep(int millis) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> private void sleep(int millis)");
		}
		
		logger.debug("Thread will go to sleep for " + millis + " milliseconds");
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.info("Sleep in thread with name \"" + Thread.currentThread().getName() + "\" has been interrupted");
			logger.info(Helper.getExceptionAsString(e));
		}
		
		if (logger.isTraceEnabled()) {
			logger.trace("<<< private void sleep(int millis)");
		}
	}

	/**
	 * Will be called when a DoIP TCP message has been received
	 * It is a callback from the class DoipTcpConnectionTest.
	 */
	@Override
	public void onDoipTcpMessageReceived() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onDoipTcpMessageReceived()");
		}

		// Wake up test thread while it is sleeping
		interruptTestThread();

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void onDoipTcpMessageReceived()");
		}
	}
	
	/**
	 * Will be called when a TCP connection had been closed.
	 * It is a callback from the class DoipTcpConnectionTest.
	 */
	@Override
	public void onConnectionClosed() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onConnectionClosed()");
		}
		
		// Wake up test thread while it is sleeping
		interruptTestThread();
		
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onConnectionClosed()");
		}
	}

	/**
	 * Will be called when a DoIP UDP message had been received.
	 * The function is a callback from the class DoipUdpMessageHandlerTest.
	 */
	@Override
	public void onDoipUdpMessageReceived() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onDoipUdpMessageReceived()");
		}
		
		// Wake up test thread while it is sleeping
		interruptTestThread();
		
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onDoipUdpMessageReceived()");
		}
	}
	
	/**
	 * Helper function to log interruption of test thread and which thread caused the interrupt
	 */
	private void interruptTestThread() {
		this.testThread.interrupt();
		logger.debug("Interrupt test thread with name " + this.testThread.getName() + " from thread with name " + Thread.currentThread().getName());
	}
}
