package doip.simulation;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doip.library.message.UdsMessage;
import doip.library.util.LookupTable;
import doip.simulation.api.Ecu;

public abstract class EcuBase implements Ecu {

	/**
	 * log4j2 logger
	 */
	private static Logger logger = LogManager.getLogger(EcuBase.class);

	private EcuConfig config = null;

	private LinkedList<EcuListener> listeners = new LinkedList<EcuListener>();

	public EcuBase(EcuConfig config) {
		if (config.getName() == null) {
			throw new IllegalArgumentException("The value of 'name' in class EcuConfig is null, it must not be null");
		}
		this.config = config;
	}
	
	@Override
	public String getName() {
		return this.getConfig().getName();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

	@Override
	public LookupTable getConfiguredLookupTable() {
		return this.getConfig().getUdsLookupTable();
	}

	@Override
	public LookupTable getRuntimeLookupTable() {
		return this.getConfig().getUdsLookupTable();
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

	public abstract void putRequest(UdsMessage request);
}
