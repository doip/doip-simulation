package doip.simulation;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

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

public class DoipUdpMessageHandlerTest implements DoipUdpMessageHandlerListener {
	
	private DoipUdpVehicleIdentRequest lastDoipUdpVehicleIdentRequest = null;
	private int onDoipUdpVehicleIdentRequestCounter = 0;
	
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

	private DoipUdpVehicleAnnouncementMessage lastDoipUdpVehicleAnnouncementMessage = null;
	private int onDoipUdpVehicleAnnouncementMessageCounter = 0;
	
	private DoipUdpMessageHandler doipUdpMessageHandler = null;
	
	public void start(DatagramSocket socket) {
		doipUdpMessageHandler = new DoipUdpMessageHandler("TEST-UDP", new LookupTable());
		doipUdpMessageHandler.addListener(this);
		doipUdpMessageHandler.start(socket);
	}
	
	public void stop() {
		doipUdpMessageHandler.stop();
		doipUdpMessageHandler.removeListener(this);
	}
	
	public void reset( ) {
		this.lastDoipUdpVehicleIdentRequest = null;
		this.onDoipUdpVehicleIdentRequestCounter = 0;
		
		this.lastDoipUdpVehicleAnnouncementMessage = null;
		this.onDoipUdpVehicleAnnouncementMessageCounter = 0;
	}

	@Override
	public void onDoipUdpVehicleIdentRequest(DoipUdpVehicleIdentRequest doipMessage, DatagramPacket packet) {
		this.lastDoipUdpVehicleIdentRequest = doipMessage;
		this.onDoipUdpVehicleIdentRequestCounter++;
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

}
