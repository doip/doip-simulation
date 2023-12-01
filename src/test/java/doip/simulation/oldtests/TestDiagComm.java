package doip.simulation.oldtests;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.starcode88.jtest.Assertions.*;
import doip.library.comm.DoipTcpConnection;
import doip.library.message.DoipTcpDiagnosticMessage;
import doip.library.message.DoipTcpDiagnosticMessagePosAck;
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.properties.EmptyPropertyValue;
import doip.library.properties.MissingProperty;
import doip.library.timer.NanoTimer;
import doip.library.util.Conversion;
import doip.library.util.Helper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import doip.simulation.api.Gateway;
import doip.simulation.nodes.GatewayConfig;
import doip.simulation.standard.StandardGateway;

public class TestDiagComm implements DoipTcpConnectionTestListener {
	
	private static Logger logger = LogManager.getLogger(TestDiagComm.class);
	
	private static Gateway gateway = null;
	
	private static GatewayConfig gatewayConfig = null;
	
	private static InetAddress localhost = null;
	
	DoipTcpConnectionTest connTest = null;
	
	Socket tcpSocket = null;
	
	private volatile int messageCounter = 0;
	
	private static final int testerAddress = 0xFF00;
	
	private static final int ecuAddress = 4711;

	@BeforeAll
	public static void setUpBeforeClass() {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public static void setUpBeforeClass()");

		sleep(10);
		
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			logger.error("Unexpected " + e.getClass().getName() + " in setUpBeforeClass()");
			logger.error(Helper.getExceptionAsString(e));
			fail("Could not get address for local host");
		}
		
		gatewayConfig = new GatewayConfig();
		
		try {
			gatewayConfig.loadFromFile("src/test/resources/gateway.properties");
		} catch (IOException | MissingProperty | EmptyPropertyValue e) {
			logger.error("Unexpected " + e.getClass().getName() + " in setUpBeforeClass()");
			logger.error(Helper.getExceptionAsString(e));
			fail("Error during reading file file src/test/resources/gateway.properties");
		}
		
		gateway = new StandardGateway(gatewayConfig);
		
		try {
			gateway.start();
		} catch (IOException e) {
			logger.error("Unexpected " + e.getClass().getName() + " in setUpBeforeClass()");
			logger.error(Helper.getExceptionAsString(e));
			fail("Failed to start gateway");
		}
		
		sleep(10);
		
		logger.info("<<< public static void setUpBeforeClass()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public static void tearDownAfterClass()");
		
		sleep(10);
		
		if (gateway != null) {
			gateway.stop();
		}
		gateway = null;
		
		sleep(10);
		
