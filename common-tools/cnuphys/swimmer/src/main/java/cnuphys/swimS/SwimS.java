package cnuphys.swimS;

import java.util.ArrayList;
import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.IMagField;
import cnuphys.magfield.MagneticField;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.rk4.IStopper;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.rk4.RungeKuttaS;
import cnuphys.swim.StateVec;
import cnuphys.swim.Swim;
import cnuphys.swim.SwimException;
import cnuphys.swim.Trajectory;

/**
 * SwimS is a swimmer that uses the pathlength S as the independent variable.
 * In general it is somewhat slower that SwimZ for stopping at a fixed Z. However,
 * it is safer in that you can easily ask SwimZ to do the impossible (i.e., swim to an
 * unphysical value of Z) in which case SwimZ will thrash around before throwing an
 * exception or returning nonsense. SwimZ fails for any particle that "turns around" in
 * Z. SwimS, on the other hand doesn't have to deal with unphysical demands. However
 * it has six independent variables: x, y, z, dx/ds, dy/ds and dz/ds where as for SwimZ
 * (given constant p) there are only four (x, y, tx, and ty)
 * 
 * Unlike the original swim classes (Swimmer and Swimmer2) SwimS (like SwimZ) uses cm for distance unit
 * @author heddle
 *
 */
public class SwimS extends Swim {
	
	// need an integrator
	private RungeKuttaS _rkS = new RungeKuttaS();
	
	// storage for values of independent variable s
	private ArrayList<Double> sArray = new ArrayList<Double>(100);

	// storage for values of the dependent variables (state vectors u)
	private ArrayList<double[]> uArray = new ArrayList<double[]>(100);

	// the derivatives (i.e., the ODEs)
	private SwimSDerivative _deriv;

	//the maximum pathlength in cm
	private double _SMAX = 900;
	
	//the default accuracy in cm
	private double _ACCURACY = 1.0e-3; //10 microns
	
	/**
	 * SwimZ null constructor. Here we create a Swimmer that will use the current active
	 * magnetic field.
	 * 
	 * @param field
	 *            interface into a magnetic field
	 */
	public SwimS() {
	}

	/**
	 * Create a swimmer specific to a magnetic field
	 * 
	 * @param magneticField
	 *            the magnetic field
	 */
	public SwimS(MagneticField magneticField) {
		super(magneticField);
	}

	/**
	 * Create a swimmer specific to a magnetic field
	 * 
	 * @param magneticField
	 *            the magnetic field
	 */
	public SwimS(IMagField magneticField) {
		super(magneticField);
	}
	
	/**
	 * Create a swimmer specific to a magnetic field probe
	 * 
	 * @param probe
	 *            the magnetic field probe
	 */
	public SwimS(FieldProbe probe) {
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
		for (int i = 0; i < 3; i++) {
			_absoluteTolerance[i] = xTol;
			_absoluteTolerance[i + 3] = pTol;
		}
	}


	// some initialization
	@Override
	protected void initialize() {

		if (_probe instanceof RotatedCompositeProbe) {
			_deriv = new SectorSwimSDerivative();
		} else {
			_deriv = new SwimSDerivative();
		}

		setAbsoluteTolerance(1.0e-4);
	}


	/**
	 * Set the maximum pathlength in cm.
	 * The integration will stop here no matter what.
	 * @param smax the maximum pathlength in meters
	 */
	public void setSMax(double smax) {
		_SMAX = smax;
	}
	
	/**
	 * Set the accuracy for fixed z swims cm.
	 * @param accuracy the fixed z accuracy in cm
	 */
	public void setAccuracy(double accuracy) {
		_ACCURACY = accuracy;
	}

