package doip.simulation.unittests.basics;

import static com.starcode88.jtest.Assertions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.starcode88.jtest.InitializationError;
import com.starcode88.jtest.TestCaseDescribed;
import com.starcode88.jtest.TestExecutionError;

class UT_1002_DiagnosticMessage extends TestCaseDescribed {
	
	public static final String BASE_ID = "1001";
	
	private static Logger logger = LogManager.getLogger(UT_1002_DiagnosticMessage.class);

	@BeforeAll
	static void setUpBeforeClass() throws InitializationError {
		TestCaseDescribed.setUpBeforeClass("UT-" + BASE_ID);
	}

	@Test
	void test() throws TestExecutionError {
		this.runTest("UT-" + BASE_ID, () -> testImpl());
	}
	
	void testImpl() {
		
	}

}