		logger.info("<<< public static void tearDownAfterClass()");
		logger.info("-----------------------------------------------------------------------------");
	}
	
	@BeforeEach
	public void setUp() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void setUp()");
		
		sleep(10);
		
		connTest = new DoipTcpConnectionTest();
		connTest.addListener(this);
		
		tcpSocket = new Socket(localhost, 13400);
		assertNotNull(tcpSocket, "Could not create a TCP connection to localhost port 13400");
		connTest.start(tcpSocket);
		
		sleep(10);
		
		logger.info("<<< public void setUp()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@AfterEach
	public void tearDown() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void tearDown()");

		connTest.stop();
		connTest.removeListener(this);
		connTest = null;
		tcpSocket = null;
		logger.info("<<< public void tearDown()");
		logger.info("-----------------------------------------------------------------------------");
	}

	/**
	 * Simple test for diagnostic session handling
	 * 
	 * Will crash when simulation is not running
	 */
	@Test
	public void testSession() {
		logger.info("#############################################################################");
		logger.info(">>> public void testSession()");
		
		this.routingActivation();
		this.sendRequestCheckResponse("10 03", "50 03 00 32 01 F4");
		this.sendRequestCheckResponse("22 F1 86", "62 F1 86 03");
		this.sendRequestCheckResponse("10 01", "50 01 00 32 01 F4");
		this.sendRequestCheckResponse("22 F1 86", "62 F1 86 01");
		
		logger.info("<<< public void testSession()");
		logger.info("#############################################################################");
	}
	
	@Test
	public void testTesterPresent() {
		logger.info("#############################################################################");
		logger.info(">>> public void testTesterPresent()");
		
		this.routingActivation();
		this.sendRequestCheckResponse("3E 00", "7E 00");
		
		logger.info("<<< public void testTesterPresent()");
		logger.info("#############################################################################");
		
	}
	
	/**
	 * Performs a routing activation. Throws an AssertionError if routing activation was not 
	 * successful.
	 */
	private void routingActivation() {
		DoipTcpRoutingActivationRequest doipRequest = new DoipTcpRoutingActivationRequest(testerAddress, 0x00, -1);
		
		DoipTcpConnection conn = this.connTest.getDoipTcpConnection();
		
		this.messageCounter = 0;
		conn.send(doipRequest);
		
		boolean result = this.waitForMessageCounter(1, 100);
		assertTrue(result);
	}
	
	/**
	 * Sends a diagnostic request and checks the response
	 * @param request The request as a hex string
	 * @param responseRegex The expected response. This can also be a regular expression.
	 */
	private void sendRequestCheckResponse(String request, String responseRegex) {
		
		byte[] udsRequest = Conversion.hexStringToByteArray(request);
		DoipTcpDiagnosticMessage doipUdsRequest = new DoipTcpDiagnosticMessage(testerAddress, ecuAddress, udsRequest);
		DoipTcpConnection conn = this.connTest.getDoipTcpConnection();
		
		this.messageCounter = 0;

		logger.info("UDS-SEND: " + request);
		conn.send(doipUdsRequest);
		
		boolean result = this.waitForMessageCounter(1, 100);
		assertTrue(result);
		DoipTcpDiagnosticMessagePosAck posAck = connTest.getLastDoipTcpDiagnosticMessagePosAck();
		assertNotNull(posAck, "Didn't receive a positive acknowledgement message on diagnostic request");
		assertEquals(0x00, posAck.getAckCode(), "The acknowledgement code is unequal to 0x10");
		
		
		result = this.waitForMessageCounter(2, 100);
		assertTrue(result, "Didn't receive any message from ECU, expected was to receive a UDS response");
		DoipTcpDiagnosticMessage doipUdsResponse = connTest.getLastDoipTcpDiagnosticMessage();
		assertNotNull(doipUdsResponse, "Didn't receive a UDS response from ECU");
		
		assertEquals(testerAddress, doipUdsResponse.getTargetAddress(), 
				"The target address of the UDS response is wrong, "
				+ "it does not match to the tester address");
		
		assertEquals( ecuAddress, doipUdsResponse.getSourceAddress(), 
				"The source address of the UDS response is wrong, "
				+ "it does not match to the ECU address");
		
		byte[] udsResponse = doipUdsResponse.getDiagnosticMessage();
		assertNotNull(udsResponse, "The UDS response was null");
		String udsResponseString = Conversion.byteArrayToHexString(udsResponse);
		
		logger.info("UDS-RECV: " + udsResponseString);
		
		posAck = new DoipTcpDiagnosticMessagePosAck(ecuAddress, testerAddress, 0x00, new byte[0]);
		conn.send(posAck);
		
		String regex = responseRegex.replaceAll(" ", "").toUpperCase();
	
		result = udsResponseString.replaceAll(" ",  "").matches(regex);
		if (result) {
			logger.info("CHECK: The UDS response " + udsResponseString + " matches the regular expression " + responseRegex);
		} else {
			
			logger.error("CHECK: The UDS response " + udsResponseString + " does not match the regular expression " + responseRegex);
		}
	}
	
	/**
	 * Waits until message counter has reached a given value
	 * @param numberOfMessages The number of received messages to wait for
	 * @param timeout Timeout value in milliseconds
	 * @return Returns true if the number of messages had been received
	 */
	private boolean waitForMessageCounter(int numberOfMessages, long timeout) {
		NanoTimer timer = new NanoTimer();
		long targetTime = timeout * 1000000;
		
		while (this.messageCounter < numberOfMessages && timer.getElapsedTime() < targetTime) {
			sleep(1);
		}
		
		if (this.messageCounter >= numberOfMessages) {
			return true;
		}
		return false;
	} 
	
	/**
	 * Wrapper for Thread.sleep(int millis)
	 * @param millis
	 */
	private static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDoipTcpMessageReceived() {
		this.messageCounter++;
	}

	@Override
	public void onConnectionClosed() {
	}

}
