package doip.simulation.standard;

import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doip.library.comm.DoipTcpConnection;
import doip.library.message.DoipTcpAliveCheckRequest;
import doip.library.message.DoipTcpAliveCheckResponse;
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.timer.NanoTimer;
import doip.simulation.AliveCheckResponseCollector;

// TODO: Instead of synchronized methods we should use a lock
// to prevent consistent list of current connections
// we should use two locks. One for the access to the list
// of current connections and one to prevent that multiple
// alive checks will run in parallel
public class ConnectionManager {
	
	private static Logger logger = LogManager.getLogger(ConnectionManager.class);

	/** Represents the connection table like 
	 *  specified in the ISO specification */
	private volatile LinkedList<StandardTcpConnectionGateway> connections = new LinkedList<StandardTcpConnectionGateway>();
	
	private int maxNumberOfRegisteredConnections = 0;

	public ConnectionManager(int maxNumberOfRegisteredConnections) {
		this.maxNumberOfRegisteredConnections = maxNumberOfRegisteredConnections;
	}
	
	public synchronized boolean addConnection(StandardTcpConnectionGateway connection) {
		connections.add(connection);
		return true;
	}
	
	public synchronized void removeConnection(StandardTcpConnectionGateway connection) {
		connections.remove(connection);
	}
	
	public void stopAllConnections() {
		for (StandardTcpConnectionGateway conn : connections) {
			conn.stop();
		}
	}
	
	/**
	 * Returns a new list which contains all registered connections
	 * @return A new list which contains all registered connections
	 */
	private synchronized LinkedList<StandardTcpConnectionGateway> getRegisteredConnections() {
		LinkedList<StandardTcpConnectionGateway> registeredConnections =
				new LinkedList<StandardTcpConnectionGateway>();
		for (StandardTcpConnectionGateway conn : connections) {
			if (conn.isRegistered()) {
				registeredConnections.add(conn);
			}
		}
		return registeredConnections;
	}
	
	public synchronized StandardTcpConnectionGateway getConnectionBySourceAddress(int sourceAddress) {
		for (StandardTcpConnectionGateway conn : connections) {
			if (conn.getRegisteredSourceAddress() == sourceAddress) {
				return conn;
			}
		}
		return null;
	}
	
	/**
	 * Implements the routing activation socket handler according
	 * to figure 26 in ISO 13400-2:2019. The name of the figure
	 * is "TCP_DATA socket handler" which is not the best name, because
	 * it is only for routing activation.
	 * This function here is NOT(!) the DoIP routing activation handler
	 * according to figure 22 is ISO specification.
	 * 
	 * @param connection
	 * @param request
	 * @return Returns the routing activation response code.
	 *         <ul>
	 *             <li>Positive results</li>
	 *             <ul>
	 *                 <li>0x10: Routing successful activated</li>
	 *             </ul>
	 *             <li>Negative results</li>
	               <ul>
	                   <li>
	                       0x02: Routing activation denied because the SA
	                       received is different from the table connection
	                       entry on the already activated TCP_DATA socket.
	                   </li>
	               </ul>
	 *         </ul>
	 */
	public synchronized int routingActivationSocketHandler(StandardTcpConnectionGateway connection, DoipTcpRoutingActivationRequest request) {
//		List<StandardTcpConnectionGateway> registeredConnections = getRegisteredConnections();
		
//		if (registeredConnections.size() == 0) {
//			connection.setRegisteredSourceAddress(request.getSourceAddress());
//			return 0x10; // routing successful activated
//		}
		
		// Check if current connection is already registered
		if (connection.isRegistered()) {
			if (connection.getRegisteredSourceAddress() == request.getSourceAddress()) {
				// Registered source address is already registered on
				// this socket, we can send back positive result
				// [DoIP-089]		
				logger.info("Routing activation successful because source address is already registered on this socket, we send back 0x10");
				return 0x10; // routing successful activated
			} else {
				// [DoIP-106]
				logger.info("Routing activation denied because an SA different from the table connection entry was received on the already activated TCP_DATA socket, we will send back 0x02");
				return 0x02; // routing activation denied because an SA different from the table connection entry was received on the already activated TCP_DATA socket
			}
		} else {
			// Connection is not registered, check if it registered on a
			// different connection
			StandardTcpConnectionGateway alreadyRegisteredConnection = getConnectionBySourceAddress(request.getSourceAddress());
			if (alreadyRegisteredConnection == null) {
				// It is not registered on a different socket
				// TODO: Check if maximum registered connections is reached
				// In such a case we need to perform alive check on
				// all other registered connections 
				// Just for now we assume it is not reached
				List<StandardTcpConnectionGateway> registeredConnections = getRegisteredConnections();
				if (registeredConnections.size() >= this.maxNumberOfRegisteredConnections) {
					
					logger.info("Maximum number of registered connections reached, we need to perform alive check on all other TCP_DATA sockets");
					HashMap<StandardTcpConnectionGateway, LinkedList<Integer>> map =
						this.performAliveCheck(registeredConnections);
					if (map.size() >= this.maxNumberOfRegisteredConnections) {
						logger.info("All sockets are still in use after alive check, routing activation denied because all concurrently supported TCP_DATA sockets are registered and active, we send back 0x01");
						return 0x01; // routing activation denied because all concurrently supported TCP_DATA sockets are registered and active 
					} else {
						logger.info("After alive check one or more sockets are not used any more, they will be closed and routing activation for current socket will be successful, we return 0x10");
						connection.setRegisteredSourceAddress(request.getSourceAddress());
						return 0x10;
					}
				} else {
					// [DoIP-090]
					// Maximum number of registered connections is not reached
					connection.setRegisteredSourceAddress(request.getSourceAddress());
					logger.info("Routing activation successful because connection was not registered and source address is not registered to any other TCP_DATA socket, we send back 0x10");
					return 0x10; // routing successful activated
				}
			} else {
				// [DoIP-091]
				// Current connection is not registered, but source address
				// is registered on a different socket.
				
				// Create list with only one element, this element is the
				// other connection whis is registered to the same
				// source address
				List<StandardTcpConnectionGateway> conns =
						new LinkedList<StandardTcpConnectionGateway>();
				conns.add(alreadyRegisteredConnection);
				
				// Perform alive check
				logger.info("Current socket is not registered, but source address is registered to a different TCP_DATA socket, this requires alive check");
				//PlantUml.logCall(this, this, "performAliveCheck(...)");
				HashMap<StandardTcpConnectionGateway, 
						LinkedList<Integer>> map =
						this.performAliveCheck(conns);
				//PlantUml.logReturn(this, this);
				
				// Check integrity of map. It must contain at maximum one
				// connection as a key. Alive check responses on other sockets
				// will not be collected by the AliveCheckResponseCollector.
				if (map.size() > 1) {
					logger.fatal("An alive check on a single socket has been done, but the result contains multiple connections on which an alive check response has been received");
				}
						
				// Now check if alive check response has been received and
				// if the source address in the alive check response
				// matches the new requested source address
				logger.info("Alive check has been performed and we did receive responses on {} sockets", map.size());
				if (map.containsKey(alreadyRegisteredConnection)) {
					// [DoIP-093]
					List<Integer> responses = map.get(alreadyRegisteredConnection);
					for (Integer response : responses) {
						if (response == request.getSourceAddress()) {
							logger.info("Routing activation denied because the SA is already registered and active on a different TCP_DATA socket, we send back 0x03");
							return 0x03; // routing activation denied because the SA is already registered and active on a different TCP_DATA socket
						}
					}

					// Source address not found in the responses, that means
					// Source address is free now.
					// TODO: Update source addresses in list of connections
					logger.info("Source address hasn't been found in alive check response, routing activation successful, we send back 0x10");
					return 0x10; 
				} else {
					// [DoIP-092]
					// No response on alive check
					logger.info("No alive check response received, routing activation successful, we send back 0x10");
					return 0x10;
				}
			}
		}
	}
	
