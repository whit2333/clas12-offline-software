package cnuphys.swim;

import java.util.Map;

import Jama.Matrix;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.swim.halfstep.AdvancingObject;
import cnuphys.swimS.SwimS;
import cnuphys.swimZ.SectorSwimZDerivative;
import cnuphys.swim.halfstep.Advance;

public class CovMatTransport {

	public static final double ERROR = 0.01;
	public static final double MINSTEP = 1.0e-5; // cm
	
	static boolean DEBUG = false;

	public static StateVec transport(final RotatedCompositeProbe probe, final int sector, final int i, int f,
			final StateVec iVec, final CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj,
			final Map<Integer, CovMat> trackCov, final double[] A, final double[] dA) { // s
																						// =
																						// signed
																						// step-size

		if (iVec == null) {
			return null;
		}

		//the original iVec is unchanged
		StateVec start = new StateVec(iVec);
		
		CovMatAdvancer advancer = new CovMatAdvancer(probe, sector, i, f, start, covMat, zf, trackTraj, trackCov, A, dA);
		int numComputes = Advance.advance(advancer, start.z, zf, Math.abs(zf - start.z) / 10, ERROR, MINSTEP);
		System.out.println("\n**Number of computes = " + numComputes);
		return advancer.getEnd();
		
	}

