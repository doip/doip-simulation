package src.simulation;

import static doip.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import doip.logging.LogManager;
import doip.logging.Logger;
import doip.simulation.nodes.Gateway;
import doip.simulation.nodes.GatewayConfig;
import doip.simulation.standard.StandardGateway;

public class TestSimulation {
	
	private static Logger logger = LogManager.getLogger(TestSimulation.class);
	
	private static Gateway gateway = null;
	
	private static GatewayConfig gatewayConfig = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public static void setUpBeforeClass()");
		
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
		
		gateway.stop();
		
		logger.info("<<< public static void tearDownAfterClass()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@Before
	public void setUp() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void setUp()");
		logger.info("<<< public void setUp()");
		logger.info("-----------------------------------------------------------------------------");
	}

	@After
	public void tearDown() throws Exception {
		logger.info("-----------------------------------------------------------------------------");
		logger.info(">>> public void tearDown()");
		logger.info("<<< public void tearDown()");
		logger.info("-----------------------------------------------------------------------------");
	}
	
	@Test
	public void test() {
		logger.info("#############################################################################");
		logger.info(">>> public void test()");
		logger.info("<<< public void test()");
		logger.info("#############################################################################");
	}
}