	/**
	 * Swim to a fixed z. If the particle does not make it to
	 * that value of z (e.g., heading in the wrong direction) it
	 * will swim to the maximum path length.
	 * 
	 * @param start
	 *            the starting state vector
	 * @param zf
	 *            the final z value (cm)
	 * @param stepSize
	 *            the initial step size. Usualu 0.01 is safe.
	 * @param relTolerance
	 *            the absolute tolerances on each state variable [x, y, tx, ty]
	 *            So it is an array with four entries, like [1.0e-4
	 *            cm, 1.0e-4 cm, 1.0e-5, 1.0e05]
	 * @param hdata
	 *            An array with three elements. Upon return it will have the
	 *            min, average, and max step size (in that order).
	 * @return the swim result
	 * @throws SwimException
	 */
	public Trajectory adaptiveRK(StateVec start, final double zf, double stepSize,
			double hdata[]) throws SwimException {
		
		if (start == null) {
			throw new SwimException("Null starting state vector in SwimS adaptiveRK.");
		}
		
		int q = start.getCharge();
		
		//low momentum? If so, one point track
		if (start.getP() < MINMOMENTUM) {
			Trajectory traj = new Trajectory();
			traj.add(start);
			return traj;
		}
		
		// straight line?
		if ((q == 0) || (_probe == null) || _probe.isZeroField()) {
			return straightLineResult(start, zf);
		}

		// normally we swim from small z to a larger z cutoff.
		// but we can handle either
		final boolean normalDirection = (zf > start.z);
		IStopper stopper = new ZStopper(0, _SMAX, zf, _ACCURACY, normalDirection);

		//initial state array from the SwimZ state vector
		double u0[] = new double[6];
		stateVecToU(start, u0);
		
		// need to set the derivative
		_deriv.set(q, start.getP(), _probe);

		
		// create the lists to hold the trajectory
		sArray.clear();
		uArray.clear();

		int nStep = 0;
		try {
			nStep = _rkS.adaptiveStep(u0, 0, stepSize, sArray, uArray, _deriv, stopper,
					_absoluteTolerance, hdata);
		} catch (RungeKuttaException e) {
			e.printStackTrace();
		}
		
		if (nStep == 0) {
			
			Trajectory result = new Trajectory();
			result.add(start);
			StateVec stop = new StateVec(start);
			
			//go in one step
			
			double x[] = {start.x, start.y, start.tx, start.ty};
			double dfdz[] = new double[4];
			_deriv.derivative(start.z, x, dfdz);
			double dz = zf - start.z;
			stop.x = start.x + dfdz[0]*dz;
			stop.y = start.y + dfdz[1]*dz;
			stop.tx = start.tx + dfdz[2]*dz;
			stop.ty = start.ty + dfdz[3]*dz;
			stop.z = zf;
			
			result.add(start);
			result.add(stop);
			return result;
		}

		Trajectory result = new Trajectory(nStep);
		//result.add(start);
		for (int i = 0; i < sArray.size(); i++) {
			
			double u[] = uArray.get(i);
			
			StateVec sv = new StateVec();
			uToStateVec(start.getCharge(), start.getP(), sv, u);
			
			result.add(sv);
		}

		return result;
	}
	
