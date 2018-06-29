package cnuphys.fastMCed.snr;

import java.awt.Color;
import java.util.List;
import java.util.StringTokenizer;

import org.jlab.geom.DetectorHit;
import org.jlab.geom.DetectorId;

import cnuphys.fastMCed.fastmc.AugmentedDetectorHit;
import cnuphys.fastMCed.fastmc.ParticleHits;
import cnuphys.snr.ExtendedWord;
import cnuphys.snr.NoiseReductionParameters;
import cnuphys.snr.clas12.Clas12NoiseAnalysis;
import cnuphys.snr.clas12.Clas12NoiseResult;


public class SNRManager  {
	
	//bend direction
	public static final int LEFT  = 0;
	public static final int RIGHT = 1;
	
	//used for creating composite hash keys
	private static final String HASHDELIM = "|";
	
	private static final String _fbColor = "$wheat$";

	/** noise mask color for left benders */
	public static final Color maskFillLeft = new Color(255, 128, 0, 48);

	/** noise mask color for right benders */
	public static final Color maskFillRight = new Color(0, 128, 255, 48);

	// singleton
	private static SNRManager instance;

	// The analysis package
	private Clas12NoiseAnalysis _noisePackage = new Clas12NoiseAnalysis();

	// result container
	private Clas12NoiseResult _noiseResults = new Clas12NoiseResult();

	// private constructor
	private SNRManager() {
		//turn on (or off) SNR "composite" track finding
		//default is off
		NoiseReductionParameters.setLookForTracks(false);
	}

	/**
	 * Public access to the singleton
	 * 
	 * @return the SNRManager singleton
	 */
	public static SNRManager getInstance() {
		if (instance == null) {
			instance = new SNRManager();
		}
		return instance;
	}

	/**
	 * see if SNR found a segment in every superlayer of given sector
	 * @param sect0 zero based sector
	 * @param direction
	 *            should be LEFT or RIGHT
	 * @return <code>true</code> if segments found in all six superlayers
	 */
	public boolean segmentsInAllSuperlayers(int sect0, int direction) {
		
		for (int supl0 = 0; supl0 < 6; supl0++) {
			if (!segmentInSuperlayer(sect0, supl0, direction)) {
				return false;
			}
		}	
		return true;
	}

	/**
	 * See if SNR found a segment in the given sector and superlayer
	 * 
	 * @param sect0
	 *            zero based sector
	 * @param supl0
	 *            zero based superlayer
	 * @param direction
	 *            should be LEFT or RIGHT
	 * @return <code>true</code> if segment found in given sector and superlayer
	 */
	public boolean segmentInSuperlayer(int sect0, int supl0, int direction) {
		NoiseReductionParameters params = getParameters(sect0, supl0);

		if (direction == LEFT) {
			return !params.getLeftSegments().isZero();
		} else {
			return !params.getRightSegments().isZero();
		}
	}

	/**
	 * Get a hash key for the segments in the given sector
	 * 
	 * @param sect0
	 *            the zero based sector
	 * @param direction
	 *            should be LEFT or RIGHT
	 * @return hash key for the segments in the given sector
	 */
	public String hashKey(int sect0, int direction) {
		StringBuilder sb = new StringBuilder(128);

		for (int supl0 = 0; supl0 < 6; supl0++) {
			NoiseReductionParameters params = getParameters(sect0, supl0);
			if (sb.length() > 0) {
				sb.append(HASHDELIM);
			}
			if (direction == LEFT) {
				sb.append(params.getLeftSegments().hashKey());
			} else {
				sb.append(params.getRightSegments().hashKey());
			}
		}

		return sb.toString();
	}
	
	/**
	 * Convert a hash key created with hashKey above back into 
	 * an array of ExtendedWord objects
	 * @param hashKey a hash key created with the hashKey methhod
	 * @param ewords the unhashed words that created the key
	 */
	public void fromHash(String hashKey, ExtendedWord[] ewords) {
		StringTokenizer t = new StringTokenizer(hashKey, HASHDELIM);
		int num = t.countTokens();
		
		for (int i = 0; i < num; i++) {
			String ewordHash = t.nextToken();
			ewords[i] = ExtendedWord.fromHash(ewordHash);
		}
	}


