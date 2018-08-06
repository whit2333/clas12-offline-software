package cnuphys.swim;

import cnuphys.magfield.FastMath;

/**
 * This is the state vector used in the Kalman Filter
 * 
 * @author heddle
 *
 */
public class StateVec {

	// the angle in degrees for rotating between tilted and sector CS
	private static final double _angle = 25.0;
	private static final double _sin25 = Math.sin(Math.toRadians(_angle));
	private static final double _cos25 = Math.cos(Math.toRadians(_angle));


	// NOTE: not including q = Q/p as an element because it is a constant. It is
	// held in the SwimZResult object

	/** the x coordinate (cm) */
	public double x;

	/** the y coordinate (cm) */
	public double y;

	/** the z coordinate (cm) */
	public double z;

	/** the x track slope, px/pz */
	public double tx;

	/** the y track slope, py/pz */
	public double ty;
	
	//auxiliary data
	public int k = -1; // index used in reconstruction/Kalman filtering
	public double Q = Double.NaN; // track q/p
	public double B = 0; // optional B magnitude (T) at the point
	public double deltaPath;  //cm
	
	//this strange variable is needed because the sign of pz
	//is determined by whether, in the swimming, zf is greater
	//than or less than zi. That is because z is the independent variable.
	//SwimZ assumes it is monotonically increasing or decreasing. SwimZ
	//fails catastrophically for trajectories in which particles change
	//direction in Z (loopers)
	public int pzSign = +1;

	/**
	 * Create a state vector for the SwimZ package
	 * with all NaNs for the components and +1 for sign of pZ
	 */
	public StateVec() {
		this(Double.NaN,Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 1);
	}
	
