package cnuphys.fastMCed.frame;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.application.BaseMDIApplication;
import cnuphys.bCNU.application.Desktop;
import cnuphys.bCNU.component.MagnifyWindow;
import cnuphys.bCNU.dialog.TextDisplayDialog;
import cnuphys.bCNU.log.Log;
import cnuphys.bCNU.menu.MenuManager;
import cnuphys.bCNU.util.Environment;
import cnuphys.bCNU.util.FileUtilities;
import cnuphys.bCNU.util.PropertySupport;
import cnuphys.bCNU.view.ViewManager;
import cnuphys.bCNU.view.VirtualView;
import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.eventio.IPhysicsEventListener;
import cnuphys.fastMCed.fastmc.FastMCMenuAddition;
import cnuphys.fastMCed.properties.PropertiesManager;
import cnuphys.fastMCed.view.alldc.AllDCView;
import cnuphys.fastMCed.view.data.DataTableModel;
import cnuphys.fastMCed.view.data.DataView;
import cnuphys.fastMCed.view.trajinfo.TrajectoryInfoView;
import cnuphys.magfield.FastMath;
import cnuphys.magfield.MagneticFieldChangeListener;
import cnuphys.magfield.MagneticFields;
import cnuphys.splot.example.MemoryUsageDialog;
import cnuphys.swim.SwimMenu;
import cnuphys.swim.Swimming;
import geometry.GeometryManager;

/**
 * This is a brother to ced that works with the fastMC system rather than evio
 * or hipo
 * 
 * @author heddle
 *
 */
public class FastMCed extends BaseMDIApplication implements MagneticFieldChangeListener, IPhysicsEventListener {

	// the singleton
	private static FastMCed _instance;

	// release (version) string
	private static final String _release = "build 0.50";

	// used for one time inits
	private int _firstTime = 0;

	// event number label on menu bar
	private static JLabel _eventNumberLabel;

	// memory usage dialog
	private MemoryUsageDialog _memoryUsage;

	// Environment display
	private TextDisplayDialog _envDisplay;

	// the views
	private VirtualView _virtualView;
	private TrajectoryInfoView _trajInfoView;
	private AllDCView _allDCView;
	private DataView _dcDataView;
	private DataView _ftofDataView;

	/**
	 * Constructor (private--used to create singleton)
	 * 
	 * @param keyVals
	 *            an optional variable length list of attributes in type-value
	 *            pairs. For example, PropertySupport.NAME, "my application",
	 *            PropertySupport.CENTER, true, etc.
	 */
	private FastMCed(Object... keyVals) {
		super(keyVals);


		ComponentListener cl = new ComponentListener() {

			@Override
			public void componentHidden(ComponentEvent ce) {
			}

			@Override
			public void componentMoved(ComponentEvent ce) {
				placeViewsOnVirtualDesktop();
			}

			@Override
			public void componentResized(ComponentEvent ce) {
				placeViewsOnVirtualDesktop();
			}

			@Override
			public void componentShown(ComponentEvent ce) {
				placeViewsOnVirtualDesktop();
			}

		};

		addComponentListener(cl);
		
		addHeadsUp();
		
	}
	
	// arrange the views on the virtual desktop
	private void placeViewsOnVirtualDesktop() {
		if (_firstTime == 1) {
			// rearrange some views in virtual space
			_virtualView.reconfigure();
			restoreDefaultViewLocations();

			// now load configuration
			Desktop.getInstance().loadConfigurationFile();
			Desktop.getInstance().configureViews();
		}
		_firstTime++;
	}

