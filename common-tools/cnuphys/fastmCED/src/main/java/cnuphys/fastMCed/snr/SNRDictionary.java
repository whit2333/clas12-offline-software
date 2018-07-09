package cnuphys.fastMCed.snr;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import cnuphys.bCNU.util.SerialIO;
import cnuphys.snr.ExtendedWord;

public class SNRDictionary extends HashMap<String, String> implements Serializable {
		
	private boolean _useTorus = true;
	private boolean _useSolenoid = false;
	
	private double _solenoidScale = 1.0;
	private double _torusScale = -1.0;

	public static final int GOODSIZE = 393241;
	
	//NOT thread safe
	private static ExtendedWord _w1 = new ExtendedWord(112);
	private static ExtendedWord _w2 = new ExtendedWord(112);
	private static ExtendedWord _w3 = new ExtendedWord(112);

	private static ExtendedWord _ewords1[] = new ExtendedWord[6];
	private static ExtendedWord _ewords2[] = new ExtendedWord[6];
	
	static {
		for (int i = 0; i < 6; i++) {
			_ewords1[i] = new ExtendedWord(112);
			_ewords2[i] = new ExtendedWord(112);
		}
	}


	/**
	 * Create a dictionary for a mag field setting
	 * @param useTorus whether we are using the torus
	 * @param torusScale the torus scale factor
	 * @param useSolenoid whether we are using the solenoid
	 * @param solenoidScale the solenoid scale factor
	 */
	public SNRDictionary(boolean useTorus, double torusScale, boolean useSolenoid, double solenoidScale) {
		this(useTorus, torusScale, useSolenoid, solenoidScale, GOODSIZE);
	}
	
	/**
	 * Create a dictionary for a mag field setting
	 * @param useTorus whether we are using the torus
	 * @param torusScale the torus scale factor
	 * @param useSolenoid whether we are using the solenoid
	 * @param solenoidScale the solenoid scale factor
	 * @param size the hash map size
	 */
	public SNRDictionary(boolean useTorus, double torusScale, boolean useSolenoid, double solenoidScale, int size) {
		super(size);
		_useTorus = useTorus;
		_torusScale = torusScale;
		_useSolenoid = useSolenoid;
		_solenoidScale = solenoidScale;
	}
	
	public File write(String dirPath) {
		String fn = getFileName(_useTorus, _torusScale, _useSolenoid, _solenoidScale);
		File file = new File(dirPath, fn);
		if (file.exists()) {
			file.delete();
		}
		SerialIO.serialWrite(this, file.getPath());
		return file;
	}

	public static SNRDictionary read(String dirPath, String fileName) {

		SNRDictionary dictionary = null;
		File file = new File(dirPath, fileName);

		if (file.exists()) {
			try {
				dictionary = (SNRDictionary) (SerialIO.serialRead(file.getPath()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return dictionary;
	}

	
	//get the file name based on mag field settings
	public static String getFileName(boolean useTorus, double torusScale, boolean useSolenoid, double solenoidScale) {
		
		boolean incTorus = (useTorus && (Math.abs(torusScale) > 0.001));
		boolean incSolenoid = (useSolenoid && (Math.abs(solenoidScale) > 0.001));
		
		if (!incTorus && !incSolenoid) {
			return "nofield.dct";
		}
		String tStr = incTorus ? String.format("T%4.2f", torusScale) : "";
		String sStr = incSolenoid ? String.format("S%4.2f", solenoidScale) : "";
		String sep = (incTorus && incSolenoid) ? "_" : "";
		
		String fn = tStr + sep + sStr + ".dct";
		fn = fn.replace(" ", "");
		return fn;
	}

	/**
	 * Find the nearest key, i.e. the key that has a bit pattern of segments
	 * that has the most in common with the bits associated with the testHash.
	 * 
	 * @param testHash
	 *            a hash that is not in the dictionary, so we'd like to find the
	 *            closest as our guess.
	 * @return the nearest key
	 */
	public String nearestKey(String testHash) {
		ExtendedWord w1 = new ExtendedWord(112);
		ExtendedWord w2 = new ExtendedWord(112);
		ExtendedWord w3 = new ExtendedWord(112);

		ExtendedWord testWord[] = new ExtendedWord[6];
		ExtendedWord valWord[] = new ExtendedWord[6];

		for (int i = 0; i < 6; i++) {
			testWord[i] = new ExtendedWord(112);
			valWord[i] = new ExtendedWord(112);
		}
		String testSummary = SNRManager.getInstance().fromHashKey(testHash, testWord);

		int maxCommonBits = 0;
		String nearestKey = null;

		// Arggh. Have to search all the keys
		for (String hashKey : keySet()) {
			String summaryStr = SNRManager.getInstance().getSummaryString(hashKey);

			// first test if the summary strings match
			if (testSummary.equals(summaryStr)) {
				SNRManager.getInstance().fromHashKey(hashKey, valWord);
				int cb = commonBits(testWord, valWord, w1, w2, w3);

				if (cb > maxCommonBits) {
					maxCommonBits = cb;
					nearestKey = hashKey;
				}
			} //matching summaries
		}

		return nearestKey;
	}

	// match by counting common bits
	private static int commonBits(ExtendedWord testWord[], ExtendedWord valWord[], ExtendedWord w1, ExtendedWord w2,
			ExtendedWord w3) {
		int commonBits = 0;
		for (int i = 0; i < testWord.length; i++) {
			commonBits += ExtendedWord.countCommonBits(testWord[i], valWord[i], w1, w2, w3);
		}
		return commonBits;
	}
	
	/**
	 * Count the number of common bits in the bit patterns represented by these two hash keys.
	 * An expensive operation. Use sparingly.
	 * @param hashKey1 one hash key
	 * @param hashKey2 the other hash key
	 * @return the number of common bits (max value = 6 EW x 2 Long per EW x 64 = 768)
	 */
	public static int commonBits(String hashKey1, String hashKey2) {

		SNRManager.getInstance().fromHashKey(hashKey1, _ewords1);
		SNRManager.getInstance().fromHashKey(hashKey2, _ewords2);

		return commonBits(_ewords1, _ewords2, _w1, _w2, _w3);
	}

}
