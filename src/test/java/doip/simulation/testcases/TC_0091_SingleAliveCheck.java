package doip.simulation.testcases;

import static com.starcode88.jtest.Assertions.*;

import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.starcode88.jtest.InitializationError;
import com.starcode88.jtest.TestExecutionError;

import doip.library.message.DoipTcpAliveCheckResponse;
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.message.DoipTcpRoutingActivationResponse;
import doip.tester.toolkit.CheckResult;
import doip.tester.toolkit.EventChecker;
import doip.tester.toolkit.TesterTcpConnection;
import doip.tester.toolkit.TextBuilder;
import doip.tester.toolkit.event.DoipEvent;
import doip.tester.toolkit.event.DoipEventTcpAliveCheckRequest;
import doip.tester.toolkit.event.DoipEventTcpRoutingActivationResponse;
import doip.tester.toolkit.exception.RoutingActivationFailed;

public class TC_0091_SingleAliveCheck extends TestCaseSimulation {
	
	public static final String BASE_ID = "0091";
	
	public static final String PREFIX = "TC-";

	private static Logger logger = LogManager.getLogger(TC_0091_SingleAliveCheck.class);
	
	//String instanceName; 
	
	@BeforeAll
	public static void setUpBeforeClass( ) throws InitializationError {
		ThreadContext.put("context", "tester");
		//PlantUml.setColor("tester", PlantUml.LIGHT_BLUE);
		//PlantUml.startUml(TC_0091_SingleAliveCheck.class);
		TestCaseSimulation.setUpBeforeClass(PREFIX + BASE_ID);
	}
	
	@AfterAll
	public static void tearDownAfterClass() {
		TestCaseSimulation.tearDownAfterClass();
		//PlantUml.endUml();
	}
	
	@Test
	@DisplayName(PREFIX + BASE_ID + "-01")
	public void test_01() throws TestExecutionError {
		//PlantUml.logCall(this, this, "runTest(...)");
		try {
			this.runTest(PREFIX + BASE_ID + "-01", () -> testImpl_01());
		} finally {
			//PlantUml.logReturn(this, this);
		}
	}
	
	public void testImpl_01() throws TestExecutionError {
		TesterTcpConnection conn1 = null;
		TesterTcpConnection conn2 = null;
		try {
			//PlantUml.addSeparator("START " + PREFIX + BASE_ID + "-01");
			// Create first connection
			logger.info("Create first connection to DoIP server and perform routing activation");
			
			conn1 = createTcpConnection();
			
			DoipTcpRoutingActivationRequest request =
					new DoipTcpRoutingActivationRequest(0x0EF1, 0, -1);
			
			conn1.send(request);
			DoipEvent event = conn1.waitForEvents(1, 100);
			CheckResult result = EventChecker.checkEvent(event, DoipEventTcpRoutingActivationResponse.class);
			checkResultForNoError(result);
			
			// Create second connection
			logger.info("Create second connecttion to DoIP server");
			conn2 = createTcpConnection();
			conn1.clearEvents();
			
			logger.info("Send routing activation on second connection with same source address used in first connection");
			conn2.send(request);
			// Now alive check should be received on first connection
			logger.info("Wait for alive check in first connection");
			event = conn1.waitForEvents(1, 100);
			result = EventChecker.checkEvent(event, DoipEventTcpAliveCheckRequest.class);
			checkResultForNoError(result);
		} catch (IOException | InterruptedException e) {
			throw logger.throwing(new TestExecutionError(TextBuilder.unexpectedException(e), e));
		} finally {
			if (conn1 != null) {
				removeTcpConnection(conn1);
			}
			if (conn2 != null) {
				removeTcpConnection(conn2);
			}
			//PlantUml.addSeparator("END " + PREFIX + BASE_ID + "-01");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}
}