	/**
	 * Restore the default locations of the default views. Cloned views are
	 * unaffected.
	 */
	private void restoreDefaultViewLocations() {

		_virtualView.moveTo(_allDCView, 1);
		_virtualView.moveTo(_trajInfoView, 0, VirtualView.UPPERRIGHT);
		_virtualView.moveTo(_dcDataView, 2, VirtualView.BOTTOMLEFT);
		_virtualView.moveTo(_ftofDataView, 2, VirtualView.BOTTOMRIGHT);

		// _virtualView.moveToStart(_sectorView14, 0, VirtualView.UPPERLEFT);
		// _virtualView.moveToStart(_sectorView25, 0, VirtualView.UPPERLEFT);
		// _virtualView.moveToStart(_sectorView36, 0, VirtualView.UPPERLEFT);
		//
		// _virtualView.moveTo(_plotView, 0, VirtualView.CENTER);
		//
		// _virtualView.moveTo(dcHistoGrid, 13);
		// _virtualView.moveTo(ftofHistoGrid, 14);
		// _virtualView.moveTo(bstHistoGrid, 15);
		// _virtualView.moveTo(pcalHistoGrid, 16);
		// _virtualView.moveTo(ecHistoGrid, 17);
		//
		// _virtualView.moveTo(_allDCView, 3);
		// _virtualView.moveTo(_eventView, 6, VirtualView.CENTER);
		// _virtualView.moveTo(_centralXYView, 2, VirtualView.BOTTOMLEFT);
		// _virtualView.moveTo(_centralZView, 2, VirtualView.UPPERRIGHT);
		//
		// // note no constraint means "center"
		// _virtualView.moveTo(_dcXyView, 7);
		// _virtualView.moveTo(_projectedDCView, 8);
		//
		// _virtualView.moveTo(_pcalView, 4);
		// _virtualView.moveTo(_ecView, 5);
		// _virtualView.moveTo(_logView, 12, VirtualView.UPPERRIGHT);
		// _virtualView.moveTo(_monteCarloView, 1, VirtualView.TOPCENTER);
		// _virtualView.moveTo(_reconEventView, 1, VirtualView.BOTTOMCENTER);
		//
		// _virtualView.moveTo(_tofView, 11, VirtualView.CENTER);
		//
		// _virtualView.moveTo(_ftcalXyView, 12, VirtualView.CENTER);
		//
		// if (_use3D) {
		// _virtualView.moveTo(_forward3DView, 9, VirtualView.CENTER);
		// _virtualView.moveTo(_central3DView, 10, VirtualView.BOTTOMLEFT);
		// _virtualView.moveTo(_ftCal3DView, 10, VirtualView.BOTTOMRIGHT);
		// }

		Log.getInstance().config("reset views on virtual dekstop");

	}

	/**
	 * Add the initial views to the desktop.
	 */
	private void addInitialViews() {

		// add an object that can respond to a "swim all MC" request.

		// FastMCEventManager.getInstance().setAllMCSwimmer(new SwimAllMC());

		// add a virtual view
		_virtualView = VirtualView.createVirtualView(8);
		ViewManager.getInstance().getViewMenu().addSeparator();

		// add an alldc view
		_allDCView = AllDCView.createAllDCView();

		// add monte carlo view
		_trajInfoView = new TrajectoryInfoView();

		// data views
		_dcDataView = new DataView("Drift Chamber Hits", DataTableModel.DETECTOR_DC);
		_ftofDataView = new DataView("FTOF Hits", DataTableModel.DETECTOR_FTOF);

		// // add event view
		// _eventView = ClasIoEventView.createEventView();
		//
		// // add three sector views
		// ViewManager.getInstance().getViewMenu().addSeparator();
		// _sectorView36=SectorView.createSectorView(DisplaySectors.SECTORS36);
		// _sectorView25=SectorView.createSectorView(DisplaySectors.SECTORS25);
		// _sectorView14=SectorView.createSectorView(DisplaySectors.SECTORS14);
		// ViewManager.getInstance().getViewMenu().addSeparator();
		//
		// // add monte carlo view
		// _monteCarloView = new ClasIoMonteCarloView();
		//
		//
		// // add a reconstructed tracks view
		// _reconEventView = ClasIoReconEventView.getInstance();
		//
		// ViewManager.getInstance().getViewMenu().addSeparator();
		//
		// // add an alldc view
		// _allDCView = AllDCView.createAllDCView();
		//
		// _tofView = TOFView.createTOFView();
		//
		// // add a bstZView
		// _centralZView = CentralZView.createCentralZView();
		//
		// // add a bstXYView
		// _centralXYView = CentralXYView.createCentralXYView();
		//
		// // add a ftcalxyYView
		// _ftcalXyView = FTCalXYView.createFTCalXYView();
		//
		// // add a DC XY View
		// _dcXyView = DCXYView.createDCXYView();
		//
		// // projected dC view
		// _projectedDCView = ProjectedDCView.createProjectedDCView();
		//
		// // add an ec view
		// _ecView = ECView.createECView();
		//
		// // add an pcal view
		// _pcalView = PCALView.createPCALView();
		//
		// // 3D view?
		// if (_use3D) {
		// ViewManager.getInstance().getViewMenu().addSeparator();
		// _forward3DView = new ForwardView3D();
		// _central3DView = new CentralView3D();
		// _ftCal3DView = new FTCalView3D();
		// }
		//
		// // add logview
		// ViewManager.getInstance().getViewMenu().addSeparator();
		// //plot view
		// _plotView = new PlotView();
		//
		// _logView = new LogView();
		//
		//
		// //add histograms
		// addDcHistogram();
		// addFtofHistogram();
		// addBstHistogram();
		// addPcalHistogram();
		// addEcHistogram();

		// log some environment info
		Log.getInstance().config(Environment.getInstance().toString());

		// use config file info
		// Desktop.getInstance().configureViews();

		_virtualView.toFront();
	}

