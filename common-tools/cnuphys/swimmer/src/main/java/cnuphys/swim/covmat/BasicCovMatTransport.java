package cnuphys.swim.covmat;

import java.util.HashMap;
import java.util.Map;

import Jama.Matrix;
import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.swimS.SwimS;
import cnuphys.magfield.MagneticFields.FieldType;
import cnuphys.swim.StateVec;
import cnuphys.swim.Swim;
import cnuphys.swim.SwimException;
import cnuphys.swim.Trajectory;

/**
 * Veronique's original transporter
 * 
 * @author heddle
 *
 */
public class BasicCovMatTransport {

	private final double speedLight = 0.002997924580;

	private double Bmax = 2.366498; // averaged

	/** Argon radiation length in cm */
	public static final double ARGONRADLEN = 14.;

	private final double[] A = new double[2];
	private final double[] dA = new double[4];
	private final float[] bf = new float[3];

	double hdata[] = new double[3];

	private double stepSize = 0.2; // step size

	public BasicCovMatTransport() {
		// Max Field Location: (phi, rho, z) = (29.50000, 44.00000, 436.00000)
		// get the maximum value of the B field
		double phi = Math.toRadians(29.5);
		double rho = 44.0;
		double z = 436.0;
		FieldProbe probe = FieldProbe.factory(MagneticFields.getInstance().getIField(FieldType.COMPOSITE));
		Bmax = probe.fieldMagnitude((float) (rho * Math.cos(phi)), (float) (rho * Math.sin(phi)), (float) z);
		Bmax = Bmax * (2.366498 / 4.322871999651699); // scales according to
														// torus scale by
														// reading the map and
														// averaging the value
		// convert to tesla
		Bmax = Bmax / 10;
	}

	
	
	/**
	 * Veronique's original transport
	 * 
	 * @param probe
	 * @param sector
	 * @param i
	 * @param f
	 * @param iVec
	 * @param covMat
	 * @param zf
	 * @param trackTraj
	 * @param trackCov
	 * @param A
	 * @param dA
	 * @return
	 */
	public StateVec transport(final RotatedCompositeProbe probe, final int sector, int i, int f, StateVec iVec,
			CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj, final Map<Integer, CovMat> trackCov,
			final double[] A, final double[] dA) { // s = signed step-size

		if (iVec == null)
			return null;

		double[][] u = new double[5][5];
		double[][] C = new double[5][5];

		double x = iVec.x;
		double y = iVec.y;
		double tx = iVec.tx;
		double ty = iVec.ty;
		double Q = iVec.Q;

		// B-field components at state vector coordinates
		Swim.teslaField(probe, sector, x, y, iVec.z, bf);

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

		double del = zf - iVec.z;
		int nSteps = (int) (Math.abs((del) / stepSize) + 1);

		double s = del / (double) nSteps;
		double z = iVec.z;
		double dPath = 0;

		for (int j = 0; j < nSteps; j++) {
			// get the sign of the step
			if (j == nSteps - 1) {
				s = Math.signum(del) * Math.abs(z - zf);
			}

			Swim.teslaField(probe, sector, x, y, z, bf);

			double v[] = { x, y, z, tx, ty };
			dPath += oneStep(probe, sector, del > 0 ? 1 : -1, v, Q, s, covMat, C, u, bf);
			x = v[0];
			y = v[1];
			z = v[2];
			tx = v[3];
			ty = v[4];

		} // loop over nsteps

		StateVec fVec = new StateVec(f);
		fVec.z = zf;
		fVec.x = x;
		fVec.y = y;
		fVec.tx = tx;
		fVec.ty = ty;
		fVec.Q = Q;
		fVec.B = Math.sqrt(bf[0] * bf[0] + bf[1] * bf[1] + bf[2] * bf[2]);
		fVec.deltaPath = dPath;
		fVec.pzSign = iVec.pzSign;
		// StateVec = fVec;
		trackTraj.put(f, fVec);

		if (covMat.covMat != null) {
			CovMat fCov = new CovMat(f);
			fCov.covMat = covMat.covMat;
			// CovMat = fCov;
			trackCov.put(f, fCov);
		}

		// Trajectory traj = getTrajectory(probe, sector, iVec, zf);
		// fVec = traj.last();

		return fVec;
	}

