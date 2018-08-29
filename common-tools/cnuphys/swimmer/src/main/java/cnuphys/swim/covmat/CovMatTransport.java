package cnuphys.swim.covmat;

import java.util.Map;

import Jama.Matrix;
import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.magfield.MagneticFields.FieldType;
import cnuphys.swim.StateVec;
import cnuphys.swim.Swim;
import cnuphys.swim.SwimException;
import cnuphys.swim.Trajectory;
import cnuphys.swimS.SwimS;
import cnuphys.swimZ.SwimZ;

/**
 * For transporting the covariance matrix
 * @author heddle
 *
 */
public class CovMatTransport extends Matrix {
	
	/** The speed of light in these units: (GeV/c)(1/T)(1/cm) */
	public static final double speedLight = 2.99792458e-03;

	private double Bmax; // averaged
	
	/** Mag field probe */
	private final RotatedCompositeProbe probe;
	
	//this is NOT thread safe
	//EVERY thread should have its own CovMatDeriv
	private double a[] = new double[2];
	private double dA[] = new double[4];
	double[][] u = new double[5][5];
	double[][] C = new double[5][5];
	
	// might use a SwimS swimmer
	private SwimS _swimS;
	
	//might use a SwimZ swimmer
	private SwimZ _swimZ;

	/**
	 * Create an object with multiple ways to transport a covariance matrix
	 * @param rcprobe must be a RotatedCompositeProbe
	 */
	public CovMatTransport(RotatedCompositeProbe rcprobe) {
		super(5, 5);
		probe = rcprobe;
				
		// Max Field Location: (phi, rho, z) = (29.50000, 44.00000, 436.00000)
		// get the maximum value of the B field
		double phi = Math.toRadians(29.5);
		double rho = 44.0;
		double z = 436.0;
		
		
		FieldProbe cprobe = FieldProbe.factory(MagneticFields.getInstance().getIField(FieldType.COMPOSITE));
		Bmax = cprobe.fieldMagnitude((float) (rho * Math.cos(phi)), (float) (rho * Math.sin(phi)), (float) z);
		Bmax = Bmax * (2.366498 / 4.322871999651699); // scales according to
														// torus scale by
														// reading the map and
														// averaging the value
		// convert to tesla
		Bmax = Bmax / 10;
	}
	

	
	/**
	 * Based on the Spiridonov paper defined here:
	 * here: http://arxiv.org/pdf/physics/0511177v1.pdf<br>
	 * @param probe to compute the field
	 * @param sector the sector 1..6
	 * @param s the step in cm
	 * @param direction the overall sign of Zf - Zi
	 * @param vi the starting state vector (input)
	 * @param vf the ending state vector (output)
	 * @param bf the field components in Tesla for vi (input) the vf (output)
	 * @return pathlength increment
	 */
	private double step(int sector, int direction, double v[], double Q, double s,
			float bf[], CovMat cmi, CovMat cmf) {
				
		//for convenience
		double x = v[0];
		double y = v[1];
		double z = v[2];
		double tx = v[3];
		double ty = v[4];
		
		//get the underling matrices inside the CovMat wraper
		Matrix mi = cmi.covMat;
		
		//the input field fieldIn[] should already be calculated 
		computeAandDA(tx, ty, bf[0], bf[1], bf[2], a, dA);
		
		
		//non zero (and non unity)  Jacobian (5x5) elements
		double j02, j03, j04; 
		double j12, j13, j14; 
		double j23, j24;
		double j32, j34;
		
		//compute some derivatives from approximation B of the
		// Spiridonov paper
		
		double s2 = s * s;
		double QCs = Q * speedLight * s;
		double hCs2 = 0.5 * speedLight * s2;
		double QCs2 = Q * hCs2;
		double Cs = speedLight * s;
		
		double delx_deltx0 = s;
		double dely_deltx0 = QCs2 * dA[2];
		double deltx_delty0 = QCs * dA[1];
		double delx_delQ = hCs2 * a[0];
		double deltx_delQ = Cs * a[0];
		double delx_delty0 = QCs2 * dA[1];
		double dely_delty0 = s;
		double delty_deltx0 = QCs * dA[2];
		double dely_delQ = hCs2 * a[1];
		double delty_delQ = Cs * a[1];

		//get the nontrivial Jacobian elements
		j02 = delx_deltx0;
		j03 = delx_delty0;
		j04 = delx_delQ;

		j12 = dely_deltx0;
		j13 = dely_delty0;
		j14 = dely_delQ;

		j23 = deltx_delty0;
		j24 = deltx_delQ;

		j32 = delty_deltx0;
		j34 = delty_delQ;
		
		// covMat = FCF^T; u = FC;
		for (int i = 0; i < 5; i++) {
			u[0][i] = mi.get(0, i) + mi.get(2, i) * j02 + mi.get(3, i) * j03 + mi.get(4, i) * j04;
			u[1][i] = mi.get(1, i) + mi.get(2, i) * j12 + mi.get(3, i) * j13 + mi.get(4, i) * j14;
			u[2][i] = mi.get(2, i) + mi.get(3, i) * j23 + mi.get(4, i) * j24;
			u[3][i] = mi.get(3, i) + mi.get(2, i) * j32 + mi.get(4, i) * j34;
			u[4][i] = mi.get(4, i);
		}

		for (int i = 0; i < 5; i++) {
			C[i][0] = u[i][0] + u[i][2] * j02 + u[i][3] * j03 + u[i][4] * j04;
			C[i][1] = u[i][1] + u[i][2] * j12 + u[i][3] * j13 + u[i][4] * j14;
			C[i][2] = u[i][2] + u[i][3] * j23 + u[i][4] * j24;
			C[i][3] = u[i][2] * j32 + u[i][3] + u[i][4] * j34;
			C[i][4] = u[i][4];
		}

		// Q process noise matrix estimate
		double p = Math.abs(1. / Q);

		double pz = p / Math.sqrt(1 + tx * tx + ty * ty);
		double px = tx * pz;
		double py = ty * pz;

		//path length in radiation length units = t/X0 [true path length/ X0]
		double t_ov_X0 = direction * s / Swim.ARGONRADLEN; 
		
		// double mass = this.MassHypothesis(this.massHypo); // assume given
		// mass hypothesis
		double mass = 0.000510998; // assume given mass hypothesis
		if (Q > 0) {
			mass = 0.938272029;
		}

		double beta = p / Math.sqrt(p * p + mass * mass); // use particle
															// momentum
		double cosEntranceAngle = Math.abs((x * px + y * py + z * pz) / (Math.sqrt(x * x + y * y + z * z) * p));
		double pathLength = t_ov_X0 / cosEntranceAngle;

		double sctRMS = (0.0136 / (beta * p)) * Math.sqrt(pathLength) * (1 + 0.038 * Math.log(pathLength)); // Highland-Lynch-Dahl
																											// formula

		double fact = (1 + tx * tx + ty * ty) * sctRMS * sctRMS; 
		double cov_txtx = (1 + tx * tx) * fact;
		double cov_tyty = (1 + ty * ty) * fact;
		double cov_txty = tx * ty * fact;

		if (s > 0) {
			C[2][2] += cov_txtx;
			C[2][3] += cov_txty;
			C[3][2] += cov_txty;
			C[3][3] += cov_tyty;
		}
		
		cmf.covMat = new Matrix(C);
		
		// transport stateVec
		double dx = tx * s + 0.5 * Q * speedLight * a[0] * s2;
		x += dx;
		double dy = ty * s + 0.5 * Q * speedLight * a[1] * s2;
		y += dy;
		tx += Q * speedLight * a[0] * s;
		ty += Q * speedLight * a[1] * s;

		z += s;

		v[0] = x;
		v[1] = y;
		v[2] = z;
		v[3] = tx;
		v[4] = ty;

		return Math.sqrt(dx * dx + dy * dy + s * s);
	}
	
