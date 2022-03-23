package doip.simulation;

import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static doip.junit.Assertions.*;
import doip.library.comm.DoipTcpConnection;
import doip.library.message.DoipTcpDiagnosticMessage;
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.message.DoipTcpRoutingActivationResponse;
import doip.library.timer.NanoTimer;
import doip.library.util.Helper;
import doip.logging.LogManager;
import doip.logging.Logger;
import doip.simulation.nodes.EcuConfig;
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

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public static void setUpBeforeClass()");
		
		try {
			config = new GatewayConfig();
			config.setName("GW");
			config.setLocalPort(13400);
			config.setMaxByteArraySizeLogging(64);
			config.setMaxByteArraySizeLookup(64);
			
			List<EcuConfig> ecuConfigList = config.getEcuConfigList();
			EcuConfig ecuConfig = new EcuConfig();
			ecuConfig.setPhysicalAddress(815);
			ecuConfig.setName("ECU");
			ecuConfigList.add(ecuConfig);
			
			gateway = new BusyGateway(config);
			gateway.start();
		} catch (Exception e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		logger.info("<<< public static void setUpBeforeClass()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public static void tearDownAfterClass()");
		
		try {
			if (gateway != null) {
				gateway.stop();
				gateway = null;
			}
		} catch (Exception e) {
			logger.error(Helper.getExceptionAsString(e));
		}

		logger.info("<<< public static void tearDownAfterClass()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@BeforeEach
	public void setUp() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void setUp()");
		
		try {
			connTest = new DoipTcpConnectionTest();
			connTest.addListener(this);
			InetAddress localhost = InetAddress.getLocalHost();
			Socket tcpSocket = new Socket(localhost, 13400);
			connTest.start(tcpSocket);
		} catch (Exception e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		logger.info("<<< public void setUp()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@AfterEach
	public void tearDown() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void tearDown()");
		
		try {
			if (connTest != null) {
				connTest.stop();
				connTest.removeListener(this);
				connTest = null;
			}
		} catch (Exception e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		
		logger.info("<<< public void tearDown()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@Test
	public void testBusyRepeatRequest() {
		logger.info("#############################################################################");
		logger.info(">>> testBusyRepeatRequest()");
	
		try {
			performRoutingActivation();
			
			DoipTcpDiagnosticMessage requestA = new DoipTcpDiagnosticMessage(0xFF00, 815, new byte[] { 0x10, 0x02 });
			DoipTcpDiagnosticMessage requestB = new DoipTcpDiagnosticMessage(0xFF00, 815, new byte[] { 0x31, 0x01, 0x02, 0x03 });
			
			byte[] twoRequests = Helper.concat(requestA.getMessage(), requestB.getMessage());
			
			this.messageCounter = 0;
			DoipTcpConnection conn = connTest.getDoipTcpConnection();
			conn.send(twoRequests);
					
			this.waitForMessageReceived(1000, 4);
		} catch (Exception e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		logger.info("<<< testBusyRepeatRequest()");
		logger.info("#############################################################################");
	}
	
	private boolean performRoutingActivation() {
		DoipTcpConnection conn = connTest.getDoipTcpConnection();
		DoipTcpRoutingActivationRequest request = new DoipTcpRoutingActivationRequest(0xFF00, 0x00, -1);
		conn.send(request);
		this.messageCounter = 0;
		boolean ret = this.waitForMessageReceived(1000, 1);
		assertTrue(ret, "Did not receive any response on routing activation request");
		DoipTcpRoutingActivationResponse response = connTest.getLastDoipTcpRoutingActivationResponse();
		assertNotNull(response, "Did not receive a routing activation response");
		int responseCode = response.getResponseCode();
		assertEquals(0x10, responseCode, "The response code in routing activation response");
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
