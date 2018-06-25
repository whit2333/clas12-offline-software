package cnuphys.fastMCed.snr;

import java.awt.Color;
import java.util.List;
import java.util.Vector;

import org.jlab.geom.DetectorHit;
import org.jlab.geom.DetectorId;
import org.jlab.io.base.DataEvent;

import cnuphys.fastMCed.fastmc.AugmentedDetectorHit;
import cnuphys.fastMCed.fastmc.ParticleHits;
import cnuphys.snr.NoiseReductionParameters;
import cnuphys.snr.clas12.Clas12NoiseAnalysis;
import cnuphys.snr.clas12.Clas12NoiseResult;


public class SNRManager  {
	
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
	}

	/**
	 * Public access to the singleton
	 * 
	 * @return the NoiseManager singleton
	 */
	public static SNRManager getInstance() {
		if (instance == null) {
			instance = new SNRManager();
		}
		return instance;
	}

//	/**
//	 * Get the noise array which is parallel to the other dc_dgtz arrays such as
//	 * dgtz_sector etc.
//	 * 
//	 * @return the noise array
//	 */
//	public boolean[] getNoise() {
//		return _noiseResults.noise;
//	}

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
	 * Perform the SNR analysis
	 * @param particleHits the input hit data
	 */
	public void analyzeSNR(List<ParticleHits> particleHits) {
		_noisePackage.clear();
		_noiseResults.clear();
		
		if ((particleHits == null) || particleHits.isEmpty()) {
			return;
		}
		
		//total dc hit count
		int dcCount = 0;
		for (ParticleHits ph : particleHits) {
			dcCount += ph.hitCount(DetectorId.DC);
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
			List<AugmentedDetectorHit> dcHits = ph.getHits(DetectorId.DC);
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
			List<AugmentedDetectorHit> dcHits = ph.getHits(DetectorId.DC);
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