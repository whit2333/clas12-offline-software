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
	 * Opened a new lund formatted file
	 * 
	 * @param path
	 *            the path to the new file
	 */
	public void openedNewLundFile(final String path);
		
	/**
	 * New fast mc event
	 * @param event the generated physics event
	 */
	public void newPhysicsEvent(PhysicsEvent event);

}
