package cnuphys.fastMCed.eventio;

import java.awt.Toolkit;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.event.EventListenerList;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.PhysicsEvent;
import org.jlab.geom.DetectorHit;
import org.jlab.geom.prim.Path3D;
import org.jlab.physics.io.LundReader;

import cnuphys.bCNU.magneticfield.swim.ISwimAll;
import cnuphys.bCNU.util.Environment;
import cnuphys.fastMCed.fastmc.AcceptanceManager;
import cnuphys.fastMCed.fastmc.NoiseData;
import cnuphys.fastMCed.fastmc.ParticleHits;
import cnuphys.fastMCed.frame.FastMCed;
import cnuphys.lund.LundFileSupport;
import cnuphys.lund.LundId;
import cnuphys.lund.LundSupport;
import cnuphys.swim.SwimTrajectory;
import cnuphys.swim.Swimming;
import geometry.GeometryManager;

/**
 * Manager class for the PhysicsEvent data provided by the FastMC engine
 * @author heddle
 *
 */
public class PhysicsEventManager {
	
	// the event number
	private int _eventNum = 0;
	
	// event count
	private int _eventCount;

	// current generated event
	private PhysicsEvent _currentEvent;

	private Vector<ParticleHits> _particleHits = new Vector<ParticleHits>();

	// Lund reader
	private LundReader _lundReader;

	// current lund file
	private File _currentFile;

	/** Last selected data file */
	private static String dataFilePath = Environment.getInstance().getCurrentWorkingDirectory() + "/../../../data";

	/** possible lund file extensions */
	public static String extensions[] = { "dat", "DAT", "lund" };

	// filter to look for lund files
	private static FileNameExtensionFilter _lundFileFilter = new FileNameExtensionFilter("Lund Event Files",
			extensions);

	// Unique lund ids in the event (if any)
	private Vector<LundId> _uniqueLundIds = new Vector<LundId>();

	//manager singleton
	private static PhysicsEventManager instance;
	
	// are we training?
	private boolean _training = false;
	
	// list of view listeners. There are three lists. Those in index 0 are
	// notified first. Then those in index 1. Finally those in index 2. The
	private EventListenerList _listeners[] = new EventListenerList[3];

	// someone who can swim all particles in the current event
	private ISwimAll _allSwimmer;
	
	//private constructor for manager
	private PhysicsEventManager() {
		_allSwimmer = new SwimAll();
	}
	
	/**
	 * Public access to the singleton
	 * @return
	 */
	public static PhysicsEventManager getInstance() {
		if (instance == null) {
			instance = new PhysicsEventManager();
		}
		return instance;
	}
	
	/**
	 * Accessor for the all swimmer
	 * @return the all swimmer
	 */
	public ISwimAll getAllSwimmer() {
		return _allSwimmer;
	}
	
	/**
	 * Get a collection of unique LundIds in the current event
	 * 
	 * @return a collection of unique LundIds
	 */
	public Vector<LundId> uniqueLundIds() {
		
		_uniqueLundIds.clear();
		

		if ((_currentEvent != null) && (_currentEvent.count() > 0)) {
			for (int index = 0; index < _currentEvent.count(); index++) {
				Particle particle = _currentEvent.getParticle(index);
				LundId lid = LundSupport.getInstance().get(particle.pid());
				_uniqueLundIds.remove(lid);
				_uniqueLundIds.add(lid);

			}
		}

		return _uniqueLundIds;
	}
	
	/**
	 * Check whether we are training
	 * 
	 * @return the training flag
	 */
	public boolean isTraining() {
		return _training;
	}

	/**
	 * Set whether we are training
	 * 
	 * @param training
	 *            the new training flag
	 */
	public void setTraining(boolean training) {
		_training = training;
	}
	

	/**
	 * Reload the current event
	 */
	public void reloadCurrentEvent() {
		if (_currentEvent != null) {
			parseEvent(_currentEvent);
		}
	}
	
