package doip.simulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doip.library.properties.EmptyPropertyValue;
import doip.library.properties.MissingProperty;
import doip.library.properties.PropertyFile;
import doip.library.util.Helper;

/**
 * Configuration for a platform. The platform configuration can be loaded
 * from a file. 
 */
public class PlatformConfig {
	
	private static Logger logger = LogManager.getLogger(PlatformConfig.class);
	
	// Name of the platform
	private String name;
	
	// value from property file for this platform, property name = "gateways"
	String gatewayFiles;

	List<GatewayConfig> gatewayConfigList = new ArrayList<GatewayConfig>();

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void addGatewayConfig(GatewayConfig config) {
		synchronized (gatewayConfigList) {
			this.gatewayConfigList.add(config);
		}
	}
	
	public List<GatewayConfig> getCopyOfGatewayConfigList() {
		List<GatewayConfig> copy = new ArrayList<>();
		
		synchronized(gatewayConfigList) {
			copy.addAll(gatewayConfigList);
		}
		return copy;
	}
	
	public void loadFromFile(String filename) throws IOException, MissingProperty, EmptyPropertyValue {
		String method = "public void loadFromFile(String filename)";
		try {
			logger.trace(">>> {}", method);
			PropertyFile file = new PropertyFile(filename);
			this.name = file.getMandatoryPropertyAsString("name");
			this.gatewayFiles = file.getMandatoryPropertyAsString("gateway.files");
			String path = Helper.getPathOfFile(filename);
			loadGatewayConfigs(path, gatewayFiles);

		} catch (IOException e) {
			throw logger.throwing(e);
		} catch (MissingProperty e) {
			throw logger.throwing(e);
		} catch (EmptyPropertyValue e) {
			throw logger.throwing(e);
		} finally {
			logger.trace("<<< {}", method);
		}
	}
	
	public void loadGatewayConfigs(String path, String gatewayFiles) throws IOException, MissingProperty, EmptyPropertyValue {
		
		String[] files = gatewayFiles.split(";");
		for (int i = 0; i < files.length; i++) {
			String filenameWithPath = path + files[i];
			GatewayConfig config = new GatewayConfig();
			config.loadFromFile(filenameWithPath);
			this.gatewayConfigList.add(config);
		}
	}
}