	private Trajectory getTrajectory(final RotatedCompositeProbe probe, int sector, StateVec iVec, double zf) {

		SwimS swimS = new SwimS(probe);
		swimS.setAbsoluteTolerance(1.0e-3);
		boolean flipped = false;
		double hdata[] = new double[3];
		Trajectory traj = null;

		try {
			StateVec start = new StateVec(iVec);

			double del = zf - start.z;

			if (del < 0) {
				flipped = true;
				start.Q = -start.Q;
			}

			double stepSize = Math.abs(zf - start.z) / 10;
			traj = swimS.sectorAdaptiveRK(sector, start, zf, stepSize, hdata);
			// System.out.println("NSTEPS " + traj.size());
		} catch (SwimException e) {
			e.printStackTrace();
			return null;
		}

		if ((traj != null) && flipped) {
			for (StateVec sv : traj) {
				sv.Q = iVec.Q;
				sv.pzSign = iVec.pzSign;
			}
		}

		return traj;
	}

	/**
	 * 
	 * @param probe
	 * @param sector
	 * @param i
	 * @param f
	 * @param iVec
	 * @param covMat
	 * @param zf
	 * @param trackTraj
	 * @param trackCov
	 * @param A
	 * @param dA
	 * @return
	 */
	public StateVec rkTransport(final RotatedCompositeProbe probe, final int sector, int i, int f, StateVec iVec,
			CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj, final Map<Integer, CovMat> trackCov,
			final double[] A, final double[] dA) { // s = signed step-size

		if (iVec == null)
			return null;

		double[][] u = new double[5][5];
		double[][] C = new double[5][5];

		double Q = iVec.Q;
		double del = zf - iVec.z;
		double dPath = 0;

		SwimS swimS = new SwimS(probe);
		swimS.setAbsoluteTolerance(1.0e-3);
		StateVec fVec = null;
		Trajectory traj = getTrajectory(probe, sector, iVec, zf);

		if (traj == null) {
			System.out.println("Null TRAJECTORY!");
			return null;
		}

		for (int index = 0; index < traj.size() - 1; index++) {
			StateVec sv = traj.get(index);
			StateVec sv2 = traj.get(index + 1);

			double s = sv2.z - sv.z;

			Swim.teslaField(probe, sector, sv.x, sv.y, sv.z, bf);
			double v[] = { sv.x, sv.y, sv.z, sv.tx, sv.ty };
			dPath += oneStep(probe, sector, del > 0 ? 1 : -1, v, Q, s, covMat, C, u, bf);
		}

		fVec = new StateVec(traj.last());

		fVec.B = Math.sqrt(bf[0] * bf[0] + bf[1] * bf[1] + bf[2] * bf[2]);
		fVec.deltaPath = dPath;
		// StateVec = fVec;
		trackTraj.put(f, fVec);

		if (covMat.covMat != null) {
			CovMat fCov = new CovMat(f);
			fCov.covMat = covMat.covMat;
			// CovMat = fCov;
			trackCov.put(f, fCov);
		}

		return fVec;
	}
	


	/**
	 * 
	 * @param probe
	 * @param sector
	 * @param i
	 * @param f
	 * @param iVec
	 * @param covMat
	 * @param zf
	 * @param trackTraj
	 * @param trackCov
	 * @param A
	 * @param dA
	 * @return
	 */
	public StateVec YhalfStepTransport(final RotatedCompositeProbe probe, final int sector, int i, int f, StateVec iVec,
			CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj, final Map<Integer, CovMat> trackCov,
			final double[] A, final double[] dA) { // s = signed step-size

		if (iVec == null) {
			return null;
		}

		double[][] u = new double[5][5];
		double[][] C = new double[5][5];

		double Q = iVec.Q;

		double del = zf - iVec.z;
		int direction = (del > 0) ? 1 : -1;

		SwimS swimS = new SwimS(probe);
		swimS.setAbsoluteTolerance(1.0e-3);
		StateVec fVec = null;
		Trajectory traj = getTrajectory(probe, sector, iVec, zf);

		if (traj == null) {
			System.out.println("Null TRAJECTORY!");
			return null;
		}

		fVec = new StateVec(traj.last());
		Swim.teslaField(probe, sector, iVec.x, iVec.y, iVec.z, bf);
		double v[] = { iVec.x, iVec.y, iVec.z, iVec.tx, iVec.ty };

		double s = fVec.z - iVec.z;

		fVec.deltaPath = halfStep(probe, sector, direction, v, Q, s, covMat, C, u, bf);

		Swim.teslaField(probe, sector, fVec.x, fVec.y, fVec.z, bf);

		fVec.B = Math.sqrt(bf[0] * bf[0] + bf[1] * bf[1] + bf[2] * bf[2]);
		// StateVec = fVec;
		trackTraj.put(f, fVec);

		if (covMat.covMat != null) {
			CovMat fCov = new CovMat(f);
			fCov.covMat = covMat.covMat;
			// CovMat = fCov;
			trackCov.put(f, fCov);
		}

		return fVec;
	}

