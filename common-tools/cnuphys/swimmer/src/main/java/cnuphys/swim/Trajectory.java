package cnuphys.swim;

import java.util.ArrayList;

import cnuphys.lund.GeneratedParticleRecord;
import cnuphys.magfield.FieldProbe;

/**
 * Holds the result of a swimZ integration
 * 
 * @author heddle
 *
 */
public class Trajectory extends ArrayList<StateVec> {
	
	//the |dl x B| integral in kG cm
	private double _bdl = Double.NaN;
	
	//the pathlength in cm
	private double _pathLength = Double.NaN;;

	/**
	 * Create a trajectory with default initial capacity
	 */
	public Trajectory() {
	}
	
	/**
	 * Create a trajectory with a given capacity
	 * @param capacity the initial capacity of StateVecs
	 */
	public Trajectory(int capacity) {
		super(capacity);
	}

	
	/**
	 * Get the approximate path length in cm
 	 * @return the approximate path length in cm
	 */
	public double getPathLength() {
		
		// only compute if necessary
		if (Double.isNaN(_pathLength)) {
			_pathLength = 0;
			int size = size();
			
			StateVec prev = null;
			if (size > 1) {
				
				double  dr[] = new double[3];
				
				for (StateVec next : this) {
					if (prev != null) {
						prev.dRVec(next, dr);
						_pathLength += vecmag(dr);
						
				}
					prev = next;
				}
			}
		}
		
		return _pathLength;
	}
	
	/**
	 * Get the approximate integral |B x dL|
     * @param probe the probe use to compute this result trajectory
	 * @return the approximate integral |B x dL| in kG*cm
	 */
	public double getBDL(FieldProbe probe) {
		
		// only compute if necessary
		if (Double.isNaN(_bdl)) {
			_bdl = 0;
			_pathLength = 0;
			int size = size();
			
			StateVec prev = null;
			if (size > 1) {
				
				double  dr[] = new double[3];
				
				float b[] = new float[3];
				double bxdl[] = new double[3];

				for (StateVec next : this) {
					if (prev != null) {
						prev.dRVec(next, dr);
						_pathLength += vecmag(dr);
					
						//get the field at the midpoint
						float xmid = (float) ((prev.x + next.x) / 2);
						float ymid = (float) ((prev.y + next.y) / 2);
						float zmid = (float) ((prev.z + next.z) / 2);
						probe.field(xmid, ymid, zmid, b);
						
						cross(b, dr, bxdl);
						_bdl += vecmag(bxdl);

					}
					prev = next;
				}
			}
		}
		
		return _bdl;
	}
	
	
	/**
	 * Get the approximate integral |B x dL|
	 * @param sector sector 1..6
     * @param probe the probe use to compute this result trajectory
	 * @return the approximate integral |B x dL| in kG*cm
	 */
	public double sectorGetBDL(int sector, FieldProbe probe) {
		
		// only compute if necessary
		if (Double.isNaN(_bdl)) {
			_bdl = 0;
			_pathLength = 0;

			int size = size();
			
			StateVec prev = null;
			if (size > 1) {
				
				double  dr[] = new double[3];
	
				float b[] = new float[3];
				double bxdl[] = new double[3];

				for (StateVec next : this) {
					if (prev != null) {
						prev.dRVec(next, dr);
						_pathLength += vecmag(dr);
						
						//get the field at the midpoint
						float xmid = (float) ((prev.x + next.x) / 2);
						float ymid = (float) ((prev.y + next.y) / 2);
						float zmid = (float) ((prev.z + next.z) / 2);
						probe.field(sector, xmid, ymid, zmid, b);
						
						cross(b, dr, bxdl);
						_bdl += vecmag(bxdl);

					}
					prev = next;
				}
			}
		}
		
		return _bdl;
	}
	
	// usual cross product c = a x b
	private static void cross(float a[], double b[], double c[]) {
		c[0] = a[1] * b[2] - a[2] * b[1];
		c[1] = a[2] * b[0] - a[0] * b[2];
		c[2] = a[0] * b[1] - a[1] * b[0];
	}

