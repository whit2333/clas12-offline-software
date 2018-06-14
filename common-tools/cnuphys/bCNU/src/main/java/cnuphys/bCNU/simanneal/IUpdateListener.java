package cnuphys.bCNU.simanneal;

import java.util.EventListener;

public interface IUpdateListener extends EventListener {

	/**
	 * Notifies listeners of a new solution. This is in the thread of the simulation,
	 * not the GUI thread.
	 * @param simulation the simulation
	 * @param newSolution the new solution
	 * @param oldSolution the old solution
	 */
	public void updateSolution(Simulation simulation, Solution newSolution, Solution oldSolution);
}
