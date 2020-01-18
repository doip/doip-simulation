package doip.simulation.nodes;

import java.io.IOException;

import doip.logging.LogManager;
import doip.logging.Logger;

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
	private String filename = null;
	private String name = null;
	private int physicalAddress = 0;
	private int functionalAddress = 0;
	private String udsFiles;
	private LookupTable udsLookupTable = null;
	private String path = null;

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
		this.filename = filename;
		this.name = file.getMandatoryPropertyAsString("name");
		this.physicalAddress = file.getMandatoryPropertyAsInt("address.physical");
		this.functionalAddress = file.getOptionalPropertyAsInt("address.functional");
		this.udsFiles = file.getOptionalPropertyAsString("uds.files");

		this.udsLookupTable = new LookupTable();
		
		this.path = Helper.getPathOfFile(this.filename);
		if (this.udsFiles != null)
			loadUdsLookupTable();
	}

	/** 
	 * Loads the lookup tables from the files which are defined in member variable "udsFiles".
	 * 
	 * @throws IOException
	 */
	public void loadUdsLookupTable() throws IOException {
		String[] files = this.udsFiles.split(";");
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
}
