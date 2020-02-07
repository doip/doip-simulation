package doip.tester;

import doip.simulation.nodes.Ecu;
import doip.simulation.nodes.EcuConfig;
import doip.simulation.nodes.GatewayConfig;
import doip.simulation.standard.StandardGateway;

public class BusyGateway extends StandardGateway {

	public BusyGateway(GatewayConfig config) {
		super(config);
	}

	@Override
	public Ecu createEcu(EcuConfig config) {
		return new BusyEcu(config);
	}

}
