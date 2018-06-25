package cnuphys.fastMCed.fastmc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Vector;

import org.jlab.geom.DetectorHit;
import org.jlab.geom.DetectorId;
import org.jlab.geom.prim.Path3D;

import cnuphys.fastMCed.geometry.DCGeometry;
import cnuphys.fastMCed.geometry.FTOFGeometry;
import cnuphys.lund.GeneratedParticleRecord;
import cnuphys.lund.LundId;
import cnuphys.splot.plot.DoubleFormat;

/**
 * These are the hits as determined by the fastMC engine for a single
 * trajectory.
 * 
 * @author heddle
 *
 */
public class ParticleHits {

	// the particle lund id object
	private LundId _lundId;

	// the 3D path that generated the hits
	private Path3D _path;
	
	//contains the vertex and moment information
	private GeneratedParticleRecord _genParticleRecord;

	/**
	 * A mapping of the detector type to a list of augmented detector hits
	 */
	private EnumMap<DetectorId, List<AugmentedDetectorHit>> hits;

	/**
	 * The particle hits for a single trajectory as determined by fastMC
	 * 
	 * @param lundId
	 *            the Lund Id
	 * @ parm particleRec
	 *            contains the vertex and momentum information
	 * @param path
	 *            the path
	 */
	public ParticleHits(LundId lundId, GeneratedParticleRecord particleRec, Path3D path) {
		_lundId = lundId;
		_genParticleRecord = particleRec;
		_path = path;
		int charge = lundId.getCharge();

		hits = new EnumMap<>(DetectorId.class);
		if (charge != 0) {
			// these calls will get the hits
			hits.put(DetectorId.DC, DCGeometry.getHits(path));
			hits.put(DetectorId.FTOF, FTOFGeometry.getHits(path));
		}
	}

	/**
	 * This obtains the path that FastMC used to generate the hits
	 * 
	 * @return the path that FastMC used to generate the hits
	 */
	public Path3D getPath() {
		return _path;
	}

	/**
	 * Get the number of DC Hits
	 * 
	 * @return the number of DC Hits
	 */
	public int hitCount(DetectorId id) {
		List<AugmentedDetectorHit> list = hits.get(id);
		return (list == null) ? 0 : list.size();
	}

	/**
	 * Get the list of ftof hits
	 * 
	 * @return list of ftof hits
	 */
	public List<AugmentedDetectorHit> getHits(DetectorId id) {
		return hits.get(id);
	}

	/**
	 * Get the Lund Id object
	 * 
	 * @return the LundId object
	 */
	public LundId getLundId() {
		return _lundId;
	}
	
	/**
	 * Get the generated particle record for this trajectory/track.
	 * It contains information like the vertex and momentum.
	 * @return the generated particle record for this trajectory/track.
	 */
	public GeneratedParticleRecord getGeneratedParticleRecord() {
		return _genParticleRecord;
	}

	/**
	 * Get the integer Lund Id
	 * 
	 * @return the integer Lund Id
	 */
	public int lundId() {
		return ((_lundId != null) ? _lundId.getId() : 0);
	}

	/**
	 * Get the line color for this particle
	 * 
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
	 * 
	 * @return the fill color for this particle
	 */
	public Color getFillColor() {
		if (_lundId == null) {
			return Color.gray;
		}
		return _lundId.getStyle().getFillColor();
	}

	/**
	 * Filter a raw list of hits on specific values of sector, superlayer and
	 * layer. If you don't want to filter on a parameter, call with a value < 1
	 * 
	 * @param rawList
	 *            the raw list of detector hits
	 * @param sector
	 *            the 1-based sector (<=0 if exclude from filtering)
	 * @param superLayer
	 *            the 1-based superlayer (<=0 if exclude from filtering)
	 * @param layer
	 *            the 1-based layer (<=0 if exclude from filtering)
	 * @return a filtered list
	 */
	public static List<AugmentedDetectorHit> filter(List<AugmentedDetectorHit> rawList, int sector, int superLayer,
			int layer) {

		Vector<AugmentedDetectorHit> filteredHits = new Vector<>();
		// convert to 0 based
		sector--;
		superLayer--;
		layer--;

		if (rawList != null) {
			for (AugmentedDetectorHit aughit : rawList) {
				DetectorHit hit = aughit._hit;
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
	 * 
	 * @param detectorHits
	 *            the list to upgrade.
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
	 * 
	 * @param hit
	 *            the hit that the mouse is over
	 * @param lid
	 *            the Lund Id
	 * @param feedbackStrings
	 *            the list of feedbackstrings being added to
	 */
	public static void addHitFeedback(AugmentedDetectorHit hit, LundId lid, List<String> feedbackStrings) {
		if (hit != null) {
			String lidName = (lid != null) ? lid.getName() : "???";
			int lidId = (lid != null) ? lid.getId() : -99999;
			feedbackStrings.add("$yellow$pid " + lidName + " (" + lidId + ")");
			feedbackStrings.add("$yellow$energy dep " + DoubleFormat.doubleFormat(hit.getEnergy(), 4));
			feedbackStrings.add("$yellow$time " + DoubleFormat.doubleFormat(hit.getTime(), 4));

			if (hit.isDetectorHit(DetectorId.DC)) {
				feedbackStrings.add("$white$called noise by SNR: " + (hit.isNoise() ? "yes" : "no"));
			}
		}
	}

}
