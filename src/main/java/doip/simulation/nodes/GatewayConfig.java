package doip.simulation.nodes;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;

import doip.logging.LogManager;
import doip.logging.Logger;

import doip.library.properties.EmptyPropertyValue;
import doip.library.properties.MissingProperty;
import doip.library.properties.PropertyFile;
import doip.library.util.Helper;
import doip.library.util.LookupTable;

/**
 * Contains the data from the configuration file for a gateway, for example for
 * the file GW.properties.
 */
public class GatewayConfig {

	private Logger logger = LogManager.getLogger(this.getClass());

	private String filename = null;
	private String path = null;
	private String name = null;
	private InetAddress localAddress = null;
	private int localPort = 0;
	private InetAddress multicastAddress = null;
	private String udpFiles = null;
	private String tcpFiles = null;

	private byte[] eid = null;
	private byte[] gid = null;
	private byte[] vin = null;
	private int logicalAddress = 0;

	private LookupTable udpLookupTable = null;
	private LookupTable tcpLookupTable = null;

	private int maxByteArraySizeLogging = 0;
	private int maxByteArraySizeLookup = 0;
	
	private String ecuFiles = null;
	private LinkedList<EcuConfig> ecuConfigList = new LinkedList<EcuConfig>();

	public LinkedList<EcuConfig> getEcuConfigList() {
		return ecuConfigList;
	}

	public String getEcuFiles() {
		return ecuFiles;
	}

	public byte[] getEid() {
		return eid;
	}

	public String getFilename() {
		return filename;
	}

	public byte[] getGid() {
		return gid;
	}

	public InetAddress getLocalAddress() {
		return localAddress;
	}

	public int getLocalPort() {
		return localPort;
	}

	public int getLogicalAddress() {
		return logicalAddress;
	}

	public InetAddress getMulticastAddress() {
		return multicastAddress;
	}

	public String getName() {
		return name;
	}

	public String getTcpFiles() {
		return this.tcpFiles;
	}

	public String getUdpFiles() {
		return udpFiles;
	}

	public LookupTable getUdpLookupTable() {
		return udpLookupTable;
	}

	public byte[] getVin() {
		return vin;
	}

	public void loadEcus() throws IOException, MissingProperty, EmptyPropertyValue {
		logger.trace(">>> public void loadEcus() throws IOException, MissingProperty, EmptyPropertyValue");
		if (this.ecuFiles == null) {
			logger.trace("<<< public void loadEcus() throws IOException, MissingProperty, EmptyPropertyValue");
			return;
		}
		String[] files = this.ecuFiles.split(";");
		for (int i = 0; i < files.length; i++) {
			String filenameWithPath = this.path + files[i];
			EcuConfig ecuConfig = new EcuConfig();
			ecuConfig.loadFromFile(filenameWithPath);
			this.ecuConfigList.add(ecuConfig);
		}
		logger.trace("<<< public void loadEcus() throws IOException, MissingProperty, EmptyPropertyValue");
	}

	/**
	 * Loads the gateway configuration from a file.
	 * @param filename
	 * @throws IOException
	 * @throws MissingProperty
	 * @throws EmptyPropertyValue
	 */
	public void loadFromFile(String filename) throws IOException, MissingProperty, EmptyPropertyValue {
		logger.trace(">>> public void loadFromFile(String filename)");
		logger.info("Load properties from file " + filename);
		try {
			PropertyFile file = new PropertyFile(filename);
			this.filename = filename;
			this.name = file.getMandatoryPropertyAsString("name");
			this.localAddress = file.getOptionalPropertyAsInetAddress("local.address");
			this.localPort = file.getMandatoryPropertyAsInt("local.port");
			this.multicastAddress = file.getOptionalPropertyAsInetAddress("multicast.address");
			this.udpFiles = file.getOptionalPropertyAsString("udp.files");
			this.tcpFiles = file.getOptionalPropertyAsString("tcp.files");
			this.ecuFiles = file.getOptionalPropertyAsString("ecu.files");
			this.maxByteArraySizeLogging = file.getMandatoryPropertyAsInt("maxByteArraySize.logging");
			this.maxByteArraySizeLookup = file.getMandatoryPropertyAsInt("maxByteArraySize.lookup");
			this.eid = file.getMandatoryPropertyAsByteArray("eid");
			this.gid = file.getMandatoryPropertyAsByteArray("gid");
			this.vin = file.getMandatoryPropertyAsByteArray("vin.hex");
			this.logicalAddress = file.getMandatoryPropertyAsInt("logicalAddress");

			this.path = Helper.getPathOfFile(this.filename);

			this.loadLookupTables();
			this.loadEcus();
		} catch (IOException e) {
			logger.trace("<<< public void loadFromFile(String filename) return with IOException");
			throw e;
		} catch (MissingProperty e) {
			logger.trace("<<< public void loadFromFile(String filename) return with MissingProperty");
			throw e;
		} catch (EmptyPropertyValue e) {
			logger.trace("<<< public void loadFromFile(String filename) return with EmptyPropertyValue");
			throw e;
		}
		logger.trace("<<< public void loadFromFile(String filename)");
	}

	
	public void loadLookupTables() throws IOException {
		logger.trace(">>> public void loadLookupTables() throws IOException");
		// Load UDP lookup table
		this.udpLookupTable = new LookupTable();
		if (this.udpFiles != null) {
			String[] files = this.udpFiles.split(";");
			this.udpLookupTable.addLookupEntriesFromFiles(path, files);
		}

		// Load TCP lookup table
		this.tcpLookupTable = new LookupTable();
		if (this.tcpFiles != null) {
			String[] files = this.tcpFiles.split(";");
			this.tcpLookupTable.addLookupEntriesFromFiles(path, files);
		}
		logger.trace("<<< public void loadLookupTables() throws IOException");
	}

	public void setEcuConfigList(LinkedList<EcuConfig> ecuConfigList) {
		this.ecuConfigList = ecuConfigList;
	}

	public void setEcuFiles(String ecuFiles) {
		this.ecuFiles = ecuFiles;
	}

	public void setEid(byte[] eid) {
		this.eid = eid;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void setGid(byte[] gid) {
		this.gid = gid;
	}

	public void setLocalAddress(InetAddress localAddress) {
		this.localAddress = localAddress;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public void setLogicalAddress(int logicalAddress) {
		this.logicalAddress = logicalAddress;
	}

	public void setMulticastAddress(InetAddress multicastAddress) {
		this.multicastAddress = multicastAddress;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setTcpFiles(String tcpFiles) {
		this.tcpFiles = tcpFiles;
	}

	public void setUdpFiles(String udpFiles) {
		this.udpFiles = udpFiles;
	}

	public void setUdpLookupTable(LookupTable udpLookupTable) {
		this.udpLookupTable = udpLookupTable;
	}

	public void setVin(byte[] vin) {
		this.vin = vin;
	}

	public int getMaxByteArraySizeLogging() {
		return maxByteArraySizeLogging;
	}

	public void setMaxByteArraySizeLogging(int maxByteArraySizeLogging) {
		this.maxByteArraySizeLogging = maxByteArraySizeLogging;
	}

	public int getMaxByteArraySizeLookup() {
		return maxByteArraySizeLookup;
	}

	public void setMaxByteArraySizeLookup(int maxByteArraySizeLookup) {
		this.maxByteArraySizeLookup = maxByteArraySizeLookup;
	}
}