	/**
	 * 
	 * @param probe
	 * @param sector
	 * @param i
	 * @param f
	 * @param iVec
	 * @param covMat
	 * @param zf
	 * @param trackTraj
	 * @param trackCov
	 * @param A
	 * @param dA
	 * @return
	 */
	public StateVec XhalfStepTransport(final RotatedCompositeProbe probe, final int sector, int i, int f, StateVec iVec,
			CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj, final Map<Integer, CovMat> trackCov,
			final double[] A, final double[] dA) { // s = signed step-size

		if (iVec == null) {
			return null;
		}

		double[][] u = new double[5][5];
		double[][] C = new double[5][5];

		double Q = iVec.Q;

		double del = zf - iVec.z;
		int direction = (del > 0) ? 1 : -1;

		double dPath = 0;

		SwimS swimS = new SwimS(probe);
		swimS.setAbsoluteTolerance(1.0e-3);
		StateVec fVec = null;
		Trajectory traj = getTrajectory(probe, sector, iVec, zf);

		if (traj == null) {
			System.out.println("Null TRAJECTORY!");
			return null;
		}

		for (int index = 0; index < traj.size() - 1; index++) {
			StateVec sv = traj.get(index);
			StateVec sv2 = traj.get(index + 1);

			double s = sv2.z - sv.z;

			Swim.teslaField(probe, sector, sv.x, sv.y, sv.z, bf);
			double v[] = { sv.x, sv.y, sv.z, sv.tx, sv.ty };
			dPath += halfStep(probe, sector, direction, v, Q, s, covMat, C, u, bf);
		}

		fVec = new StateVec(traj.last());

		fVec.B = Math.sqrt(bf[0] * bf[0] + bf[1] * bf[1] + bf[2] * bf[2]);
		fVec.deltaPath = dPath;
		// StateVec = fVec;
		trackTraj.put(f, fVec);

		if (covMat.covMat != null) {
			CovMat fCov = new CovMat(f);
			fCov.covMat = covMat.covMat;
			// CovMat = fCov;
			trackCov.put(f, fCov);
		}

		return fVec;
	}

	/**
	 * 
	 * @param probe
	 * @param sector
	 * @param direction
	 *            the overall sign of Zf - Zi
	 * @param v
	 * @param Q
	 * @param s
	 *            the signed step size
	 * @param covMat
	 * @param C
	 * @param u
	 * @param field
	 * @return
	 */
	public double oneStep(final RotatedCompositeProbe probe, final int sector, final int direction, double[] v,
			double Q, double s, CovMat covMat, double C[][], double u[][], float field[]) {

		// B bf = new B(i, z, x, y, tx, ty, s);
		// bfieldPoints.add(bf);

		double x = v[0];
		double y = v[1];
		double z = v[2];
		double tx = v[3];
		double ty = v[4];

		A(tx, ty, field[0], field[1], field[2], A);
		delA_delt(tx, ty, field[0], field[1], field[2], dA);

		// transport covMat
		double delx_deltx0 = s;
		double dely_deltx0 = 0.5 * Q * speedLight * s * s * dA[2];
		double deltx_delty0 = Q * speedLight * s * dA[1];
		double delx_delQ = 0.5 * speedLight * s * s * A[0];
		double deltx_delQ = speedLight * s * A[0];
		double delx_delty0 = 0.5 * Q * speedLight * s * s * dA[1];
		double dely_delty0 = s;
		double delty_deltx0 = Q * speedLight * s * dA[2];
		double dely_delQ = 0.5 * speedLight * s * s * A[1];
		double delty_delQ = speedLight * s * A[1];

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

		double t_ov_X0 = direction * s / ARGONRADLEN; // path length in
														// radiation length
														// units = t/X0 [true
														// path length/ X0] ; Ar
														// radiation length = 14
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
		double dx = tx * s + 0.5 * Q * speedLight * A[0] * s * s;
		x += dx;
		double dy = ty * s + 0.5 * Q * speedLight * A[1] * s * s;
		y += dy;
		tx += Q * speedLight * A[0] * s;
		ty += Q * speedLight * A[1] * s;

		z += s;

		v[0] = x;
		v[1] = y;
		v[2] = z;
		v[3] = tx;
		v[4] = ty;

		return Math.sqrt(dx * dx + dy * dy + s * s);
	}

