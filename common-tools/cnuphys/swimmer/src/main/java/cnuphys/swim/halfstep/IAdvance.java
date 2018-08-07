package cnuphys.swim.halfstep;

public interface IAdvance {

	/**
	 * Advances from z to z+h. (h can be < 0)
	 * @param z the starting value
	 * @param h the complete step (can be < 0)
	 */
	public void advance(double z, double h);
	
	/**
	 * A substep has been accepted
	 * @param z the value of z for the substep
	 */
	public void acceptedSubstep(double z);
}
