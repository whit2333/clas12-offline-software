package cnuphys.rk4;

/**
 * Basic 4th order Runge Kutta advance
 * As written, this is NOT thread save.
 * @author heddle
 *
 */
public class UniformAdvance implements IAdvance {

	//this makes it not thread safe unless
	//the advance method is synchronized.
	private double[] k2;
	private double[] k3;
	private double[] k4;
	private double[] ytemp;

	/**
	 * Basic 4th order Runge Kutta Step
	 * @param t current value of independent variable
	 * @param u current value of the state variable
	 * @param k1 current value of the derivatives
	 * @param h the step
	 * @param deriv derivative computer
	 * @param yout the estimate of the state vector at t+h
	 *            the error vector which is filled in if the advancer knows how
	 *            to compute errors
	 */
	@Override
	public void advance(double t, double[] u, double[] k1, // derivatives
			double h, IDerivative deriv, double[] yout, double[] error) {
		int nDim = u.length;

		if (k2 == null) {
			k2 = new double[nDim];
			k3 = new double[nDim];
			k4 = new double[nDim];
			ytemp = new double[nDim];
		}

		double hh = h * 0.5; // half step
		double h6 = h / 6.0;

		// advance t to mid point
		double tmid = t + hh;

		// first step: initial derivs to midpoint
		// after this,
		for (int i = 0; i < nDim; i++) {
			ytemp[i] = u[i] + hh * k1[i];
		}
		deriv.derivative(tmid, ytemp, k2);

		// second step (like 1st, but use midpoint just computed derivatives
		// dyt)
		for (int i = 0; i < nDim; i++) {
			ytemp[i] = u[i] + hh * k2[i];
		}
		deriv.derivative(tmid, ytemp, k3);

		// third (full) step
		for (int i = 0; i < nDim; i++) {
			ytemp[i] = u[i] + h * k3[i];
		}
		deriv.derivative(t + h, ytemp, k4);

		for (int i = 0; i < nDim; i++) {
			yout[i] = u[i] + h6 * (k1[i] + +2.0 * k2[i] + 2 * k3[i] + k4[i]);
		}

	}

	@Override
	public boolean computesError() {
		return false;
	}
}
