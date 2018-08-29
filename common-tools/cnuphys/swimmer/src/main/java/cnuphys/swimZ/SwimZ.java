package cnuphys.swimZ;

import java.util.ArrayList;

import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.IMagField;
import cnuphys.magfield.MagneticField;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.rk4.DefaultStopper;
import cnuphys.rk4.IRkListener;
import cnuphys.rk4.IStopper;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.rk4.RungeKuttaZ;
import cnuphys.swim.StateVec;
import cnuphys.swim.Swim;
import cnuphys.swim.SwimException;
import cnuphys.swim.Trajectory;

/**
 * The swimZ integration follows the method described for the HERA-B magnet
 * here: http://arxiv.org/pdf/physics/0511177v1.pdf<br>
 * <p>
 * The state vector has five elements: <br>
 * (x, y, tx, ty, q) <br>
 * Where x and y are the transverse coordinates (meters), tx = px/pz, ty =
 * py/pz, and q = Q/|p| where Q is the integer charge (e.g. -1 for an electron)
 * <p>
 * UNITS
 * <ul>
 * <li>x, y, and z are in cm
 * <li>p is in GeV/c
 * <li>B (mag field) is in kGauss
 * </ul>
 * <p>
 * 
 * @author heddle
 *
 */
public class SwimZ extends Swim {

	// create a do nothing stopper for now
	private IStopper _stopper = new DefaultStopper();

	// need an integrator
	private RungeKuttaZ _rkZ = new RungeKuttaZ();

	// storage for values of independent variable z
	private ArrayList<Double> zArray = new ArrayList<Double>(100);

	// storage for values of the dependent variables (state vector)
	private ArrayList<double[]> yArray = new ArrayList<double[]>(100);

	// the derivatives (i.e., the ODEs)
	private SwimZDerivative _deriv;

	/**
	 * SwimZ null constructor. Here we create a Swimmer that will use the
	 * current active magnetic field.
	 * 
	 * @param field
	 *            interface into a magnetic field
	 */
	public SwimZ() {
	}

	/**
	 * Create a swimmer specific to a magnetic field
	 * 
	 * @param magneticField
	 *            the magnetic field
	 */
	public SwimZ(MagneticField magneticField) {
		super(magneticField);
	}

	/**
	 * Create a swimmer specific to a magnetic field
	 * 
	 * @param magneticField
	 *            the magnetic field
	 */
	public SwimZ(IMagField magneticField) {
		super(magneticField);
	}
	
	/**
	 * Create a swimmer specific to a magnetic field probe
	 * 
	 * @param probe
	 *            the magnetic field probe
	 */
	public SwimZ(FieldProbe probe) {
		super(probe);
	}


	/**
	 * Set the tolerance used by the CLAS_Tolerance array
	 * 
	 * @param eps
	 *            the baseline absolute tolerance.
	 */
	@Override
	public void setAbsoluteTolerance(double eps) {
		_eps = eps;
		double xscale = 1.0; // position scale order of cm
		double pscale = 1.0; // track slope scale order of 1
		double xTol = eps * xscale;
		double pTol = eps * pscale;
		for (int i = 0; i < 2; i++) {
			_absoluteTolerance[i] = xTol;
			_absoluteTolerance[i + 2] = pTol;
		}
	}

	// some initialization
	@Override
	protected void initialize() {

		if (_probe instanceof RotatedCompositeProbe) {
			_deriv = new SectorSwimZDerivative();
		} else {
			_deriv = new SwimZDerivative();
		}

		setAbsoluteTolerance(1.0e-4);
	}