	/**
	 * private access to the FastMCed singleton.
	 * 
	 * @return the singleton FastMCed (the main application frame.)
	 */
	private static FastMCed getInstance() {
		if (_instance == null) {
			_instance = new FastMCed(PropertySupport.TITLE, "fastmCED " + versionString(),
					PropertySupport.BACKGROUNDIMAGE, "images/cnuinv.png", PropertySupport.FRACTION, 0.9);

			_instance.addInitialViews();
			_instance.createMenus();
			_instance.placeViewsOnVirtualDesktop();

			_instance.createEventNumberLabel();
			MagneticFields.getInstance().addMagneticFieldChangeListener(_instance);

		}
		return _instance;
	}

	/**
	 * Add items to existing menus and/or create new menus NOTE: Swim menu is
	 * created by the SwimManager
	 */
	private void createMenus() {
		MenuManager mmgr = MenuManager.getInstance();

		// create the mag field menu
		MagneticFields.getInstance().setActiveField(MagneticFields.FieldType.TORUS);
		mmgr.addMenu(MagneticFields.getInstance().getMagneticFieldMenu());

		// the swimmer menu
		mmgr.addMenu(SwimMenu.getInstance());

		// remove the option menu until I need it
		// mmgr.removeMenu(mmgr.getOptionMenu());

		// add to the file menu
		addToFileMenu();

		// FastMC
		new FastMCMenuAddition(mmgr.getFileMenu());

		// the options menu
		addToOptionMenu(mmgr.getOptionMenu());

	}

	// add to the file menu
	private void addToFileMenu() {
		MenuManager mmgr = MenuManager.getInstance();
		JMenu fmenu = mmgr.getFileMenu();
	}

	/**
	 * Refresh all views (with containers)
	 */
	public static void refresh() {
		ViewManager.getInstance().refreshAllViews();
	}

	/**
	 * Set the event number label
	 * 
	 * @param num
	 *            the event number
	 */
	public static void setEventNumberLabel(int num) {

		if (num < 0) {
			_eventNumberLabel.setText("  Event Num:      ");
		} else {
			_eventNumberLabel.setText("  Event Num: " + num);
		}
	}

	/**
	 * public access to the singleton
	 * 
	 * @return the singleton FastMCed (the main application frame.)
	 */
	public static FastMCed getFastMCed() {
		return _instance;
	}

	/**
	 * Generate the version string
	 * 
	 * @return the version string
	 */
	public static String versionString() {
		return _release;
	}

	// create the event number label
	private void createEventNumberLabel() {
		_eventNumberLabel = new JLabel("  Event Num:      is GEMC: false");
		_eventNumberLabel.setOpaque(true);
		_eventNumberLabel.setBackground(Color.black);
		_eventNumberLabel.setForeground(Color.yellow);
		_eventNumberLabel.setFont(new Font("Dialog", Font.BOLD, 12));
		_eventNumberLabel.setBorder(BorderFactory.createLineBorder(Color.cyan, 1));
		setEventNumberLabel(-1);

		getJMenuBar().add(Box.createHorizontalGlue());
		getJMenuBar().add(_eventNumberLabel);
		getJMenuBar().add(Box.createHorizontalStrut(5));
	}