	private final double TOLERANCE = 1.0e-4;
	private static final double HGROW = 1.2;
	private static final double HMIN = 0.05;

	private static final double CLOSE = 1.0e-10;

	public double halfStep(final RotatedCompositeProbe probe, final int sector, final int direction, double[] v,
			double Q, double s, CovMat covMat, double C[][], double u[][], float field[]) {

		boolean done = false;

		int nStep = 0;

		double zf = v[2] + s;

		CovMat curCovMat = new CovMat(covMat);
		double tempV[] = new double[v.length];
		double currentV[] = new double[v.length];
		float fieldHalf[] = new float[3];

		System.arraycopy(v, 0, currentV, 0, v.length);

		double C1[][] = new double[5][5];
		double C2[][] = new double[5][5];

		double h = s;

		double ds = 0;

		boolean hitHMIN = false;

		while (!done) {

			// make a copy of the current V
			System.arraycopy(currentV, 0, tempV, 0, v.length);
			CovMat tempCovMat = new CovMat(curCovMat);

			// first half step, changes currentV and currentCovMat
			double ds1 = oneStep(probe, sector, direction, currentV, Q, h / 2, curCovMat, C1, u, field);

			// get the field in the middle
			Swim.teslaField(probe, sector, currentV[0], currentV[1], currentV[2], fieldHalf);

			// second half step keeps changing currentV and currentCovMat
			double ds2 = oneStep(probe, sector, direction, currentV, Q, h / 2, curCovMat, C2, u, fieldHalf);

			// full step changes tempV and tempCovMat
			oneStep(probe, sector, direction, tempV, Q, h, tempCovMat, C, u, fieldHalf);

			double diff = tempCovMat.diff(curCovMat);

			boolean accept = (diff < TOLERANCE) || hitHMIN;

			nStep += 3;

			if (accept) {
				// the good results are in currentV and currentCovMat
				// so copy them to the "real" objects

				System.arraycopy(currentV, 0, v, 0, v.length);
				covMat.copy(curCovMat);

				ds += (ds1 + ds2);
				double zNew = currentV[2];
				done = Math.abs(zf - zNew) < CLOSE;

				// grow h
				if (!done) {
					double hlimit = zf - zNew;
					h = HGROW * h;
					if (direction > 0) {
						h = Math.min(hlimit, h);
					} else {
						h = Math.max(hlimit, h);
					}
				} else {
					// System.out.println("HALFSTEP COMPS: " + nStep);
				}
			} else {
				// failed so have to set cuurent back to
				// the starting values
				System.arraycopy(v, 0, currentV, 0, v.length);
				curCovMat = new CovMat(covMat);

				// shrink
				h = h / 2;

				// too small?
				if (Math.abs(h) < HMIN) {
					System.out.println("Hit HMIN LIMIT");

					h = (direction > 0) ? HMIN : -HMIN;

					hitHMIN = true;
				}

			}

		}

		return ds;
	}

	/**
	 * This is the 2-element A[2] with A[0] = Ax and A[1] = Ay here:
	 * http://arxiv.org/pdf/physics/0511177v1.pdf<br>
	 * 
	 * @param tx
	 * @param ty
	 * @param Bx
	 * @param By
	 * @param Bz
	 * @param a
	 */
	public static void A(double tx, double ty, double Bx, double By, double Bz, double[] a) {

		double C = Math.sqrt(1 + tx * tx + ty * ty);
		a[0] = C * (ty * (tx * Bx + Bz) - (1 + tx * tx) * By);
		a[1] = C * (-tx * (ty * By + Bz) + (1 + ty * ty) * Bx);
	}

