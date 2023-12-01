package doip.simulation.nodes;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doip.library.properties.EmptyPropertyValue;
import doip.library.properties.MissingProperty;
import doip.library.properties.PropertyFile;
import doip.library.util.Helper;
import doip.library.util.LookupTable;

/**
 * Stores the configuration for an ECU
 */
public class EcuConfig {

	private static Logger logger = LogManager.getLogger(EcuConfig.class);
	private String name = null;
	private int physicalAddress = 0;
	private int functionalAddress = 0;
	private LookupTable udsLookupTable = null;
	
	private int maxByteArraySizeLookup = 0;
	
	private int maxByteArraySizeLogging = 0;

	public int getFunctionalAddress() {
		return functionalAddress;
	}

	public String getName() {
		return name;
	}

	public int getPhysicalAddress() {
		return physicalAddress;
	}

	public LookupTable getUdsLookupTable() {
		return udsLookupTable;
	}

	/**
	 * Loads the configuration from a file
	 * @param filename
	 * @throws IOException
	 * @throws MissingProperty
	 * @throws EmptyPropertyValue
	 */
	public void loadFromFile(String filename) throws IOException, MissingProperty, EmptyPropertyValue {
		logger.info("Load properties from file " + filename);

		PropertyFile file = new PropertyFile(filename);
		this.name = file.getMandatoryPropertyAsString("name");
		this.physicalAddress = file.getMandatoryPropertyAsInt("address.physical");
		this.functionalAddress = file.getOptionalPropertyAsInt("address.functional", -1);
		String udsFiles = file.getOptionalPropertyAsString("uds.files");

		this.udsLookupTable = createLookupTable();
		
		String path = Helper.getPathOfFile(filename);
		if (udsFiles != null)
			loadUdsLookupTable(path, udsFiles);
	}
	
	/**
	 * Creates a new empty lookup table
	 * @return The new lookup table
	 */
	public LookupTable createLookupTable() {
		return new LookupTable();
	}

	/** 
	 * Loads the lookup tables from the files which are defined in member variable "udsFiles".
	 * 
	 * @throws IOException
	 */
	public void loadUdsLookupTable(String path, String udsFiles) throws IOException {
		String[] files = udsFiles.split(";");
		this.udsLookupTable.addLookupEntriesFromFiles(path, files);
	}

	public void setFunctionalAddress(int functionalAddress) {
		this.functionalAddress = functionalAddress;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPhysicalAddress(int physicalAddress) {
		this.physicalAddress = physicalAddress;
	}

	public void setUdsLookupTable(LookupTable udsLookupTable) {
		this.udsLookupTable = udsLookupTable;
	}

	public int getMaxByteArraySizeLookup() {
		return maxByteArraySizeLookup;
	}

	public void setMaxByteArraySizeLookup(int maxByteArraySizeLookup) {
		this.maxByteArraySizeLookup = maxByteArraySizeLookup;
	}

	public int getMaxByteArraySizeLogging() {
		return maxByteArraySizeLogging;
	}

	public void setMaxByteArraySizeLogging(int maxByteArraySizeLogging) {
		this.maxByteArraySizeLogging = maxByteArraySizeLogging;
	}
}