	/**
	 * Swim to a fixed z. If the particle does not make it to
	 * that value of z (e.g., heading in the wrong direction) it
	 * will swim to the maximum path length.
	 * 
	 * @param sector
	 *            the sector [1..6]
	 * @param start
	 *            the starting state vector
	 * @param zf
	 *            the final z value (cm)
	 * @param stepSize
	 *            the initial step size. Usualu 0.01 is safe.
	 * @param relTolerance
	 *            the absolute tolerances on each state variable [x, y, tx, ty]
	 *            So it is an array with four entries, like [1.0e-4
	 *            cm, 1.0e-4 cm, 1.0e-5, 1.0e05]
	 * @param hdata
	 *            An array with three elements. Upon return it will have the
	 *            min, average, and max step size (in that order).
	 * @return the swim result
	 * @throws SwimException
	 */
	public Trajectory sectorAdaptiveRK(int sector, StateVec start, final double zf, double stepSize,
			double hdata[]) throws SwimException {
		
		if (!(_probe instanceof RotatedCompositeProbe)) {
			System.err.println("Can only call sectorAdaptiveRK with a RotatedComposite Probe");
			System.exit(1);
			return null;
		}

		if (start == null) {
			throw new SwimException("Null starting state vector in SwimS adaptiveRK.");
		}
		
		
		int q = start.getCharge();
		
		//low momentum? If so, one point track
		if (start.getP() < MINMOMENTUM) {
			System.out.println("Low momentum track. p: " + start.getP());
			Trajectory traj = new Trajectory();
			traj.add(start);
			return traj;
		}
		
		// straight line?
		if ((q == 0) || (_probe == null) || _probe.isZeroField()) {
			System.out.println("Low momentum track. q: " + start.getCharge() + " probe: " + _probe);
			return straightLineResult(start, zf);
		}

		// normally we swim from small z to a larger z cutoff.
		// but we can handle either
		final boolean normalDirection = (zf > start.z);
		IStopper stopper = new ZStopper(0, _SMAX, zf, _ACCURACY, normalDirection);

		//initial state array from the SwimZ state vector
		double u0[] = new double[6];
		stateVecToU(start, u0);
		
		// need to set the derivative
		((SectorSwimSDerivative)_deriv).set(sector, q, start.getP(), (RotatedCompositeProbe)_probe);

		
		// create the lists to hold the trajectory
		sArray.clear();
		uArray.clear();

		int nStep = 0;
		try {
			nStep = _rkS.adaptiveStep(u0, 0, stepSize, sArray, uArray, _deriv, stopper,
					_absoluteTolerance, hdata);
		} catch (RungeKuttaException e) {
			e.printStackTrace();
		}
		
		if (nStep == 0) {
			System.out.println("Nstep = 0 track");
			Trajectory result = new Trajectory();
			result.add(start);
			StateVec stop = new StateVec(start);
			
			//go in one step
			
			double x[] = {start.x, start.y, start.tx, start.ty};
			double dfdz[] = new double[4];
			_deriv.derivative(start.z, x, dfdz);
			double dz = zf - start.z;
			stop.x = start.x + dfdz[0]*dz;
			stop.y = start.y + dfdz[1]*dz;
			stop.tx = start.tx + dfdz[2]*dz;
			stop.ty = start.ty + dfdz[3]*dz;
			stop.z = zf;
			
			result.add(start);
			result.add(stop);
			return result;
		}

		Trajectory result = new Trajectory(nStep);
		//result.add(start);
		for (int i = 0; i < sArray.size(); i++) {
			
			double u[] = uArray.get(i);
			
			StateVec sv = new StateVec();
			uToStateVec(start.getCharge(), start.getP(), sv, u);
			
			result.add(sv);
		}

		return result;
	}
	
	
	// a straight line just containing the end points
	//might be charge is zero or might be B is zero
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

	/**
	 * Converts the state vec used by Swim Z and used by the reconstruction
	 * to the u array, which is actually the state vector used by SwimS:
	 * u = [x, y, z, px/p, py/p, pz/p]
	 * @param sv the SwimZ state vector
	 * @param u the SwimS state array. Should be dim 6
	 */
	public static void stateVecToU(StateVec sv, double u[]) {
		double p = sv.getP();
		double pz = sv.getPz();
		double px = pz * sv.tx;
		double py = pz * sv.ty;

		u[0] = sv.x;
		u[1] = sv.y;
		u[2] = sv.z;
		u[3] = px/p;
		u[4] = py/p;
		u[5] = pz/p;
	}
	
	/**
	 * Converts the state vec used by Swim Z and used by the reconstruction
	 * to the u array, which is actually the state vector used by SwimS:
	 * u = [x, y, z, px/p, py/p, pz/p]
	 * @param charge the integer charge
	 * @param sv the SwimZ state vector
	 * @param u the SwimS state array. Should be dim 6
	 */
	public static void uToStateVec(int charge, double p, StateVec sv, double u[]) {
		
		//these are normalized components
		double px = u[3];
		double py = u[4];
		double pz = u[5];
				
		sv.set(charge, p, u[0], u[1], u[2], p*px, p*py, p*pz);

	}
	
	private void printU(double u[]) {
		System.out.println(String.format("(%-9.6f, %-9.6f, %-9.6f, %-9.6f, %-9.6f, %-9.6f)", u[0], u[1], u[2], u[3], u[4], u[5]));
	}
	


}
