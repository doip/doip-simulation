package doip.simulation.standard;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import doip.simulation.nodes.EcuBase;
import doip.simulation.nodes.EcuConfig;
import doip.library.message.UdsMessage;
import doip.library.util.Conversion;
import doip.library.util.Helper;
import doip.library.util.LookupTable;

/**
 * Implements the standard behavior of an ECU. The ECU is implemented as a
 * thread and needs to be started with the function "start()" and can be stopped
 * with the function "stop()". A new request can be hand over to the ECU with
 * the function "putRequest(...)".
 */
public class StandardEcu extends EcuBase implements Runnable {

	private static Logger logger = LogManager.getLogger(StandardEcu.class);

	/**
	 * The current request which will be processed by the ECU. In case the ECU
	 * is not busy a new request will also be stored in this variable.
	 * 
	 */
	private volatile UdsMessage currentRequest = null;

	/**
	 * The message processing is implemented as a thread. This thread will be
	 * stored in this variable.
	 */
	private volatile Thread thread = null;

	/**
	 * Flag if the thread shall run. When function "stop()" will be called the
	 * flag will be set to false and the thread returns from his "run" function
	 */
	private volatile boolean runFlag = false;

	/**
	 * Flag to store the information if the ECU is busy which means that there
	 * is still a request which the ECU is processing at the moment. When the
	 * ECU has finished processing the current request the ECU must call the
	 * function "clearCurrentRequest()".
	 */
	private volatile boolean isBusy = false;

	/**
	 * Constructor
	 * 
	 * @param config The configuration for this ECU
	 */
	public StandardEcu(EcuConfig config) {
		super(config);
		if (logger.isTraceEnabled()) {
			logger.trace(">>> public StandardEcu(EcuConfig config)");
			logger.debug("Create Standard ECU with name " + config.getName());
			logger.trace("<<< public StandardEcu(EcuConfig config)");
		}
	}

	/**
	 * Shall be called from outside to handle a new request.
	 * 
	 * @param request The new request.
	 */
	public synchronized void putRequest(UdsMessage request) {
		logger.trace(">>> public void putRequest(UdsMessage message)");

		if (isBusy) {
			logger.info(
					"ECU is busy, request can not be queued for processing");
			this.handleRequestIfBusy(request);
		} else {
			this.setCurrentRequest(request);
			logger.info("UDS request queued for processing");
		}

		logger.trace("<<< public void putRequest(UdsMessage message)");
	}

	/**
	 * Handles a request in case the ECU is still processing the last request.
	 * Will be called by function "putRequest(...)".
	 * 
	 * @param request
	 */
	public void handleRequestIfBusy(UdsMessage request) {
		if (logger.isTraceEnabled()) {
			logger.trace(
					">>> public void handleRequestIfBusy(UdsMessage request)");
		}

		// Send busy repeat request
		byte[] response = new byte[] { 0x7F, request.getMessage()[0], 0x21 };
		UdsMessage udsMsg = new UdsMessage(
				this.getConfig().getPhysicalAddress(),
				request.getSourceAdrress(), response);
		this.onSendUdsMessage(udsMsg);

		if (logger.isTraceEnabled()) {
			logger.trace(
					"<<< public void handleRequestIfBusy(UdsMessage request)");
		}
	}

	/**
	 * Getter for the current request which will be processed at the moment.
	 * 
	 * @return
	 */
	public synchronized UdsMessage getCurrentRequest() {
		return currentRequest;
	}

	/**
	 * Sets the current request.
	 * 
	 * @param currentRequest
	 */
	public synchronized void setCurrentRequest(UdsMessage currentRequest) {
		this.currentRequest = currentRequest;
	}

	/**
	 * Clears the current request (which means that current request will be set
	 * to null) and clears the "isBusy" flag. After calling this function the
	 * ECU is ready to receive new requests.
	 */
	public synchronized void clearCurrentRequest() {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> private synchronized void clearCurrentRequest()");
		}

		logger.info(
				"Processing of request finished, ready to receive new request");

		this.currentRequest = null;
		this.isBusy = false;

