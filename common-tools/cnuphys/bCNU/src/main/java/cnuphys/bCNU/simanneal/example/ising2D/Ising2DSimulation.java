package cnuphys.bCNU.simanneal.example.ising2D;

import cnuphys.bCNU.attributes.Attributes;
import cnuphys.bCNU.simanneal.Simulation;
import cnuphys.bCNU.simanneal.Solution;

public class Ising2DSimulation extends Simulation {
	
	//custom attributes
	public static final String NUMROWS = "num rows";
	public static final String NUMCOLUMNS = "num columns";


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

	@Override
	protected Solution setInitialSolution() {
		// TODO Auto-generated method stub
		return null;
	}

}
