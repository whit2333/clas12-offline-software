package cnuphys.bCNU.simanneal.example.ising2D;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import cnuphys.bCNU.simanneal.SimulationDisplay;

public class Ising2DDisplay extends SimulationDisplay {

	public Ising2DDisplay(Ising2DSimulation simulation) {
		super(simulation);
	}
	
	@Override
	public void paintComponent(Graphics g) {
		Rectangle b = getBounds();
		
		g.setColor(Color.green);
		g.fillRect(b.x, b.y, b.width, b.height);
	}
	

}
