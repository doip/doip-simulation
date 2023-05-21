package doip.simulation;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.LinkedList;

import doip.library.comm.DoipUdpMessageHandler;
import doip.library.comm.DoipUdpMessageHandlerListener;
import doip.library.message.DoipUdpDiagnosticPowerModeRequest;
import doip.library.message.DoipUdpDiagnosticPowerModeResponse;
import doip.library.message.DoipUdpEntityStatusRequest;
import doip.library.message.DoipUdpEntityStatusResponse;
import doip.library.message.DoipUdpHeaderNegAck;
import doip.library.message.DoipUdpVehicleAnnouncementMessage;
import doip.library.message.DoipUdpVehicleIdentRequest;
import doip.library.message.DoipUdpVehicleIdentRequestWithEid;
import doip.library.message.DoipUdpVehicleIdentRequestWithVin;
import doip.library.util.LookupTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DoipUdpMessageHandlerTest implements DoipUdpMessageHandlerListener {
	
	private Logger logger = LogManager.getLogger(DoipUdpMessageHandlerTest.class);
	
	private DoipUdpVehicleIdentRequest lastDoipUdpVehicleIdentRequest = null;

	private int onDoipUdpVehicleIdentRequestCounter = 0;

	private LinkedList<DoipUdpMessageHandlerTestListener> listeners = new LinkedList<DoipUdpMessageHandlerTestListener>();
	
	private DoipUdpVehicleAnnouncementMessage lastDoipUdpVehicleAnnouncementMessage = null;
	
	private int onDoipUdpVehicleAnnouncementMessageCounter = 0;
	
	private DoipUdpMessageHandler doipUdpMessageHandler = null;
	
	public void addListener(DoipUdpMessageHandlerTestListener listener) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void addListener(DoipUdpMessageHandlerTestListener listener)");
		}

		this.listeners.add(listener);

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void addListener(DoipUdpMessageHandlerTestListener listener)");
		}
	}
	
	public void removeListener(DoipUdpMessageHandlerTestListener listener) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void removeListener(DoipUdpMessageHandlerTestListener listener)");
		}
		
		this.listeners.remove(listener);

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void removeListener(DoipUdpMessageHandlerTestListener listener)");
		}
	}
	
	
	public void start(DatagramSocket socket) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void start(DatagramSocket socket)");
		}
		
		doipUdpMessageHandler = new DoipUdpMessageHandler("TEST-UDP", new LookupTable());
		doipUdpMessageHandler.addListener(this);
		doipUdpMessageHandler.start(socket);
		
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void start(DatagramSocket socket)");
		}
	}
	
	public void stop() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void stop()");
		}
		
		doipUdpMessageHandler.stop();
		doipUdpMessageHandler.removeListener(this);

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void stop()");
		}
	}
	
	public void reset( ) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void reset()");
		}
		
		this.lastDoipUdpVehicleIdentRequest = null;
		this.onDoipUdpVehicleIdentRequestCounter = 0;
		
		this.lastDoipUdpVehicleAnnouncementMessage = null;
		this.onDoipUdpVehicleAnnouncementMessageCounter = 0;
		
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void reset()");
		}
	}
	
	/**
	 * Will be called when any DoIP UDP message had been received.
	 * It just calls the listeners of this class.
	 */
	private void onDoipUdpMessageReceived() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> private void onDoipUdpMessageReceived()");
		}
		
		for (DoipUdpMessageHandlerTestListener listener : this.listeners) {
			listener.onDoipUdpMessageReceived();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("<<< private void onDoipUdpMessageReceived()");
		}
	}
	

	@Override
	public void onDoipUdpVehicleIdentRequest(DoipUdpVehicleIdentRequest doipMessage, DatagramPacket packet) {
		this.lastDoipUdpVehicleIdentRequest = doipMessage;
		this.onDoipUdpVehicleIdentRequestCounter++;

		// Call listener
		this.onDoipUdpMessageReceived();
	}

	@Override
	public void onDoipUdpVehicleIdentRequestWithEid(DoipUdpVehicleIdentRequestWithEid doipMessage,
			DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpVehicleIdentRequestWithVin(DoipUdpVehicleIdentRequestWithVin doipMessage,
			DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpVehicleAnnouncementMessage(DoipUdpVehicleAnnouncementMessage doipMessage,
			DatagramPacket packet) {
		this.lastDoipUdpVehicleAnnouncementMessage = doipMessage;
		this.onDoipUdpVehicleAnnouncementMessageCounter++;
		// Call listeners
		this.onDoipUdpMessageReceived();
		
	}

	@Override
	public void onDoipUdpDiagnosticPowerModeRequest(DoipUdpDiagnosticPowerModeRequest doipMessage,
			DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpDiagnosticPowerModeResponse(DoipUdpDiagnosticPowerModeResponse doipMessage,
			DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpEntityStatusRequest(DoipUdpEntityStatusRequest doipMessage, DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpEntityStatusResponse(DoipUdpEntityStatusResponse doipMessage, DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoipUdpHeaderNegAck(DoipUdpHeaderNegAck doipMessage, DatagramPacket packet) {
		// TODO Auto-generated method stub
		
	}
	
	public DoipUdpVehicleIdentRequest getLastDoipUdpVehicleIdentRequest() {
		return lastDoipUdpVehicleIdentRequest;
	}

	public int getOnDoipUdpVehicleIdentRequestCounter() {
		return onDoipUdpVehicleIdentRequestCounter;
	}

	public DoipUdpVehicleAnnouncementMessage getLastDoipUdpVehicleAnnouncementMessage() {
		return lastDoipUdpVehicleAnnouncementMessage;
	}

	public int getOnDoipUdpVehicleAnnouncementMessageCounter() {
		return onDoipUdpVehicleAnnouncementMessageCounter;
	}

}