	/**
	 * Based on the Spiridonov paper defined here:
	 * here: http://arxiv.org/pdf/physics/0511177v1.pdf<br>
	 * @param probe to compute the field
	 * @param sector the sector 1..6
	 * @param s the step in cm
	 * @param direction the overall sign of Zf - Zi
	 * @param vi the starting state vector (input)
	 * @param vf the ending state vector (output)
	 * @param bf the field components in Tesla for vi (input) the vf (output)
	 */
	private void stepCovMat(int sector, int direction, StateVec svi, double Q, double s,
			float bf[], CovMat cm) {
				
		//for convenience
		double x = svi.x;
		double y = svi.y;
		double z = svi.z;
		double tx = svi.tx;
		double ty = svi.ty;
		
		//get the underling matrices inside the CovMat wraper
		Matrix mi = cm.covMat;
		
		//the input field fieldIn[] should already be calculated 
		computeAandDA(tx, ty, bf[0], bf[1], bf[2], a, dA);
		
		
		//non zero (and non unity)  Jacobian (5x5) elements
		double j02, j03, j04; 
		double j12, j13, j14; 
		double j23, j24;
		double j32, j34;
		
		//compute some derivatives from approximation B of the
		// Spiridonov paper
		
		double s2 = s * s;
		double QCs = Q * speedLight * s;
		double hCs2 = 0.5 * speedLight * s2;
		double QCs2 = Q * hCs2;
		double Cs = speedLight * s;
		
		double delx_deltx0 = s;
		double dely_deltx0 = QCs2 * dA[2];
		double deltx_delty0 = QCs * dA[1];
		double delx_delQ = hCs2 * a[0];
		double deltx_delQ = Cs * a[0];
		double delx_delty0 = QCs2 * dA[1];
		double dely_delty0 = s;
		double delty_deltx0 = QCs * dA[2];
		double dely_delQ = hCs2 * a[1];
		double delty_delQ = Cs * a[1];

		//get the nontrivial Jacobian elements
		j02 = delx_deltx0;
		j03 = delx_delty0;
		j04 = delx_delQ;

		j12 = dely_deltx0;
		j13 = dely_delty0;
		j14 = dely_delQ;

		j23 = deltx_delty0;
		j24 = deltx_delQ;

		j32 = delty_deltx0;
		j34 = delty_delQ;
		
		// covMat = FCF^T; u = FC;
		for (int i = 0; i < 5; i++) {
			u[0][i] = mi.get(0, i) + mi.get(2, i) * j02 + mi.get(3, i) * j03 + mi.get(4, i) * j04;
			u[1][i] = mi.get(1, i) + mi.get(2, i) * j12 + mi.get(3, i) * j13 + mi.get(4, i) * j14;
			u[2][i] = mi.get(2, i) + mi.get(3, i) * j23 + mi.get(4, i) * j24;
			u[3][i] = mi.get(3, i) + mi.get(2, i) * j32 + mi.get(4, i) * j34;
			u[4][i] = mi.get(4, i);
		}

		for (int i = 0; i < 5; i++) {
			C[i][0] = u[i][0] + u[i][2] * j02 + u[i][3] * j03 + u[i][4] * j04;
			C[i][1] = u[i][1] + u[i][2] * j12 + u[i][3] * j13 + u[i][4] * j14;
			C[i][2] = u[i][2] + u[i][3] * j23 + u[i][4] * j24;
			C[i][3] = u[i][2] * j32 + u[i][3] + u[i][4] * j34;
			C[i][4] = u[i][4];
		}

		// Q process noise matrix estimate
		double p = Math.abs(1. / Q);

		double pz = p / Math.sqrt(1 + tx * tx + ty * ty);
		double px = tx * pz;
		double py = ty * pz;

		//path length in radiation length units = t/X0 [true path length/ X0]
		double t_ov_X0 = direction * s / Swim.ARGONRADLEN; 
		
		// double mass = this.MassHypothesis(this.massHypo); // assume given
		// mass hypothesis
		double mass = 0.000510998; // assume given mass hypothesis
		if (Q > 0) {
			mass = 0.938272029;
		}

		double beta = p / Math.sqrt(p * p + mass * mass); // use particle
															// momentum
		double cosEntranceAngle = Math.abs((x * px + y * py + z * pz) / (Math.sqrt(x * x + y * y + z * z) * p));
		double pathLength = t_ov_X0 / cosEntranceAngle;

		double sctRMS = (0.0136 / (beta * p)) * Math.sqrt(pathLength) * (1 + 0.038 * Math.log(pathLength)); // Highland-Lynch-Dahl
																											// formula

		double fact = (1 + tx * tx + ty * ty) * sctRMS * sctRMS; 
		double cov_txtx = (1 + tx * tx) * fact;
		double cov_tyty = (1 + ty * ty) * fact;
		double cov_txty = tx * ty * fact;

		if (s > 0) {
			C[2][2] += cov_txtx;
			C[2][3] += cov_txty;
			C[3][2] += cov_txty;
			C[3][3] += cov_tyty;
		}
		
		cm.covMat = new Matrix(C);
		
	}
	
