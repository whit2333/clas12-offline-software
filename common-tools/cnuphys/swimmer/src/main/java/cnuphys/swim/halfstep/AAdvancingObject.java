package cnuphys.swim.halfstep;

public abstract class AAdvancingObject<T> implements IAdvance {
	
	
	public abstract double difference(AAdvancingObject<T> aobj);
	
	public abstract AAdvancingObject<T> copy();
	
	public abstract void copyFrom(AAdvancingObject<T> source);


}