	public static void oneStep(RotatedCompositeProbe probe, int sector, int i, int f, StateVec start, StateVec end, CovMat covMat,
			double zf, Map<Integer, StateVec> trackTraj, Map<Integer, CovMat> trackCov, double[] A, double[] dA) {

	//	System.out.println("CALLING ONE STEP");

		double[][] u = new double[5][5];
		double[][] C = new double[5][5];

		//iVec is only used for its current values. It remains unchanged
		double x = start.x;
		double y = start.y;
		double tx = start.tx;
		double ty = start.ty;
		double Q = start.Q;
		
		float[] bf = new float[3];

//		double diff = zf - start.z;

		double s = (zf - start.z);
		double z = start.z;
		double dPath = 0;

		// get the sign of the step
		s = Math.signum(zf - start.z) * Math.abs(z - zf);
		System.out.println("S = " + s);

		// B bf = new B(i, z, x, y, tx, ty, s);
		// bfieldPoints.add(bf);
		teslaField(probe, sector, x, y, z, bf);

		A(tx, ty, bf[0], bf[1], bf[2], A);
		delA_delt(tx, ty, bf[0], bf[1], bf[2], dA);

		// transport covMat
		double delx_deltx0 = s;
		double dely_deltx0 = 0.5 * Q * Swim.speedLight * s * s * dA[2];
		double deltx_delty0 = Q * Swim.speedLight * s * dA[1];
		double delx_delQ = 0.5 * Swim.speedLight * s * s * A[0];
		double deltx_delQ = Swim.speedLight * s * A[0];
		double delx_delty0 = 0.5 * Q * Swim.speedLight * s * s * dA[1];
		double dely_delty0 = s;
		double delty_deltx0 = Q * Swim.speedLight * s * dA[2];
		double dely_delQ = 0.5 * Swim.speedLight * s * s * A[1];
		double delty_delQ = Swim.speedLight * s * A[1];

		double transpStateJacobian02 = delx_deltx0;
		double transpStateJacobian03 = delx_delty0;
		double transpStateJacobian04 = delx_delQ;

		double transpStateJacobian12 = dely_deltx0;
		double transpStateJacobian13 = dely_delty0;
		double transpStateJacobian14 = dely_delQ;

		double transpStateJacobian23 = deltx_delty0;
		double transpStateJacobian24 = deltx_delQ;

		double transpStateJacobian32 = delty_deltx0;

		double transpStateJacobian34 = delty_delQ;


		// covMat = FCF^T; u = FC;
		for (int j1 = 0; j1 < 5; j1++) {
			u[0][j1] = covMat.covMat.get(0, j1) + covMat.covMat.get(2, j1) * transpStateJacobian02
					+ covMat.covMat.get(3, j1) * transpStateJacobian03
					+ covMat.covMat.get(4, j1) * transpStateJacobian04;
			u[1][j1] = covMat.covMat.get(1, j1) + covMat.covMat.get(2, j1) * transpStateJacobian12
					+ covMat.covMat.get(3, j1) * transpStateJacobian13
					+ covMat.covMat.get(4, j1) * transpStateJacobian14;
			u[2][j1] = covMat.covMat.get(2, j1) + covMat.covMat.get(3, j1) * transpStateJacobian23
					+ covMat.covMat.get(4, j1) * transpStateJacobian24;
			u[3][j1] = covMat.covMat.get(2, j1) * transpStateJacobian32 + covMat.covMat.get(3, j1)
					+ covMat.covMat.get(4, j1) * transpStateJacobian34;
			u[4][j1] = covMat.covMat.get(4, j1);
		}

		for (int i1 = 0; i1 < 5; i1++) {
			C[i1][0] = u[i1][0] + u[i1][2] * transpStateJacobian02 + u[i1][3] * transpStateJacobian03
					+ u[i1][4] * transpStateJacobian04;
			C[i1][1] = u[i1][1] + u[i1][2] * transpStateJacobian12 + u[i1][3] * transpStateJacobian13
					+ u[i1][4] * transpStateJacobian14;
			C[i1][2] = u[i1][2] + u[i1][3] * transpStateJacobian23 + u[i1][4] * transpStateJacobian24;
			C[i1][3] = u[i1][2] * transpStateJacobian32 + u[i1][3] + u[i1][4] * transpStateJacobian34;
			C[i1][4] = u[i1][4];
		}

		// Q process noise matrix estimate
		double p = Math.abs(1. / Q);

		double pz = p / Math.sqrt(1 + tx * tx + ty * ty);
		double px = tx * pz;
		double py = ty * pz;

		double t_ov_X0 = Math.signum(zf - start.z) * s / Swim.ARGONRADLEN; 

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

		double cov_txtx = (1 + tx * tx) * (1 + tx * tx + ty * ty) * sctRMS * sctRMS;
		double cov_tyty = (1 + ty * ty) * (1 + tx * tx + ty * ty) * sctRMS * sctRMS;
		double cov_txty = tx * ty * (1 + tx * tx + ty * ty) * sctRMS * sctRMS;

		if (s > 0) {
			C[2][2] += cov_txtx;
			C[2][3] += cov_txty;
			C[3][2] += cov_txty;
			C[3][3] += cov_tyty;
		}

		//update the covariance matrix
		covMat.covMat = new Matrix(C);
		
//		Swim.printCovMatrix("UPDATED COV MAT", covMat);
		
		SectorSwimZDerivative szd = new SectorSwimZDerivative();
		szd.set(sector, start.getCharge(), pathLength, probe);
		double xx[] = {x, y, tx, ty};
		double dd[] = new double[4];
		szd.derivative(z, xx, dd);
		double dx = dd[0]*s;
		double dy = dd[1]*s;
		double dtx = dd[2]*s;
		double dty = dd[3]*s;
		x -= dx;
		y -= dy;
		tx -= dtx;
		ty -= dty;
		
		// transport stateVec
//		double dx = tx * s + 0.5 * Q * Swim.speedLight * A[0] * s * s;
//		x += dx;
//		double dy = ty * s + 0.5 * Q * Swim.speedLight * A[1] * s * s;
//		y += dy;
//		tx += Q * Swim.speedLight * A[0] * s;
//		ty += Q * Swim.speedLight * A[1] * s;

		z += s;
		dPath += Math.sqrt(dx * dx + dy * dy + s * s);
		
		end.z = zf;
		end.x = x;
		end.y = y;
		end.tx = tx;
		end.ty = ty;
		end.Q = Q;
		end.B = Math.sqrt(bf[0] * bf[0] + bf[1] * bf[1] + bf[2] * bf[2]);
		end.deltaPath = dPath;
//		if (covMat.covMat != null) {
//			CovMat fCov = new CovMat(f);
//			fCov.covMat = covMat.covMat;
//		}
		
	} // end onestep