	/**
	 * Performs a alive check. Both alive checks can be handled here, 
	 * alive check for single connection or alive check for all registered
	 * connections. The list of connections on which an alive check
	 * shall be performed is given as an argument to the method.
	 * @param conns List of Connections on which an alive check shall be
	 * performed
	 * @return Returns a map which contains the connections as a key on which an alive
	 *         check response has been received. For each key a list of alive
	 *         check responses will be created. Current ISO specification
	 *         assumes that for each connection only one alive check response
	 *         can be received. For a proof of concept it is possible that
	 *         multiple testers use the same connection, that means we can
	 *         receive multiple alive check responses. Because of the proof
	 *         of concept we already implement a list of alive check responses
	 *         for each connection.
	 */
	public synchronized HashMap<StandardTcpConnectionGateway,
				        LinkedList<Integer>> 
			performAliveCheck(List<StandardTcpConnectionGateway> conns) {
		
		//PlantUml.note(this, "Perform alive check on " + conns.size() + " connection(s)");
		
		AliveCheckResponseCollector collector = new AliveCheckResponseCollector();
		for (StandardTcpConnectionGateway conn : conns) {
			conn.addListener(collector);
		}
		
		DoipTcpAliveCheckRequest request = new DoipTcpAliveCheckRequest();
		for (StandardTcpConnectionGateway conn : conns) {
			//PlantUml.logCall(this, conn, "send(DoipTcpAliveCheckRequest request)");
			conn.send(request);
			//PlantUml.logReturn(this, conn);
		}
		
		try {	
			Thread.sleep(500); // This can be optimized
		} catch (InterruptedException e) {
			logger.fatal("Waiting for alive check responses has been interrupted");
		}
		
		for (StandardTcpConnectionGateway conn : conns) {
			conn.removeListener(collector);
		}
		
		HashMap<StandardTcpConnectionGateway,
        LinkedList<Integer>> map = collector.getAliveCheckResponses();
		
		// Connections with no responses we already can close
		for (StandardTcpConnectionGateway conn : conns) {
			if (map.containsKey(conn) == false) {
				//PlantUml.logCall(this, conn, "stop()");
				conn.stop();
				//PlantUml.logReturn(this, conn);
			}
		}
		
		return map;
	}
}
