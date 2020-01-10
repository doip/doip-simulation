package doip.simulation;

import static doip.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import doip.junit.Assert;
import doip.library.comm.DoipUdpMessageHandler;
import doip.library.comm.DoipUdpMessageHandlerListener;
import doip.library.message.DoipTcpDiagnosticMessage;
import doip.library.message.DoipUdpDiagnosticPowerModeRequest;
import doip.library.message.DoipUdpDiagnosticPowerModeResponse;
import doip.library.message.DoipUdpEntityStatusRequest;
import doip.library.message.DoipUdpEntityStatusResponse;
import doip.library.message.DoipUdpHeaderNegAck;
import doip.library.message.DoipUdpMessage;
import doip.library.message.DoipUdpVehicleAnnouncementMessage;
import doip.library.message.DoipUdpVehicleIdentRequest;
import doip.library.message.DoipUdpVehicleIdentRequestWithEid;
import doip.library.message.DoipUdpVehicleIdentRequestWithVin;
import doip.library.util.LookupTable;
import doip.logging.LogManager;
import doip.logging.Logger;
import doip.simulation.nodes.Gateway;
import doip.simulation.nodes.GatewayConfig;
import doip.simulation.standard.StandardGateway;

public class TestSimulation implements DoipUdpMessageHandlerListener {
	
	private static Logger logger = LogManager.getLogger(TestSimulation.class);
	
	private static Gateway gateway = null;
	
	private static GatewayConfig gatewayConfig = null;
	
	private DatagramSocket udpSocket = null;
	
	private Socket tcpSocket = null;
	
	private DoipUdpMessageHandlerTest doipUdpMessageHandlerTest = null;
	
	private DoipTcpConnectionTest doipTcpConnectionTest = null;
	
	private static InetAddress localhost = null;

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
		
		udpSocket = new DatagramSocket(13401);
		doipUdpMessageHandlerTest = new DoipUdpMessageHandlerTest();
		doipUdpMessageHandlerTest.start(udpSocket);
		
		tcpSocket = new Socket(localhost, 13400);
		doipTcpConnectionTest = new DoipTcpConnectionTest();
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
			doipUdpMessageHandlerTest = null;
		}
		
		if (doipTcpConnectionTest != null) {
			doipTcpConnectionTest.stop();
			doipTcpConnectionTest = null;
		}
		
		logger.info("<<< public void tearDown()");
		logger.info("-----------------------------------------------------------------------------");
	}
	
	@Test
	public void testUdpVehicleIdentRequest() throws IOException {
		logger.info("#############################################################################");
		logger.info(">>> public void testVehicleIdentRequest()");
		
		DoipUdpVehicleIdentRequest doipUdpVehicleIdentRequest = new DoipUdpVehicleIdentRequest();
		sendUdpMessage(doipUdpVehicleIdentRequest);
		sleep(100);
	
		// TODO: Add check
		
		logger.info("<<< public void testVehicleIdentRequest()");
		logger.info("#############################################################################");
	}
	
	@Test
	public void testTcpWrongTargetAddress() throws IOException {
		logger.info("#############################################################################");
		logger.info(">>> testTcpWrongTargetAddress()");
		DoipTcpDiagnosticMessage doipTcpDiagnosticMessage = new DoipTcpDiagnosticMessage(0xF0, 0, new byte[] { 0x10, 0x03 });
		doipTcpConnectionTest.getDoipTcpConnection().send(doipTcpDiagnosticMessage);
		sleep(100);
		
		// TODO: Add check
		
		logger.info("<<< testTcpWrongTargetAddress()");
		logger.info("#############################################################################");
	}
	
	private void sendUdpMessage(DoipUdpMessage doipUdpMessage) throws IOException {
		byte[] message = doipUdpMessage.getMessage();
		int length = message.length;
		DatagramPacket packet = new DatagramPacket(message, length, localhost, 13400);
		udpSocket.send(packet);
	}
	
	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void onDoipUdpVehicleIdentRequest(DoipUdpVehicleIdentRequest doipMessage, DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpVehicleIdentRequestWithEid(DoipUdpVehicleIdentRequestWithEid doipMessage,
			DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpVehicleIdentRequestWithVin(DoipUdpVehicleIdentRequestWithVin doipMessage,
			DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpVehicleAnnouncementMessage(DoipUdpVehicleAnnouncementMessage doipMessage,
			DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpDiagnosticPowerModeRequest(DoipUdpDiagnosticPowerModeRequest doipMessage,
			DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpDiagnosticPowerModeResponse(DoipUdpDiagnosticPowerModeResponse doipMessage,
			DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpEntityStatusRequest(DoipUdpEntityStatusRequest doipMessage, DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpEntityStatusResponse(DoipUdpEntityStatusResponse doipMessage, DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpHeaderNegAck(DoipUdpHeaderNegAck doipMessage, DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}
}