	/**
	 * Get the next event
	 * @return <code>true</code> upon success
	 */
	public boolean nextEvent() {
		boolean gotOne = _lundReader.next();

		if (gotOne) {
			_currentEvent = _lundReader.getEvent();
			_eventNum++;
			parseEvent(_currentEvent);
		} else {
			_currentEvent = null;
			Toolkit.getDefaultToolkit().beep();
		}
		return gotOne;
	}

	/**
	 * Get the DC hit count for a given particle
	 * 
	 * @param lundid
	 *            the integer lund id
	 * @return the number of dc hits for tht particle
	 */
	public int particleDCHitCount(int lundid) {
		for (ParticleHits phits : _particleHits) {
			if (phits.lundId() == lundid) {
				return phits.DCHitCount();
			}
		}
		return 0;
	}
	

	// parse the event
	//this gets the event first.
	private void parseEvent(PhysicsEvent event) {
		_particleHits.clear();
		if ((event == null) || (event.count() < 1)) {
			return;
		}

		_allSwimmer.swimAll();

		// how many trajectories?
		List<SwimTrajectory> trajectories = Swimming.getMCTrajectories();
		
		
		int trajCount = (trajectories == null) ? 0 : trajectories.size();
		 System.err.println("NUMBER of FASTMC TRAJ: " + trajCount);

		// get DC hits for charged particles

		if (trajectories != null) {
			for (SwimTrajectory traj : trajectories) {
				if (traj.getLundId() != null) {
					Path3D path3D = GeometryManager.fromSwimTrajectory(traj);					
					_particleHits.add(new ParticleHits(traj.getLundId(), path3D));
				}
			}
		}

		// notify all listeners of the event
		AcceptanceManager.getInstance().testAccepted(event);
		notifyPhysicsListeners(event);
	}

	/**
	 * Get the hits for all particles
	 * 
	 * @return the detector hits
	 */
	public Vector<ParticleHits> getParticleHits() {
		return _particleHits;
	}

	/**
	 * Get the current Lund File
	 * 
	 * @return the current Lund file.
	 */
	public File getCurrentFile() {
		return _currentFile;
	}

	/**
	 * Get the current event number
	 * 
	 * @return the current event number
	 */
	public int getEventNumber() {
		return _eventNum;
	}
	
	/**
	 * Get the event count
	 * 
	 * @return the current event count
	 */
	public int getEventCount() {
		return _eventCount;
	}


	/**
	 * Set the default directory in which to look for event files.
	 * 
	 * @param defaultDataDir
	 *            default directory in which to look for event files
	 */
	public static void setDefaultDataDir(String defaultDataDir) {
		dataFilePath = defaultDataDir;
	}

	/**
	 * Get the current generated event
	 * 
	 * @return the current generated event
	 */
	public PhysicsEvent getCurrentEvent() {
		return _currentEvent;
	}

	/**
	 * Get the data needed to run the SNR analysis
	 * 
	 * @return the data for the snr analysis
	 */
	public NoiseData getNoiseData() {

		if ((_particleHits == null) || (_particleHits.isEmpty())) {
			return null;
		}

		// count DC hits
		int count = 0;
		for (ParticleHits phits : _particleHits) {
			count += phits.DCHitCount();
		}

		if (count == 0) {
			return null;
		}

		NoiseData nd = new NoiseData();
		nd.count = count;
		nd.sector = new byte[count];
		nd.superlayer = new byte[count];
		nd.layer = new byte[count];
		nd.wire = new short[count];
		int index = 0;
		for (ParticleHits phits : _particleHits) {
			List<DetectorHit> ldh = phits.getDCHits();
			if (ldh != null) {
				for (DetectorHit hit : ldh) {
					nd.sector[index] = (byte) (hit.getSectorId() + 1);
					nd.superlayer[index] = (byte) (hit.getSuperlayerId() + 1);
					nd.layer[index] = (byte) (hit.getLayerId() + 1);
					nd.wire[index] = (short) (hit.getComponentId() + 1);
					index++;
				}
			}
		}

		return nd;
	}
	
