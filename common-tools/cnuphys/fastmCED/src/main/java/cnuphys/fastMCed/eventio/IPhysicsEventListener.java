package cnuphys.fastMCed.eventio;

import java.util.EventListener;
import java.util.List;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.fastMCed.fastmc.ParticleHits;


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
	 * @param event the FastMC generated physics event
	 * @param particleHits a list (each entry corresponding to a particle and its trajectory) of particle hits which can
	 * be queried  for hits in a given detector.
	 * @see cnuphys.fastMCed.streaming.IStreamProcessor
	 */
	public void newPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits);

}
