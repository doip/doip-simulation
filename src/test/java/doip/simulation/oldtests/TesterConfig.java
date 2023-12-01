package doip.simulation.oldtests;

import java.io.IOException;

import doip.library.properties.EmptyPropertyValue;
import doip.library.properties.MissingProperty;
import doip.library.properties.PropertyFile;

public class TesterConfig {

	private PropertyFile file = null;
	
	public TesterConfig(String filename) throws IOException, MissingProperty, EmptyPropertyValue {
		file = new PropertyFile(filename);
		file.getMandatoryPropertyAsInt("maxByteArraySize.logging");
	}
}