	// usual vec mag
	private static double vecmag(double a[]) {
		double asq = a[0] * a[0] + a[1] * a[1] + a[2] * a[2];
		return Math.sqrt(asq);
	}


	/**
	 * Get the initial three momentum in GeV/c
	 * 
	 * @return the initial three momentum [0,1,2] = [px,py,pz]
	 */
	public double[] getInitialThreeMomentum() {
		StateVec first = first();
		if (first != null) {
			return first.getThreeMomentum();
		}
		return null;
	}
	
	/**
	 * Get the final three momentum in GeV/c
	 * 
	 * @return the initial three momentum [0,1,2] = [px,py,pz]
	 */
	public double[] getFinalThreeMomentum() {
		StateVec last = first();
		if (last != null) {
			return last.getThreeMomentum();
		}
		return null;
	}

	/**
	 * Get the first state vector
	 * 
	 * @return the first state vector
	 */
	public StateVec first() {
		if (size() > 1) {
			return this.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Get the last state vector
	 * 
	 * @return the last state vector
	 */
	public StateVec last() {
		if (size() > 1) {
			return this.get(size()-1);
		} else {
			return null;
		}
	}


	/**
	 * Get the values of theta and phi from the momentum and a state vector.
	 * 
	 * @param sv
	 *            the State Vector, presumably on this trajectory
	 * @return theta and phi in an array, in that order, in degrees.
	 */
	public double[] getThetaAndPhi(StateVec sv) {
		double thetaPhi[] = { Double.NaN, Double.NaN };

		if (sv != null) {
			thetaPhi[0] = sv.getTheta();
			thetaPhi[1] = sv.getPhi();
		}
		return thetaPhi;
	}

	/**
	 * Get the values of theta and phi from the momentum and the final state
	 * vector.
	 * 
	 * @return theta and phi in an array, in that order, in degrees.
	 */
	public double[] getFinalThetaAndPhi() {
		return getThetaAndPhi(last());
	}

	/**
	 * Get the values of theta and phi from the momentum and the initial state
	 * vector.
	 * 
	 * @return theta and phi in an array, in that order, in degrees.
	 */
	public double[] getInitialThetaAndPhi() {
		return getThetaAndPhi(first());
	}

	/**
	 * Obtain a GeneratedParticleRecord for this result
	 * 
	 * @return a GeneratedParticleRecord for this result
	 */
	public GeneratedParticleRecord getGeneratedParticleRecord() {
		StateVec sv = first();
		if (sv == null) {
			return null;
		}

		double xo = sv.x / 100.0; // cm to m
		double yo = sv.y / 100.0; // cm to m
		double zo = sv.z / 100.0; // cm to m
		return new GeneratedParticleRecord(sv.getCharge(), xo, yo, zo, sv.getP(), sv.getTheta(), sv.getPhi());
	}


	/**
	 * Create a SwimTrajectory (used in ced) for this result object
	 * 
	 * @return a SwimTrajectory corresponding to this result.
	 */
	public SwimTrajectory toSwimTrajectory() {
		StateVec sv = first();
		if (sv == null) {
			return null;
		}

		double xo = sv.x / 100.0; // cm to m
		double yo = sv.y / 100.0; // cm to m
		double zo = sv.z / 100.0; // cm to m

		SwimTrajectory traj = new SwimTrajectory(sv.getCharge(), xo, yo, zo, sv.getP(), sv.getTheta(), sv.getPhi());

		double p3[] = new double[3];
		for (StateVec v : this) {
			double x = v.x / 100; // cm to m
			double y = v.y / 100; // cm to m
			double z = v.z / 100; // cm to m
			double p = v.getP();
			v.getThreeMomentum(p3);
			double u[] = {x, y, z, p3[0]/p, p3[1]/p, p3[2]/p};
			traj.add(u);
		}
		return traj;
	}
}
