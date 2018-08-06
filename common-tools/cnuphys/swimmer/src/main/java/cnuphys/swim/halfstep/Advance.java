package cnuphys.swim.halfstep;

public class Advance {

	private static double HGROW = 1.25;
	private static double HSHRINK = 0.5;
	private static double TINY = 1.0e-8;
	
	/**
	 * Adaptive advance of an object over a range
	 * @param object the object to advance
	 * @param to the starting value of an independent variable
	 * @param tf the ending value of an independent variable (can be < to)
	 * @param initialStep the initial step size (positive)
	 * @param absoluteError the absolute error (positive)
	 * @param minStep the minimum step size (if the the step gets smaller than this we are in an error condition)
	 * @return the number of times an advance is called. There are three calls per step attempted. If
	 * this number is negative, it indicates an error condition where the minimum step size was reached 
	 */
	public static int advance(AAdvancingObject object, double to, double tf, double initialStep, double absoluteError, double minStep) {
		
		int nComputes = 0;
		boolean done = false;
		int sign;
		
		initialStep = Math.abs(initialStep);
		minStep = Math.abs(minStep);
		
		sign = (tf > to) ? 1 : -1;

		double del = Math.abs(tf - to);
		
		double h = Math.min(initialStep, del/2);
		h = Math.max(h, Math.abs(del/100.));
		h = sign*h;
		
		double t = to;
		
		AAdvancingObject half1 = object.copy();
		AAdvancingObject half2 = object.copy();
		AAdvancingObject full = object.copy();
		
		while (!done) {
			
			System.out.println("h = " + h);
			
			double halfStep = h/2;
			half1.advance(t, halfStep);
			
			half2.copyFrom(half1);
			half2.advance(t + halfStep, halfStep);
			
			//full step
			full.advance(t, h);
			
			//computed an advance three times
			nComputes += 3;
			
			//accept this step or not?
			double diff = full.difference(half2);
			
			boolean acceptStep = diff < absoluteError;
			
			if (acceptStep) {
				//copy2 should be more accurate than full
				object.copyFrom(half2);
				t += h;
				done = Math.abs(tf - t) < TINY;
				
				if (!done) {
					System.out.println("HGROW");
					h *= HGROW;
					double remaining = tf - t;
					if (Math.abs(remaining) < Math.abs(h)) {
						h = remaining;
					}
					
					//reset
					half1.copyFrom(half2);
					full.copyFrom(half2);
				}
			}
			else { //dif not accept
				half1.copyFrom(object);
				half2.copyFrom(object);
				full.copyFrom(object);
				System.out.println("HSHRINK");

				h *= HSHRINK;
				if (Math.abs(h) < minStep) {
					return -nComputes;
				}
			}
			
		}
		
		return nComputes;
	}
}