	/**
	 * Based on the Spiridonov paper defined here:
	 * here: http://arxiv.org/pdf/physics/0511177v1.pdf<br>
	 * @param probe to compute the field
	 * @param sector the sector 1..6
	 * @param s the step in cm (signed)
	 * @param direction the overall sign of Zf - Zi
	 * @param vi the starting state vector (input)
	 * @param vf the ending state vector (output)
	 * @param bf the field components in Tesla for vi (input) the vf (output)
	 * @return pathlength increment
	 */
	private double stepStateVec(int sector, int direction, double v[], double Q, double s,
			float bf[]) {
				
		//for convenience
		double x = v[0];
		double y = v[1];
		double z = v[2];
		double tx = v[3];
		double ty = v[4];
		
		//the input field fieldIn[] should already be calculated 
		computeA(tx, ty, bf[0], bf[1], bf[2], a);
		
		double qvs = Q * speedLight * s;
		double hqvs = 0.5*qvs;
		
		// transport stateVec
		double dx = tx * s + hqvs * a[0] * s;
		x += dx;
		double dy = ty * s + hqvs * a[1] * s;
		y += dy;
		tx += qvs * a[0];
		ty += qvs * a[1];

		z += s;

		v[0] = x;
		v[1] = y;
		v[2] = z;
		v[3] = tx;
		v[4] = ty;

		return Math.sqrt(dx * dx + dy * dy + s * s);
	}


