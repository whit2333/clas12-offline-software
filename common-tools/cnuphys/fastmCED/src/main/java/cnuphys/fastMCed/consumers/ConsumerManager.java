package cnuphys.fastMCed.consumers;

import java.io.File;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.util.Environment;
import cnuphys.bCNU.util.PluginLoader;
import cnuphys.fastMCed.eventio.IPhysicsEventListener;
import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.streaming.IStreamProcessor;
import cnuphys.fastMCed.streaming.StreamManager;
import cnuphys.fastMCed.streaming.StreamProcessStatus;
import cnuphys.fastMCed.streaming.StreamReason;

/**
 * Managers consumers
 * @author heddle
 *
 */
public class ConsumerManager extends Vector<PhysicsEventConsumer> implements IPhysicsEventListener, IStreamProcessor  {
	
	//map consumers to menu tems
	private Hashtable<PhysicsEventConsumer, JCheckBoxMenuItem> hash =  new Hashtable<>();
	
	//where the PhysicsEventConsumer plugins are found
	private File _consumerDir;

	//singleton
	private static ConsumerManager instance;
	
	// the base class forconsumer plugins
	protected Class<PhysicsEventConsumer> _consumerClaz;

	//the menu
	private JMenu _menu;
	
	//why an event was flagged
	private String _flagExplanation;

	//private singleton constructor
	private ConsumerManager() {
		String cwd = Environment.getInstance().getCurrentWorkingDirectory();
		
		_consumerDir = new File(cwd, "consumers");
		
		if (_consumerDir.exists() && (_consumerDir.isDirectory())) {
			System.err.println("Found Consumers Directory");
		}
		else {
			System.err.println("Did not find Consumers Directory");
			_consumerDir = null;
			return;
		}
		
		try {
			_consumerClaz = (Class<PhysicsEventConsumer>) Class.forName("cnuphys.fastMCed.consumers.PhysicsEventConsumer");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.err.println("Found PhysicsEventConsumer class");
		
		//let's try to load

		String classPath = _consumerDir.getPath();
		
		//TODO append other class paths from command args
		PluginLoader loader = new PluginLoader(classPath, _consumerClaz, null);
		
		List<Object> objs = loader.load();
		
		if (objs != null) {
			System.err.println("Found: " + objs.size() + " consumers.");
			for (Object obj : objs) {
				add((PhysicsEventConsumer)obj);
			}
		}
		
		PhysicsEventManager.getInstance().addPhysicsListener(this, 1);
		StreamManager.getInstance().addStreamListener(this);
	}
	
	/**
	 * Access for the singleton
	 * @return the singleton ConsumerManager
	 */
	public static ConsumerManager getInstance() {
		if (instance == null) {
			instance = new ConsumerManager();
		}
		return instance;
	}
	
	private JCheckBoxMenuItem createMenuItem(PhysicsEventConsumer consumer, boolean selected) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(consumer.getConsumerName(), selected);
		hash.put(consumer, item);
		return item;
	}
	
	/**
	 * Get the consumer menu
	 * @return the consumer menu
	 */
	public JMenu getMenu() {
		if (_menu == null) {
			_menu = new JMenu("Consumers");
			
			for (PhysicsEventConsumer consumer : this) {
				_menu.add(createMenuItem(consumer, true));
			}
		}
		
		
		return _menu;
	}
	
	private boolean isActive(PhysicsEventConsumer consumer) {
		if (consumer == null) {
			System.err.println("null consumer in ConsumerManager.isActive()");
			return false;
		}
		
		JCheckBoxMenuItem item = hash.get(consumer);
		
		if (item == null) {
			System.err.println("null item in ConsumerManager.isActive()");
			return false;
		}
		
		return item.isSelected();
	}

	@Override
	public void streamingChange(StreamReason reason) {
		for (PhysicsEventConsumer consumer : this) {
			consumer.streamingChange(reason);
		}		
	}

	@Override
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event) {
		for (PhysicsEventConsumer consumer : this) {
			if (isActive(consumer)) {
				StreamProcessStatus status = consumer.streamingPhysicsEvent(event);
				if (status == StreamProcessStatus.FLAG) {
					System.err.println("FLAGGED");
					_flagExplanation = consumer.flagExplanation() + 
							"\nConsumer: " + consumer.getConsumerName();
					return StreamProcessStatus.FLAG;
				}
			}
		}
		
		return StreamProcessStatus.CONTINUE;
	}
	
	@Override
	public String flagExplanation() {
		return _flagExplanation;
	}


	@Override
	public void openedNewLundFile(String path) {
		for (PhysicsEventConsumer consumer : this) {
			consumer.openedNewLundFile(path);
		}		
	}

	@Override
	public void newPhysicsEvent(PhysicsEvent event) {
		for (PhysicsEventConsumer consumer : this) {
			consumer.newPhysicsEvent(event);
		}		
	}
	
}
