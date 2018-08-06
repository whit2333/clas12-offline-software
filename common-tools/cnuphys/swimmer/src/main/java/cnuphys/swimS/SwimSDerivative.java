package cnuphys.swimS;

import cnuphys.magfield.FieldProbe;
import cnuphys.rk4.IDerivative;

public class SwimSDerivative implements IDerivative {

	//magnetic field probe
	protected FieldProbe _probe;

	protected double _momentum;  //GeV/c

	// alpha is qC/p where q is the integer charge,
	// C is the speed of light in (GeV/c)(1/kG)(1/cm)
	// p is in GeV/c
	protected double _alpha;
	
	//for mag field result
	protected float b[] = new float[3];
	
	/**
	 * Null constructor
	 */
	public SwimSDerivative() {
		set(0, Double.NaN, null);
	}

	/**
	 * The derivative for swimming through a magnetic field with the pathlength
	 * s as the independent variable
	 * 
	 * @param charge
	 *            -1 for electron, +1 for proton, etc.
	 * @param momentum
	 *            the magnitude of the momentum.
	 * @param field
	 *            the magnetic field
	 */
	public SwimSDerivative(int charge, double momentum, FieldProbe probe) {
		set(charge, momentum, probe);
	}
	
	
	/**
	 * Set the parameters of this derivative
	 * @param charge the integer charge
	 * @param momentum the momentum in GeV/c
	 * @param probe the magnetic field probe
	 */
	public void set(int charge, double momentum, FieldProbe probe) {
		_probe = probe;
		_momentum = momentum;
		
//units of this  alpha are 1/(kG*cm)
		_alpha = charge * SwimS.speedLight / _momentum;		
	}

	/**
	 * Compute the derivatives given the value of s (path length) and the values
	 * of the state vector.
	 * 
	 * @param s
	 *            the value of the independent variable path length (input).
	 * @param u
	 *            the values of the state vector ([x,y,z, px/p, py/p, pz/p]) at
	 *            s (input).
	 * @param du
	 *            will be filled with the values of the derivatives wrt s at s
	 *            (output).
	 */
	@Override
	public void derivative(double s, double[] u, double[] du) {
		double Bx = 0.0;
		double By = 0.0;
		double Bz = 0.0;

		if (_probe != null) {
			float b[] = new float[3];
			_probe.field((float) u[0], (float) u[1], (float) u[2], b);
			Bx = b[0];
			By = b[1];
			Bz = b[2];
		}
		
		du[0] = u[3];
		du[1] = u[4];
		du[2] = u[5];
		du[3] = _alpha * (u[4] * Bz - u[5] * By); // vyBz-vzBy
		du[4] = _alpha * (u[5] * Bx - u[3] * Bz); // vzBx-vxBz
		du[5] = _alpha * (u[3] * By - u[4] * Bx); // vxBy-vyBx
	}

}