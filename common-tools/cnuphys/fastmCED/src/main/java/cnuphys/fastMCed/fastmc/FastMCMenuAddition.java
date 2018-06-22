package cnuphys.fastMCed.fastmc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.util.Environment;
import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.fastmc.accept.AcceptanceManager;
import cnuphys.fastMCed.streaming.IStreamProcessor;
import cnuphys.fastMCed.streaming.StreamDialog;
import cnuphys.fastMCed.streaming.StreamManager;
import cnuphys.fastMCed.streaming.StreamProcessStatus;
import cnuphys.fastMCed.streaming.StreamReason;
import cnuphys.fastMCed.eventio.IPhysicsEventListener;

public class FastMCMenuAddition implements ActionListener, ItemListener, IPhysicsEventListener, IStreamProcessor {

	//the physics event manager
	private PhysicsEventManager _physicsEventManager = PhysicsEventManager.getInstance();

	// the open menu
	private JMenuItem _openItem;

	// define acceptance
	private JMenu _acceptanceMenu;
	
	private StreamDialog _streamDialog;

	// hard coded acceptance definitions
	private JCheckBoxMenuItem _eItem;
	private JCheckBoxMenuItem _pItem;

	// the next menu item
	private JMenuItem _nextItem;

	//the stream menu item
	private JMenuItem _streamItem;

	// the parent menu
	private JMenu _menu;

	// the recent file menu
	private JMenu _recentMenu;

	// to find recently opened files from the preferences
	private static String _recentFileKey = "RecentEventFiles";

	// a hash table of menu items used by recent file feature
	private static Hashtable<String, JMenuItem> _menuItems;

	/**
	 * Create a set of FastMC Menu items, add to the given menu
	 */
	public FastMCMenuAddition(JMenu menu) {
		_menu = menu;

		// had hardcoded rules
		_acceptanceMenu = new JMenu("Acceptance Rule");
		_eItem = addCheckBoxItem("e- in 36 DC layers",
				AcceptanceManager.getInstance().getElectronCondition().isActive());
		_pItem = addCheckBoxItem("p in 36 DC layers", AcceptanceManager.getInstance().getProtonCondition().isActive());

		// add things in reverse order because of the items already in the file
		// menu
		_menu.insertSeparator(0);
		_streamItem = addItem("Stream Events...", KeyEvent.VK_S);
		_nextItem = addItem("Next Event", KeyEvent.VK_N);
		_menu.insertSeparator(0);
		_menu.add(_acceptanceMenu, 0);
		_menu.insertSeparator(0);

		_menu.add(getRecentEventFileMenu(), 0);

		_openItem = addItem("Open Lund File...");

		_physicsEventManager.addPhysicsListener(this, 2);
		StreamManager.getInstance().addStreamListener(this);

		fixMenuState();
		// setEnabled(false);
	}

	/**
	 * Get the menu from which you can choose a recently opened file
	 * 
	 * @return the menu from which you can choose a recently opened file
	 */
	public JMenu getRecentEventFileMenu() {
		_recentMenu = new JMenu("Recent Lund Files");

		// get the recent files from the prefs
		Vector<String> recentFiles = Environment.getInstance().getPreferenceList(_recentFileKey);

		if (recentFiles != null) {
			for (String fn : recentFiles) {

				// make sure the file still exists
				File file = new File(fn);
				if (file.exists()) {
					addMenu(fn, false);
				}
			}
		}

		return _recentMenu;
	}
	

	/**
	 * Update the recent files for the recent file menu.
	 * 
	 * @param path
	 *            the path to the file
	 */
	private void updateRecentFiles(String path) {
		if (path == null) {
			return;
		}
		Vector<String> recentFiles = Environment.getInstance()
				.getPreferenceList(_recentFileKey);
		if (recentFiles == null) {
			recentFiles = new Vector<String>(10);
		}

		// keep no more than 10
		recentFiles.remove(path);
		recentFiles.add(0, path);
		if (recentFiles.size() > 9) {
			recentFiles.removeElementAt(9);
		}
		Environment.getInstance().savePreferenceList(_recentFileKey,
				recentFiles);

		// add to menu
		addMenu(path, true);
	}

	// use to open recent files
	private void addMenu(String path, boolean atTop) {

		// if it is in the hash, remove from has and from menu
		if (_menuItems != null) {
			JMenuItem item = _menuItems.remove(path);
			if (item != null) {
				_recentMenu.remove(item);
			}
		} else {
			_menuItems = new Hashtable<String, JMenuItem>(41);
		}

		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				try {
					String fn = ae.getActionCommand();
					
//					System.err.println("RECENT FILE: [" + fn + "]");
					File file = new File(fn);
					
					_physicsEventManager.openFile(file);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		};
		JMenuItem item = new JMenuItem(path);
		item.addActionListener(al);
		_menuItems.put(path, item);
		if (atTop) {
			_recentMenu.add(item, 0);
		} else {
			_recentMenu.add(item);
		}

	}

	// add a menu item
	private JCheckBoxMenuItem addCheckBoxItem(String label, boolean on) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, on);
		item.addItemListener(this);
		_acceptanceMenu.add(item);
		return item;
	}
	
	// add a menu item
	private JMenuItem addItem(String label) {
		return addItem(label, 0);
	}


	// convenience method to add menu item
	private JMenuItem addItem(String label, int accelKey) {
		JMenuItem item = new JMenuItem(label);
		if (accelKey > 0) {
			item.setAccelerator(KeyStroke.getKeyStroke(accelKey,
					ActionEvent.CTRL_MASK));
		}

		item.addActionListener(this);
		_menu.add(item, 0);
		return item;
	}

	/**
	 * New fast mc event
	 * 
	 * @param event
	 *            the generated physics event
	 */
	@Override
	public void newPhysicsEvent(PhysicsEvent event) {
		fixMenuState();
	}

	@Override
	public void openedNewLundFile(String path) {
		updateRecentFiles(path);
		fixMenuState();
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == _openItem) {
			_physicsEventManager.openFile();
		} else if (o == _nextItem) {
			_physicsEventManager.nextEvent();
		} else if (o == _streamItem) {
			if ((_streamDialog == null) || (StreamManager.getInstance().getStreamState() == StreamReason.STOPPED)) {
				_streamDialog = new StreamDialog();
			}
			_streamDialog.setVisible(true);
			_streamDialog.toFront();
			fixMenuState();
		} 
	}

	// fix the menus state
	private void fixMenuState() {
		boolean goodFile = (_physicsEventManager.getCurrentFile() != null);
		boolean streaming = StreamManager.getInstance().isStarted();
	//	boolean paused = StreamManager.getInstance().isPaused();
		boolean stopped = StreamManager.getInstance().isStopped();
		boolean dialogVis = (_streamDialog != null) && _streamDialog.isVisible();
		
		_recentMenu.setEnabled(!dialogVis && stopped);
		_openItem.setEnabled(!dialogVis && stopped);

		_nextItem.setEnabled(goodFile && !streaming);
		_streamItem.setEnabled(goodFile && !streaming);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object o = e.getSource();
		if (o == _eItem) {
			AcceptanceManager.getInstance().getElectronCondition().setActive(_eItem.isSelected());

		} else if (o == _pItem) {
			AcceptanceManager.getInstance().getProtonCondition().setActive(_pItem.isSelected());
		}
		AcceptanceManager.getInstance().testEvent(PhysicsEventManager.getInstance().getCurrentEvent());
	}

	@Override
	public void streamingChange(StreamReason reason) {
		fixMenuState();
	}

	@Override
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event) {		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String flagExplanation() {
		return "No way";
	}


}