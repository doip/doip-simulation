package doip.tester;

import doip.library.message.UdsMessage;
import doip.simulation.nodes.EcuConfig;
import doip.simulation.standard.StandardEcu;

public class BusyEcu extends StandardEcu {

	@Override
	public boolean processRequestAfterLookupTable(UdsMessage request) {
		
		// Send back only one response pending and don't clear
		// the current diagnostic request message
		byte[] response = new byte[] {0x7F, request.getMessage()[0], 0x78};
		
		UdsMessage udsMsg = new UdsMessage(this.getConfig().getPhysicalAddress(), request.getSourceAdrress(), response);
		this.onSendUdsMessage(udsMsg);
		
		return false;
	}

	public BusyEcu(EcuConfig config) {
		super(config);
	}

}
