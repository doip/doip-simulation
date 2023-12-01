package doip.simulation.nodes;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doip.library.comm.DoipTcpConnection;
import doip.library.comm.DoipTcpConnectionListener;
import doip.library.message.DoipTcpAliveCheckRequest;
import doip.library.message.DoipTcpAliveCheckResponse;
import doip.library.message.DoipTcpDiagnosticMessage;
import doip.library.message.DoipTcpDiagnosticMessageNegAck;
import doip.library.message.DoipTcpDiagnosticMessagePosAck;
import doip.library.message.DoipTcpHeaderNegAck;
import doip.library.message.DoipTcpRoutingActivationRequest;
import doip.library.message.DoipTcpRoutingActivationResponse;
import doip.simulation.standard.StandardTcpConnectionGateway;

public class AliveCheckResponseCollector implements DoipTcpConnectionListener {
		
	private static Logger logger = LogManager.getLogger(AliveCheckResponseCollector.class);

	private volatile HashMap<StandardTcpConnectionGateway, LinkedList<Integer>> responseMap =
			new HashMap<StandardTcpConnectionGateway, LinkedList<Integer>>();

	public HashMap<StandardTcpConnectionGateway, LinkedList<Integer>> getAliveCheckResponses() {
		return responseMap;
	}
	
	private synchronized void addAliveCheckResponse(StandardTcpConnectionGateway conn, DoipTcpAliveCheckResponse response) {
		LinkedList<Integer> responseList = null;
		if (responseMap.containsKey(conn)) {
			responseList = responseMap.get(conn);
		} else {
			responseList = new LinkedList<Integer>();
			responseMap.put(conn, responseList);
		}
		responseList.add(response.getSourceAddress());
	}

	@Override
	public void onConnectionClosed(DoipTcpConnection doipTcpConnection) {
	}

	@Override
	public void onDoipTcpDiagnosticMessage(DoipTcpConnection doipTcpConnection, DoipTcpDiagnosticMessage doipMessage) {
	}

	@Override
	public void onDoipTcpDiagnosticMessagePosAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessagePosAck doipMessage) {
	}

	@Override
	public void onDoipTcpDiagnosticMessageNegAck(DoipTcpConnection doipTcpConnection,
			DoipTcpDiagnosticMessageNegAck doipTcpDiagnosticMessageNegAck) {
	}

	@Override
	public void onDoipTcpRoutingActivationRequest(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationRequest doipMessage) {
	}

	@Override
	public void onDoipTcpRoutingActivationResponse(DoipTcpConnection doipTcpConnection,
			DoipTcpRoutingActivationResponse doipMessage) {
	}

	@Override
	public void onDoipTcpAliveCheckRequest(DoipTcpConnection doipTcpConnection, DoipTcpAliveCheckRequest doipMessage) {
	}

	@Override
	public void onDoipTcpAliveCheckResponse(DoipTcpConnection doipTcpConnection,
			DoipTcpAliveCheckResponse doipMessage) {
		int sourceAddressInResponse = doipMessage.getSourceAddress();
		StandardTcpConnectionGateway conn = (StandardTcpConnectionGateway) doipTcpConnection;
		int sourceAddressRegistered = conn.getRegisteredSourceAddress();
		if (sourceAddressInResponse == sourceAddressRegistered) {
			this.addAliveCheckResponse(conn, doipMessage);
		}
	}

	@Override
	public void onDoipTcpHeaderNegAck(DoipTcpConnection doipTcpConnection, DoipTcpHeaderNegAck doipMessage) {
	}
}