	/**
	 * Veronique's basic transport
	 * 
	 * @param sector the sec
	 * @param i
	 * @param f
	 * @param iVec
	 * @param covMat
	 */
	public static StateVec basicTransport(RotatedCompositeProbe probe, int sector, int i, int f, StateVec iVec,
			CovMat covMat, double zf, Map<Integer, StateVec> trackTraj, Map<Integer, CovMat> trackCov, double[] A,
			double[] dA) { // s = signed step-size

		if (iVec == null)
			return null;
		// StateVec iVec = trackTraj.get(i);
		// bfieldPoints = new ArrayList<B>();
		// CovMat covMat = icovMat;
		// double[] A = new double[5];
		// double[] dA = new double[5];
		double[][] u = new double[5][5];
		double[][] C = new double[5][5];
		// Matrix Cpropagated = null;
		// double[][] transpStateJacobian = null;

		double x = iVec.x;
		double y = iVec.y;
		double tx = iVec.tx;
		double ty = iVec.ty;
		double Q = iVec.Q;
		double Bf = iVec.B;
		double stepSize = 0.2;

		float[] bf = new float[3];

		// boolean PRINT = (Math.abs(Z[i]-491.90442) < .001) &&
		// (Math.abs(Z[f]-494.70984) < 0.001) ;

		double diff = zf - iVec.z;
		boolean PRINT = (diff < -100);

		if (PRINT && DEBUG) {
			System.out.println("\nTRANSPORT from Zi = " + iVec.z + " to  Zf = " + zf);
			System.out.println("Mag Field Config:");
			System.out.println(MagneticFields.getInstance().getCurrentConfigurationMultiLine());
			System.out.println("sector = " + sector);
			// System.out.println("WAS CHARGE FLIPPED: " +
			// dcSwim.isChargeFlipped());
			System.out.println("Initial State Vec:\n" + iVec);
			Swim.printCovMatrix("\nInitial Covariance Matrix:", covMat);
		}

		// B-field components at state vector coordinates
		teslaField(probe, sector, x, y, iVec.z, bf);

		double Bmax = 2.366498;
		// if (bfieldPoints.size() > 0) {
		// double B = new Vector3D(bfieldPoints.get(bfieldPoints.size() - 1).Bx,
		// bfieldPoints.get(bfieldPoints.size() - 1).By,
		// bfieldPoints.get(bfieldPoints.size() - 1).Bz).mag();
		if (bf != null) { // get the step size used in swimming as a function of
							// the field intensity in the region traversed
			double B = Math.sqrt(bf[0] * bf[0] + bf[1] * bf[1] + bf[2] * bf[2]);
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

		int nSteps = (int) (Math.abs((iVec.z - zf) / stepSize) + 1);

		if (PRINT && DEBUG) {
			System.out.println("\nNum Steps = " + nSteps);
		}

		double s = (zf - iVec.z) / nSteps;
		double z = iVec.z;
		double dPath = 0;

		for (int j = 0; j < nSteps; j++) {
			// get the sign of the step
			if (j == nSteps - 1) {
				s = Math.signum(zf - iVec.z) * Math.abs(z - zf);
			}

			// B bf = new B(i, z, x, y, tx, ty, s);
			// bfieldPoints.add(bf);
			teslaField(probe, sector, x, y, z, bf);

			A(tx, ty, bf[0], bf[1], bf[2], A);
			delA_delt(tx, ty, bf[0], bf[1], bf[2], dA);

			// transport covMat
			double delx_deltx0 = s;
			double dely_deltx0 = 0.5 * Q * Swim.speedLight * s * s * dA[2];
			double deltx_delty0 = Q * Swim.speedLight * s * dA[1];
			double delx_delQ = 0.5 * Swim.speedLight * s * s * A[0];
			double deltx_delQ = Swim.speedLight * s * A[0];
			double delx_delty0 = 0.5 * Q * Swim.speedLight * s * s * dA[1];
			double dely_delty0 = s;
			double delty_deltx0 = Q * Swim.speedLight * s * dA[2];
			double dely_delQ = 0.5 * Swim.speedLight * s * s * A[1];
			double delty_delQ = Swim.speedLight * s * A[1];

			// double transpStateJacobian00=1;
			// double transpStateJacobian01=0;
			double transpStateJacobian02 = delx_deltx0;
			double transpStateJacobian03 = delx_delty0;
			double transpStateJacobian04 = delx_delQ;
			// double transpStateJacobian10=0;
			// double transpStateJacobian11=1;
			double transpStateJacobian12 = dely_deltx0;
			double transpStateJacobian13 = dely_delty0;
			double transpStateJacobian14 = dely_delQ;
			// double transpStateJacobian20=0;
			// double transpStateJacobian21=0;
			// double transpStateJacobian22=1;
			double transpStateJacobian23 = deltx_delty0;
			double transpStateJacobian24 = deltx_delQ;
			// double transpStateJacobian30=0;
			// double transpStateJacobian31=0;
			double transpStateJacobian32 = delty_deltx0;
			// double transpStateJacobian33=1;
			double transpStateJacobian34 = delty_delQ;
			// double transpStateJacobian40=0;
			// double transpStateJacobian41=0;
			// double transpStateJacobian42=0;
			// double transpStateJacobian43=0;
			// double transpStateJacobian44=1;

			// covMat = FCF^T; u = FC;
			for (int j1 = 0; j1 < 5; j1++) {
				u[0][j1] = covMat.covMat.get(0, j1) + covMat.covMat.get(2, j1) * transpStateJacobian02
						+ covMat.covMat.get(3, j1) * transpStateJacobian03
						+ covMat.covMat.get(4, j1) * transpStateJacobian04;
				u[1][j1] = covMat.covMat.get(1, j1) + covMat.covMat.get(2, j1) * transpStateJacobian12
						+ covMat.covMat.get(3, j1) * transpStateJacobian13
						+ covMat.covMat.get(4, j1) * transpStateJacobian14;
				u[2][j1] = covMat.covMat.get(2, j1) + covMat.covMat.get(3, j1) * transpStateJacobian23
						+ covMat.covMat.get(4, j1) * transpStateJacobian24;
				u[3][j1] = covMat.covMat.get(2, j1) * transpStateJacobian32 + covMat.covMat.get(3, j1)
						+ covMat.covMat.get(4, j1) * transpStateJacobian34;
				u[4][j1] = covMat.covMat.get(4, j1);
			}

			for (int i1 = 0; i1 < 5; i1++) {
				C[i1][0] = u[i1][0] + u[i1][2] * transpStateJacobian02 + u[i1][3] * transpStateJacobian03
						+ u[i1][4] * transpStateJacobian04;
				C[i1][1] = u[i1][1] + u[i1][2] * transpStateJacobian12 + u[i1][3] * transpStateJacobian13
						+ u[i1][4] * transpStateJacobian14;
				C[i1][2] = u[i1][2] + u[i1][3] * transpStateJacobian23 + u[i1][4] * transpStateJacobian24;
				C[i1][3] = u[i1][2] * transpStateJacobian32 + u[i1][3] + u[i1][4] * transpStateJacobian34;
				C[i1][4] = u[i1][4];
			}

			// Q process noise matrix estimate
			double p = Math.abs(1. / Q);

			double pz = p / Math.sqrt(1 + tx * tx + ty * ty);
			double px = tx * pz;
			double py = ty * pz;

			double t_ov_X0 = Math.signum(zf - iVec.z) * s / Swim.ARGONRADLEN; // path
																				// length
																				// in
																				// radiation
																				// length
																				// units
																				// =
																				// t/X0
																				// [true
																				// path
																				// length/
																				// X0]
																				// ;
																				// Ar
																				// radiation
																				// length
																				// =
																				// 14
																				// cm

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

			double cov_txtx = (1 + tx * tx) * (1 + tx * tx + ty * ty) * sctRMS * sctRMS;
			double cov_tyty = (1 + ty * ty) * (1 + tx * tx + ty * ty) * sctRMS * sctRMS;
			double cov_txty = tx * ty * (1 + tx * tx + ty * ty) * sctRMS * sctRMS;

			if (s > 0) {
				C[2][2] += cov_txtx;
				C[2][3] += cov_txty;
				C[3][2] += cov_txty;
				C[3][3] += cov_tyty;
			}

			covMat.covMat = new Matrix(C);
			// transport stateVec
			double dx = tx * s + 0.5 * Q * Swim.speedLight * A[0] * s * s;
			x += dx;
			double dy = ty * s + 0.5 * Q * Swim.speedLight * A[1] * s * s;
			y += dy;
			tx += Q * Swim.speedLight * A[0] * s;
			ty += Q * Swim.speedLight * A[1] * s;

			z += s;
			dPath += Math.sqrt(dx * dx + dy * dy + s * s);
		} //end loop over nsteps

		StateVec fVec = new StateVec(f);
		fVec.z = zf;
		fVec.x = x;
		fVec.y = y;
		fVec.tx = tx;
		fVec.ty = ty;
		fVec.Q = Q;
		fVec.B = Math.sqrt(bf[0] * bf[0] + bf[1] * bf[1] + bf[2] * bf[2]);
		fVec.deltaPath = dPath;
		// StateVec = fVec;
		trackTraj.put(f, fVec);

		// if(transpStateJacobian!=null) {
		// F = new Matrix(transpStateJacobian);
		// }
		if (covMat.covMat != null) {
			CovMat fCov = new CovMat(f);
			fCov.covMat = covMat.covMat;
			// CovMat = fCov;
			trackCov.put(f, fCov);

			if (PRINT && DEBUG) {
				System.out.println("Final State Vec:\n" + fVec);
				Swim.printCovMatrix("\nFinal Covariance Matrix:", fCov);

				double dx = fVec.x - iVec.x;
				double dy = fVec.y - iVec.y;
				double dz = fVec.z - iVec.z;

				System.out.println("dPath = " + dPath + "  Euclidean dist: " + Math.sqrt(dx * dx + dy * dy + dz * dz));
			}

		}

		if (PRINT && DEBUG) {
			DEBUG = false;
		}

		return fVec;
	}

	private static void A(double tx, double ty, double Bx, double By, double Bz, double[] a) {

		double C = Math.sqrt(1 + tx * tx + ty * ty);
		a[0] = C * (ty * (tx * Bx + Bz) - (1 + tx * tx) * By);
		a[1] = C * (-tx * (ty * By + Bz) + (1 + ty * ty) * Bx);
	}

	private static void delA_delt(double tx, double ty, double Bx, double By, double Bz, double[] dela_delt) {

		double C2 = 1 + tx * tx + ty * ty;
		double C = Math.sqrt(1 + tx * tx + ty * ty);
		double Ax = C * (ty * (tx * Bx + Bz) - (1 + tx * tx) * By);
		double Ay = C * (-tx * (ty * By + Bz) + (1 + ty * ty) * Bx);

		dela_delt[0] = tx * Ax / C2 + C * (ty * Bx - 2 * tx * By); // delAx_deltx
		dela_delt[1] = ty * Ax / C2 + C * (tx * Bx + Bz); // delAx_delty
		dela_delt[2] = tx * Ay / C2 + C * (-ty * By - Bz); // delAy_deltx
		dela_delt[3] = ty * Ay / C2 + C * (-tx * By + 2 * ty * Bx); // delAy_delty
	}

	// compute the field in Tesla
	private static void teslaField(RotatedCompositeProbe probe, int sector, double x, double y, double z,
			float[] result) {
		probe.field(sector, (float) x, (float) y, (float) z, result);
		// to tesla from kG
		for (int i = 0; i < 3; i++) {
			result[i] /= 10;
		}
	}

}
