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
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.timer.NanoTimer;
import doip.logging.LogManager;
import doip.logging.Logger;
import doip.simulation.nodes.GatewayConfig;
import doip.simulation.standard.StandardGateway;

/**
 * Implements the test if ECU handles a request correctly 
 * when another request still is in progress.
 */
public class TestBusyRepeatRequest implements DoipTcpConnectionTestListener {

	private static Logger logger = LogManager.getLogger(TestBusyRepeatRequest.class);
	
	/**
	 * Gateway which will be tested
	 */
	private static StandardGateway gateway = null;
	
	/**
	 * Configuration for the gateway
	 */
	private static GatewayConfig config = null;
	
	/**
	 * Test class for testing connection to gateway
	 */
	DoipTcpConnectionTest connTest = null;
	
	/**
	 * Counter for received DoIP messages
	 */
	int messageCounter = 0;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public static void setUpBeforeClass()");
		
		
		config = new GatewayConfig();
		config.setName("GW");
		config.setLocalPort(13400);
		config.setMaxByteArraySizeLogging(64);
		config.setMaxByteArraySizeLookup(64);
		gateway = new StandardGateway(config);
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
			gateway = null;
		}

		logger.info("<<< public static void tearDownAfterClass()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@Before
	public void setUp() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void setUp()");
		
		connTest = new DoipTcpConnectionTest();
		connTest.addListener(this);
		InetAddress localhost = InetAddress.getLocalHost();
		Socket tcpSocket = new Socket(localhost, 13400);
		connTest.start(tcpSocket);

		logger.info("<<< public void setUp()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@After
	public void tearDown() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void tearDown()");
		
		if (connTest != null) {
			connTest.stop();
			connTest.removeListener(this);
			connTest = null;
		}
		
		logger.info("<<< public void tearDown()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@Test
	public void testBusyRepeatRequest() {
		logger.info("#############################################################################");
		logger.info(">>> testBusyRepeatRequest()");
		
		performRoutingActivation();
		
		logger.info("<<< testBusyRepeatRequest()");
		logger.info("#############################################################################");
	}
	
	private boolean performRoutingActivation() {
		DoipTcpConnection conn = connTest.getDoipTcpConnection();
		DoipTcpRoutingActivationRequest request = new DoipTcpRoutingActivationRequest(0xFF00, 0x00, -1);
		conn.send(request);
		this.messageCounter = 0;
		boolean ret = this.waitForMessageReceived(1000, 1);
		Assert.assertTrue("Did not receive any response on routing activation request", ret);
		return true;
	}
	
	private boolean waitForMessageReceived(int millis, int expectedMessageCounter) {
		NanoTimer timer = new NanoTimer();
		long timeout = millis * 1000000;
		
		while (this.messageCounter < expectedMessageCounter && timer.getElapsedTime() < timeout) {
			sleep(1);
		}
		
		if (this.messageCounter >= expectedMessageCounter) {
			return true;
		}
		
		return false;
	}
	
	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
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