	/**
	 * Swim to a fixed z over short distances using RK adaptive stepsize
	 * 
	 * @param start
	 *            the starting state vector
	 * @param zf
	 *            the final z value (cm)
	 * @param stepSize
	 *            the initial step size
	 * @param relTolerance
	 *            the absolute tolerances on each state variable [x, y, tx, ty]
	 *            (q = const). So it is an array with four entries, like [1.0e-4
	 *            cm, 1.0e-4 cm, 1.0e-5, 1.0e05]
	 * @param hdata
	 *            An array with three elements. Upon return it will have the
	 *            min, average, and max step size (in that order).
	 * @return the swim result
	 * @throws SwimException
	 */
	public Trajectory adaptiveRK(StateVec start, final double zf, double stepSize, double hdata[])
			throws SwimException {

		if (start == null) {
			throw new SwimException("Null starting state vector in SwimZ adaptiveRK.");
		}

		if (!start.pzSignCorrect(start.z, zf)) {
			throw new SwimException("Inconsistent sign for Pz in adaptiveRK" + "\nzi: " + start.z + "  zf: " + zf
					+ "  sign: " + start.pzSign);
		}

		int q = start.getCharge();

		// straight line?
		if ((q == 0) || (_probe == null) || _probe.isZeroField()) {
			return straightLineResult(start, zf);
		}

		// ARGGH
		if (start.pzSign < 0) {
			q = -q;
		}

		// need to set the derivative
		_deriv.set(q, start.getP(), _probe);

		double yo[] = { start.x, start.y, start.tx, start.ty };

		// create the lists to hold the trajectory
		zArray.clear();
		yArray.clear();

		int nStep = 0;
		try {
			nStep = _rkZ.adaptiveStepZoZf(yo, start.z, zf, stepSize, zArray, yArray, _deriv, _stopper,
					_absoluteTolerance, hdata);
		} catch (RungeKuttaException e) {
			e.printStackTrace();
		}

		if (nStep == 0) {

			Trajectory result = new Trajectory();
			result.add(start);
			StateVec stop = new StateVec(start);

			// go in one step

			double x[] = { start.x, start.y, start.tx, start.ty };
			double dfdz[] = new double[4];
			_deriv.derivative(start.z, x, dfdz);
			double dz = zf - start.z;
			stop.x = start.x + dfdz[0] * dz;
			stop.y = start.y + dfdz[1] * dz;
			stop.tx = start.tx + dfdz[2] * dz;
			stop.ty = start.ty + dfdz[3] * dz;
			stop.z = zf;

			result.add(start);
			result.add(stop);
			return result;
		}

		Trajectory result = new Trajectory(nStep);
		// result.add(start);
		for (int i = 0; i < zArray.size(); i++) {
			double v[] = yArray.get(i);

			StateVec sv = new StateVec(zArray.get(i), v, start.Q, start.pzSign);
			result.add(sv);
		}

		return result;

	}

	/**
	 * Swim to a fixed z over short distances using RK adaptive stepsize
	 * This is for swimming in a local, tilted system.
	 * 
	 * @param sector
	 *            the sector [1..6]
	 * @param start
	 *            the starting state vector
	 * @param zf
	 *            the final z value (cm)
	 * @param stepSize
	 *            the initial step size
	 * @param relTolerance
	 *            the absolute tolerances on each state variable [x, y, tx, ty]
	 *            (q = const). So it is an array with four entries, like [1.0e-4
	 *            cm, 1.0e-4 cm, 1.0e-5, 1.0e05]
	 * @param hdata
	 *            An array with three elements. Upon return it will have the
	 *            min, average, and max step size (in that order).
	 * @return the swim result
	 * @throws SwimException
	 */
	public Trajectory sectorAdaptiveRK(int sector, StateVec start, final double zf, double stepSize, double hdata[])
			throws SwimException {

		if (!(_probe instanceof RotatedCompositeProbe)) {
			System.err.println("Can only call sectorAdaptiveRK with a RotatedComposite Probe");
			System.exit(1);
			return null;
		}

		if (start == null) {
			throw new SwimException("Null starting state vector.");
		}

		if (!start.pzSignCorrect(start.z, zf)) {
			throw new SwimException("Inconsistent sign for Pz in sectorAdaptiveRK" + "\nzi: " + start.z + "  zf: " + zf
					+ "  sign: " + start.pzSign);
		}

		int q = start.getCharge();

		// straight line?
		if ((q == 0) || (_probe == null) || _probe.isZeroField()) {
			return straightLineResult(start, zf);
		}

		// ARGGH
		if (start.pzSign < 0) {
			q = -q;
		}

		// need to set the derivative
		((SectorSwimZDerivative) _deriv).set(sector, q, start.getP(), _probe);

		double yo[] = { start.x, start.y, start.tx, start.ty };

		// create the lists to hold the trajectory
		zArray.clear();
		yArray.clear();

		int nStep = 0;
		try {
			nStep = _rkZ.adaptiveStepZoZf(yo, start.z, zf, stepSize, zArray, yArray, _deriv, _stopper,
					_absoluteTolerance, hdata);
		} catch (RungeKuttaException e) {
			// System.err.println("Integration Failure");
			// System.err.println("Q = " + Q + " p = " + p + " zf = " + zf);
			// int pzSign = (zf < start.z) ? -1 : 1;
			// System.err.println("Start SV: " + start.normalPrint(p, pzSign));
			// e.printStackTrace();
			throw new SwimException("Runge Kutta Failure in SwimZ sectorAdaptiveRK [" + e.getMessage() + "]");
		}

		if (nStep == 0) {

			Trajectory result = new Trajectory(nStep);
			result.add(start);
			StateVec stop = new StateVec(start);

			// go in one step

			double x[] = { start.x, start.y, start.tx, start.ty };
			double dfdz[] = new double[4];
			_deriv.derivative(start.z, x, dfdz);
			double dz = zf - start.z;
			stop.x = start.x + dfdz[0] * dz;
			stop.y = start.y + dfdz[1] * dz;
			stop.tx = start.tx + dfdz[2] * dz;
			stop.ty = start.ty + dfdz[3] * dz;
			stop.z = zf;

			result.add(start);
			result.add(stop);
			// System.out.println("start:\n" + start);
			// System.out.println("stop:\n" + stop);
			// System.out.println("zf: " + zf + " stepSize: " + stepSize);
			return result;
		}

		Trajectory result = new Trajectory(nStep);
		// result.add(start);
		for (int i = 0; i < zArray.size(); i++) {
			double v[] = yArray.get(i);
			StateVec sv = new StateVec(zArray.get(i), v, start.Q, start.pzSign);
			result.add(sv);
		}

		return result;

	}

