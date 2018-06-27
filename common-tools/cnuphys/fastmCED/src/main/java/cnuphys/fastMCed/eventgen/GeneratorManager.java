package cnuphys.fastMCed.eventgen;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import cnuphys.fastMCed.eventgen.random.RandomEventGenerator;
import cnuphys.fastMCed.eventgen.sweep.SweepEventGenerator;
import cnuphys.fastMCed.eventio.PhysicsEventManager;

public class GeneratorManager implements ActionListener {
	
	//menu stuff
	private JMenu _menu;
	private static JMenuItem _sweepGenerator;
	private static JMenuItem _randomGenerator;
	
	//singleton
	private static GeneratorManager instance;
	
	//private constructor for 
	private GeneratorManager() {
	}
	
	/**
	 * Access to the singleton GeneratorManager
	 * @return the GeneratorManager
	 */
	public static GeneratorManager getInstance() {
		if (instance == null) {
			instance = new GeneratorManager();
		}
		return instance;
	}
	
	/**
	 * Get the Generator menu
	 * @return
	 */
	public JMenu getMenu() {
		if (_menu == null) {
			createMenu();
		}
		return _menu;
	}

	//createb theb menu
	private void createMenu() {
		_menu = new JMenu("Generators");
		_sweepGenerator = menuItem("Sweep Generator...");
		_randomGenerator = menuItem("Random Generator...");
	}
	
	private JMenuItem menuItem(String label) {
				
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(this);
		_menu.add(item);

		return item;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == _randomGenerator) {
			RandomEventGenerator generator = RandomEventGenerator.createRandomGenerator();
			if (generator != null) {
				PhysicsEventManager.getInstance().setEventGenerator(generator);
			}
		}
		else if (source == _sweepGenerator) {
			SweepEventGenerator generator = SweepEventGenerator.createSweepGenerator();
			if (generator != null) {
				PhysicsEventManager.getInstance().setEventGenerator(generator);
			}
		}
	}
}
