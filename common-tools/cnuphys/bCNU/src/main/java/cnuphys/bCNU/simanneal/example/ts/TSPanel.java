package cnuphys.bCNU.simanneal.example.ts;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JPanel;

import cnuphys.bCNU.attributes.Attributes;
import cnuphys.bCNU.simanneal.Simulation;
import cnuphys.bCNU.simanneal.SimulationPanel;
import cnuphys.bCNU.simanneal.SimulationState;
import cnuphys.bCNU.simanneal.Solution;

public class TSPanel extends JPanel {
	
	//Simulation panel for display
	private TSDisplay _tsDisplay;
	
	//the simulation panel
	private SimulationPanel _simPanel;

	//the simulation
	private TSSimulation _simulation;
	
	public TSPanel(TSSimulation simulation) {
		
		_simulation = simulation;
		
		//initial solution
		TSSolution initSolution = (TSSolution)(_simulation.getInitialSolution());
		
		
		System.out.println("City count: " + initSolution.count());
		System.out.println("Initial distance: " + initSolution.getDistance());
		System.out.println("Initial energy: " + initSolution.getEnergy());
		TSSolution neighbor = (TSSolution) initSolution.getRearrangement();
		System.out.println("Initial distance: " + initSolution.getDistance());
		System.out.println("Initial energy: " + initSolution.getEnergy());
		System.out.println("Neighbor distance: " + neighbor.getDistance());
		System.out.println("Neighbor energy: " + neighbor.getEnergy());
		
		
		Attributes attributes = new Attributes();
		attributes.add(Simulation.COOLRATE, 0.03);
		attributes.add(Simulation.RANDSEED, -1L);
		attributes.add(Simulation.THERMALCOUNT, 200);
		attributes.add(Simulation.MAXSTEPS, 1000);
		
		_tsDisplay = new TSDisplay(initSolution);

		_simulation.addUpdateListener(_tsDisplay);

		
		
		//add initial values
		initSolution.temps.add(_simulation.getTemperature());
		initSolution.dists.add(initSolution.getDistance());
		
		_tsDisplay.setPreferredSize(new Dimension(600, 600));
		
		_simPanel = new SimulationPanel(_simulation, _tsDisplay);
		
		add(_simPanel);
	}
	
	/**
	 * Get the underlying simulation
	 * @return the underlying simulation
	 */
	public Simulation getSimulation() {
		return _simulation;
	}

	
	@Override
	public Insets getInsets() {
		Insets def = super.getInsets();
		return new Insets(def.top + 2, def.left + 2, def.bottom + 2,
				def.right + 2);
	}

}
