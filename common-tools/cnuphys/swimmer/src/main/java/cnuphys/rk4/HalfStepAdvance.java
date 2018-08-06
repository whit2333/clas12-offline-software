package cnuphys.rk4;

public class HalfStepAdvance implements IAdvance {

	//a uniform advancer
	private UniformAdvance uniAdvance;
	
	public double[] yfull;

	public HalfStepAdvance() {
		// get a uniform advancer
		uniAdvance = new UniformAdvance();
	}

	@Override
	public void advance(double t,
			double[] y,
			double[] dydt,
			double h,
			IDerivative deriv,
			double[] yout,
			double[] error) {

		// advance the full step
		int ndim = y.length;
		
		if (yfull == null) {
			yfull = new double[ndim];
		}
		
		uniAdvance.advance(t, y, dydt, h, deriv, yfull, null);

		// advance two half steps
		double h2 = h / 2;
		double tmid = t + h2;

		uniAdvance.advance(t, y, dydt, h2, deriv, yout, null);
		deriv.derivative(tmid, yout, dydt);
		uniAdvance.advance(tmid, yout, dydt, h2, deriv, yout, null);

		// compute absolute errors
		for (int i = 0; i < ndim; i++) {
			error[i] = Math.abs(yfull[i] - yout[i]);
		}
	}

	@Override
	public boolean computesError() {
		return true;
	}

}