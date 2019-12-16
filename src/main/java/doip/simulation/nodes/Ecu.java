package doip.simulation.nodes;

import java.util.Iterator;
import java.util.LinkedList;

import doip.logging.LogManager;
import doip.logging.Logger;

import doip.library.message.UdsMessage;

public abstract class Ecu {

	/**
	 * log4j2 logger
	 */
	private static Logger logger = LogManager.getLogger(Ecu.class);

	private EcuConfig config = null;

	private LinkedList<EcuListener> listeners = new LinkedList<EcuListener>();

	public Ecu(EcuConfig config) {
		this.config = config;
	}

	public void addListener(EcuListener listener) {
		logger.trace(">>> public void addListener(EcuListener listener)");
		this.listeners.add(listener);
		logger.trace("<<< public void addListener(EcuListener listener)");
	}

	public EcuConfig getConfig() {
		return config;
	}

	public void removeListener(EcuListener listener) {
		logger.trace(">>> public void removeListener(EcuListener listener)");
		this.listeners.remove(listener);
		logger.trace("<<< public void removeListener(EcuListener listener)");
	}

	/**
	 * Will be called when the ECU needs to send a message. The ECU itself does
	 * not have a reference to a socket. Therefore there must be implemented
	 * some mechanism that an ECU can send an UDS message. This is realized here 
	 * with the listener. A class which is capable to send messages shall register itself
	 * at this class and send the message.
	 * 
	 * @param message The message which shall be send.
	 */
	public void onSendUdsMessage(UdsMessage message) {
		logger.trace(">>> public void onSendUdsMessage(UdsMessage message)");
		Iterator<EcuListener> iter = listeners.iterator();
		while (iter.hasNext()) {
			EcuListener listener = iter.next();
			listener.onSendUdsMessage(message);
		}
		logger.trace("<<< public void onSendUdsMessage(UdsMessage message)");
	}

	public void setConfig(EcuConfig config) {
		this.config = config;
	}

	public abstract void start();

	public abstract void stop();
}