		if (logger.isTraceEnabled()) {
			logger.trace("<<< private synchronized void clearCurrentRequest()");
		}
	}

	/**
	 * Will be called by the thread of the ECU when a new request had been
	 * received. It calls the functions
	 * <ol>
	 * <li>processRequestBeforeLookupTable</li>
	 * <li>processRequestByLookupTable</li>
	 * <li>processRequestAfterLookupTable</li>
	 * </ol>
	 * 
	 * If one of these functions returns true processing is finished and the
	 * next functions will not be called any more.
	 * 
	 * @param request The new request which shall be processed
	 */
	public void handleRequest(UdsMessage request) {
		if (logger.isTraceEnabled()) {
			logger.trace(
					">>> public void onRequestReceived(UdsMessage request)");
		}

		if (logger.isDebugEnabled()) {
			logger.info("UDS request, data = "
					+ Conversion.byteArrayToHexStringShortDotted(
							request.getMessage(), 16));
		}

		boolean ret = false;

		ret = processRequestBeforeLookupTable(request);

		if (ret) {
			if (logger.isTraceEnabled()) {
				logger.trace(
						"<<< public void onRequestReceived(UdsMessage request)");
			}
			return;
		}

		ret = processRequestByLookupTable(request);

		if (ret) {
			if (logger.isTraceEnabled()) {
				logger.trace(
						"<<< public void onRequestReceived(UdsMessage request)");
			}
			return;
		}

		processRequestAfterLookupTable(request);

		if (logger.isTraceEnabled()) {
			logger.trace(
					"<<< public void onRequestReceived(UdsMessage request)");
		}
	}

	/**
	 * Will be called to handle the request with some "special" handling. The
	 * function here has no implementation, it is supposed to get overridden by
	 * a child class to realize some special handling or behavior.
	 * 
	 * @param udsRequest The new request
	 * @return returns true if the request had been handled and no further
	 *         processing is required.
	 */
	public boolean processRequestBeforeLookupTable(UdsMessage udsRequest) {
		if (logger.isTraceEnabled()) {
			logger.trace(
					">>> public boolean processRequestBeforeLookupTable(UdsMessage request)");
		}

		if (logger.isTraceEnabled()) {
			logger.trace(
					"<<< public boolean processRequestBeforeLookupTable(UdsMessage request)");
		}
		return false;
	}

	/**
	 * Will process a UDS request by finding a matching request pattern in the
	 * lookup table. If a request pattern matches it will send the response and
	 * clear the request to be ready to receive a new request.
	 * 
	 * @param request The UDS request message
	 * 
	 * @return Returns true if a matching request could be found in the lookup
	 *         table and a response had been sent back to the tester. If no
	 *         matching request pattern could be found the function returns
	 *         false to indicate that further processing of this request needs
	 *         to be done.
	 */
	public boolean processRequestByLookupTable(UdsMessage request) {
		if (logger.isTraceEnabled()) {
			logger.trace(
					">>> public boolean processRequestByLookupTable(UdsMessage request)");
		}

		LookupTable lookupTable = this.getConfig().getUdsLookupTable();

		if (lookupTable == null) {
			logger.info("No UDS lookup table defined");
			if (logger.isTraceEnabled()) {
				logger.trace(
						"<<< public boolean processRequestByLookupTable(UdsMessage request)");
			}
			return false;
		}

		boolean ret = false;

		byte[] requestMessage = request.getMessage();
		byte[] requestMessageShort = null;

		int maxByteArraySizeLookup = this.getConfig().getMaxByteArraySizeLookup();
		
		if (requestMessage.length > maxByteArraySizeLookup) {
			requestMessageShort = Arrays.copyOf(requestMessage, maxByteArraySizeLookup);
		} else {
			requestMessageShort = requestMessage;
		}

		byte[] response = lookupTable
				.findResultAndApplyModifiers(requestMessageShort);

		if (response != null) {
			if (logger.isInfoEnabled()) {
				int maxByteArraySizeLogging = this.getConfig().getMaxByteArraySizeLogging();
				logger.info("Found matching request pattern, response = "
						+ Conversion.byteArrayToHexStringShortDotted(response,
								maxByteArraySizeLogging));
			}
			
			if (response.length <= 0) {
				// Response with 0 bytes -> Don't send an answer
				// Just clear current request and be read to receive
				// new requests
				this.clearCurrentRequest();
			} else {
				// Send response message
				UdsMessage udsResponse = new UdsMessage(
						/* source address */ this.getConfig().getPhysicalAddress(),
						/* target address */ request.getSourceAdrress(),
						/* response */ UdsMessage.PHYSICAL, response);

				this.clearCurrentRequest();
				this.onSendUdsMessage(udsResponse);
			}
			
			ret = true;

		} else {
			if (logger.isInfoEnabled()) {
				logger.info("Could not find a matching request pattern");
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace(
					"<<< public boolean processRequestByLookupTable(UdsMessage request)");
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
	public boolean processRequestAfterLookupTable(UdsMessage request) {
		if (logger.isTraceEnabled()) {
			logger.trace(
					">>> public void processUdsMessageByMessageInterpretation(UdsMessage request)");
		}

		int sourceAddress = request.getSourceAdrress();
		byte[] requestMessage = request.getMessage();
		byte[] responseMessage = new byte[] { 0x7F, 0x00, 0x10 };

		if (requestMessage.length > 0) {
			responseMessage[1] = requestMessage[0];
		}

		UdsMessage response = new UdsMessage(
				this.getConfig().getPhysicalAddress(), sourceAddress,
				UdsMessage.PHYSICAL, responseMessage);

		this.clearCurrentRequest();
		this.onSendUdsMessage(response);

		if (logger.isTraceEnabled()) {
			logger.trace(
					"<<< public void processUdsMessageByMessageInterpretation(UdsMessage request)");
		}

		return true;
	}

	@Override
	public void run() {
		logger.trace(">>> public void run()");

		while (this.runFlag) {
			checkAndHandleNewRequest();
			sleep(1);
		}

		logger.trace("<<< public void run()");
	}

	/**
	 * Checks if a new request is available and handles the new request.
	 */
	protected synchronized void checkAndHandleNewRequest() {

		// Don't log function entry and exit! It would be called every
		// millisecond.

		if (isBusy == false) {

			// Check if there is a new request
			UdsMessage currentRequest = this.getCurrentRequest();
			if (currentRequest != null) {

				// Now ECU is busy with processing the new request
				this.isBusy = true;
				if (logger.isInfoEnabled()) {
					logger.info("Picked up new request to handle it");
				}
				this.handleRequest(currentRequest);
			}
		}
	}

	/**
	 * Simple wrapper for Thread.sleep(int). If the thread will be interrupted a
	 * log message with level FATAL will be logged.
	 * 
	 * @param millis The time to sleep in milliseconds.
	 * @return Returns true if the thread did sleep time. Returns false if the
	 *         thread had been interrupted.
	 */
	// TODO: Move this function to a new base class "DoipThread"
	protected boolean sleep(int millis) {
		try {
			Thread.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			logger.fatal(
					"The function Thread.sleep(millis) had been unexpectly interrupted.");
			logger.fatal(Helper.getExceptionAsString(e));
		}
		return false;
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
