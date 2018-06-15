package cnuphys.bCNU.simanneal.example.ts;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import cnuphys.bCNU.attributes.Attribute;
import cnuphys.bCNU.attributes.Attributes;
import cnuphys.bCNU.simanneal.Simulation;
import cnuphys.bCNU.simanneal.Solution;
import cnuphys.bCNU.util.Fonts;

public class TSSimulation extends Simulation {
	
	//custom attributes
	public static final String NUMCITY = "num city";
	public static final String RIVER = "river penalty";
	
	private TSSolution _tsSolution;
	
	//for river penalty
	private JSlider _riverSlider;
	
	public TSSimulation() {
		
	}

	@Override
	public Solution setInitialSolution() {
		_tsSolution = new TSSolution(this, getNumCity());
		return _tsSolution;
	}

	@Override
	protected Attributes setInitialAttributes() {
		Attributes attributes = new Attributes();
		attributes.add(Simulation.COOLRATE, 0.03);
		attributes.add(Simulation.RANDSEED, -1L);
		attributes.add(Simulation.THERMALCOUNT, 200);
		attributes.add(Simulation.MAXSTEPS, 1000);
		attributes.add(NUMCITY, 200);
		
		JSlider slider = new JSlider(-10, 10, 0);
		
		slider.setMajorTickSpacing((slider.getMaximum()-slider.getMinimum())/2);
	//	slider.setPaintTicks(true);
		slider.setPaintLabels(true);
//		slider.setBorder(
//                BorderFactory.createEmptyBorder(2, 2, 2, 2));
		slider.setFont(Fonts.tinyFont);
		Dimension d = slider.getPreferredSize();
		d.width = 150;
		slider.setPreferredSize(d);
		attributes.add(new Attribute(RIVER, slider));

		
		
		return attributes;
	}
	

	public int getNumCity() {
		try {
			return _attributes.getAttribute(NUMCITY).getInt();
		} catch (InvalidTargetObjectTypeException e) {
			e.printStackTrace();
		}	
		return -1;
	}

	//main program for testing
	public static void main(String arg[]) {

		final JFrame frame = new JFrame();

		// set up what to do if the window is closed
		WindowAdapter windowAdapter = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				System.exit(1);
			}
		};

		frame.addWindowListener(windowAdapter);

		frame.setLayout(new BorderLayout());
		
		TSSimulation simulation = new TSSimulation();

		TSPanel tsPanel = new TSPanel(simulation);
		
		frame.add(tsPanel, BorderLayout.CENTER);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.pack();
				frame.setVisible(true);
				frame.setLocationRelativeTo(null);
			}
		});

		
		
	//	tsPanel.getSimulation().run();
		
//		makePlot(temps, dists).setVisible(true);
	}

}
