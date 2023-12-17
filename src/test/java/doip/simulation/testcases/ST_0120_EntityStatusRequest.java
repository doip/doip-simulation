package doip.simulation.testcases;

import static com.starcode88.jtest.Assertions.*;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.starcode88.jtest.InitializationError;
import com.starcode88.jtest.TestExecutionError;

import doip.tester.toolkit.EventChecker;
import doip.tester.toolkit.TesterTcpConnection;
import doip.tester.toolkit.TesterUdpCommModule;
import doip.tester.toolkit.event.DoipEvent;
import doip.tester.toolkit.event.DoipEventUdpEntityStatusResponse;
import doip.library.message.DoipUdpEntityStatusResponse;
import doip.tester.toolkit.CheckResult;

public class ST_0120_EntityStatusRequest extends TestCaseSimulation {

	public static final String BASE_ID = "0120";
	
	public static final String PREFIX = "ST-";
	
	/** Log4j logger */
	private static Logger logger = LogManager.getLogger(ST_0120_EntityStatusRequest.class);
	
	/** Log4j marker for function entry */
	private static Marker enter = MarkerManager.getMarker("ENTER");
	
	/** Log4j marker for function exit */
	private static Marker exit = MarkerManager.getMarker("EXIT");
	
	@BeforeAll
	public static void setUpBeforeClass() throws InitializationError {
		String method = "public static void setUpBeforeClass()";
		try {
			logger.trace(enter, ">>> {}", method);
			TestCaseSimulation.setUpBeforeClass(PREFIX + BASE_ID);
		} finally {
			logger.trace(exit, "<<< {}", method);
		}
	}
	
	@AfterAll
	public static void tearDownAfterClass() {
		String method = "public static void tearDownAfterClass()";
		logger.trace(enter, ">>> {}", method);
		TestCaseSimulation.tearDownAfterClass();
		logger.trace(exit, ">>> {}", method);
	}
	
	@Test
	@DisplayName(PREFIX + BASE_ID + "-01")
	public void test_01() throws TestExecutionError {
		runTest(PREFIX + BASE_ID + "-01", () -> testImpl_01());
	}
	
	public void testImpl_01() throws TestExecutionError {
		String method = "public void testImpl_01()"; 
		try {
			logger.trace(enter, ">>> {}", method);
			
			// Step 1: Send DoIP entity status request
			TesterUdpCommModule udpComm = this.getTesterUdpCommModule();
			udpComm.sendDoipUdpEntityStatusRequest(getLocalHost());
			DoipEvent event = udpComm.waitForEvents(1, 100);
			CheckResult result = EventChecker.checkEvent(event, DoipEventUdpEntityStatusResponse.class);
			this.checkResultForNoError(result);
			DoipUdpEntityStatusResponse response = (DoipUdpEntityStatusResponse)
					(((DoipEventUdpEntityStatusResponse)event).getDoipMessage());
			assertEquals(0, response.getCurrentNumberOfSockets(), "The number of current sockets in the response message doesn't match the expected value of 0");
			
			// Step 2: Establish a TCP connection
			TesterTcpConnection conn = createTcpConnection();
			
			// Step 3: Send second DoIP entity status request
			udpComm.clearEvents();
			udpComm.sendDoipUdpEntityStatusRequest(getLocalHost());
			event = udpComm.waitForEvents(1, 100);
			result = EventChecker.checkEvent(event, DoipEventUdpEntityStatusResponse.class);
			this.checkResultForNoError(result);
			response = (DoipUdpEntityStatusResponse)
					(((DoipEventUdpEntityStatusResponse)event).getDoipMessage());
			assertEquals(1, response.getCurrentNumberOfSockets(), "The number of current sockets in the response message doesn't match the expected value of 1");
			
		} catch (IOException | InterruptedException e) {
			throw logger.throwing(new TestExecutionError(e));
		} finally {
			logger.trace(exit, ">>> {}", method);
		}
	}
}
