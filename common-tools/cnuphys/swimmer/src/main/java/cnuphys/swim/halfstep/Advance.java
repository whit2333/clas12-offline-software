package cnuphys.swim.halfstep;

public class Advance {

	private static double HGROW = 1.25;
	private static double HSHRINK = 0.5;
	private static double TINY = 1.0e-8;
	
	/**
	 * Adaptive advance of an object over a range from zo to zf 
	 * @param object the object to advance
	 * @param zo the starting value of an independent variable
	 * @param zf the ending value of an independent variable (can be < zo)
	 * @param initialStep the initial step size (forced to be positive)
	 * @param tolerance the absolute error (positive)
	 * @param minStep the minimum step size (if the the step gets smaller than this we are in an error condition)
	 * @return the number of times an advance is called. There are three calls per step attempted. If
	 * this number is negative, it indicates an error condition where the minimum step size was reached 
	 */
	public static int advance(AdvancingObject object, double zo, double zf, double initialStep, double tolerance, double minStep) {
		
		//how many times do we try to advance. This is at least three times for each step
		int nComputes = 0;
		
		boolean done = false;
		int sign;
		
		//force positives. The sign variable will handle going "backwards"
		initialStep = Math.abs(initialStep);
		minStep = Math.abs(minStep);
		
		sign = (zf > zo) ? 1 : -1;

		double del = Math.abs(zf - zo);
		
		double h = Math.min(initialStep, del/2);
		h = Math.max(h, Math.abs(del/100.));
		h = sign*h;
		
		double z = zo;
		
		AdvancingObject half1 = object.copy();
		AdvancingObject half2 = object.copy();
		AdvancingObject full = object.copy();
		AdvancingObject current = object.copy();
		
		while (!done) {
			
//			System.out.println("h = " + h);
			
			double halfStep = h/2;
			half1.advance(z, halfStep);
			
			//start half2 where half1 ended
			half2.copyEndToStart(half1);
			half2.advance(z + halfStep, halfStep);
			
			//full step
			full.advance(z, h);
			
			//computed an advance three times
			nComputes += 3;
			
			//accept this step or not?
			double diff = full.difference(half2);
			
			boolean acceptStep = diff < tolerance;
			
			if (acceptStep) {
				//half2 should be more accurate than full

				z += h;
				
				//let the object know a substep was accepted
				object.acceptedSubstep(z);

				done = Math.abs(zf - z) < TINY;
				
				if (!done) {
					System.out.println("HGROW");
					h *= HGROW;
					double remaining = zf - z;
					if (Math.abs(remaining) < Math.abs(h)) {
						h = remaining;
					}
					
					//reset
					half1.copyEndToStart(half2);
					full.copyEndToStart(half2);
					current = full.copy();
				}
				else {
				  System.out.println("DONE");
				  object.copyEndToEnd(half2);
				}
			}
			else { //did not accept, so we shrink h
				
				//reset
				half1 = current.copy();
				half2 = current.copy();
				full = current.copy();
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
