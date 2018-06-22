package cnuphys.fastMCed.eventio;

import java.util.EventListener;

import org.jlab.clas.physics.PhysicsEvent;


/**
 * Interface used to listen for FASTMC event messages and updates
 * @author heddle
 *
 */
public interface IPhysicsEventListener extends EventListener {
	
	/**
	 * A new Lund file has been opened
	 * @param path the full path to the file
	 */
	public void openedNewLundFile(final String path);
		
	/**
	 * New event has arrived from the FastMC engine via the "next event" mechanism.
	 * Note that in streaming mode, do not get broadcast this way, they
	 * are broadcasted via streamingPhysicsEvent
	 * @param event the generated physics event
	 * @see cnuphys.fastMCed.streaming.IStreamProcessor
	 */
	public void newPhysicsEvent(PhysicsEvent event);

}
