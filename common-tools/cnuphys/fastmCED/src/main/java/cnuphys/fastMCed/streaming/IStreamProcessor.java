package cnuphys.fastMCed.streaming;

import java.util.EventListener;

import org.jlab.clas.physics.PhysicsEvent;

public interface IStreamProcessor extends EventListener {

	/**
	 * A message about a change in the streaming state.
	 * @param reason the reason for the change. It will be
	 * one of the self-explanatory values of the StreamReason
	 * class:<br>
	 * STARTED, STOPPED, PAUSED, FINISHED, RESUMED
	 */
	public void streamingChange(StreamReason reason);
	
	/**
	 * A new event in the stream. This occures when FastMCed is  not
	 * looking event by event, but when it is quickly streaming through
	 * a large number of events. NOTE: this is NOT on a separate thread.It
	 * will in fact be on the GUI thread. This is by design.
	 * @param event the new event arriving through the FastMCed streaming mechanism.
	 * @return StreamingReason.CONTINUE (success) or StreamingReason.FLAG (problem). 
	 * Any consumer returning StreamingReason.FLAG will halt the process
	 * and cause the GUI to display the event that caused the StreamingReason.FLAG. 
	 * The normal return (nothing interesting) is StreamingReason.CONTINUE
	 */
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event);
	
	
	/**
	 * If the processor flags an event, it will be asked
	 * to return the reason. 
	 * @return the reason an event was flagged.
	 */
	public String flagExplanation();

	
}
