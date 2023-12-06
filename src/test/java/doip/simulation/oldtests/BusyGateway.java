package doip.simulation.oldtests;

import doip.simulation.EcuBase;
import doip.simulation.EcuConfig;
import doip.simulation.GatewayConfig;
import doip.simulation.standard.StandardGateway;

public class BusyGateway extends StandardGateway {

	public BusyGateway(GatewayConfig config) {
		super(config);
	}

	@Override
	public EcuBase createEcu(EcuConfig config) {
		return new BusyEcu(config);
	}

}
