package doip.simulation;

import static doip.junit.Assert.assertFalse;
import static doip.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import doip.library.message.UdsMessage;
import doip.library.util.LookupEntry;
import doip.library.util.LookupTable;
import doip.simulation.nodes.EcuConfig;
import doip.simulation.standard.StandardEcu;

public class MaxByteArraySizeLookup {
	
	private StandardEcu ecu = null;
	
	private EcuConfig config = null;


	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		config = new EcuConfig();
		config.setName("ECU");
		config.setMaxByteArraySizeLogging(1);
		config.setMaxByteArraySizeLookup(1);
		
		LookupTable lut = new LookupTable();
		LookupEntry entry = new LookupEntry("10", "50");
	
		lut.addEntry(entry);
		config.setUdsLookupTable(lut);
		ecu = new StandardEcu(config);
	
		UdsMessage msg = new UdsMessage(0x0E00, 0x100, new byte[] {0x10, 0x03});
		boolean ret = ecu.processRequestByLookupTable(msg);
		assertTrue("The UDS message did not match the regular expression", ret);
		
		config.setMaxByteArraySizeLookup(2);
		msg = new UdsMessage(0x0E00, 0x100, new byte[] {0x10, 0x03});
		ret = ecu.processRequestByLookupTable(msg);
		assertFalse("The UDS message did match the regular expression", ret);
	}
}
