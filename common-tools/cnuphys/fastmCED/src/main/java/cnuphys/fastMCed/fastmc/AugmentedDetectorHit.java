package cnuphys.fastMCed.fastmc;

import org.jlab.geom.DetectorHit;
import org.jlab.geom.DetectorId;

public class AugmentedDetectorHit  {
	
	/** The underlying raw detector hit from FastMC */
	public DetectorHit hit;
		
	private boolean _noise;
	
	public AugmentedDetectorHit(DetectorHit hit) {
		this.hit = hit;
	}
	
	/**
	 * Is this considered a noise hit?
	 * @return <code>true</code>
	 */
	public boolean isNoise() {
		return _noise;
	}
	
	/**
	 * Get the detector id
	 * @return the detector id
	 */
	public DetectorId getDetectorId() {
		return hit.getDetectorId();
	}
	
	/**
	 * get the energy from the underlying raw FastMC DetectorHit
	 * @return the energy
	 */
	public double getEnergy() {
		return hit.getEnergy();
	}
	
	/**
	 * get the time from the underlying raw FastMC DetectorHit
	 * @return the time
	 */
	public double getTime() {
		return hit.getTime();
	}
	
	/**
	 * Convenience check as to whether this is a DC hit
	 * @return <code>true</code> if this is a Drift Chamber hit
	 */
	public boolean isDCHit() {
		return hit.getDetectorId() == DetectorId.DC;
	}
	
	/**
	 * Set whether this is noise
	 * @param noise the noise parameter value
	 */
	public void setNoise(boolean noise) {
		_noise = noise;
	}
	
	/**
	 * Get the zero-based sector from the underlying raw FastMC DetectorHit
	 * @return the zero-based sector
	 */
	public int getSectorId() {
		return hit.getSectorId();
	}
	
	/**
	 * Get the zero-based superlayer from the underlying raw FastMC DetectorHit
	 * @return the zero-based superlayer
	 */
	public int getSuperlayerId() {
		return hit.getSuperlayerId();
	}

	/**
	 * Get the zero-based layer from the underlying raw FastMC DetectorHit
	 * @return the zero-based layer
	 */
	public int getLayerId() {
		return hit.getLayerId();
	}
	
	/**
	 * Get the zero-based component from the underlying raw FastMC DetectorHit
	 * @return the zero-based conponent
	 */
	public int getComponentId() {
		return hit.getComponentId();
	}


}
