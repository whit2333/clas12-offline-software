package cnuphys.bCNU.simanneal.example.ising2D;

import javax.management.modelmbean.InvalidTargetObjectTypeException;

import cnuphys.bCNU.attributes.Attributes;
import cnuphys.bCNU.simanneal.Simulation;
import cnuphys.bCNU.simanneal.Solution;

public class Ising2DSimulation extends Simulation {
	
	//custom attributes
	public static final String NUMROWS = "num rows";
	public static final String NUMCOLUMNS = "num columns";
	
	private Ising2DSolution _i2dSolution;


	@Override
	protected Solution setInitialSolution() {
		_i2dSolution = new Ising2DSolution(this, getNumRows(), getNumColumns());
		return _i2dSolution;
	}
	
	/**
	 * Get the number of rows in the current simulation
	 * @return the number of rows
	 */
	public int getNumRows() {
		try {
			return _attributes.getAttribute(NUMROWS).getInt();
		} catch (InvalidTargetObjectTypeException e) {
			e.printStackTrace();
		}	
		return -1;
	}

	/**
	 * Get the number of columns in the current simulation
	 * @return the number of columns
	 */
	public int getNumColumns() {
		try {
			return _attributes.getAttribute(NUMCOLUMNS).getInt();
		} catch (InvalidTargetObjectTypeException e) {
			e.printStackTrace();
		}	
		return -1;
	}


	@Override
	protected void setInitialAttributes(Attributes attributes) {
		
		//change some defaults
		attributes.setValue(Simulation.PLOTTITLE, "2D Ising Model");
		attributes.setValue(Simulation.YAXISLABEL, "|Magnetization|");
		attributes.setValue(Simulation.XAXISLABEL, "Log(Temp)");
		attributes.setValue(Simulation.USELOGTEMP, true);
		
		//custom
		attributes.add(NUMROWS, 20);
		attributes.add(NUMCOLUMNS, 20);
		
	}

}