	/**
	 * Open a Lund File
	 * 
	 * @return the lund file
	 */
	public File openFile() {
		_currentFile = null;

		JFileChooser chooser = new JFileChooser(dataFilePath);
		chooser.setSelectedFile(null);
		chooser.setFileFilter(_lundFileFilter);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			openFile(chooser.getSelectedFile());
		}

		return _currentFile;
	}
	
	/**
	 * Open a lund file
	 * @param path the full path
	 */
	public void openFile(File file) {
		_currentFile = file;
		_lundReader = new LundReader();
		dataFilePath = _currentFile.getParent();
		
		_eventCount = LundFileSupport.getInstance().countEvents(file);
		System.err.println("Event count: " + _eventCount);


		_lundReader.addFile(_currentFile.getPath());
		_lundReader.open();
		
		PhysicsEventManager.getInstance().notifyEventListeners(_currentFile);
	}
	
	/**
	 * Determines whether any next event control should be enabled.
	 * 
	 * @return <code>true</code> if any next event control should be enabled.
	 */
	public boolean isNextOK() {
		return (_currentFile != null);
	}
	
		
	//notify listeners that we have opened a file
	public void notifyEventListeners(File file) {

		Swimming.clearAllTrajectories();

		for (int index = 0; index < 3; index++) {
			if (_listeners[index] != null) {
				// Guaranteed to return a non-null array
				Object[] listeners = _listeners[index].getListenerList();

				// This weird loop is the bullet proof way of notifying all
				// listeners.
				for (int i = listeners.length - 2; i >= 0; i -= 2) {
					if (listeners[i] == IPhysicsEventListener.class) {
						System.err.println("NOTIFYING [" +  listeners[i + 1].getClass().getName() + "]" );
						((IPhysicsEventListener) listeners[i + 1]).openedNewLundFile(file.getAbsolutePath());
					}
				}
			}
		}
		FastMCed.getFastMCed().fixTitle();
	}
	
	//notify the listeners
	private void notifyPhysicsListeners(PhysicsEvent event) {

		Swimming.setNotifyOn(false); //prevent refreshes
		Swimming.clearAllTrajectories();
		Swimming.setNotifyOn(true); //prevent refreshes
			
		_uniqueLundIds.clear();

		for (int index = 0; index < 3; index++) {
			if (_listeners[index] != null) {
				// Guaranteed to return a non-null array
				Object[] listeners = _listeners[index].getListenerList();


				// This weird loop is the bullet proof way of notifying all
				// listeners.
				for (int i = listeners.length - 2; i >= 0; i -= 2) {
					IPhysicsEventListener listener = (IPhysicsEventListener) listeners[i + 1];
					if (listeners[i] == IPhysicsEventListener.class) {
						listener.newPhysicsEvent(event);
					}
				}
			}
		} // index loop
	}
	
	/**
	 * Remove a IPhysicsEventListener. IPhysicsEventListener listeners listen for
	 * new physics events.
	 * 
	 * @param listener
	 *            the IPhysicsEventListener listener to remove.
	 */
	public void removePhysicsListener(IPhysicsEventListener listener) {

		if (listener == null) {
			return;
		}

		for (int i = 0; i < 3; i++) {
			if (_listeners[i] != null) {
				_listeners[i].remove(IPhysicsEventListener.class, listener);
			}
		}
	}

	/**
	 * Add a IPhysicsEventListener. IPhysicsEventListener listeners listen for new
	 * events.
	 * 
	 * @param listener
	 *            the IPhysicsEventListener listener to add.
	 * @param index
	 *            Determines gross notification order. Those in index 0 are
	 *            notified first. Then those in index 1. Finally those in index
	 *            2. The Data containers should be in index 0. The trajectory
	 *            and noise in index 1, and the regular views in index 2 (they
	 *            are notified last)
	 */
	public void addPhysicsListener(IPhysicsEventListener listener, int index) {

		if (listener == null) {
			return;
		}

		if (_listeners[index] == null) {
			_listeners[index] = new EventListenerList();
		}

		_listeners[index].add(IPhysicsEventListener.class, listener);
	}


}