	/**
	 * Swim to a fixed z using RK adaptive stepsize
	 * 
	 * @param start
	 *            the starting state vector
	 * @param stop
	 *            will hold the final state vector
	 * @param zf
	 *            the final z value
	 * @param stepSize
	 *            the initial step size
	 * @param hdata
	 *            An array with three elements. Upon return it will have the
	 *            min, average, and max step size (in that order).
	 * @return the number of steps
	 * @throws SwimException
	 */
	public int adaptiveRK(StateVec start, StateVec stop, final double zf, double stepSize, double hdata[])
			throws SwimException {
		if (start == null) {
			throw new SwimException("Null starting state vector.");
		}

		int q = start.getCharge();
		double p = start.getP();

		stop.copy(start);

		// straight line?
		if ((q == 0) || (_probe == null) || _probe.isZeroField()) {
			System.out.println("Z adaptive swimmer detected straight line.");
			straightLineResult(q, p, start, stop, zf);
			return 2;
		}

		// need to set the derivative
		_deriv.set(q, p, _probe);

		double yo[] = { start.x, start.y, start.tx, start.ty };

		IRkListener listener = new IRkListener() {

			@Override
			public void nextStep(double newZ, double[] newStateVec, double h) {
				stop.x = newStateVec[0];
				stop.y = newStateVec[1];
				stop.tx = newStateVec[2];
				stop.ty = newStateVec[3];
				stop.z = newZ;
			}

		};

		int nStep = 0;
		try {
			nStep = _rkZ.adaptiveStepZoZf(yo, start.z, zf, stepSize, _deriv, _stopper, listener, _absoluteTolerance,
					hdata);
		} catch (RungeKuttaException e) {
			e.printStackTrace();
		}

		return nStep;
	}

	/**
	 * Swim to a fixed z over short distances using a parabolic estimate,
	 * without intermediate points
	 * 
	 * @param Q
	 *            the integer charge of the particle (-1 for electron)
	 * @param p
	 *            the momentum in GeV/c
	 * @param start
	 *            the starting state vector
	 * @param stop
	 *            at end, holds final state vector
	 * @param zf
	 *            the final z value (cm)
	 * @return the swim result
	 * @throws SwimException
	 */

	public void parabolicEstimate(double p, StateVec start, StateVec stop, double zf) throws SwimException {

		if (start == null) {
			throw new SwimException("Null starting state vector.");
		}

		int Q = start.getCharge();

		// straight line?
		if ((Q == 0) || (_probe == null) || _probe.isZeroField()) {
			System.out.println("Z parabolicEstimate swimmer detected straight line.");
			straightLineResult(Q, p, start, stop, zf);
			return;
		}

		double q = Q / p;

		// get the field
		float B[] = new float[3];
		double x0 = start.x;
		double y0 = start.y;
		double z0 = start.z;
		double tx0 = start.tx;
		double ty0 = start.ty;

		_probe.field((float) x0, (float) y0, (float) z0, B);

		// some needed factors
		double txsq = tx0 * tx0;
		double tysq = ty0 * ty0;
		double fact = Math.sqrt(1 + txsq + tysq);
		double Ax = fact * (ty0 * (tx0 * B[0] + B[2]) - (1 + txsq) * B[1]);
		double Ay = fact * (-tx0 * (ty0 * B[1] + B[2]) + (1 + tysq) * B[0]);

		double s = (stop.z - start.z);
		double qvs = q * speedLight * s;
		double qvsq = 0.5 * qvs * s;

		stop.z = zf;
		stop.x = start.x + tx0 * s + qvsq * Ax;
		stop.y = start.y + ty0 * s + qvsq * Ay;
		stop.tx = tx0 + qvs * Ax;
		stop.ty = ty0 + qvs * Ay;

	}

