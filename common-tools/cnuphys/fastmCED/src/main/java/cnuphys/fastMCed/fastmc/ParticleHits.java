package cnuphys.fastMCed.fastmc;

import java.awt.Color;
import java.util.List;
import java.util.Vector;

import org.jlab.geom.DetectorHit;
import org.jlab.geom.prim.Path3D;

import cnuphys.lund.LundId;
import cnuphys.splot.plot.DoubleFormat;
import geometry.DCGeometry;
import geometry.FTOFGeometry;

/**
 * These are the hits as determind bt the fastMC engine for a single trajectory.
 * @author heddle
 *
 */
public class ParticleHits {


	//the particle lund id object
	private LundId _lundId;
	
	//drift chamber hits
	private List<DetectorHit> _dcHits;
	
	//ftof hits
	private List<DetectorHit> _ftofHits;
	
	//ftof layer hits. 
	//The first index is the sector [0..5]
	//The second index is [0-2] for panel [1A, 1B, 2]
	private List<DetectorHit>[][] _ftofLayerHits = new List[6][3];
	
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
			
			System.err.println("DC HIT COUNT: " + DCHitCount());
			System.err.println("FTOF HIT COUNT: " + FTOFHitCount());
		
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
	public int FTOFLayerHitCount(int sect0, int ptype) {
		 List<DetectorHit> hits = _ftofLayerHits[sect0][ptype];
		 return (hits == null) ? 0 : hits.size();
	}
	
	/**
	 * Get the number of DC Hits
	 * @return the number of DC Hits
	 */
	public int DCHitCount() {
		return (_dcHits == null) ? 0 : _dcHits.size();
	}
	
	/**
	 * Get the number of FTOF Hits
	 * @return the number of FTOF Hits
	 */
	public int FTOFHitCount() {
		return (_ftofHits == null) ? 0 : _ftofHits.size();
	}
	
	/**
	 * Get the counts for an ftof layer 
	 * @param sect0 the zero based sector [0..5]
	 * @param ptype is [0, 1, 2] for [1A, 1B, 2]
	 * @return the list of these hits
	 */
	public List<DetectorHit> getTOFLayerHits(int sect0, int ptype) {
		return _ftofLayerHits[sect0][ptype];
	}
	
	/**
	 * Get the list of ftof hits
	 * @return list of ftof hits
	 */
	public List<DetectorHit> getFTOFHits() {
		return _ftofHits;
	}

	/**
	 * Get the list of drift chamber hits
	 * @return list of drift chamber hits
	 */
	public List<DetectorHit> getDCHits() {
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
	public static List<DetectorHit> filter(List<DetectorHit> rawList, int sector, int superLayer, int layer) {
		
		Vector<DetectorHit> filteredHits = new Vector<>();
		//convert to 0 based
		sector--;
		superLayer--;
		layer--;
		
		if (rawList != null) {
			for (DetectorHit hit : rawList) {
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
					filteredHits.add(hit);
				}
			}
		}
		
		return filteredHits;
	}
	
	public static void addHitFeedback(DetectorHit hit, LundId lid, List<String> feedbackStrings) {
		if (hit != null) {
			
			String lidName = (lid != null) ? lid.getName() : "???";
			int lidId = (lid != null) ? lid.getId() : -99999;
			feedbackStrings.add("$yellow$pid " + lidName + " (" + lidId + ")");
			feedbackStrings.add("$yellow$energy dep " + DoubleFormat.doubleFormat(hit.getEnergy(), 4));
			feedbackStrings.add("$yellow$time " + DoubleFormat.doubleFormat(hit.getTime(), 4));
		}
	}
	
}
