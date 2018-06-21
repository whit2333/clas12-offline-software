package cnuphys.fastMCed.streaming;

import java.util.EventListener;

import org.jlab.clas.physics.PhysicsEvent;

public interface IStreamProcessor extends EventListener {

	/**
	 * A message about a change in the streaming
	 * @param reason the reason for the change
	 */
	public void streamingChange(StreamReason reason);
	
	/**
	 * A new event in the stream
	 * @param event the new event
	 * @return StreamingReason.CONTINUE (success) or StreamingReason.FLAG (problem)
	 */
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event);
	
	/**
	 * Check whether this processor is active (receives streamingPhysicsEvent message)
	 * @return <code>true</code> if the processor is active;
	 */
	public boolean isActive();
}
