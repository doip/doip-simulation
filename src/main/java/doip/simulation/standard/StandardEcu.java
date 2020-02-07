package doip.simulation.standard;

import java.util.Arrays;

import doip.logging.LogManager;
import doip.logging.Logger;
import doip.simulation.nodes.Ecu;
import doip.simulation.nodes.EcuConfig;
import doip.library.message.UdsMessage;
import doip.library.util.Conversion;
import doip.library.util.Helper;

public class StandardEcu extends Ecu implements Runnable {

	private static Logger logger = LogManager.getLogger(StandardEcu.class);

	private volatile UdsMessage currentRequest = null;

	private volatile Thread thread = null;

	private volatile boolean runFlag = false;

	private volatile boolean busyFlag = false;

	public StandardEcu(EcuConfig config) {
		super(config);
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public StandardEcu(EcuConfig config)");
			logger.debug("Create Srandard ECU with name " + config.getName());
			logger.trace("<<< public StandardEcu(EcuConfig config)");
		}
	}

	/**
	 * Will be called from outside to handle a new request. The implementation here
	 * stores the new request in a local variable and returns immediately. The
	 * request will be handled later by the thread of the ECU. If there is still a
	 * request stored locally which had not been handled "busy" variable will be set
	 * to indicate that later in the thread of the ECU a negative response with NRC
	 * 0x21 (busy, repeat request) should be send.
	 * 
	 * @param request The new request.
	 */
	public synchronized void dropRequest(UdsMessage request) {
		logger.trace(">>> public void dropRequest(UdsMessage message)");
		if (this.getCurrentRequest() == null) {
			this.setCurrentRequest(request);
			logger.info("UDS request queued for processing");
		} else {
			this.busyFlag = true;
		}
		logger.trace("<<< public void dropRequest(UdsMessage message)");
	}

	private UdsMessage getCurrentRequest() {
		return currentRequest;
	}

	/**
	 * Will be called by the thread of the ECU when a new request had been received.
	 * 
	 * @param request The new request which had been received.
	 */
	public void onUdsMessageReceived(UdsMessage request) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void onUdsMessageReceived(UdsMessage request)");
		}

		if (logger.isDebugEnabled()) {
			logger.info("UDS request, data = " + Conversion.byteArrayToHexStringShortDotted(request.getMessage(), 16));
		}

		boolean ret = false;
		
		ret = processUdsMessageBeforeLookupTable(request);
		if (ret) {
			if (logger.isTraceEnabled()) {
				logger.trace("<<< public void onUdsMessageReceived(UdsMessage request)");
			}
			return;
		}
		ret = processUdsMessageByLookupTable(request);
		if (ret) {
			if (logger.isTraceEnabled()) {
				logger.trace("<<< public void onUdsMessageReceived(UdsMessage request)");
			}
			return;
		}
		processUdsMessageAfterLookupTable(request);

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void onUdsMessageReceived(UdsMessage request)");
		}
	}

	/**
	 * Will be called to handle the request with some "special" handling. The
	 * function here has no implementation, it is supposed to get overridden by a
	 * child class to realize some special handling or behavior.
	 * 
	 * @param udsRequest The new request
	 * @return returns true if the request had been handled and no further
	 *         processing is required.
	 */
	public boolean processUdsMessageBeforeLookupTable(UdsMessage udsRequest) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public boolean processUdsMessageByFunction(UdsMessage request)");
		}

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public boolean processUdsMessageByFunction(UdsMessage request)");
		}
		return false;
	}

	/**
	 * Will process a UDS message by finding a matching pattern in the lookup table.
	 * 
	 * @param request
	 * @return
	 */
	public boolean processUdsMessageByLookupTable(UdsMessage request) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public boolean processUdsMessageByLookupTable(UdsMessage request)");
		}
		
		if (this.getConfig().getUdsLookupTable() == null) {
			logger.info("No UDS lookup table defined");
			return false;
		}
		
		boolean ret = false;
		byte[] requestMessage = request.getMessage();
		byte[] requestMessageShort = null;
		if (requestMessage.length > 16) {
			requestMessageShort = Arrays.copyOf(requestMessage, 16);
		} else {
			requestMessageShort = requestMessage;
		}
		byte[] response = this.getConfig().getUdsLookupTable().findResultAndApplyModifiers(requestMessageShort);
		if (response != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found matching request pattern, response = "
						+ Conversion.byteArrayToHexStringShortDotted(response, 64));
			}
			UdsMessage udsResponse = new UdsMessage(this.getConfig().getPhysicalAddress(), request.getSourceAdrress(),
					UdsMessage.PHYSICAL, response);
			
			this.onSendUdsMessage(udsResponse);
			this.setCurrentRequest(null);
			ret = true;
		} else {
			if (logger.isInfoEnabled()) {
				logger.info("Could not find a matching request pattern");
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("<<< public boolean processUdsMessageByLookupTable(UdsMessage request)");
		}
		return ret;
	}

	/**
	 * Handles a UDS request with a default implementation. The default
	 * implementation sends a negative response with NRC 0x10 (general reject).
	 * 
	 * @param request The received UDS request message.
	 * @return Returns true if the message had been handled
	 */
	public boolean processUdsMessageAfterLookupTable(UdsMessage request) {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public void processUdsMessageByMessageInterpretation(UdsMessage request)");
		}
		int source = request.getSourceAdrress();
		byte[] requestMessage = request.getMessage();
		byte[] responseMessage = new byte[] { 0x7F, 0x00, 0x10 };
		
		if (requestMessage.length > 0) {
			responseMessage[1] = requestMessage[0];
		}
		
		UdsMessage response = new UdsMessage(this.getConfig().getPhysicalAddress(), source, UdsMessage.PHYSICAL,
				responseMessage);
		
		this.onSendUdsMessage(response);
		this.setCurrentRequest(null);

		if (logger.isTraceEnabled()) {
			logger.trace("<<< public void processUdsMessageByMessageInterpretation(UdsMessage request)");
		}
		
		return true;
	}

	@Override
	public void run() {
		logger.trace(">>> public void run()");

		while (this.runFlag) {

			handleRequest();
			sleep(1);
		}

		logger.trace("<<< public void run()");
	}

	private synchronized void handleRequest() {

		// Don't log function entry and exit! It would be called every millisecond.
		
		if (this.busyFlag) {
			
			// Found potential bug: If busy Flag is set the  the current request
			// is still the old request. The service ID shall be the SID from the
			// new request. 
			UdsMessage request = this.getCurrentRequest();
			byte sid = request.getMessage()[0];
			UdsMessage response = new UdsMessage(this.getConfig().getPhysicalAddress(), request.getSourceAdrress(),
					UdsMessage.PHYSICAL, new byte[] { 0x7F, sid, 0x21 });
			this.onSendUdsMessage(response);
			this.setCurrentRequest(null);
			this.busyFlag = false;
		}

		UdsMessage currentRequest = this.getCurrentRequest();
		
		if (currentRequest != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Picked up new request to handle it");
			}
			onUdsMessageReceived(currentRequest);
		}
	}

	private void setCurrentRequest(UdsMessage currentRequest) {
		this.currentRequest = currentRequest;
	}

	/**
	 * Simple wrapper for Thread.sleep(int)
	 * @param millis The time to sleep in milliseconds.
	 */
	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.debug(Helper.getExceptionAsString(e));
		}
	}

	@Override
	public void start() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> void start()");
		}
		this.runFlag = true;
		this.thread = new Thread(this, this.getConfig().getName());
		this.thread.start();
		if (logger.isTraceEnabled()) {
			logger.trace("<<< void start()");
		}
	}

	@Override
	public void stop() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> void stop()");
		}
		this.runFlag = false;
		try {
			this.thread.join();
		} catch (InterruptedException e) {
			logger.error(Helper.getExceptionAsString(e));
		}
		this.thread = null;
		if (logger.isTraceEnabled()) {
			logger.trace("<<< void stop()");
		}
	}
}