	/**
	 * Get the parameters for a given 0-based superlayer
	 * 
	 * @param sect0
	 *            the 0-based sector
	 * @param supl0
	 *            the 0-based superlayer in question
	 * @return the parameters for that superlayer
	 */
	public NoiseReductionParameters getParameters(int sect0, int supl0) {
		return _noisePackage.getParameters(sect0, supl0);
	}
	
	public NoiseReductionParameters getCompositeParameters(int sect0) {
		return _noisePackage.getCompositeParameters(sect0, 1);
	}
	
	//add feedback string with common color
	private void addFBStr(String s, List<String>feedbackStrings) {
		feedbackStrings.add(_fbColor+s);
	}
	
	/**
	 * So that we can see the snr parameters
	 * @param sector the sector [1..6]
	 * @param superlayer the superlayer [1..6]
	 * @param feedbackStrings the strings to add to
	 */
	public void addParametersToFeedback(int sector, int superlayer, List<String>feedbackStrings) {
		NoiseReductionParameters params = getParameters(sector-1, superlayer-1);
		addFBStr("SNR parameters: ", feedbackStrings);
		addFBStr("  allowed missing layers " + params.getAllowedMissingLayers(), feedbackStrings);
		
		int[] ls = params.getLeftLayerShifts();
		int[] rs = params.getRightLayerShifts();
		
		addFBStr(String.format("  left shifts  [%d, %d, %d, %d, %d, %d]", ls[0], ls[1], ls[2], ls[3], ls[4], ls[5]), feedbackStrings);
		addFBStr(String.format("  right shifts [%d, %d, %d, %d, %d, %d]", rs[0], rs[1], rs[2], rs[3], rs[4], rs[5]), feedbackStrings);
		
	}
	
	/**
	 * Clear the SNR data
	 */
	public void clear() {
		_noisePackage.clear();
		_noiseResults.clear();
	}
	
	/**
	 * Perform the SNR analysis
	 * @param particleHits the input hit data
	 */
	public void analyzeSNR(List<ParticleHits> particleHits) {
		clear();
		
		if ((particleHits == null) || particleHits.isEmpty()) {
			return;
		}
		
		//total dc hit count
		int dcCount = 0;
		for (ParticleHits ph : particleHits) {
			dcCount += ph.totalHitCount(DetectorId.DC);
		}
		
		if (dcCount < 1) {
			return;
		}
		
		int sector[] = new int[dcCount];
		int superlayer[] = new int[dcCount];
		int layer[] = new int[dcCount];
		int wire[] = new int[dcCount];

		int index = 0;
		for (ParticleHits ph : particleHits) {
			List<AugmentedDetectorHit> dcHits = ph.getAllHits(DetectorId.DC);
			if ((dcHits != null) && !dcHits.isEmpty()) {
				for (AugmentedDetectorHit aughit : dcHits) {
					DetectorHit hit = aughit._hit;
					// a hit has 0-based indices noise package wants 1-based
					sector[index] = hit.getSectorId() + 1;
					superlayer[index] = hit.getSuperlayerId() + 1;
					layer[index] = hit.getLayerId() + 1;
					wire[index] = hit.getComponentId() + 1;
					index++;
				}
			}
		}

		_noisePackage.findNoise(sector, superlayer, layer, wire, _noiseResults);

		//now augment
		index = 0;
		boolean[] isNoise = _noiseResults.noise;
		for (ParticleHits ph : particleHits) {
			List<AugmentedDetectorHit> dcHits = ph.getAllHits(DetectorId.DC);
			if ((dcHits != null) && !dcHits.isEmpty()) {
				for (AugmentedDetectorHit aughit : dcHits) {
					aughit.setNoise(isNoise[index]);
					index++;
				}
			}
		}
		
	}

	/**
	 * Get the total number of noise hits in the current event
	 * @return the total number of noise hits
	 */
	public int getNoiseCount() {
		if (_noiseResults == null) {
			return 0;
		}
		return _noiseResults.noiseCount();
	}

}