	/**
	 * This is the 2-element A[2] with A[0] = Ax and A[1] = Ay
	 * Based on the Spiridonov paper defined here:
	 * here: http://arxiv.org/pdf/physics/0511177v1.pdf<br>
	 * all indices: (x, y, tx, ty) == (0, 1, 2, 3)
	 * @param tx px/pz
	 * @param ty py/pz
	 * @param Bx x component of field in Tesla
	 * @param By y component of field in Tesla
	 * @param Bz z component of field in Tesla
	 * @param a a two element vector that will hold Ax and Ay as defined in the Spiridonov paper
	 * @param dela_delt a four element vector ∂Ax/∂tx, ∂Ax/∂ty, ∂Ay/∂tx, ∂Ay/∂ty as defined in the Spiridonov paper
	 */
	private void computeAandDA(double tx, double ty, double Bx, double By, double Bz, double a[], double dela_delt[]) {

		double C2 = 1 + tx * tx + ty * ty;
		double C = Math.sqrt(C2);
		a[0] = C * (ty * (tx * Bx + Bz) - (1 + tx * tx) * By);
		a[1] = C * (-tx * (ty * By + Bz) + (1 + ty * ty) * Bx);
		
		dela_delt[0] = tx * a[0] / C2 + C * (ty * Bx - 2 * tx * By); // delAx_deltx
		dela_delt[1] = ty * a[0] / C2 + C * (tx * Bx + Bz); // delAx_delty
		dela_delt[2] = tx * a[1] / C2 + C * (-ty * By - Bz); // delAy_deltx
		dela_delt[3] = ty * a[1] / C2 + C * (-tx * By + 2 * ty * Bx); // delAy_delty

	}
	