	public static void delA_delt(double tx, double ty, double Bx, double By, double Bz, double[] dela_delt) {

		double C2 = 1 + tx * tx + ty * ty;
		double C = Math.sqrt(1 + tx * tx + ty * ty);
		double Ax = C * (ty * (tx * Bx + Bz) - (1 + tx * tx) * By);
		double Ay = C * (-tx * (ty * By + Bz) + (1 + ty * ty) * Bx);

		dela_delt[0] = tx * Ax / C2 + C * (ty * Bx - 2 * tx * By); // delAx_deltx
		dela_delt[1] = ty * Ax / C2 + C * (tx * Bx + Bz); // delAx_delty
		dela_delt[2] = tx * Ay / C2 + C * (-ty * By - Bz); // delAy_deltx
		dela_delt[3] = ty * Ay / C2 + C * (-tx * By + 2 * ty * Bx); // delAy_delty
	}

	public static void main(String argv[]) {

		header("Mag field Initialization");
		MagneticFields.getInstance().initializeMagneticFields();
		MagneticFields.getInstance().setActiveField(FieldType.COMPOSITEROTATED);
		MagneticFields.getInstance().getTorus().setScaleFactor(-0.5);
		MagneticFields.getInstance().getSolenoid().setScaleFactor(0);
		System.out.println(MagneticFields.getInstance().getCurrentConfigurationMultiLine());

		RotatedCompositeProbe rcp = new RotatedCompositeProbe(MagneticFields.getInstance().getRotatedCompositeField());
		int sector = 2;
		System.out.println("sector: " + sector);
		
		HashMap<Integer, StateVec> trajMap = new HashMap<>();
		HashMap<Integer, CovMat> matMap = new HashMap<>();


		double zi = 528.8406160000001;
		double zf = 229.23648;
		header(" TEST (1)  zi: " + zi + "  zf: " + zf + "   pzSign: " + ((zf > zi) ? 1 : -1));

		// momentum and charge
		double p = 1.2252495;
		int q = 1;

		// the initial state vector
		StateVec svi = new StateVec(-119.8901385, 0.5410029, zi, -0.0287247, -0.0083868, q/p, (zf > zi) ? 1 : -1);

		double array[][] = { { 5.26357439, -1.64761026, 0.04338227, -0.00814181, 0.01041586 },
				{ -1.64761026, 304.13559440, -0.04366342, 1.24484383, 0.00310091 },
				{ 0.04338227, -0.04366342, 0.00091018, -0.00020141, 0.00011534 },
				{ -0.00814181, 1.24484383, -0.00020141, 0.00702420, 0.00000639 },
				{ 0.01041586, 0.00310091, 0.00011534, 0.00000639, 0.00132762 } };
		
		doTest(rcp, sector, svi, zf, trajMap, matMap, array);

		System.exit(1);

		zi = 245.96332;
		zf = 353.40971799999994;
		header(" ANOTHER TEST (2)  zi: " + zi + "  zf: " + zf + "   pzSign: " + ((zf > zi) ? 1 : -1));

		// momentum and charge
		p = 1.203859;
		q = 1;

		// the initial state vector
		svi = new StateVec(-77.3874623, 3.6370810, zi, -0.3743674, 0.0056915, q/p, (zf > zi) ? 1 : -1);

		double array2[][] = { { 1.39050442, -0.60880155, 0.00758016, -0.03796520, 0.00000122 },
				{ -0.60880155, 125.40922609, -0.04350925, 0.58199222, -0.00000562 },
				{ 0.00758016, -0.04350925, 0.00100377, -0.00001820, 0.00000029 },
				{ -0.03796520, 0.58199222, -0.00001820, 0.07081348, -0.00000013 },
				{ 0.01041586, 0.00310091, 0.00011534, 0.00000639, 0.00132762 } };
		
		doTest(rcp, sector, svi, zf, trajMap, matMap, array2);


		zi = 525.9066700000001;
		zf = 229.23648;
		header(" ANOTHER TEST (3)  zi: " + zi + "  zf: " + zf + "   pzSign: " + ((zf > zi) ? 1 : -1));

		// momentum and charge
		p = 0.504176;
		q = -1;

		// the initial state vector
		svi = new StateVec(6.8033300, 62.6135951, zi, -0.16484314, 0.0529304, q/p, (zf > zi) ? 1 : -1);

		double array3[][] = { { 0.55745442, -1.76379746, 0.02411801, -0.01480981, -0.00002965 },
				{ -1.76379746, 27.38677719, -0.11588196, 0.20976464, 0.00034266 },
				{ 0.02411801, -0.11588196, 0.00176094, -0.00082367, -0.00000209 },
				{ -0.01480981, 0.20976464, -0.00082367, 0.00405705, 0.00000578 },
				{ -0.00002965, 0.00034266, -0.00000209, 0.00000578, 0.00025341 } };

		doTest(rcp, sector, svi, zf, trajMap, matMap, array3);


		zi = 245.96332000000004;
		zf = 353.4097179999999;
		header(" ANOTHER TEST (4)  zi: " + zi + "  zf: " + zf + "   pzSign: " + ((zf > zi) ? 1 : -1));

		// momentum and charge
		p = 1.051625;
		q = -1;

		// the initial state vector
		svi = new StateVec(-9.3661347, 38.1486121, zi, -0.0183989, 0.1716338, q/p, (zf > zi) ? 1 : -1);

		
		double array4[][] = { { 0.13673342, 0.02140961, 0.00065784, -0.00099076, -0.00037266 },
				{ 0.02140961, 10.56046595, -0.00486797, -0.03981038, 0.00186852 },
				{ 0.00065784, -0.00486797, 0.00019551, 0.00000421, -0.00005127 },
				{ -0.00099076, -0.03981038, 0.00000421, 0.00163020, -0.00004374 },
				{ -0.00037266, 0.00186852, -0.00005127, -0.00004374, 0.00065393 } };

		doTest(rcp, sector, svi, zf, trajMap, matMap, array4);
	}
	
