package doip.simulation.testcases;

import static com.starcode88.jtest.Assertions.fail;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.starcode88.jtest.InitializationError;
import com.starcode88.jtest.TestExecutionError;

import doip.simulation.GatewayConfig;
import doip.tester.toolkit.TesterTcpConnection;
import doip.tester.toolkit.TextBuilder;

public class TC_0095_MultiAliveCheckWithNoResponse extends TestCaseSimulation {
    
    public static final String BASE_ID = "0095";

    public static final String PREFIX = "TC-";    

    private static Logger logger = LogManager.getLogger(TC_0095_MultiAliveCheckWithNoResponse.class);

    @BeforeAll
    public static void setupBeforeClass() throws InitializationError {
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

    /**
     * @throws TestExecutionError
     */
    public void testImpl_01() throws TestExecutionError {
        LinkedList<TesterTcpConnection> connections = new LinkedList<TesterTcpConnection>();
        GatewayConfig gwc = getGatewayConfig();
        TesterTcpConnection newConn = null;
        try {
            // Establish maxNumberOfRegisteredConnections connections
            for (int i = 0; i < gwc.getMaxNumberOfRegisteredConnections(); i++) {
                TesterTcpConnection connection = createTcpConnection();
                connections.add(connection);
           }

           // Perform routing activation on all connections
           int sourceAddress = 0x0EF1;
           for (TesterTcpConnection connection : connections) {
               performRoutingActivation(connection, sourceAddress);
               sourceAddress++;
           }

           // Create new connection
           newConn = createTcpConnection();
           // Perform routing activation on new connection
           performRoutingActivation(newConn, sourceAddress);
           

        } catch (IOException e) {
            throw logger.throwing(new TestExecutionError(TextBuilder.unexpectedException(e), e));
        } finally {
            if (newConn != null) {
                removeTcpConnection(newConn);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

            for (TesterTcpConnection connection : connections) {
                if (!connection.getSocket().isConnected()) {
                    fail("It was expected that all connections would have been closed because there was no response on alive check reuqests");
                }
            }

            for (TesterTcpConnection connection : connections) {
                removeTcpConnection(connection);
            }
        }
    }
}