	/**
	 * Create a state variable from another (copy constructor)
	 * 
	 * @param sv
	 *            the state vector to copy
	 */
	public StateVec(StateVec sv) {
		copy(sv);
	}

	
	/**
	 * Constructor for a state vector for the SwimZ package.
	 * Note that it uses CM for distance units
	 * 
	 * @param x
	 *            the x coordinate (cm)
	 * @param y
	 *            the x coordinate (cm)
	 * @param z
	 *            the z coordinate (cm). Note: z is not an actual component of
	 *            the state vector, it is the independent variable. But it rides
	 *            along here.
	 * @param tx
	 *            the x track slope, px/pz
	 * @param ty
	 *            the y track slope, py/pz
	 * @param Q Q = q/p, q is the integer charge
	 * @param pzsign should be +1 of the swim is from zi to a bigger zf,
	 * and -1 if the swim is from zi to a smaller zf
	 */
	public StateVec(double x, double y, double z, double tx, double ty, double Q, int pzSign) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.tx = tx;
		this.ty = ty;
		this.Q = Q;
		this.pzSign = (pzSign < 0) ? -1 : 1;
	}

	/**
	 * Create a state variable from an array (probably from RK integration)
	 * 
	 * @param z
	 *            the value of z in cm. Note: z is not an actual component of
	 *            the state vector, it is the independent variable. But it rides
	 *            along here.
	 * @param v
	 *            the array with, in order, x,y,tx,ty,q
	 * @param pzsign should be +1 of the swim is from zi to a bigger zf,
	 * and -1 if the swim is from zi to a smaller zf
	 */
	public StateVec(double z, double v[], double Q, int pzSign) {
		this(v[0], v[1], z, v[2], v[3], Q, pzSign);
	}
	

	/**
	 * Constructor
	 * 
	 * @param x
	 *            the x coordinate (cm)
	 * @param y
	 *            the y coordinate (cm)
	 * @param z
	 *            the z coordinate (cm)
	 * @param Q  the integer charge divided by the momentum in GeV/c
	 * @param theta
	 *            the initial polar angle (degrees)
	 * @param phi
	 *            the initial azimuthal angle(degrees)
	 */
	public StateVec(double x, double y, double z, double Q, double theta, double phi) {
		this.x = x;
		this.y = y;
		this.z = z;
		theta = Math.toRadians(theta);
		phi = Math.toRadians(phi);
		
		this.Q = Q;
		double p = Math.abs(1/Q);
		double pz = p * Math.cos(theta);
		
		pzSign = (pz < 0) ? -1 : 1;
		double pt = p * Math.sin(theta);
		double px = pt * Math.cos(phi);
		double py = pt * Math.sin(phi);
		tx = px / pz;
		ty = py / pz;
	}


	/**
	 * Create an uninitialized StateVec except for the index
	 * @param k the index
	 */
	public StateVec(int k) {
		this.k = k;
	}
	
	/**
	 * Copy from another state vector
	 * @param sourceSV the state vector to copy (the source)
	 */
	public void copy(StateVec sourceSV) {
		x = sourceSV.x;
		y = sourceSV.y;
		z = sourceSV.z;
		tx = sourceSV.tx;
		ty = sourceSV.ty;
		
		//copy auxiliary data too
		k = sourceSV.k;
		Q = sourceSV.Q;
		B = sourceSV.B;
		deltaPath = sourceSV.deltaPath;

		pzSign = sourceSV.pzSign;
	}
	
	
	/**
	 * Set the state vector
	 * @param z
	 *            the value of z in cm. Note: z is not an actual component of
	 *            the state vector, it is the independent variable. But it rides
	 *            along here.
	 * @param v
	 *            the array with, in order, x,y,tx,ty,
	 */
	public void set(double z, double v[]) {
		this.z = z;
		x = v[0];
		y = v[1];
		tx = v[0];
		ty = v[1];
	}

	/**
	 * Compute the difference between this state vector's location
	 * and another state vector's location
	 * @param zv the other state vector
	 * @param dr will hold the delta in cm
	 */
	public void dRVec(StateVec zv, double dr[]) {
		dr[0] = zv.x - x;
		dr[1] = zv.y - y;
		dr[2] = zv.z - z;
	}	
	
	/**
	 * Get the Euclidean distance between this and another StateVec
	 * @param zv the other StateVec
	 * @return the Euclidean distance
	 */
	public double dR(StateVec zv) {
		double dx = zv.x - x;
		double dy = zv.y - y;
		double dz = zv.z - z;
		return Math.sqrt(dx*dx + dy*dy + dz*dz);
	}
	
	
	/**
	 * Convert tilted x and z to sector x and z
	 * @param tiltedX the tilted x coordinate
	 * @param tiltedZ the tilted z coordinate
	 * @return the sector coordinates, with v[0] = sectorX and v[1] = sectorZ
	 */
	public static double[] tiltedToSector(double tiltedX, double tiltedZ) {
		double[] v = new double[2];
		v[0] = tiltedX * _cos25 + tiltedZ * _sin25;
		v[1] = tiltedZ * _cos25 - tiltedX * _sin25;
		return v;
	}
	
	/**
	 * Convert sector x and z to tilted x and z
	 * @param sectorX the sector x coordinate
	 * @param sectorZ the sector z coordinate
	 * @return the tilted coordinates, with v[0] = tiltedX and v[1] = tiltedZ
	 */
	public static double[] sectorToTilted(double sectorX, double sectorZ) {
		double[] v = new double[2];
		v[0] = sectorX * _cos25 - sectorZ * _sin25;
		v[1] = sectorZ * _cos25 + sectorX * _sin25;
		return v;
	}
	
	/**
	 * Transform a StateVec from the sector system to the tilted system
	 * @param sectorSV the state vector in the sector system
	 * @param tiltedSV the state vector in the tilted system. You can pass the
	 * same SteVec as both arguments and it will be overwritten.
	 */
	public static void sectorToTilted(StateVec sectorSV, StateVec tiltedSV) {
		double txz[] = sectorToTilted(sectorSV.x, sectorSV.z);
		
		double denom =  _cos25 + sectorSV.tx * _sin25;
		double ttx = (sectorSV.tx * _cos25 - _sin25)/denom;
		double tty = sectorSV.ty/denom;
		   
		tiltedSV.x = txz[0];
		tiltedSV.y = sectorSV.y;
		tiltedSV.z = txz[1];
		tiltedSV.tx = ttx;
		tiltedSV.ty = tty;
		
		
		//copy auxiliary data too
		tiltedSV.k = sectorSV.k;
		tiltedSV.Q = sectorSV.Q;
		tiltedSV.B = sectorSV.B;
		tiltedSV.deltaPath = sectorSV.deltaPath;

		tiltedSV.pzSign = sectorSV.pzSign;

	}
	
	/**
	 * Transform a StateVec from the tilted system to the sector system
	 * @param sectorSV the state vector in the sector system. You can pass the
	 * same SteVec as both arguments and it will be overwritten.
	 * @param tiltedSV the state vector in the tilted system 
	 */
	public static void tiltedToSector(StateVec sectorSV, StateVec tiltedSV) {
		double sxz[] = tiltedToSector(tiltedSV.x, tiltedSV.z);
		
		double denom =  _cos25 - tiltedSV.tx * _sin25;
		double stx = (tiltedSV.tx * _cos25 + _sin25)/denom;
		double sty = tiltedSV.ty/denom;
		   
		sectorSV.x = sxz[0];
		sectorSV.y = tiltedSV.y;
		sectorSV.z = sxz[1];
		sectorSV.tx = stx;
		sectorSV.ty = sty;
		
		//copy auxiliary data too
		sectorSV.k = tiltedSV.k;
		sectorSV.Q = tiltedSV.Q;
		sectorSV.B = tiltedSV.B;
		sectorSV.deltaPath = tiltedSV.deltaPath;

		sectorSV.pzSign = tiltedSV.pzSign;

		
	}

	/**
	 * Get the z component of the momentum. 
	 * @return the z component of the momentum
	 */
	public double getPz() {
		return  pzSign*getP()/FastMath.sqrt(1 + tx*tx + ty*ty);
	}
	

	/**
	 * Get theta, the polar angle
	 * @return theta in degrees
	 */
	public double getTheta() {
		return FastMath.acos2Deg(1/Math.sqrt(1 + tx*tx + ty*ty));
	}
	
	/**
	 * Get phi, the azimuthal angle
	 * @return phi in degrees
	 */
	public double getPhi() {
		return FastMath.atan2Deg(ty, tx);
	}
	
	/**
	 * Get a string representation
	 * 
	 * @return a string representation of the state vector
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(255);
		double r = Math.sqrt(x * x + y * y + z * z);
		
		double pz = getPz();
		double px = pz*tx;
		double py = pz*ty;

		sb.append(String.format("R = [%-10.7f, %-10.7f, %-10.7f] |R| = %7.4f cm  ", x, y, z, r));
		sb.append("B: " + B);
		sb.append(String.format("\npvec = [%-10.7f, %-10.7f, %-10.7f] Gev/c  ", px, py, pz));
		sb.append(String.format("\np: %-9.6f GeV/c   theta: %-9.6f   phi: %-9.6f  ", getP(), getTheta(), getPhi()));
		sb.append(String.format("\nQ: %-9.6f  tx: %-10.7f  ty: %-10.7f    charge: %d  pzSign: %d", Q, tx, ty, getCharge(), pzSign));
		return sb.toString();
	}
	
	/**
	 * A print that takes into account the sign of pz from the swim direction
	 * 
	 * @param p
	 *            the momentum
	 * @return a descriptive string
	 */
	public String normalPrint() {
		StringBuffer sb = new StringBuffer(255);
		sb.append(String.format("R = [%9.6f, %9.6f, %9.6f] cm", x, y, z));

		double p = getP();

		if (Double.isNaN(p)) {
			sb.append(" momentum undefined");
		} else {
			double pz = getPz();
			double px = pz * tx;
			double py = pz * ty;

			double theta = FastMath.acos2Deg(pz / p);
			double phi = FastMath.atan2Deg(py, px);
			sb.append(String.format("\nP, theta, phi = [%9.6f, %9.6f, %9.6f]  pz sign: %d", p, theta, phi, pzSign));
		}

		return sb.toString();
	}
	
	/**
	 * Get the momentum in GeV/c
	 * Bases it on the sign of Q, which is q/P. If Q has not
	 * been set (is NaN) it returns NaN; 
	 * @return the momentum in GeV/c
	 */
	public double getP() {
		if (Double.isNaN(Q)) {
			return Double.NaN;
		}
		return Math.abs(1/Q);
		
	}

	/**
	 * Gets the integer charge, which assumes is +1 or -1
	 * Bases it on the sign of Q, which is q/P. If Q has not
	 * been set (is NaN) it returns 0; 
	 * @return integer charge _1, +1 or 0 if not set
	 */
	public int getCharge() {
		if (Double.isNaN(Q)) {
			return 0;
		}
		return (Q < 0) ? -1 : 1;
	}
	
	/**
	 * A check that the sign of pz is consistent with swimming endpoints
	 * @param zi the initial z
	 * @param zf the final z
	 * @return <code>true</code> if the sign is consistent.
	 */
	public boolean pzSignCorrect(double zi, double zf) {
		int pzs = (zf > zi) ? 1 : -1;
		return pzs == pzSign;
	}
		
	/**
	 * Get the three momentum in GeV/c
	 * @return the three momentum [0, 1,2] for [px, py, pz]
	 */
	public double[] getThreeMomentum() {
		double pv[] = new double[3];
		getThreeMomentum(pv);
		return pv;
	}
	
	/**
	 * Get the three momentum in GeV/c
	 * @param  pv will hold the three momentum [0, 1,2] for [px, py, pz]
	 */
	public void getThreeMomentum(double pv[]) {
		pv[2] = getPz();
		pv[0] = pv[2] * tx;
		pv[1] = pv[2] * ty;
	}
	
	/**
	 * Set the values of the state vector
	 * @param charge the integer charge
	 * @param x the x coordinate in cm
	 * @param y the y coordinate in cm
	 * @param z the z coordinate in cm
	 * @param px the x component of momentum in GeV/c
	 * @param py the y component of momentum in GeV/c
	 * @param pz the z component of momentum in GeV/c
	 */
	public void set(int charge, double p, double x, double y, double z, double px, double py, double pz) {
		this.x = x;
		this.y = y;
		this.z = z;
		tx = px/pz;
		ty = py/pz;
		
		Q = charge/p;
		
		pzSign = (pz < 0) ? -1 : 1;
		
	}

}
