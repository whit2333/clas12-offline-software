package cnuphys.rk4;

/**
 * Integrators used by the S swimmer
 * @author heddle
 *
 */

import java.util.List;

/**
 * Static methods for Runge-Kutta 4 integration, including a constant stepsize
 * method and an adaptive stepsize method.
 * 
 * @author heddle
 * 
 */
public class RungeKuttaS {

	// for adaptive stepsize, this is how much h will grow
	private static final double HGROWTH = 1.5;
	
	// use a simple half-step advance
	private IAdvance _advancer = new HalfStepAdvance();


	//think in cm
	public static double DEFMINSTEPSIZE = 1.0e-3;
	public static double DEFMAXSTEPSIZE = 1;
	
	private double _minStepSize = DEFMINSTEPSIZE;
	private double _maxStepSize = DEFMAXSTEPSIZE;
	
	// the dimension is 6 [x, y, z, px/p, py/p, pz/p]
	private static int DIM = 6;
	
	private double u[] = new double[DIM];
	private double yt2[] = new double[DIM];
	private double du[] = new double[DIM];
	private double error[] = new double[DIM];

	/**
	 * Create a RungeKutta object that can be used for integration
	 */
	public RungeKuttaS() {
	}


	/**
	 * Integrator that uses the RungeKutta advance with a Half Step advancer and
	 * adaptive stepsize and a tolerance vector.
	 * 
	 * @param uo
	 *            initial values. Probably something like (xo, yo, zo, vxo, vyo,
	 *            vzo).
	 * @param so
	 *            the initial value of the independent variable (path length) usually 0
	 * @param h
	 *            the starting steps size
	 * @param s
	 *            a list of the values of s at each step
	 * @param u
	 *            a list of the values of the state vector at each step
	 * @param deriv
	 *            the derivative computer (interface). This is where the problem
	 *            specificity resides.
	 * @param stopper
	 *            if not <code>null</code> will be used to exit the integration
	 *            early because some condition has been reached.
	 * @param relTolerance
	 *            the error tolerance as fractional diffs. Note it is a vector,
	 *            the same dimension of the problem, i.e. 4
	 * @param hdata
	 *            if not null, should be double[3]. Upon return, hdata[0] is the
	 *            min stepsize used, hdata[1] is the average stepsize used, and
	 *            hdata[2] is the max stepsize used
	 * @return the number of steps used.
	 * @throws RungeKuttaException
	 */
	public int adaptiveStep(double uo[],
			double so,
			double h,
			final List<Double> s,
			final List<double[]> u,
			IDerivative deriv,
			IStopper stopper,
			double relTolerance[],
			double hdata[]) throws RungeKuttaException {
		

		// put starting step in
		s.add(so);
		u.add(copy(uo));

		//listen for each step
		IRkListener listener = new IRkListener() {

			@Override
			public void nextStep(double nextS, double nextU[], double h) {
				s.add(nextS);
				u.add(copy(nextU));
			}

		};
		return adaptiveStep(uo, so, h, deriv, stopper, listener, relTolerance, hdata);
	}


	/**
	 * Integrator that uses the RungeKutta advance with a Half Step advancer and
	 * adaptive stepsize and a tolerance vector.
	 * 
	 * This version uses an IRk4Listener to notify the listener that the next
	 * step has been advanced.
	 * 
	 * A very typical case is a 2nd order ODE converted to a 1st order where the
	 * dependent variables are x, y, z, vx, vy, vz and the independent variable
	 * is time.
	 * 
	 * @param yo
	 *            initial values. Probably something like (xo, yo, zo, vxo, vyo,
	 *            vzo).
	 * @param so
	 *            the initial value of the independent variable
	 * @param h
	 *            the starting steps size
	 * @param deriv
	 *            the derivative computer (interface). This is where the problem
	 *            specificity resides.
	 * @param stopper
	 *            if not <code>null</code> will be used to exit the integration
	 *            early because some condition has been reached.
	 * @param listener
	 *            listens for each step * @param tableau the Butcher Tableau
	 * @param relTolerance
	 *            the error tolerance as fractional diffs. Note it is a vector,
	 *            the same
	 * @param hdata
	 *            if not null, should be double[3]. Upon return, hdata[0] is the
	 *            min stepsize used, hdata[1] is the average stepsize used, and
	 *            hdata[2] is the max stepsize used
	 * @return the number of steps used.
	 * @throws RungeKuttaException
	 */
	public int adaptiveStep(double yo[],
			double so, double h, IDerivative deriv, IStopper stopper, IRkListener listener,
			double relTolerance[], double hdata[]) throws RungeKuttaException {

		int nStep = 0;
		try {
			nStep = driver(yo, so, h, deriv, stopper, listener, _advancer, relTolerance, hdata);
		} catch (RungeKuttaException e) {
//			System.err.println("Trying to integrate from " + to + " to " + tf);
			throw e;
		}
		return nStep;
	}
	

