package cnuphys.swimZ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.util.FastMath;

import Jama.Matrix;
import cnuphys.magfield.IMagField;
import cnuphys.magfield.MagneticField;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.magfield.MagneticFields.FieldType;
import cnuphys.rk4.DefaultStopper;
import cnuphys.rk4.IRkListener;
import cnuphys.rk4.IStopper;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.rk4.RungeKuttaZ;
import cnuphys.swim.CovMat;
import cnuphys.swim.StateVec;
import cnuphys.swim.Swim;
import cnuphys.swim.SwimException;
import cnuphys.swim.Trajectory;
import cnuphys.swimtest.SwimTest;

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

	/** Argon radiation length in cm */
	public static final double ARGONRADLEN = 14.;

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
	 * SwimZ null constructor. Here we create a Swimmer that will use the current active
	 * magnetic field.
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
	 * Set the tolerance used by the CLAS_Tolerance array
	 * 
	 * @param eps
	 *            the baseline absolute tolerance.
	 */
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

		setAbsoluteTolerance(1.0e-3);
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
	public Trajectory adaptiveRK(StateVec start, final double zf, double stepSize,
			double hdata[]) throws SwimException {

		if (start == null) {
			throw new SwimException("Null starting state vector in SwimZ adaptiveRK.");
		}
		
		if (!start.pzSignCorrect(start.z, zf)) {
			throw new SwimException("Inconsistent sign for Pz in adaptiveRK" +
		"\nzi: " + start.z + "  zf: " + zf + "  sign: " + start.pzSign);
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
		for (int i = 0; i < zArray.size(); i++) {
			double v[] = yArray.get(i);
						
			StateVec sv = new StateVec(zArray.get(i), v, start.Q, start.pzSign);
			result.add(sv);
		}

		return result;

	}

	/**
	 * Swim to a fixed z over short distances using RK adaptive stepsize
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
	public Trajectory sectorAdaptiveRK(int sector, StateVec start, final double zf,
			double stepSize, double hdata[]) throws SwimException {

		if (!(_probe instanceof RotatedCompositeProbe)) {
			System.err.println("Can only call sectorAdaptiveRK with a RotatedComposite Probe");
			System.exit(1);
			return null;
		}

		if (start == null) {
			throw new SwimException("Null starting state vector.");
		}
		
		if (!start.pzSignCorrect(start.z, zf)) {
			throw new SwimException("Inconsistent sign for Pz in sectorAdaptiveRK" +
		"\nzi: " + start.z + "  zf: " + zf + "  sign: " + start.pzSign);
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
//			System.out.println("start:\n" + start);
//			System.out.println("stop:\n" + stop);
//			System.out.println("zf: " + zf + " stepSize: " + stepSize);
			return result;
		}

		Trajectory result = new Trajectory(nStep);
//		result.add(start);
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
	public int adaptiveRK(StateVec start, StateVec stop, final double zf,
			double stepSize, double hdata[]) throws SwimException {
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

	public void parabolicEstimate(double p, StateVec start, StateVec stop, double zf)
			throws SwimException {

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

	public Trajectory parabolicEstimate(StateVec start, double zf, double stepSize)
			throws SwimException {

		if (start == null) {
			throw new SwimException("Null starting state vector.");
		}
		
		//make sure start has the righ pz sign
		int pzSign = (zf > start.z) ?  1 : -1;;
		start.pzSign = (zf > start.z) ?  1 : -1;

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

	// straight line returning the end point
	//might be charge is zero or might be B is zero
	private void straightLineResult(int Q, double p, StateVec start, StateVec stop, double zf) {
		double s = zf - start.z;
		stop.x = start.x + start.tx * s;
		stop.y = start.y + start.ty * s;
		stop.z = zf;
		stop.tx = start.tx;
		stop.ty = start.ty;
	}
	
	//compute the field in Tesla
	private void teslaField(int sector, double x, double y, double z, float[] result) {
		_probe.field(sector, (float)x, (float)y, (float)z, result);
		//to tesla from kG
		for (int i = 0; i < 3; i++) {
			result[i] /= 10;
		}
	}
	
	boolean pause = true;
		   public void transportDriver(int sector, int i, int f, StateVec iVec, CovMat covMat, double zf, 
	    		Map<Integer, StateVec> trackTraj, Map<Integer, CovMat> trackCov, double[] A,
				double[] dA) { // s = signed step-size
			   
			   CovMat covMat2 = new CovMat(covMat);
		   
	    try {
			StateVec v2 = sectorTransport(sector, i, f, iVec, covMat2, zf, trackTraj, trackCov, A, dA);
		    StateVec v1 = transport(sector, i, f, iVec, covMat, zf, trackTraj, trackCov, A, dA);
			
			double dR = (v2 != null) ? v1.dR(v2) : Double.NaN;
			double bDiff = 100.*(Math.abs(v2.B-v1.B)/(0.00001 + Math.max(v1.B, v2.B)));
			
			if (dR > 1) {
				System.err.println("\nR DIFF V1 V2: "+dR);
				System.err.println("B REL DIF V1 V2: "+ bDiff);
				System.out.println("\niVec:\n" + iVec);
				System.out.println("\nV1:\n" + v1);
				System.out.println("\nV2:\n" + v2);
			}
		} catch (SwimException e) {
			e.printStackTrace();
		}
	
	}   
	
	/**
	 * Veronique's basic transport
	 * @param sector
	 * @param i
	 * @param f
	 * @param iVec
	 * @param covMat
	 */
    static boolean DEBUG = false;

    public StateVec transport(int sector, int i, int f, StateVec iVec, CovMat covMat, double zf, 
    		Map<Integer, StateVec> trackTraj, Map<Integer, CovMat> trackCov, double[] A,
			double[] dA) { // s = signed step-size
    	
     	
        if(iVec==null)
            return null;
        //StateVec iVec = trackTraj.get(i);
        //bfieldPoints = new ArrayList<B>();
       // CovMat covMat = icovMat;
     //   double[] A = new double[5];
      //  double[] dA = new double[5];
        double[][] u = new double[5][5];       
        double[][] C = new double[5][5];
       // Matrix Cpropagated = null;
        //double[][] transpStateJacobian = null;

        double x = iVec.x;
        double y = iVec.y;
        double tx = iVec.tx;
        double ty = iVec.ty;
        double Q = iVec.Q;
        double Bf = iVec.B;
        double stepSize = 0.2;
        
        float[] bf = new float[3];
        
        
 //       boolean PRINT = (Math.abs(Z[i]-491.90442) < .001) && (Math.abs(Z[f]-494.70984) < 0.001) ;
        
        double diff = zf-iVec.z;
        boolean PRINT = (diff < -100);
        
        

        if (PRINT && DEBUG) {
            System.out.println("\nTRANSPORT from Zi = " + iVec.z + " to  Zf = " + zf);
            System.out.println("Mag Field Config:");
            System.out.println(MagneticFields.getInstance().getCurrentConfigurationMultiLine());
        	System.out.println("sector = " + sector);
      //  	System.out.println("WAS CHARGE FLIPPED: " + dcSwim.isChargeFlipped());
        	System.out.println("Initial State Vec:\n" + iVec);
            printCovMatrix("\nInitial Covariance Matrix:", covMat);
        }
        
        
        
        // B-field components at state vector coordinates
        teslaField(sector, x, y, iVec.z, bf);
        
        double Bmax = 2.366498;
       // if (bfieldPoints.size() > 0) {
        //    double B = new Vector3D(bfieldPoints.get(bfieldPoints.size() - 1).Bx, bfieldPoints.get(bfieldPoints.size() - 1).By, bfieldPoints.get(bfieldPoints.size() - 1).Bz).mag();
        if (bf!=null) { // get the step size used in swimming as a function of the field intensity in the region traversed
            double B = Math.sqrt(bf[0]*bf[0]+bf[1]*bf[1]+bf[2]*bf[2]); 
            if (B / Bmax > 0.01) {
                stepSize = 0.15*4;
            }
            if (B / Bmax > 0.02) {
                stepSize = 0.1*3;
            }
            if (B / Bmax > 0.05) {
                stepSize = 0.075*2;
            }
            if (B / Bmax > 0.1) {
                stepSize = 0.05*2;
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
        

        double s  = (zf - iVec.z) / nSteps;
        double z = iVec.z;
        double dPath=0;
       
        for (int j = 0; j < nSteps; j++) {
            // get the sign of the step
            if (j == nSteps - 1) {
                s = Math.signum(zf - iVec.z) * Math.abs(z - zf);
            }
            
            //B bf = new B(i, z, x, y, tx, ty, s);
            //bfieldPoints.add(bf);
            teslaField(sector, x, y, z, bf);
           
            A(tx, ty, bf[0], bf[1], bf[2], A);
            delA_delt(tx, ty, bf[0], bf[1], bf[2], dA);

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

            
            //double transpStateJacobian00=1; 
            //double transpStateJacobian01=0; 
            double transpStateJacobian02=delx_deltx0; 
            double transpStateJacobian03=delx_delty0; 
            double transpStateJacobian04=delx_delQ;
            //double transpStateJacobian10=0; 
            //double transpStateJacobian11=1; 
            double transpStateJacobian12=dely_deltx0; 
            double transpStateJacobian13=dely_delty0; 
            double transpStateJacobian14=dely_delQ;
            //double transpStateJacobian20=0; 
            //double transpStateJacobian21=0; 
            //double transpStateJacobian22=1; 
            double transpStateJacobian23=deltx_delty0; 
            double transpStateJacobian24=deltx_delQ;
            //double transpStateJacobian30=0; 
            //double transpStateJacobian31=0; 
            double transpStateJacobian32=delty_deltx0; 
            //double transpStateJacobian33=1; 
            double transpStateJacobian34=delty_delQ;
            //double transpStateJacobian40=0; 
            //double transpStateJacobian41=0; 
            //double transpStateJacobian42=0; 
            //double transpStateJacobian43=0; 
            //double transpStateJacobian44=1;
            

            //covMat = FCF^T; u = FC;
            for (int j1 = 0; j1 < 5; j1++) {
                u[0][j1] = covMat.covMat.get(0,j1) + covMat.covMat.get(2,j1) * transpStateJacobian02 + covMat.covMat.get(3,j1)* transpStateJacobian03 + covMat.covMat.get(4,j1) * transpStateJacobian04;
                u[1][j1] = covMat.covMat.get(1,j1) + covMat.covMat.get(2,j1) * transpStateJacobian12 + covMat.covMat.get(3,j1) * transpStateJacobian13 + covMat.covMat.get(4,j1) * transpStateJacobian14;
                u[2][j1] = covMat.covMat.get(2,j1) + covMat.covMat.get(3,j1) * transpStateJacobian23 + covMat.covMat.get(4,j1) * transpStateJacobian24;
                u[3][j1] = covMat.covMat.get(2,j1) * transpStateJacobian32 + covMat.covMat.get(3,j1) + covMat.covMat.get(4,j1) * transpStateJacobian34;
                u[4][j1] = covMat.covMat.get(4,j1);
            }

            for (int i1 = 0; i1 < 5; i1++) {
                C[i1][0] = u[i1][0] + u[i1][2] * transpStateJacobian02 + u[i1][3] * transpStateJacobian03 + u[i1][4] * transpStateJacobian04;
                C[i1][1] = u[i1][1] + u[i1][2] * transpStateJacobian12 + u[i1][3] * transpStateJacobian13 + u[i1][4] * transpStateJacobian14;
                C[i1][2] = u[i1][2] + u[i1][3] * transpStateJacobian23 + u[i1][4] * transpStateJacobian24;
                C[i1][3] = u[i1][2] * transpStateJacobian32 + u[i1][3] + u[i1][4] * transpStateJacobian34;
                C[i1][4] = u[i1][4];
            }

            // Q  process noise matrix estimate	
            double p = Math.abs(1. / Q);
            
            
            
            double pz = p / Math.sqrt(1 + tx * tx + ty * ty);
            double px = tx * pz;
            double py = ty * pz;
            
            double t_ov_X0 = Math.signum(zf - iVec.z) * s / ARGONRADLEN; //path length in radiation length units = t/X0 [true path length/ X0] ; Ar radiation length = 14 cm

            //double mass = this.MassHypothesis(this.massHypo); // assume given mass hypothesis
            double mass = 0.000510998; // assume given mass hypothesis
            if (Q > 0) {
                mass = 0.938272029;
            }

            double beta = p / Math.sqrt(p * p + mass * mass); // use particle momentum
            double cosEntranceAngle = Math.abs((x * px + y * py + z * pz) / (Math.sqrt(x * x + y * y + z * z) * p));
            double pathLength = t_ov_X0 / cosEntranceAngle;

            double sctRMS = (0.0136 / (beta * p)) * Math.sqrt(pathLength) * (1 + 0.038 * Math.log(pathLength)); // Highland-Lynch-Dahl formula

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
            y +=dy;
            tx += Q * speedLight * A[0] * s;
            ty += Q * speedLight * A[1] * s;

            z += s;
            dPath+= Math.sqrt(dx*dx+dy*dy+s*s);
        }

        StateVec fVec = new StateVec(f);
        fVec.z = zf;
        fVec.x = x;
        fVec.y = y;
        fVec.tx = tx;
        fVec.ty = ty;
        fVec.Q = Q;
        fVec.B = Math.sqrt(bf[0]*bf[0]+bf[1]*bf[1]+bf[2]*bf[2]);
        fVec.deltaPath = dPath;
        //StateVec = fVec;
        trackTraj.put(f, fVec);

        //if(transpStateJacobian!=null) {
        //	F = new Matrix(transpStateJacobian); 
        //} 
        if (covMat.covMat != null) {
            CovMat fCov = new CovMat(f);
            fCov.covMat = covMat.covMat;
            //CovMat = fCov;
            trackCov.put(f, fCov);
            
            if (PRINT && DEBUG) {
            	System.out.println("Final State Vec:\n" +  fVec);
                printCovMatrix("\nFinal Covariance Matrix:", fCov);
                
                double dx = fVec.x - iVec.x;
                double dy = fVec.y - iVec.y;
                double dz = fVec.z - iVec.z;
                               
                System.out.println("dPath = " + dPath + "  Euclidean dist: " + Math.sqrt(dx*dx + dy*dy + dz*dz));
            }

        }
        
        if (PRINT && DEBUG) {
          	DEBUG = false;
        }
        
        return fVec;
    }
	
	/**
	 * This is the covariance matrix transport used by the Kalman Filter
	 * @param sector the sector [1..6]
	 * @param i
	 * @param f
	 * @param iVec
	 * @param covMat
	 * @param trackTraj
	 * @param trackCov
	 * @param A
	 * @param dA
	 * @return
	 * @throws SwimException
	 */
	public StateVec sectorTransport(int sector, int i, int f, final StateVec iVec, CovMat covMat, double zf, Map<Integer, StateVec> trackTraj, Map<Integer, CovMat> trackCov, double[] A,
			double[] dA) throws SwimException {
		
		if (iVec == null) {
			throw new SwimException("Null starting state vector.");
		}

		if (covMat == null) {
			throw new SwimException("Null starting covariance matrix.");
		}

		double[][] u = new double[5][5];
		double[][] C = new double[5][5];

		double absDZ = Math.abs(zf-iVec.z);
		double stepSize = Math.min(0.1, absDZ/10);
		double[] hdata = new double[3];

		double Q = iVec.Q;

		//hack
		if (iVec.pzSign < 0) {
			iVec.Q = -iVec.Q;
		}
		
		
		double p = iVec.getP();

		// mag field vector in kG
		float bvec[] = new float[3];

		
		Trajectory trajectory = sectorAdaptiveRK(sector, iVec, zf, stepSize, hdata);
		
		if (trajectory == null) {
			System.out.println("NULL SZ RESULT");
			(new Throwable()).printStackTrace();
		}
		
//		StateVec last = szr.last();
//		System.out.println("NEW SectTrans: " + last);
		
		StateVec v1 = iVec;
		StateVec v2 = null;
		double plen = 0;

		for (int indx = 1; indx < trajectory.size(); indx++) {
			v2 = trajectory.get(indx);
			
			double s = v2.z - v1.z;	
			
	        teslaField(sector, v1.x, v1.y, v1.z, bvec);
	           
			A(v1.tx, v1.ty, bvec[0], bvec[1], bvec[2], A);
			delA_delt(v1.tx, v1.ty, bvec[0], bvec[1], bvec[2], dA);
			//
			// // transport covMat
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
//signs of the p's cancel so don't worry about them
			double pz = p / Math.sqrt(1 + v1.tx * v1.tx + v1.ty * v1.ty);
			
			double px = v1.tx * pz;
			double py = v1.ty * pz;

			double t_ov_X0 = Math.signum(zf - iVec.z) * s / ARGONRADLEN; 
//			double t_ov_X0 =  s / ARGONRADLEN; 

			// double mass = this.MassHypothesis(this.massHypo); // assume
			// given mass hypothesis
			double mass = 0.000510998; // assume given mass hypothesis
			if (Q > 0) {
				mass = 0.938272029;
			}

			double beta = p / Math.sqrt(p * p + mass * mass); // use
																// particle
																// momentum
			double cosEntranceAngle = Math.abs((v1.x * px + v1.y * py + v1.z * pz)
					/ (Math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z) * p));
			double pathLength = t_ov_X0 / cosEntranceAngle;

			double sctRMS = (0.0136 / (beta * p)) * Math.sqrt(pathLength) * (1 + 0.038 * Math.log(pathLength)); // Highland-Lynch-Dahl
																												// formula

			double cov_txtx = (1 + v1.tx * v1.tx) * (1 + v1.tx * v1.tx + v1.ty * v1.ty) * sctRMS
					* sctRMS;
			double cov_tyty = (1 + v1.ty * v1.ty) * (1 + v1.tx * v1.tx + v1.ty * v1.ty) * sctRMS
					* sctRMS;
			double cov_txty = v1.tx * v1.ty * (1 + v1.tx * v1.tx + v1.ty * v1.ty) * sctRMS * sctRMS;

			if (s > 0) {
				C[2][2] += cov_txtx;
				C[2][3] += cov_txty;
				C[3][2] += cov_txty;
				C[3][3] += cov_tyty;
			}

			covMat.covMat = new Matrix(C);
			
			plen += v1.dR(v2);
			v2.deltaPath = plen;
			
			v2.B = FastMath.sqrt(bvec[0]*bvec[0] + bvec[1]*bvec[1] + bvec[2]*bvec[2]);
			trackTraj.put(f, v2);
			
	        if (covMat.covMat != null) {
	            CovMat fCov = new CovMat(f);
	            fCov.covMat = covMat.covMat;
	            //CovMat = fCov;
	            trackCov.put(f, fCov);
	            
	        }

			
			v1 = v2;
		}
		
		return v2;
	}

	/**
	 * Transport to a fixed z over short distances using RK adaptive stepsize
	 * 
	 * @param sector
	 *            the sector [1..6]
	 * @param iVec
	 *            the starting state vector
	 * @param covMat
	 *            the initial covariance matrix
	 * @param zf
	 *            the final z value (cm)
	 * @param stepSize
	 *            the initial step size (cm)
	 * @param relTolerance
	 *            the absolute tolerances on each state variable [x, y, tx, ty]
	 *            (q = const). So it is an array with four entries, like [1.0e-4
	 *            cm, 1.0e-4 cm, 1.0e-5, 1.0e05]
	 * @param hdata
	 *            An array with three elements. Upon return it will have the
	 *            min, average, and max step size (in that order).
	 * @param A
	 *            a vector of dimension 2 for use in transporting covariance
	 *            matrix
	 * @param dA
	 *            a vector of dimension 4 for use in transporting covariance
	 *            matrix
	 * @throws SwimException
	 */

	public int sectorTransport(int sector, final StateVec iVec,
			StateVec stop, CovMat covMat, final double zf, double stepSize, double hdata[], double[] A,
			double[] dA) throws SwimException {

		if (iVec == null) {
			throw new SwimException("Null starting state vector.");
		}

		if (covMat == null) {
			throw new SwimException("Null starting covariance matrix.");
		}

		double[][] u = new double[5][5];
		double[][] C = new double[5][5];

		// initialize
		stop.copy(iVec);
		stop.deltaPath = 0;
		
		
		double Q = iVec.Q;
		
		double p = iVec.getP();

		// mag field vector in kG
		float bvec[] = new float[3];

		// need to set the derivative
		((SectorSwimZDerivative) _deriv).set(sector, iVec.getCharge(), iVec.getP(), _probe);

		double yo[] = { iVec.x, iVec.y, iVec.tx, iVec.ty };

		// listens for each step
		IRkListener listener = new IRkListener() {

			@Override
			public void nextStep(double newZ, double[] newStateVec, double s) {

				if (zf < iVec.z) {
					s = -s;
				}
				
		        teslaField(sector,stop.x, stop.y, stop.z, bvec);
				A(stop.tx, stop.ty, bvec[0], bvec[1], bvec[2], A);
				delA_delt(stop.tx, stop.ty, bvec[0], bvec[1], bvec[2], dA);
				
				// // transport covMat
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
//signs of the p's cancel so don't worry about them
				double pz = p / Math.sqrt(1 + stop.tx * stop.tx + stop.ty * stop.ty);
				
				double px = stop.tx * pz;
				double py = stop.ty * pz;

				double t_ov_X0 = Math.signum(zf - iVec.z) * s / ARGONRADLEN; 
//				double t_ov_X0 =  s / ARGONRADLEN; 

				// double mass = this.MassHypothesis(this.massHypo); // assume
				// given mass hypothesis
				double mass = 0.000510998; // assume given mass hypothesis
				if (Q > 0) {
					mass = 0.938272029;
				}

				double beta = p / Math.sqrt(p * p + mass * mass); // use
																	// particle
																	// momentum
				double cosEntranceAngle = Math.abs((stop.x * px + stop.y * py + stop.z * pz)
						/ (Math.sqrt(stop.x * stop.x + stop.y * stop.y + stop.z * stop.z) * p));
				double pathLength = t_ov_X0 / cosEntranceAngle;

				double sctRMS = (0.0136 / (beta * p)) * Math.sqrt(pathLength) * (1 + 0.038 * Math.log(pathLength)); // Highland-Lynch-Dahl
																													// formula

				double cov_txtx = (1 + stop.tx * stop.tx) * (1 + stop.tx * stop.tx + stop.ty * stop.ty) * sctRMS
						* sctRMS;
				double cov_tyty = (1 + stop.ty * stop.ty) * (1 + stop.tx * stop.tx + stop.ty * stop.ty) * sctRMS
						* sctRMS;
				double cov_txty = stop.tx * stop.ty * (1 + stop.tx * stop.tx + stop.ty * stop.ty) * sctRMS * sctRMS;

				if (s > 0) {
					C[2][2] += cov_txtx;
					C[2][3] += cov_txty;
					C[3][2] += cov_txty;
					C[3][3] += cov_tyty;
				}

				covMat.covMat = new Matrix(C);
				// transport stateVec

				double dx = newStateVec[0] - stop.x;
				double dy = newStateVec[1] - stop.y;
				double dz = newZ - stop.z;
				double dS = Math.sqrt(dx * dx + dy * dy + dz * dz);
				stop.deltaPath += dS;
				stop.B = FastMath.sqrt(bvec[0]*bvec[0] + bvec[1]*bvec[1] + bvec[2]*bvec[2]);
				
				stop.x = newStateVec[0];
				stop.y = newStateVec[1];
				stop.tx = newStateVec[2];
				stop.ty = newStateVec[3];

				stop.z = newZ;
			} // end nextStep
		};

		int nStep = 0;
		try {
			nStep = _rkZ.adaptiveStepZoZf(yo, iVec.z, zf, stepSize, _deriv, _stopper, listener, _absoluteTolerance,
					hdata);
		} catch (RungeKuttaException e) {
			e.printStackTrace();
		}

		return nStep;
	}



	private void A(double tx, double ty, double Bx, double By, double Bz, double[] a) {

		double C = Math.sqrt(1 + tx * tx + ty * ty);
		a[0] = C * (ty * (tx * Bx + Bz) - (1 + tx * tx) * By);
		a[1] = C * (-tx * (ty * By + Bz) + (1 + ty * ty) * Bx);
	}

	private void delA_delt(double tx, double ty, double Bx, double By, double Bz, double[] dela_delt) {

		double C2 = 1 + tx * tx + ty * ty;
		double C = Math.sqrt(1 + tx * tx + ty * ty);
		double Ax = C * (ty * (tx * Bx + Bz) - (1 + tx * tx) * By);
		double Ay = C * (-tx * (ty * By + Bz) + (1 + ty * ty) * Bx);

		dela_delt[0] = tx * Ax / C2 + C * (ty * Bx - 2 * tx * By); // delAx_deltx
		dela_delt[1] = ty * Ax / C2 + C * (tx * Bx + Bz); // delAx_delty
		dela_delt[2] = tx * Ay / C2 + C * (-ty * By - Bz); // delAy_deltx
		dela_delt[3] = ty * Ay / C2 + C * (-tx * By + 2 * ty * Bx); // delAy_delty
	}

	public String massHypo = "electron";

	public double MassHypothesis(String H) {
		double piMass = 0.13957018;
		double KMass = 0.493677;
		double muMass = 0.105658369;
		double eMass = 0.000510998;
		double pMass = 0.938272029;
		double value = piMass; // default
		if (H.equals("proton"))
			value = pMass;
		if (H.equals("electron"))
			value = eMass;
		if (H.equals("pion"))
			value = piMass;
		if (H.equals("kaon"))
			value = KMass;
		if (H.equals("muon"))
			value = muMass;
		return value;
	}

	private static void transportTest() {
		System.out.println("Testing transport");
		MagneticFields.getInstance().initializeMagneticFields();
		MagneticFields.getInstance().setActiveField(FieldType.COMPOSITEROTATED);
		MagneticFields.getInstance().getTorus().setScaleFactor(-0.5);
		MagneticFields.getInstance().getSolenoid().setScaleFactor(0);
		System.out.println(MagneticFields.getInstance().getCurrentConfigurationMultiLine());

		int sector = 1;
		System.out.println("sector: " + sector);
		
		double zi = 528.8406160000001;
		double zf = 229.23648;
		int pzSign = (zf > zi) ? 1 : -1;
		
		// momentum and charge
		double p = 1.2252495;
		int q = -1;

		double Q = q/p;

		// the initial state vector
		StateVec iv = new StateVec(-119.8901385, 0.5410029 , zi, -0.0287247,
				-0.0083868, Q, pzSign);
				
		
		System.out.println("Initial State Vector TILTED): " + iv);
		
		System.out.println("p: " + p);

		// Do a simple swim

		StateVec fv = null;
		double hdata[] = new double[3];
		SwimZ swimZ = new SwimZ();

		try {

			Trajectory traj = swimZ.sectorAdaptiveRK(sector, iv, zf, 0.01, hdata);
			System.out.println("\n\nFinal Vec from Swimming  NP = "+ traj.size() + "\n");
		StateVec last = traj.last();
        SwimTest.printSummary("Last for swimZ", traj.size(), p, last, hdata);

			
			

			double array[][] = { { 5.26357439  , -1.64761026 , 0.04338227  , -0.00814181 , 0.01041586 },
					{ -1.64761026 , 304.13559440, -0.04366342 , 1.24484383  , 0.00310091 },
					{0.04338227  , -0.04366342 , 0.00091018  , -0.00020141 , 0.00011534 },
					{-0.00814181 , 1.24484383  , -0.00020141 , 0.00702420  , 0.00000639},
					{0.01041586  , 0.00310091  , 0.00011534  , 0.00000639  , 0.00132762} };

			Matrix m = new Matrix(array);
			CovMat covMat = new CovMat(10, m);
			printCovMatrix("\nInitial cov matrix", covMat);

			// OK let's try the real transport
			StateVec stop = new StateVec(0);

			double[] A = new double[2];
			double[] dA = new double[4];

			int nStep = swimZ.sectorTransport(sector, iv, stop, covMat, zf, 0.1, hdata, A, dA);
			System.out.println("TRANSPORT Final State Vector: " + stop);
			System.out.println("TRANSPORT Number of steps: " + nStep);
			printCovMatrix("final cov matrix", covMat);
			
			//the alt transport
			
//			public int sectorTransport(int sector, int i, int f, StateVec iVec, CovMat covMat, double zf, Map<Integer, StateVec> trackTraj,  
//			Map<Integer, CovMat> trackCov, double[] A, double[] dA) throws SwimZException {
			
			covMat = new CovMat(10, m);
			
			HashMap<Integer, StateVec> trajMap = new HashMap<>();
			HashMap<Integer, CovMat> matMap = new HashMap<>();

			swimZ.sectorTransport(sector, 0, 0, iv, covMat, zf, trajMap, matMap, A, dA);
		//	System.out.println("TRANSPORT 2 Number of steps: " + nStep);
			printCovMatrix("final cov matrix", covMat);


		} catch (SwimException e) {
			e.printStackTrace();
		}

	}


	public static void main(String arg[]) {
		transportTest();
	}

}