	/**
	 * This is the 2-element A[2] with A[0] = Ax and A[1] = Ay
	 * Based on the Spiridonov paper defined here:
	 * here: http://arxiv.org/pdf/physics/0511177v1.pdf<br>
	 * all indices: (x, y, tx, ty) == (0, 1, 2, 3)
	 * @param tx px/pz
	 * @param ty py/pz
	 * @param Bx x component of field in Tesla
	 * @param By y component of field in Tesla
	 * @param Bz z component of field in Tesla
	 * @param a a two element vector that will hold Ax and Ay as defined in the Spiridonov paper
	 */
	private void computeA(double tx, double ty, double Bx, double By, double Bz, double a[]) {

		double C2 = 1 + tx * tx + ty * ty;
		double C = Math.sqrt(C2);
		a[0] = C * (ty * (tx * Bx + Bz) - (1 + tx * tx) * By);
		a[1] = C * (-tx * (ty * By + Bz) + (1 + ty * ty) * Bx);
		
	}

	
	//just used for testing
	public StateVec uniStep( final int sector, int i, int f, StateVec svi,
			CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj, final Map<Integer, CovMat> trackCov) {
	
		double zi = svi.z;
		
		double del = zf - zi;
		
		int direction = (del > 0) ? 1 : -1;
		
		double Q = svi.Q;
		
		double stepSize = 0.2;
		
		float bf[] = new float[3];
		StateVec svf = new StateVec(svi);
		
		double ds = 0;
		
		
		int nSteps = (int) (Math.abs((del) / stepSize) + 1);
		
		double s = del / (double) nSteps;
		
		double v[] = { svi.x, svi.y, svi.z, svi.tx, svi.ty };


		for (int j = 0; j < nSteps; j++) {
			// get the sign of the step
			if (j == nSteps - 1) {
				s = Math.signum(del) * Math.abs(v[2] - zf);
			}

			Swim.teslaField(probe, sector,  v[0], v[1], v[2], bf);
			
			ds += stepStateVec(sector, direction, v, Q, s, bf);
		}
		
		svf.x = v[0];
		svf.y = v[1];
		svf.z = v[2];
		svf.tx = v[3];
		svf.ty = v[4];
		Swim.teslaField(probe, sector, v[0], v[1], v[2], bf);
		double bmag = Math.sqrt(bf[0]*bf[0] + bf[1]*bf[1] + bf[2]*bf[2]);
		svf.B = bmag;
		svf.deltaPath = ds;

	
		return svf;

	}
		
	private static final double MAXZWIMSSTEP = 2; //cm
	public StateVec halfStepZ( final int sector, int i, int f, StateVec svi,
			CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj, final Map<Integer, CovMat> trackCov) {
		
		if (_swimZ == null) {
			_swimZ = new SwimZ(probe);
			_swimZ.setMaxStepSize(MAXZWIMSSTEP);
		}
		
		
		int direction = (zf > svi.z) ? 1 : -1;
		
		if (direction < 0) {
			svi.Q = -svi.Q;
		}
		
		Trajectory traj = null;
		
		double hdata[] = new double[3];
		try {
			traj = _swimZ.sectorAdaptiveRK(sector, svi, zf, 0.2, hdata);
//			System.out.println("zf: " + zf + "   NUM STEPS: " + traj.size() +  
//					"  zlast: " + traj.last().z + "  dellast z: " + Math.abs(traj.last().z-zf));
		} catch (SwimException e) {
			e.printStackTrace();
		}
		
		if ((traj == null) || (traj.isEmpty())) {
			return null;
		}
		
		if (direction < 0) {
			svi.Q = -svi.Q;  //reset
		}
		double Q = svi.Q;

		float bf[] = new float[3];
		
		//Now advance the covariance matrix
		
		StateVec now = svi;
		int len = traj.size();
		
		for (int indx = 1; indx < len; indx++) {
			StateVec next = traj.get(indx);
			if (direction < 0) {
				next.Q = -next.Q;  //reset
			}

			Swim.teslaField(probe, sector, now.x, now.y, now.z, bf);
			stepCovMat(sector, direction, now, Q, next.z - now.z, bf, covMat);
			
			now = next;
			
		}
		return traj.last();
	}

