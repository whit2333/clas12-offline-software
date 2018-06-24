package cnuphys.fastMCed.fastmc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.jlab.geom.DetectorHit;
import org.jlab.geom.prim.Path3D;

import cnuphys.fastMCed.geometry.DCGeometry;
import cnuphys.fastMCed.geometry.FTOFGeometry;
import cnuphys.lund.LundId;
import cnuphys.snr.clas12.Clas12NoiseResult;
import cnuphys.splot.plot.DoubleFormat;

/**
 * These are the hits as determined by the fastMC engine for a single trajectory.
 * @author heddle
 *
 */
public class ParticleHits {


	//the particle lund id object
	private LundId _lundId;
	
	//drift chamber hits
	private List<AugmentedDetectorHit> _dcHits;
	
	//ftof hits
	private List<AugmentedDetectorHit> _ftofHits;
	
	//ftof layer hits. 
	//The first index is the sector [0..5]
	//The second index is [0-2] for panel [1A, 1B, 2]
	private List<AugmentedDetectorHit>[][] _ftofLayerHits = new List[6][3];
	
	/**
	 * The particle hits for a single trajectory as determined by fastMC
	 * @param lundId the Lund Id
	 * @param path the path 
	 */
	public ParticleHits(LundId lundId, Path3D path) {
		_lundId = lundId;
		int charge = lundId.getCharge();

		if (charge != 0) {
			//these calls will get the hits
			_dcHits = DCGeometry.getHits(path);
			_ftofHits = FTOFGeometry.getHits(path);
			
			for (int sect0 = 0; sect0 < 6; sect0++) {
				for (int ptype = 0; ptype < 3; ptype++) {
					_ftofLayerHits[sect0][ptype] = FTOFGeometry.getHits(sect0, ptype, path);
//					System.err.println("FTOF LAYER HIT COUNT SECT: " + (sect0+1) + "  PANEL TYPE: " + ptype + " count: " + FTOFLayerHitCount(sect0, ptype));
				}
			}
			
		}
	}

	/**
	 * Get the counts for ftof layer hits (
	 * @param sect0 the zero based sector [0..5]
	 * @param ptype is [0, 1, 2] for [1A, 1B, 2]
	 * @return the count of these hits
	 */
	public int layerHitCountFTOF(int sect0, int ptype) {
		 List<AugmentedDetectorHit> hits = _ftofLayerHits[sect0][ptype];
		 return (hits == null) ? 0 : hits.size();
	}
	
	/**
	 * Get the number of DC Hits
	 * @return the number of DC Hits
	 */
	public int hitCountDC() {
		return (_dcHits == null) ? 0 : _dcHits.size();
	}
	
	/**
	 * Get the number of FTOF Hits
	 * @return the number of FTOF Hits
	 */
	public int hitCountFTOF() {
		return (_ftofHits == null) ? 0 : _ftofHits.size();
	}
	
	/**
	 * Get the counts for an ftof layer 
	 * @param sect0 the zero based sector [0..5]
	 * @param ptype is [0, 1, 2] for [1A, 1B, 2]
	 * @return the list of these hits
	 */
	public List<AugmentedDetectorHit> getTOFLayerHits(int sect0, int ptype) {
		return _ftofLayerHits[sect0][ptype];
	}
	
	/**
	 * Get the list of ftof hits
	 * @return list of ftof hits
	 */
	public List<AugmentedDetectorHit> getFTOFHits() {
		return _ftofHits;
	}

	/**
	 * Get the list of drift chamber hits
	 * @return list of drift chamber hits
	 */
	public List<AugmentedDetectorHit> getDCHits() {
		return _dcHits;
	}

	/**
	 * Get the Lund Id object
	 * @return the LundId object
	 */
	public LundId getLundId() {
		return _lundId;
	}
	
	/**
	 * Get the integer Lund Id
	 * @return the integer Lund Id
	 */
	public int lundId() {
		return ((_lundId != null) ? _lundId.getId() : 0);
	}

