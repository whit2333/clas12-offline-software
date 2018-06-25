package cnuphys.fastMCed.consumers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.log.Log;
import cnuphys.bCNU.util.Environment;
import cnuphys.bCNU.util.PluginLoader;
import cnuphys.fastMCed.eventio.IPhysicsEventListener;
import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.fastmc.ParticleHits;
import cnuphys.fastMCed.frame.FastMCed;
import cnuphys.fastMCed.streaming.IStreamProcessor;
import cnuphys.fastMCed.streaming.StreamManager;
import cnuphys.fastMCed.streaming.StreamProcessStatus;
import cnuphys.fastMCed.streaming.StreamReason;

/**
 * Managers consumers
 * @author heddle
 *
 */
public class ConsumerManager extends Vector<PhysicsEventConsumer> implements IPhysicsEventListener, IStreamProcessor, ActionListener  {
	
	// optional full path to consumers folder
	private String _consumerPath = sysPropOrEnvVar("CONSUMERDIR");
	
	/** Last selected data file */
	private static String dataFilePath = Environment.getInstance().getHomeDirectory();


	/** possible class file extensions */
	private static String extensions[] = { "class"};

	// filter to look for lund files
	private static FileNameExtensionFilter _classFileFilter = new FileNameExtensionFilter("Consumer class Files",
			extensions);

	//map consumers to menu tems
	private Hashtable<PhysicsEventConsumer, JCheckBoxMenuItem> hash =  new Hashtable<>();
	
	//where the PhysicsEventConsumer plugins are found
	private File _consumerDir;

	//singleton
	private static ConsumerManager instance;
	
	// the base class forconsumer plugins
	protected Class<PhysicsEventConsumer> _consumerClaz;
	
	//load a consumer (a .class file) directly
	private JMenuItem _loadItem;

	//the menu
	private JMenu _menu;
	
	//why an event was flagged
	private String _flagExplanation;

	//private singleton constructor
	private ConsumerManager() {
		String cwd = Environment.getInstance().getCurrentWorkingDirectory();
		
		_consumerDir = new File(cwd, "consumers");
		String classPath = _consumerDir.getPath();
		
		//TODO prepend other dirs
		if (_consumerPath != null) {
			classPath = _consumerPath + File.pathSeparator + classPath;
		}
		if (FastMCed.getUserConsumerDir() != null) {
			classPath = FastMCed.getUserConsumerDir() + File.pathSeparator + classPath;
		}
		
		System.err.println("Consumer plugin path: [" + classPath + "]");
		Log.getInstance().info("Consumer plugin path: [" + classPath + "]");
		
//		if (_consumerDir.exists() && (_consumerDir.isDirectory())) {
//			System.err.println("Found Consumers Directory");
//		}
//		else {
//			System.err.println("Did not find Consumers Directory");
//			_consumerDir = null;
//			return;
//		}
		
		try {
			_consumerClaz = (Class<PhysicsEventConsumer>) Class.forName("cnuphys.fastMCed.consumers.PhysicsEventConsumer");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.err.println("Found PhysicsEventConsumer class");
		
		//let's try to load

		
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
		
		if (hash.size() == 0) {
			_menu.addSeparator();
		}
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
			
			_loadItem = new JMenuItem("Load a consumer .class file...");
			_loadItem.addActionListener(instance);
			_menu.add(_loadItem);
			
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
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {
		for (PhysicsEventConsumer consumer : this) {
			if (isActive(consumer)) {
				StreamProcessStatus status = consumer.streamingPhysicsEvent(event, particleHits);
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
	public void newPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {
		for (PhysicsEventConsumer consumer : this) {
			consumer.newPhysicsEvent(event, particleHits);
		}		
	}
	
	// get a property or environment variable
	// the property takes precedence
	private String sysPropOrEnvVar(String key) {
		String s = System.getProperty(key);
		if (s == null) {
			s = System.getenv(key);
		}
		return s;
	}
	
	//direct load of a class
	private void handleLoad() {
		//open the file
		
		JFileChooser chooser = new JFileChooser(dataFilePath);
		chooser.setSelectedFile(null);
		chooser.setFileFilter(_classFileFilter);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			
			try {
				String className = PluginLoader.getFullClassName(file);
				Object object = PluginLoader.instantiateFromClassFile(file, className);
				if (object instanceof PhysicsEventConsumer) {
					PhysicsEventConsumer consumer = (PhysicsEventConsumer)object;
					add(consumer);
					_menu.add(createMenuItem(consumer, true));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			
		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == _loadItem) {
			handleLoad();
		}
	}


}