	private static final double MAXSWIMSSTEP = 2; //cm
	public StateVec halfStepS( final int sector, int i, int f, StateVec svi,
			CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj, final Map<Integer, CovMat> trackCov) {
		
		if (_swimS == null) {
			_swimS = new SwimS(probe);
			_swimS.setMaxStepSize(MAXSWIMSSTEP);
			
			double accuracy = 1.0e-06;
			_swimS.setAccuracy(accuracy);
			
			//since swimming to fixed S, the min step size should be no bigger than the accuracy
			_swimS.setMinStepSize(accuracy);
		}
		
		
		int direction = (zf > svi.z) ? 1 : -1;
		
		if (direction < 0) {
			svi.Q = -svi.Q;
		}
		
		Trajectory traj = null;
		
		double hdata[] = new double[3];
		try {
			traj = _swimS.sectorAdaptiveRK(sector, svi, zf, 0.2, hdata);
//			System.out.println("zf: " + zf + "   NUM STEPS: " + traj.size() +  
//					"  zlast: " + traj.last().z + "  dellast z: " + Math.abs(traj.last().z-zf));
		} catch (SwimException e) {
			e.printStackTrace();
		}
		
		if ((traj == null) || (traj.isEmpty())) {
			return null;
		}
		
		if (direction < 0) {
			svi.Q = -svi.Q;  //reset
		}
		double Q = svi.Q;

		float bf[] = new float[3];
		
		//Now advance the covariance matrix
		
		StateVec now = svi;
		int len = traj.size();
		
		for (int indx = 1; indx < len; indx++) {
			StateVec next = traj.get(indx);
			if (direction < 0) {
				next.Q = -next.Q;  //reset
			}

			Swim.teslaField(probe, sector, now.x, now.y, now.z, bf);
			stepCovMat(sector, direction, now, Q, next.z - now.z, bf, covMat);
			
			now = next;
			
		}
		return traj.last();
	}
	

	private final static double TINY = 1.0e-6;
	private static final double HGROW = 1.2;
	private static final double INITSTEP = 0.2;
	public StateVec XhalfStep( final int sector, int i, int f, StateVec svi,
			CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj, final Map<Integer, CovMat> trackCov) {
	
		double zi = svi.z;
		
		double del = zf - zi;
		
		int direction = (del > 0) ? 1 : -1;
		
		double Q = svi.Q;
		
		float bf[] = new float[3];
		float bmid[] = new float[3];
		
		double h = direction*Math.min(INITSTEP, 0.5*Math.abs(del));
		double absAccuracy = 1.0e-6;
		StateVec svf = new StateVec(svi);
		
		boolean done = false;
		
		double v[] = { svi.x, svi.y, svi.z, svi.tx, svi.ty };
		Swim.teslaField(probe, sector, v[0], v[1], v[2], bf);

		double vcop1[] = new double[6];
		double vcop2[] = new double[6];
		System.arraycopy(v, 0, vcop1, 0, 5);
		
		double ds = 0;
		
		int count = 0;
		
		while (!done) {
			count += 3;
			System.arraycopy(v, 0, vcop2, 0, 5);

			//first half step
			double ds1  = stepStateVec(sector, direction, vcop1, Q, h/2, bf);
		
			//second half step
			Swim.teslaField(probe, sector, vcop1[0], vcop1[1], vcop1[2], bmid);
			double ds2  = stepStateVec(sector, direction, vcop1, Q, h/2, bmid);

			//full step
			stepStateVec(sector, direction, vcop2, Q, h, bf);
			
			boolean accept = false;
			double xdiff = Math.abs(vcop2[0]-vcop1[0]);

			if (xdiff < absAccuracy) {
				double ydiff = Math.abs(vcop2[1]-vcop1[1]);

				if (ydiff < absAccuracy) {
					accept = true;
				}
			}
						
			
			if (accept) {
				Swim.teslaField(probe, sector, vcop1[0], vcop1[1], vcop1[2], bf);

				ds += (ds1 + ds2);
				done = (Math.abs((vcop1[2] - zf)) < TINY);
				if (done) {
					svf.x = vcop1[0];
					svf.y = vcop1[1];
					svf.z = vcop1[2];
					svf.tx = vcop1[3];
					svf.ty = vcop1[4];
					
					double bmag = Math.sqrt(bf[0]*bf[0] + bf[1]*bf[1] + bf[2]*bf[2]);
					svf.B = bmag;
					svf.deltaPath = ds;
					
					System.out.println();
				}
				else {
					//reset
					System.arraycopy(vcop1, 0, v, 0, 5);
					double hlimit = zf - v[2];
					h = HGROW * h;
					if (direction > 0) {
						h = Math.min(hlimit, h);
					} else {
						h = Math.max(hlimit, h);
					}
				}
			}
			else {
				System.arraycopy(v, 0, vcop1, 0, 5);
				h = h/2;
			}

		}
		
		System.out.println("Count: " + count);
		return svf;

	}
	
	

