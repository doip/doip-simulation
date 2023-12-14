package doip.simulation.standard;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import doip.library.exception.DoipException;
import doip.library.properties.EmptyPropertyValue;
import doip.library.properties.MissingProperty;
import doip.simulation.GatewayConfig;
import doip.simulation.api.Platform;
import doip.simulation.api.SimulationManager;

public class StandardSimulationManager implements SimulationManager {
	
	private List<Platform> platforms = new ArrayList<Platform>();
	
	private static Logger logger = LogManager.getLogger(StandardSimulationManager.class);
	
	@Override
	public void start(String regex) throws DoipException {
		String method = "public void start(String platformName)";
		try {
			logger.trace(">>> {}", method);
			logger.info("Start all platforms which match the regular expression \"{}\"", regex); 
			for (Platform platform : platforms) {
				String name = platform.getName();
				if (name.matches(regex)) {
					platform.start();				
				}
			}
		} finally {
			logger.trace("<<< {}", method);
		}
	}

	@Override
	public void stop(String regex) {
		String method = "public void stop(String platformName)";
		try {
			logger.trace(">>> {}", method);
			logger.info("Stop all platforms which match the regular expression \"{}\"", regex); 
			for (Platform platform : platforms) {
				String name = platform.getName();
				if (name.matches(regex)) {
					platform.stop();				
				}
			}
		} finally {
			logger.trace("<<< ", method);
		}
	}

	@Override
	public Platform getPlatformByName(String platformName) {
		String method = "public Platform getPlatformByName(String platformName)";
		try {
			logger.trace(">>> {}", method);
			for (Platform platform : platforms) {
				String name = platform.getName();
				if (platformName.equals(name)) {
					return platform;
				}
			}
		} finally {
			logger.trace("<<< {}", method);
		}
		return null;
	}

	@Override
	public List<Platform> getPlatforms() {
		return this.platforms;
	}
	
	public void addPlatform(Platform platform) {
		this.platforms.add(platform);
	}
	
	public void removePlatform(Platform platform) {
		this.platforms.remove(platform);
	}
}