	private static void doTest(RotatedCompositeProbe rcp, int sector, StateVec svi, double zf, 
			Map<Integer, StateVec> trajMap, Map<Integer, CovMat> matMap, double array[][]) {
		
		double[] A = new double[2];
		double[] dA = new double[4];

		Matrix m = new Matrix(array);
//		CovMat covMat = new CovMat(10, m);
		CovMat covMat2 = new CovMat(10, m);
		CovMat covMat3 = new CovMat(10, m);
		CovMat covMat4 = new CovMat(10, m);
		
		
		System.out.println("Initial State Vector TILTED): " + svi);

		Swim.printCovMatrix("Initial covariance matrix", covMat2);
		
//		BasicCovMatTransport basicTransporter = new BasicCovMatTransport();
		
		//to compare
		CovMatTransport  cmDeriv= new CovMatTransport(rcp);

		StateVec fb;
		
//		header("Basic Transport");
//		fb = basicTransporter.transport(rcp, sector, 0, 0, svi, covMat, zf, trajMap, matMap, A, dA);
//		System.out.println("BASIC final vector:\n" + fb);
//		Swim.printCovMatrix("\nBASIC final covariance matrix BASIC", covMat);

		header("CovMatDeriv Transport");
		fb = cmDeriv.euler(sector, 0, 0, svi, covMat2, zf, trajMap, matMap);
		System.out.println("COVMATDERIV final vector:\n" + fb);
		Swim.printCovMatrix("\nCOVMATDERIV final covariance matrix", covMat2);
		
		header("CovMatDeriv State Vec TransportS");
		fb = cmDeriv.halfStepS(sector, 0, 0, svi, covMat3, zf, trajMap, matMap);
		System.out.println("COVMATDERIV State Vec TransportS final vector:\n" + fb);
		Swim.printCovMatrix("\nCOVMATDERIV final TransportS covariance matrix", covMat3);

		header("CovMatDeriv State Vec TransportZ");
		fb = cmDeriv.halfStepZ(sector, 0, 0, svi, covMat4, zf, trajMap, matMap);
		System.out.println("COVMATDERIV State Vec TransportZ final vector:\n" + fb);
		Swim.printCovMatrix("\nCOVMATDERIV final TransportZ covariance matrix", covMat4);
		
	}


	private static void header(String s) {
		System.out.println("\n------------------------------------------");
		System.out.println("-------     " + s);
		System.out.println("------------------------------------------\n");
	}

}