	/**
	 * Swim to a fixed z over short distances using a parabolic estimate
	 * 
	 * @param p
	 *            the momentum in GeV/c
	 * @param start
	 *            the starting state vector
	 * @param zf
	 *            the final z value (cm)
	 * @param stepSize
	 *            the step size
	 * @return the swim result
	 * @throws SwimException
	 */

	public Trajectory parabolicEstimate(StateVec start, double zf, double stepSize) throws SwimException {

		if (start == null) {
			throw new SwimException("Null starting state vector.");
		}

		// make sure start has the righ pz sign
		int pzSign = (zf > start.z) ? 1 : -1;
		;
		start.pzSign = (zf > start.z) ? 1 : -1;

		int Q = start.getCharge();
		double p = start.getP();

		// straight line?
		if (Q == 0) {
			return straightLineResult(start, zf);
		}

		double q = start.Q;

		// obtain a range
		SwimZRange swimZrange = new SwimZRange(start.z, zf, stepSize);

		// storage for results
		Trajectory result = new Trajectory(swimZrange.getNumStep() + 1);
		result.add(start);
		StateVec v0 = start;

		for (int i = 0; i < swimZrange.getNumStep(); i++) {
			// get the field
			float B[] = new float[3];
			double x0 = v0.x;
			double y0 = v0.y;
			double z0 = v0.z;
			double tx0 = v0.tx;
			double ty0 = v0.ty;

			_probe.field((float) x0, (float) y0, (float) z0, B);

			// some needed factors
			double txsq = tx0 * tx0;
			double tysq = ty0 * ty0;
			double fact = Math.sqrt(1 + txsq + tysq);
			double Ax = fact * (ty0 * (tx0 * B[0] + B[2]) - (1 + txsq) * B[1]);
			double Ay = fact * (-tx0 * (ty0 * B[1] + B[2]) + (1 + tysq) * B[0]);

			double s = stepSize;
			double qvs = q * speedLight * s;
			double qvsq = 0.5 * qvs * s;

			double x1 = x0 + tx0 * s + qvsq * Ax;
			double y1 = y0 + ty0 * s + qvsq * Ay;
			double tx1 = tx0 + qvs * Ax;
			double ty1 = ty0 + qvs * Ay;
			// public StateVec(double x, double y, double z, double tx,
			// double ty,
			// double q) {

			StateVec v1 = new StateVec(x1, y1, swimZrange.z(i + 1), tx1, ty1, q, pzSign);

			// add to the resuts
			result.add(v1);
			v0 = v1;
		}

		return result;
	}

	// a straight line just containing the end points
	// might be charge is zero or might be B is zero
	private Trajectory straightLineResult(StateVec start, double zf) {

		int pzSign = (zf > start.z) ? 1 : -1;
		start.pzSign = pzSign;

		Trajectory result = new Trajectory(2);
		result.add(start);
		double s = zf - start.z;
		double x1 = start.x + start.tx * s;
		double y1 = start.y + start.ty * s;
		StateVec v = new StateVec(x1, y1, zf, start.tx, start.ty, start.Q, pzSign);
		result.add(v);
		return result;
	}

	// straight line returning the end point
	// might be charge is zero or might be B is zero
	private void straightLineResult(int Q, double p, StateVec start, StateVec stop, double zf) {
		double s = zf - start.z;
		stop.x = start.x + start.tx * s;
		stop.y = start.y + start.ty * s;
		stop.z = zf;
		stop.tx = start.tx;
		stop.ty = start.ty;
	}
	
	/**
	 * Set the maximum step size
	 * 
	 * @param maxSS
	 *            the maximum stepsize is whatever units you are using
	 */
	public void setMaxStepSize(double maxSS) {
		_rkZ.setMaxStepSize(maxSS);
	}

	/**
	 * Set the minimum step size
	 * 
	 * @param maxSS
	 *            the minimum stepsize is whatever units you are using
	 */
	public void setMinStepSize(double minSS) {
		_rkZ.setMinStepSize(minSS);
	}

	/**
	 * Get the maximum step size
	 * 
	 * @return the maximum stepsize is whatever units you are using
	 */
	public double getMaxStepSize() {
		return _rkZ.getMaxStepSize();
	}
	
	/**
	 * Get the minimum step size
	 * 
	 * @return the minimum stepsize is whatever units you are using
	 */
	public double getMinStepSize() {
		return _rkZ.getMinStepSize();
	}

}
