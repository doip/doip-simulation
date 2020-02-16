package doip.tester;

import static doip.junit.Assert.*;

import java.net.InetAddress;
import java.net.Socket;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import doip.junit.Assert;
import doip.library.comm.DoipTcpConnection;
import doip.library.message.DoipTcpDiagnosticMessage;
import doip.library.message.DoipTcpDiagnosticMessagePosAck;
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.timer.NanoTimer;
import doip.library.util.Conversion;
import doip.logging.LogManager;
import doip.logging.Logger;
import doip.simulation.nodes.Gateway;
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

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public static void setUpBeforeClass()");

		sleep(10);
		
		localhost = InetAddress.getLocalHost();
		
		gatewayConfig = new GatewayConfig();
		gatewayConfig.loadFromFile("src/test/resources/gateway.properties");
		
		gateway = new StandardGateway(gatewayConfig);
		gateway.start();
		
		sleep(10);
		
		logger.info("<<< public static void setUpBeforeClass()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@AfterClass
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
	
	@Before
	public void setUp() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void setUp()");
		
		sleep(10);
		
		connTest = new DoipTcpConnectionTest();
		connTest.addListener(this);
		
		tcpSocket = new Socket(localhost, 13400);
		connTest.start(tcpSocket);
		
		sleep(10);
		
		logger.info("<<< public void setUp()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@After
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
		Assert.assertTrue(result);
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
		//doip.junit.Assert.assertEquals(true, result); // TODO: Implement assertEquals
		Assert.assertTrue(result);
		DoipTcpDiagnosticMessagePosAck posAck = connTest.getLastDoipTcpDiagnosticMessagePosAck();
		Assert.assertNotNull("Didn't receive a positive acknowledgement message on diagnostic request", posAck);
		Assert.assertEquals("The acknowledgement code is unequal to 0x10", 0x00, posAck.getAckCode());
		
		
		result = this.waitForMessageCounter(2, 100);
		Assert.assertTrue("Didn't receive any message from ECU, expected was to receive a UDS response", result);
		DoipTcpDiagnosticMessage doipUdsResponse = connTest.getLastDoipTcpDiagnosticMessage();
		Assert.assertNotNull("Didn't receive a UDS response from ECU", doipUdsResponse);
		
		Assert.assertEquals("The target address of the UDS response is wrong, "
				+ "it does not match to the tester address", 
				testerAddress, doipUdsResponse.getTargetAddress());
		
		Assert.assertEquals("The source address of the UDS response is wrong, "
				+ "it does not match to the ECU address",
				ecuAddress, doipUdsResponse.getSourceAddress());
		
		byte[] udsResponse = doipUdsResponse.getDiagnosticMessage();
		Assert.assertNotNull("The UDS response was null", udsResponse);
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
