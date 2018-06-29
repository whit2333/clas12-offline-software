package cnuphys.fastMCed.snr;

import java.io.Serializable;
import java.util.HashMap;

import cnuphys.snr.ExtendedWord;

public class SNRDictionary extends HashMap<String, String> implements Serializable {

	// good capacity 393241

	public SNRDictionary(int size) {
		super(size);
	}

	/**
	 * Find the nearest key, i.e. the key that has a bit pattern of segments
	 * that has the most in common with the bits associated with the testHash.
	 * 
	 * @param testHash
	 *            a hash that is not in the dictionary, so we'd like to find the
	 *            closest as out guess.
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
		SNRManager.getInstance().fromHash(testHash, testWord);

		int maxCommonBits = 0;
		String nearestKey = null;

		for (String hashKey : keySet()) {
			SNRManager.getInstance().fromHash(hashKey, valWord);
			int cb = commonBits(testWord, valWord, w1, w2, w3);

			if (cb > maxCommonBits) {
				maxCommonBits = cb;
				nearestKey = hashKey;
			}
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
	 * Count the number of common bits in the bit patterns represented by these two hask keys.
	 * An expensive operation. Use sparingly.
	 * @param hashKey1 one hash key
	 * @param hashKey2 the other hash key
	 * @return the number of common bits (max value = 6 EW x 2 Long per EW x 64 = 768)
	 */
	public static int commonBits(String hashKey1, String hashKey2) {
		ExtendedWord w1 = new ExtendedWord(112);
		ExtendedWord w2 = new ExtendedWord(112);
		ExtendedWord w3 = new ExtendedWord(112);

		ExtendedWord ewords1[] = new ExtendedWord[6];
		ExtendedWord ewords2[] = new ExtendedWord[6];

		for (int i = 0; i < 6; i++) {
			ewords1[i] = new ExtendedWord(112);
			ewords2[i] = new ExtendedWord(112);
		}
		SNRManager.getInstance().fromHash(hashKey1, ewords1);
		SNRManager.getInstance().fromHash(hashKey2, ewords2);

		return commonBits(ewords1, ewords2, w1, w2, w3);
	}

}
