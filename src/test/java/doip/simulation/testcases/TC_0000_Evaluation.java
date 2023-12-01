package doip.simulation.testcases;

import static com.starcode88.jtest.Assertions.*;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.*;
import org.junit.jupiter.api.*;

public class TC_0000_Evaluation {
	
	private static Logger logger = LogManager.getLogger(TC_0000_Evaluation.class);
	
	private static Marker markerUml = MarkerManager.getMarker("UML");
	
	@Test
	public void test() {
		logger.info(">>> public void test()");
		List<Integer> list = new LinkedList<>();
		list.add(1);
		assertTrue(list.contains(1));
		Integer i1 = new Integer(1);
		Integer i2 = new Integer(1);
		assertFalse(i1 == i2);
		assertTrue(i1.equals(i2));

		testImpl();
		logger.info("<<< public void test()");
	}
	
	public void testImpl() {
		
	}

	@Test
	public void test2() {
		logger.info(">>> public void test2()");
		logger.info("<<< public void test2()");
	}

}