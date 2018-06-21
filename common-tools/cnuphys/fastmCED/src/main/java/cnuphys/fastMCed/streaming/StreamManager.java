package cnuphys.fastMCed.streaming;

import javax.swing.event.EventListenerList;

import org.jlab.clas.physics.PhysicsEvent;

public class StreamManager {
	
	private static StreamManager instance;
	
	// list of listeners. 
	private EventListenerList _listeners = new EventListenerList();


	//private singleton constructor
	private StreamManager() {}
	
	public static StreamManager getInstance() {
		if (instance == null) {
			instance = new StreamManager();
		}
		return instance;
	}
	
	/**
	 * Remove a IStreamProcessor. IStreamProcessor listeners listen for
	 * new physics events.
	 * 
	 * @param listener
	 *            the IStreamProcessor listener to remove.
	 */
	public void removeStreamListener(IStreamProcessor listener) {

		if (listener == null) {
			return;
		}

		_listeners.remove(IStreamProcessor.class, listener);
	}
	
	/**
	 * Notify the stream listeners of a physics event
	 * @param event the event
	 */
	public void notifyStreamListeners(PhysicsEvent event) {
		
		// Guaranteed to return a non-null array
		Object[] listeners = _listeners.getListenerList();


		// This weird loop is the bullet proof way of notifying all
		// listeners.
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			IStreamProcessor listener = (IStreamProcessor) listeners[i + 1];
			if (listener.isActive()) {
				StreamProcessStatus status = listener.streamingPhysicsEvent(event);
				
				if (status != StreamProcessStatus.CONTINUE) {
					//TODO handle this!
				}
			}
		}
	}
	
	/**
	 * Notify the stream listeners of a change
	 * @param reason the reason for the change
	 */
	public void notifyStreamListeners(StreamReason reason) {
		
		// Guaranteed to return a non-null array
		Object[] listeners = _listeners.getListenerList();


		// This weird loop is the bullet proof way of notifying all
		// listeners.
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			IStreamProcessor listener = (IStreamProcessor) listeners[i + 1];
			listener.streamingChange(reason);
		}
	}



	/**
	 * Add a IStreamProcessor. IStreamProcessor listeners listen for new
	 * events.
	 * 
	 * @param listener
	 *            the IStreamProcessor listener to add.
	 * @param index
	 *            Determines gross notification order. Those in index 0 are
	 *            notified first. Then those in index 1. Finally those in index
	 *            2. The Data containers should be in index 0. The trajectory
	 *            and noise in index 1, and the regular views in index 2 (they
	 *            are notified last)
	 */
	public void addStreamListener(IStreamProcessor listener) {

		if (listener == null) {
			return;
		}

		_listeners.add(IStreamProcessor.class, listener);
	}

}
