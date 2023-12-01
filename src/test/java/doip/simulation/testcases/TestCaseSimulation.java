package doip.simulation.testcases;

import static com.starcode88.jtest.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.starcode88.jtest.InitializationError;
import com.starcode88.jtest.TestCaseDescribed;
import com.starcode88.jtest.TestExecutionError;
import com.starcode88.jtest.TextBuilder;

import doip.library.properties.EmptyPropertyValue;
import doip.library.properties.MissingProperty;
import doip.library.properties.MissingSystemProperty;
import doip.simulation.api.Gateway;
import doip.simulation.nodes.GatewayConfig;
import doip.simulation.standard.StandardGateway;
import doip.tester.toolkit.CheckResult;
import doip.tester.toolkit.DoipTcpConnectionWithEventCollection;
import doip.tester.toolkit.TestConfig;
import doip.tester.toolkit.TestSetup;
import doip.tester.toolkit.TesterTcpConnection;
import doip.tester.toolkit.TesterUdpCommModule;
import doip.tester.toolkit.exception.RoutingActivationFailed;

/**
 * Base class for all unit tests inside doip-simulation
 */
public class TestCaseSimulation extends TestCaseDescribed {
	
	private static Logger logger = LogManager.getLogger(TestCaseSimulation.class);
	
	private static TestSetup testSetup = null;
	
	private static TestConfig testConfig = null;
	
	private static Gateway gateway = null;
	
	private static GatewayConfig gatewayConfig = null;
	
	private static TesterUdpCommModule udpComm = null;
	
	private static InetAddress localhost = null;

	/**
	 * Sets up the test environment.
	 * @param classId
	 * @throws InitializationError
	 */
	public static void setUpBeforeClass(String classId) throws InitializationError {
		ThreadContext.put("context", "tester");
		TestCaseDescribed.setUpBeforeClass(classId);
		testSetup = new TestSetup();
		gatewayConfig = new GatewayConfig();
		try {
			testSetup.initialize();
			udpComm = testSetup.getTesterUdpCommModule();
			testConfig = testSetup.getConfig();
			localhost = InetAddress.getByName("localhost");
			gatewayConfig.loadFromFile("src/test/resources/gateway.properties");
			gateway = new StandardGateway(gatewayConfig);
			gateway.start();
			Thread.sleep(2000);
		} catch (IOException | EmptyPropertyValue | MissingProperty | MissingSystemProperty | InterruptedException e) {
			throw logger.throwing(new InitializationError(TextBuilder.unexpectedException(e), e));
		}
	}
	
	public static void tearDownAfterClass() {
		if (testSetup != null) {
			testSetup.uninitialize();
			testSetup = null;
		}
		if (gateway != null) {
			gateway.stop();
			gateway = null;
		}
		udpComm = null;
		gatewayConfig = null;
		localhost = null;
		udpComm = null;
		testConfig = null;
		System.gc();
	}

	public GatewayConfig getGatewayConfig() {
		return gatewayConfig;
	}

	public TesterTcpConnection createTcpConnection() throws IOException {
		TesterTcpConnection conn = null;
		//PlantUml.logCall(this, testSetup, "createTesterTcpConnection()");
		conn = testSetup.createTesterTcpConnection();
		//PlantUml.logReturn(this, testSetup);
		return conn;
	}
	
	public void removeTcpConnection(DoipTcpConnectionWithEventCollection conn) {
		//PlantUml.logCall(this, testSetup, "removeDoipTcpConnectionTest()");
		testSetup.removeDoipTcpConnectionTest(conn);
		//PlantUml.logReturn(this, testSetup);
	}
	
	/**
	 * Performs routing activation on a TCP connection. If routing activation
	 * fails it will throw an AssertionError.
	 * @param conn
	 * @param sourceAddress
	 * @throws TestExecutionError
	 */
	public void performRoutingActivation(TesterTcpConnection conn, int sourceAddress) throws TestExecutionError {
		try {
			String sa = String.format("0x%04X", sourceAddress);
			//PlantUml.logCall(this, conn, "performRoutingActivation(sourceAddress=" + sa + ")");
			conn.performRoutingActivation(sourceAddress, 0);
		} catch (RoutingActivationFailed e) {
			//PlantUml.logReturn(this, conn);
			//PlantUml.colorNote(this, e.getMessage(), "#FFAAAA");
			fail(e.getMessage());
		} catch (InterruptedException e) {
			//PlantUml.logReturn(this, conn);
			//PlantUml.colorNote(this, "Fatal error because of unexpected InterruptException", "#FFAAAA");
			throw logger.throwing(new TestExecutionError(TextBuilder.unexpectedException(e), e));
		}
		
		//PlantUml.logReturn(this, conn);
		//PlantUml.colorNote(this, "Routing activation performed as expected", "#AAFFAA");
	}
	
	public int get_T_TCP_Initial_Inactivity() {
		return testConfig.get_T_TCP_Initial_Inactivity();
	}
	
	public void checkResultForNoError(CheckResult result) {
		if (result.getCode() == CheckResult.NO_ERROR) {
			//PlantUml.colorNote(this, result.getText(), "#AAFFAA");
			logger.info(result.getText()); 
		} else {
			//PlantUml.colorNote(this, result.getText(), "#FFAAAA");
			fail(result.getText());
		}
	}
}