	/**
	 * 
	 * @param sector
	 * @param i
	 * @param f
	 * @param vi
	 * @param covMat
	 * @param zf
	 * @param trackTraj
	 * @param trackCov
	 * @return
	 */
	public StateVec euler( final int sector, int i, int f, StateVec vi,
			CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj, final Map<Integer, CovMat> trackCov) {
		
		if (vi == null) {
			return null;
		}
		
		double x = vi.x;
		double y = vi.y;
		double tx = vi.tx;
		double ty = vi.ty;
		double Q = vi.Q;

			
		//get the initial field and create space for the final
		float b[] = new float[3];
		Swim.teslaField(probe, sector, vi.x, vi.y, vi.z, b);
		double stepSize = 0.2;  //cm
		
		StateVec vf = new StateVec();
		
		// if (bfieldPoints.size() > 0) {
		// double B = new Vector3D(bfieldPoints.get(bfieldPoints.size() - 1).Bx,
		// bfieldPoints.get(bfieldPoints.size() - 1).By,
		// bfieldPoints.get(bfieldPoints.size() - 1).Bz).mag();
		if (b != null) { // get the step size used in swimming as a function of
							// the field intensity in the region traversed
			double B = Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]);
			
			//Veronique's estimate of the stepsize
			if (B / Bmax > 0.01) {
				stepSize = 0.15 * 4;
			}
			if (B / Bmax > 0.02) {
				stepSize = 0.1 * 3;
			}
			if (B / Bmax > 0.05) {
				stepSize = 0.075 * 2;
			}
			if (B / Bmax > 0.1) {
				stepSize = 0.05 * 2;
			}
			if (B / Bmax > 0.5) {
				stepSize = 0.02;
			}
			if (B / Bmax > 0.75) {
				stepSize = 0.01;
			}
		}

		double del = zf - vi.z;
		int direction = (zf > vi.z) ? 1 : -1;
		
		int nSteps = (int) (Math.abs((del) / stepSize) + 1);

		System.out.println("NUM STEPS:" + nSteps);
		double s = del / (double) nSteps;
		double z = vi.z;
		double dPath = 0;
		
		CovMat deriv = new CovMat(covMat.k, new Matrix(5, 5));

		for (int j = 0; j < nSteps; j++) {
			// get the sign of the step
			if (j == nSteps - 1) {
				s = Math.signum(del) * Math.abs(z - zf);
			}
			
//			public double step(int sector, int direction, double v[], double Q, double s,
//					float bf[], CovMat cmi, CovMat cmf) {

			double v[] = { x, y, z, tx, ty };
		    dPath += step(sector, direction, v, Q, s, b, covMat, covMat);
		    
			x = v[0];
			y = v[1];
			z = v[2];
			tx = v[3];
			ty = v[4];
			
			Swim.teslaField(probe, sector, x, y, z, b);



		} // loop over nsteps

		vf.z = zf;
		vf.x = x;
		vf.y = y;
		vf.tx = tx;
		vf.ty = ty;
		vf.Q = Q;
		vf.B = Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]);
		vf.deltaPath = dPath;
		vf.pzSign = vi.pzSign;

		trackTraj.put(f, vf);

		if (covMat.covMat != null) {
			CovMat fCov = new CovMat(f);
			fCov.covMat = covMat.covMat;
			// CovMat = fCov;
			trackCov.put(f, fCov);
		}		
		
		return vf;
	}
	

}