	@Override
	public void magneticFieldChanged() {
		Swimming.clearMCTrajectories();
		fixTitle();
		PhysicsEventManager.getInstance().reloadCurrentEvent();
	}

	/**
	 * Fix the title of te main frame
	 */
	public void fixTitle() {
		String title = getTitle();
		int index = title.indexOf("   [Mag");
		if (index > 0) {
			title = title.substring(0, index);
		}

		title += "   [Magnetic Field: " + MagneticFields.getInstance().getActiveFieldDescription();
		title += "]";

		File currentFile = PhysicsEventManager.getInstance().getCurrentFile();
		String fstr = (currentFile == null) ? "none" : currentFile.getName();
		title += " [lund file: " + fstr + "]";

		setTitle(title);
	}

	/**
	 * Get the parent frame
	 * 
	 * @return the parent frame
	 */
	public static JFrame getFrame() {
		return _instance;
	}

	// create the options menu
	private void addToOptionMenu(JMenu omenu) {
		omenu.add(MagnifyWindow.magificationMenu());
		omenu.addSeparator();

		final JMenuItem memPlot = new JMenuItem("Memory Usage...");
		final JMenuItem environ = new JMenuItem("Environment...");
		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				Object source = e.getSource();

				if (source == memPlot) {
					if (_memoryUsage == null) {
						_memoryUsage = new MemoryUsageDialog(getFrame());
					}

					_memoryUsage.setVisible(true);
				}

				else if (source == environ) {
					if (_envDisplay == null) {
						_envDisplay = new TextDisplayDialog("Environment Information");
					}
					_envDisplay.setText(Environment.getInstance().toString());
					_envDisplay.setVisible(true);
				}

			}

		};
		environ.addActionListener(al);
		memPlot.addActionListener(al);
		omenu.add(environ);
		omenu.add(memPlot);

	}

	@Override
	public void openedNewLundFile(String path) {
		System.err.println("Opened new Lund File [" + path + "]");
	}

	@Override
	public void newPhysicsEvent(PhysicsEvent event) {
		System.err.println("Obtained new FastMC event");
	}

	/**
	 * Main program launches the ced gui.
	 * <p>
	 * Command line arguments:</br>
	 * -p [dir] dir is the default directory
	 * 
	 * @param arg
	 *            the command line arguments.
	 */
	public static void main(String[] arg) {
		FastMath.setMathLib(FastMath.MathLib.SUPERFAST);

		// read in userprefs
		PropertiesManager.getInstance();

		FileUtilities.setDefaultDir("data");

		// create a console log listener
		// Log.getInstance().addLogListener(new ConsoleLogListener());

		// splash frame
		final SplashWindowFastMCed splashWindow = new SplashWindowFastMCed("fastmCED", null, 920, _release);

		// now make the frame visible, in the AWT thread
		try {
			EventQueue.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					splashWindow.setVisible(true);
				}

			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// process command args
		if ((arg != null) && (arg.length > 0)) {
			int len = arg.length;
			int lm1 = len - 1;
			boolean done = false;
			int i = 0;
			while (!done) {
				if (arg[i].equalsIgnoreCase("-p")) {
					if (i < lm1) {
						i++;
						FileUtilities.setDefaultDir(arg[i]);
					}
				}

				i++;
				done = (i >= len);
			} // !done
		} // end command arg processing

		// initialize magnetic fields
		MagneticFields.getInstance().initializeMagneticFields();

		// initialize geometry
		GeometryManager.getInstance();

		// now make the frame visible, in the AWT thread
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				FastMCed app = getInstance();
				PhysicsEventManager.getInstance().addPhysicsListener(app, 2);
				splashWindow.setVisible(false);
				getFastMCed().setVisible(true);
				getFastMCed().fixTitle();

				System.out.println("fastmCED  " + _release + " is ready.");
			}

		});
		Log.getInstance().info(Environment.getInstance().toString());

		Log.getInstance().info("fastmCED is ready.");

	} // end main

}
