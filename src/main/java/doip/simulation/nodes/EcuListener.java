package doip.simulation.nodes;

import doip.library.message.UdsMessage;

public interface EcuListener {
	
	/**
	 * This function will be called when the class 'Ecu' wants to send 
	 * a request. The class which implements this interface shall send the
	 * message to the related target ECU.
	 * 
	 * @param udsMessage
	 */
	public void onSendUdsMessage(UdsMessage udsMessage);
}
