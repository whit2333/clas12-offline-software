package cnuphys.fastMCed.snr;

import java.io.Serializable;
import java.util.HashMap;

public class SNRDictionary extends HashMap<String, String> implements Serializable {
	
	//good capacity 393241
	
	public SNRDictionary(int size) {
		super(size);
	}
	
	public String nearestKey(String hash) {
		for (String s : keySet()) {
			
		}
		
		return null;
	}
	
}
