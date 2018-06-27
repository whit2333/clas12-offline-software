package cnuphys.fastMCed.snr;

import java.io.Serializable;
import java.util.HashMap;

public class SNRDictionary extends HashMap<String, ReducedParticleRecord> implements Serializable {
	
	//good capacity 393241
	
	public SNRDictionary(int size) {
		super(size);
	}
	
}
