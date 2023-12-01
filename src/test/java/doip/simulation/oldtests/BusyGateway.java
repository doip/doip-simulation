package doip.simulation.oldtests;

import doip.simulation.nodes.EcuBase;
import doip.simulation.nodes.EcuConfig;
import doip.simulation.nodes.GatewayConfig;
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
