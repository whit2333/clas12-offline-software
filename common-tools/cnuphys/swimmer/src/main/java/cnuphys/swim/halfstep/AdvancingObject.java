package cnuphys.swim.halfstep;

public abstract class AdvancingObject<T> implements IAdvance {
	
	/**
	 * Compute the difference, which will be compared to the tolerance,
	 * of another object and this. How the difference is calculated is
	 * not specified.
	 * @param aobj the other object
	 * @return the difference, which can be compared to a provided tolerance.
	 */
	public abstract double difference(AdvancingObject<T> aobj);
	
	/**
	 * Make a copy of the Advancing object
	 * @return a copy of the advancing object
	 */
	public abstract AdvancingObject<T> copy();
	
	/**
	 * Copy the data from a source object
	 * @param source the source object
	 */
	public abstract void copyFrom(AdvancingObject<T> source);
	
	/**
	 * Set stating conditions in this object based on
	 * the ending conditions of another object
	 * @param source the other object
	 */
	public abstract void copyEndToStart(AdvancingObject<T> source);
	
	/**
	 * Set ending conditions in this object based on
	 * the ending conditions of another object
	 * @param source the other object
	 */
	public abstract void copyEndToEnd(AdvancingObject<T> source);


	/**
	 * Set starting conditions in this object based on
	 * the starting conditions of another object
	 * @param source the other object
	 */
	public abstract void copyStartToStart(AdvancingObject<T> source);

}
