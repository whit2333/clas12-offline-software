package cnuphys.rk4;

public class UniformAdvance implements IAdvance {

	private double[] k2;
	private double[] k3;
	private double[] k4;
	private double[] ytemp;

	@Override
	public void advance(double t, double[] y, double[] k1, // derivatives
			double h, IDerivative deriv, double[] yout, double[] error) {
		int nDim = y.length;

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
			ytemp[i] = y[i] + hh * k1[i];
		}
		deriv.derivative(tmid, ytemp, k2);

		// second step (like 1st, but use midpoint just computed derivatives
		// dyt)
		for (int i = 0; i < nDim; i++) {
			ytemp[i] = y[i] + hh * k2[i];
		}
		deriv.derivative(tmid, ytemp, k3);

		// third (full) step
		for (int i = 0; i < nDim; i++) {
			ytemp[i] = y[i] + h * k3[i];
		}
		deriv.derivative(t + h, ytemp, k4);

		for (int i = 0; i < nDim; i++) {
			yout[i] = y[i] + h6 * (k1[i] + +2.0 * k2[i] + 2 * k3[i] + k4[i]);
		}

	}

	@Override
	public boolean computesError() {
		return false;
	}
}
