package doip.simulation.standard;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

import doip.library.exception.DoipException;
import doip.simulation.GatewayConfig;
import doip.simulation.PlatformConfig;
import doip.simulation.api.Gateway;
import doip.simulation.api.Platform;
import doip.simulation.api.ServiceState;

public class StandardPlatform implements Platform {
	
	private List<Gateway> gateways = new ArrayList<Gateway>();
	
	private static Logger logger = LogManager.getLogger(StandardPlatform.class);
	
	private PlatformConfig config = null;
	
	private ServiceState state = ServiceState.STOPPED;

	public StandardPlatform(PlatformConfig config) throws DoipException {
		String method = "public StandardPlatform(String name)";
		try {
			logger.trace(">>> {}", method);
			this.config = config;
			List<GatewayConfig> gatewayConfigList = config.getCopyOfGatewayConfigList(); 
			for (GatewayConfig gatewayConfig : gatewayConfigList) {
				Gateway gateway = this.createGateway(gatewayConfig);
				gateways.add(gateway);
			}
		} finally {
			logger.trace("<<< {}", method);
		}
	}
	
	public StandardGateway createGateway(GatewayConfig config) {
		return new StandardGateway(config);
	}
	
	@Override
	public void start() throws DoipException {
		String method = "public void start()";
		try {
			logger.trace(">>> {}", method);
			for (Gateway gateway : this.gateways) {
				try {
					logger.debug("Start gateway with name {}", gateway.getName());
					gateway.start();
					logger.debug("Gateway ith name {} has been started", gateway.getName());
				} catch (DoipException e) {
					logger.error("Failed to start gatewa with name '" + gateway.getName() + "'");
					logger.info("All gateways which have been started before will be shut down");
					this.stopRunningGateways();
					this.state = ServiceState.ERROR;
					break;
				}
			}
			this.state = ServiceState.RUNNING;
		} finally {
			logger.trace("<<< {}", method);
		}
		
	}
	
	public void stopRunningGateways() {
		String method = "public void stopRunningGateways()";
		try {
			logger.trace(">>> {}'", method);
			for (Gateway gateway : this.gateways) {
				if (gateway.getState() == ServiceState.RUNNING) {
					gateway.stop();
				}
			}
		} finally {
			this.state = ServiceState.STOPPED;
			logger.trace("<<< {}", method);
		}
	}

	@Override
	public void stop() {
		String method = "public void stop()";
		logger.trace(">>> {}", method);
		stopRunningGateways();
		logger.trace("<<< {}", method);
	}

	@Override
	public String getName() {
		return config.getName();
	}

	@Override
	public ServiceState getState() {
		return state;
	}

	@Override
	public Gateway getGatewayByName(String name) {
		for (Gateway gateway : gateways) {
			if (name.equals(gateway.getName())) {
				return gateway;
			}
		}
		return null;
	}
	
	public void addGateway(Gateway gateway) {
		this.gateways.add(gateway);
	}

	@Override
	public List<Gateway> getGateways() {
		return gateways;
	}
}
