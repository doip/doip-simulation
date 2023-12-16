package doip.simulation.testcases;

import static com.starcode88.jtest.Assertions.fail;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.starcode88.jtest.InitializationError;
import com.starcode88.jtest.TestExecutionError;

import doip.tester.toolkit.CheckResult;
import doip.tester.toolkit.EventChecker;
import doip.tester.toolkit.TesterTcpConnection;
import doip.tester.toolkit.TextBuilder;
import doip.tester.toolkit.event.DoipEvent;
import doip.tester.toolkit.event.DoipEventConnectionClosed;

public class TC_0132_InitialInactivityTimer extends TestCaseSimulation 	{
	
	public static final String BASE_ID = "0132";
	
	public static final String PREFIX = "TC-";
	
	private static Logger logger = LogManager.getLogger(TC_0132_InitialInactivityTimer.class);
	
	@BeforeAll
	public static void setUpBeforeClass() throws InitializationError {
		TestCaseSimulation.setUpBeforeClass(PREFIX + BASE_ID);
	}
	
	@AfterAll
	public static void tearDownAfterClass() {
		TestCaseSimulation.tearDownAfterClass();
	}
	
	@Test
	@DisplayName(PREFIX + BASE_ID)
	public void test_01() throws TestExecutionError {
		this.runTest(PREFIX + BASE_ID + "-01", () -> testImpl_01());
	}
	
	public void testImpl_01() throws TestExecutionError {
		TesterTcpConnection conn = null;
		try {
			conn = this.createTcpConnection();
			DoipEvent event = conn.waitForEvents(1, (long) (0.9 * get_T_TCP_Initial_Inactivity()));
			CheckResult result = EventChecker.checkEvent(event, null);
			if (result.getCode() != CheckResult.NO_ERROR) {
				fail(result.getText());
			}
			event = conn.waitForEvents(1, (long) (0.2 * get_T_TCP_Initial_Inactivity()));
			result = EventChecker.checkEvent(event, DoipEventConnectionClosed.class);
			if (result.getCode() != CheckResult.NO_ERROR) {
				fail(result.getText());
			}

		} catch (IOException | InterruptedException e) {
			throw logger.throwing(new TestExecutionError(TextBuilder.unexpectedException(e), e));
		} finally {
			if (conn != null) {
				removeTcpConnection(conn);
			}
		}
	}
}
