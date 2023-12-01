package doip.simulation.testcases;

import static com.starcode88.jtest.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.starcode88.jtest.Assertions.*;
import com.starcode88.jtest.InitializationError;
import com.starcode88.jtest.TestCaseDescribed;
import com.starcode88.jtest.TestExecutionError;
import com.starcode88.jtest.TextBuilder;

import doip.library.message.DoipUdpVehicleAnnouncementMessage;
import doip.library.properties.EmptyPropertyValue;
import doip.library.properties.MissingProperty;
import doip.library.properties.MissingSystemProperty;
import doip.simulation.api.Gateway;
import doip.simulation.nodes.GatewayConfig;
import doip.simulation.standard.StandardGateway;
import doip.tester.toolkit.CheckResult;
import doip.tester.toolkit.EventChecker;
import doip.tester.toolkit.TestConfig;
import doip.tester.toolkit.TestSetup;
import doip.tester.toolkit.TesterUdpCommModule;
import doip.tester.toolkit.event.DoipEvent;
import doip.tester.toolkit.event.DoipEventUdpVehicleAnnouncementMessage;

class TC_0046_VehicleIdentification extends TestCaseDescribed {
	
	private static Logger logger = LogManager.getLogger(TC_0046_VehicleIdentification.class);

	public static final String BASE_ID = "0046";
	
	public static final String PREFIX = "TC-";
	
	private static TestSetup setup = null;
	
	private static TestConfig testConfig = null;
	
	private static Gateway gateway = null;
	
	private static GatewayConfig gatewayConfig = null;
	
	private static TesterUdpCommModule udpComm = null;
	
	private static InetAddress localhost = null;

	@BeforeAll
	public static void setUpBeforeClass() throws InitializationError {
		setUpBeforeClass(PREFIX + BASE_ID);
		setup = new TestSetup();
		gatewayConfig = new GatewayConfig();
		try {
			setup.initialize();
			udpComm = setup.getTesterUdpCommModule();
			testConfig = setup.getConfig();
			localhost = InetAddress.getByName("localhost");
			gatewayConfig.loadFromFile("src/test/resources/gateway.properties");
			gateway = new StandardGateway(gatewayConfig);
			gateway.start();
		} catch (IOException | EmptyPropertyValue | MissingProperty | MissingSystemProperty e) {
			throw logger.throwing(new InitializationError(TextBuilder.unexpectedException(e), e));
		}
	}
	
	@AfterAll
	public static void tearDownAfterClass() {
		gateway.stop();
	}
	
	@Test
	@DisplayName(PREFIX + BASE_ID+"-01")
	void test_01() throws TestExecutionError {
		runTest(PREFIX + BASE_ID + "-01", () -> testImpl_01());
	}
	
	void testImpl_01() throws IOException, InterruptedException {
		udpComm.sendDoipUdpVehicleIdentRequest(localhost);
		DoipEvent event = udpComm.waitForEvents(1, testConfig.get_A_DoIP_Ctrl());
		CheckResult result = EventChecker.checkEvent(event, DoipEventUdpVehicleAnnouncementMessage.class);
		if (result.getCode() != CheckResult.NO_ERROR) {
			fail(result.getText());
		}
	}
}
