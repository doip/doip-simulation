package doip.simulation.testcases;

import static com.starcode88.jtest.Assertions.assertEquals;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import doip.tester.toolkit.event.DoipEventMessage;
import doip.tester.toolkit.event.DoipEventTcpAliveCheckRequest;
import doip.tester.toolkit.event.DoipEventTcpRoutingActivationResponse;

public class TC_0093_SingleAliveCheckWithResponse extends TestCaseSimulation {
	
	public static final String BASE_ID = "0093";
	
	public static final String PREFIX = "TC-";

	private static Logger logger = LogManager.getLogger(TC_0093_SingleAliveCheckWithResponse.class);
	
	@BeforeAll
	public static void setUpBeforeClass() throws InitializationError {
		ThreadContext.put("context", "tester");
		TestCaseSimulation.setUpBeforeClass(PREFIX + BASE_ID);
	}
	
	@AfterAll
	public static void tearDownAfterClass() {
		TestCaseSimulation.tearDownAfterClass();
	}
	
	@Test
	@DisplayName(PREFIX + BASE_ID + "-01")
	public void test_01() throws TestExecutionError {
		this.runTest(PREFIX + BASE_ID + "-01", () -> testImpl_01());
	}
	
	public void testImpl_01() throws TestExecutionError {
		TesterTcpConnection conn1 = null;
		TesterTcpConnection conn2 = null;
		try {
			
			// Create first connection
			logger.info("Create first connection to DoIP server and perform routing activation");
			
			conn1 = createTcpConnection();
			
			performRoutingActivation(conn1, 0x0EF1);
			
			// Create second connection
			logger.info("Create second connecttion to DoIP server");
			conn2 = createTcpConnection();
			
			logger.info("Send routing activation on second connection with same source address used in first connection");
			conn1.clearEvents();
			
			// Do not use performRoutingActivation(...), because this will be a blocking call waiting for response
			DoipTcpRoutingActivationRequest request = new DoipTcpRoutingActivationRequest(0x0EF1, 0, -1);
			conn2.clearEvents();
			conn2.send(request);

			
			// Now alive check should be received on first connection
			logger.info("Wait for alive check in first connection");
			DoipEvent event = conn1.waitForEvents(1, 100);
			CheckResult result = EventChecker.checkEvent(event, DoipEventTcpAliveCheckRequest.class);
			checkResultForNoError(result);
			
			// We will send response
			conn2.clearEvents();
			conn1.clearEvents();
			logger.info("We will send a '" + DoipTcpRoutingActivationResponse.getMessageNameOfClass() + "'");
			DoipTcpAliveCheckResponse response = new DoipTcpAliveCheckResponse(0x0EF1);
			conn1.send(response);
			
			// We should receive a positive routing activation response.
			// We should wait more than 500 ms because 500 ms is timeout
			// for receiving alive check responses.
			event = conn2.waitForEvents(1, 600);
			result = EventChecker.checkEvent(event, DoipEventTcpRoutingActivationResponse.class);
			checkResultForNoError(result);
			DoipTcpRoutingActivationResponse response2 = (DoipTcpRoutingActivationResponse) ((DoipEventMessage) event).getDoipMessage();
			int testerAddress = response2.getTesterAddress();
			assertEquals(0x0EF1, testerAddress, "The source address in the routing activation response does not match the expected value 0x0EF1.");
			int responseCode = response2.getResponseCode();
			assertEquals(0x03, responseCode, "The response code doesn't match the expected value 0x03.");
			
		} catch (IOException | InterruptedException e) {
			throw logger.throwing(new TestExecutionError(TextBuilder.unexpectedException(e), e));
		} finally {
			if (conn1 != null) {
				removeTcpConnection(conn1);
			}
			if (conn2 != null) {
				removeTcpConnection(conn2);
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}

		}	
	}

}