	/**
	 * Get the line color for this particle
	 * @return the line color for this particle
	 */
     public Color getLineColor() {
		if (_lundId == null) {
			return Color.black;
		}
		return _lundId.getStyle().getLineColor();
	}

	/**
	 * Get the fill color for this particle
	 * @return the fill color for this particle
	 */
	public Color getFillColor() {
		if (_lundId == null) {
			return Color.gray;
		}
		return _lundId.getStyle().getFillColor();
	}
	
	/**
	 * Filter a raw list of hits on specific values of sector, superlayer and layer.
	 * If you don't want to filter on a parameter, call with a value < 1
	 * @param rawList the raw list of detector hits
	 * @param sector the 1-based sector (<=0 if exclude from filtering)
	 * @param superLayer  the 1-based superlayer (<=0 if exclude from filtering)
	 * @param layer  the 1-based layer (<=0 if exclude from filtering)
	 * @return a filtered list
	 */
	public static List<AugmentedDetectorHit> filter(List<AugmentedDetectorHit> rawList, int sector, int superLayer, int layer) {
		
		Vector<AugmentedDetectorHit> filteredHits = new Vector<>();
		//convert to 0 based
		sector--;
		superLayer--;
		layer--;
		
		if (rawList != null) {
			for (AugmentedDetectorHit aughit : rawList) {
				DetectorHit hit = aughit.hit;
				boolean accept = true;
				
				if (accept && (sector >= 0)) {
					accept = (sector == hit.getSectorId());
				}
				if (accept && (superLayer >= 0)) {
					accept = (superLayer == hit.getSuperlayerId());
				}
				if (accept && (layer >= 0)) {
					accept = (layer == hit.getLayerId());
				}
				
				if (accept) {
					filteredHits.add(aughit);
				}
			}
		}
		
		return filteredHits;
	}
	
	/**
	 * Get a list of augmented hits. We can add information to augmented hits,
	 * such as whether this was a noise hit.
	 * @param detectorHits the list to upgrade.
	 * @return the corresponding list of augmented hits
	 */
	public static List<AugmentedDetectorHit> fromDetectorHits(List<DetectorHit> detectorHits) {
		if (detectorHits == null) {
			return null;
		}
		
		ArrayList<AugmentedDetectorHit> augHits = new ArrayList<>();
		for (DetectorHit dh : detectorHits) {
			augHits.add(new AugmentedDetectorHit(dh));
		}
		
		return augHits;
	}
	
	/**
	 * Add hit feedback data to the feedback strings
	 * @param hit the hit that the mouse is over
	 * @param lid the Lund Id
	 * @param feedbackStrings the list of feedbackstrings being added to
	 */
	public static void addHitFeedback(AugmentedDetectorHit hit, LundId lid, List<String> feedbackStrings) {
		if (hit != null) {
			String lidName = (lid != null) ? lid.getName() : "???";
			int lidId = (lid != null) ? lid.getId() : -99999;
			feedbackStrings.add("$yellow$pid " + lidName + " (" + lidId + ")");
			feedbackStrings.add("$yellow$energy dep " + DoubleFormat.doubleFormat(hit.getEnergy(), 4));
			feedbackStrings.add("$yellow$time " + DoubleFormat.doubleFormat(hit.getTime(), 4));
			
			if (hit.isDCHit()) {
				feedbackStrings.add("$white$called noise by SNR: " + (hit.isNoise() ? "yes" : "no"));
			}
		}
	}
	
	/**
	 * Add hit feedback data to the feedback strings
	 * @param hit the hit that the mouse is over
	 * @param lid the Lund Id
	 * @param feedbackStrings the list of feedbackstrings being added to
	 */
	public static void addHitFeedback(DetectorHit hit, LundId lid, List<String> feedbackStrings) {
	}
		
}