	/**
	 * Driver that uses the RungeKutta advance with an adaptive step size
	 * 
	 * This version uses an IRk4Listener to notify the listener that the next
	 * step has been advanced.
	 * 
	 * @param yo
	 *            initial values. (xo, yo, zo, px0/p, py0/p, pz0/p).
	 * @param so
	 *            the initial value of the independent variable
	 * @param h
	 *            the step size
	 * @param deriv
	 *            the derivative computer (interface). This is where the problem
	 *            specificity resides.
	 * @param stopper
	 *            if not <code>null</code> will be used to exit the integration
	 *            early because some condition has been reached.
	 * @param listener
	 *            listens for each step
	 * @param advancer
	 *            takes the next step
	 * @param absError
	 *            the absolute tolerance for eact of the state variables. Note
	 *            it is a vector, the same
	 * @param hdata
	 *            if not null, should be double[3]. Upon return, hdata[0] is the
	 *            min stepsize used, hdata[1] is the average stepsize used, and
	 *            hdata[2] is the max stepsize used
	 * @return the number of steps used.
	 * @throw(new RungeKuttaException("Step size too small in Runge Kutta
	 *            driver" ));
	 */
	private int driver(double yo[],
			double so,
			double h,
			IDerivative deriv,
			IStopper stopper,
			IRkListener listener,
			IAdvance advancer,
			double absError[],
			double hdata[]) throws RungeKuttaException {
		
		// if our advancer does not compute error we can't use adaptive stepsize
		if (!advancer.computesError()) {
			return 0;
		}

		// capture stepsize data?
		if (hdata != null) {
			hdata[0] = h;
			hdata[1] = h;
			hdata[2] = h;
		}


		double s = so;
		for (int i = 0; i < DIM; i++) {
			u[i] = yo[i];
		}

		int nstep = 0;
		boolean keepGoing = true;

		while (keepGoing) {
			// use derivs at previous t
			deriv.derivative(s, u, du);

			advancer.advance(s, u, du, h, deriv, yt2, error);
			
	//		System.out.println("h = " + h);

			boolean decreaseStep = false;
			if (keepGoing) {
				
				if (stopper != null) {
					boolean stop = stopper.stopIntegration(s+h, yt2);
					if (stop) {
						boolean terminate = stopper.terminateIntegration(s, u);
						decreaseStep = stop && !terminate;
					}
				}

				if (!decreaseStep) {
					for (int i = 0; i < DIM; i++) {
						decreaseStep = error[i] > absError[i];
						if (decreaseStep) {
							break;
						}
					}
				}
			}

			if (decreaseStep) {
				h = h / 2;
				if (h < _minStepSize) {
					keepGoing = false;
				}
			}
			else { // accepted this step

				if (hdata != null) {
					hdata[0] = Math.min(hdata[0], h);
					hdata[1] += h;
					hdata[2] = Math.max(hdata[2], h);
				}

				for (int i = 0; i < DIM; i++) {
					u[i] = yt2[i];
				}

				s += h;
				
				nstep++;
				
				// premature termination? Skip if stopper is null.
				if (stopper != null) {
					stopper.setFinalT(s);
					if (stopper.stopIntegration(s, u)) {
						// someone listening?
						if (listener != null) {
							listener.nextStep(s, u, h);
						}

						if ((hdata != null) && (nstep > 0)) {
							hdata[1] = hdata[1] / nstep;
						}
						return nstep; // actual number of steps taken
					}
				}


				// someone listening?
				if (listener != null) {
					listener.nextStep(s, u, h);
				}

				h *= HGROWTH;
				h = Math.min(h, _maxStepSize);

			} // accepted this step max error < tolerance
		} // while (keepgoing)

		// System.err.println("EXCEEDED MAX PATH: pl = " + t + " MAX: " + tf);

		if ((hdata != null) && (nstep > 0)) {
			hdata[1] = hdata[1] / nstep;
		}
		return nstep;
	}



	// copy a vector
	private double[] copy(double v[]) {
		double w[] = new double[DIM];
		System.arraycopy(v, 0, w, 0, DIM);
		return w;
	}


	/**
	 * Set the maximum step size
	 * 
	 * @param maxSS
	 *            the maximum stepsize is whatever units you are using
	 */
	public void setMaxStepSize(double maxSS) {
		_maxStepSize = maxSS;
	}

	/**
	 * Set the minimum step size
	 * 
	 * @param maxSS
	 *            the minimum stepsize is whatever units you are using
	 */
	public void setMinStepSize(double minSS) {
		_minStepSize = minSS;
	}

	/**
	 * Get the maximum step size
	 * 
	 * @return the maximum stepsize is whatever units you are using
	 */
	public double getMaxStepSize() {
		return _maxStepSize;
	}
	
	/**
	 * Get the minimum step size
	 * 
	 * @return the minimum stepsize is whatever units you are using
	 */
	public double getMinStepSize() {
		return _minStepSize;
	}


}