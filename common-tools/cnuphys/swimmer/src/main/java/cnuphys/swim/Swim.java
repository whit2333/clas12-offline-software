package cnuphys.swim;

import Jama.Matrix;
import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.IMagField;
import cnuphys.magfield.MagneticField;
import cnuphys.magfield.RotatedCompositeProbe;

/**
 * A base class for both SwimZ and Swim S
 * @author heddle
 *
 */
public abstract class Swim {
	
	/** Argon radiation length in cm */
	public static final double ARGONRADLEN = 14.;

	
	/** The speed of light in these units: (GeV/c)(1/kG)(1/cm) */
	public static final double speedLight = 2.99792458e-04;
	
	// Min momentum to swim in GeV/c
	public static final double MINMOMENTUM = 1.0e-3;

	/**
	 * In swimming routines that require a tolerance vector, this is a
	 * reasonable one to use for CLAS. These represent absolute errors in the
	 * adaptive stepsize algorithms
	 */
	protected double _eps = 1.0e-3;

	// absolute tolerances
	protected double _absoluteTolerance[] = new double[6];
	
	/** The current magnetic field probe */
	protected FieldProbe _probe;
	
	/**
	 * Null constructor. Here we create a Swimmer that will use the current active
	 * magnetic field.
	 * 
	 * @param field
	 *            interface into a magnetic field
	 */
	public Swim() {
		_probe = FieldProbe.factory();
		initialize();
	}
	
	/**
	 * Create a swimmer specific to a magnetic field probe
	 * 
	 * @param probe
	 *            the magnetic field probe
	 */
	public Swim(FieldProbe probe) {
		_probe = probe;
		initialize();
	}


	/**
	 * Create a swimmer specific to a magnetic field
	 * 
	 * @param magneticField
	 *            the magnetic field
	 */
	public Swim(MagneticField magneticField) {
		_probe = FieldProbe.factory(magneticField);
		initialize();
	}

	/**
	 * Create a swimmer specific to a magnetic field
	 * 
	 * @param magneticField
	 *            the magnetic field
	 */
	public Swim(IMagField magneticField) {
		_probe = FieldProbe.factory(magneticField);
		initialize();
	}
	
	/**
	 * Get the underlying field probe
	 * 
	 * @return the probe
	 */
	public FieldProbe getProbe() {
		return _probe;
	}
	

	// some initialization
	protected abstract void initialize();

	/**
	 * Set the tolerance used by the CLAS_Tolerance array
	 * 
	 * @param eps
	 *            the baseline absolute tolerance.
	 */
	public abstract void setAbsoluteTolerance(double eps);

	/**
	 * Get the tolerance used by the CLAS_Toleance array
	 * 
	 * @return the tolerance used by the CLAS_Toleance array
	 */
	public double getEps() {
		return _eps;
	}


	/**
	 * Print out a covariance matrix with a prefix message
	 * @param s the message
	 * @param covMat the covariance matrix
	 */
	public static void printCovMatrix(String s, CovMat covMat) {
		System.out.println(s);
		System.out.print(covMat.toString());
	}

	/**
	 * Print out a  matrix with a prefix message
	 * @param s the message
	 * @param m the  matrix
	 */
	public static void printMatrix(String s, Matrix m) {
		System.out.println();
		System.out.print(s);
		for (int i = 0; i < 5; i++) {
			System.out.println();
			for (int j = 0; j < 5; j++) {
				System.out.print(String.format("%-12.8f ", m.get(i, j)));
			}
		}

		System.out.println();

	}	
	
	// compute the field in Tesla
	public static void teslaField(RotatedCompositeProbe probe, int sector, double x, double y, double z,
			float[] result) {
		probe.field(sector, (float) x, (float) y, (float) z, result);

		// to tesla from kG
		for (int i = 0; i < 3; i++) {
			result[i] /= 10;
		}
	}


}
