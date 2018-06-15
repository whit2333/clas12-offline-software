package cnuphys.bCNU.simanneal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cnuphys.bCNU.attributes.AttributePanel;
import cnuphys.bCNU.attributes.Attributes;
import cnuphys.bCNU.util.Fonts;
import cnuphys.splot.plot.PlotCanvas;
import cnuphys.splot.plot.PlotPanel;

/**
 * This panel will display the attributes for the simulation, the
 * run and reset buttons, and a plot
 * @author heddle
 *
 */
public class SimulationPanel extends JPanel implements ActionListener {
	
	//the underlying simulation
	private Simulation _simulation;
	
	//the content (display_ component
	private JComponent _content;
	
	//the attribute panel
	private AttributePanel _attributePanel;
	
	private JLabel _stateLabel;
	
	//for the plot
	// the owner canvas
	protected PlotCanvas _plotCanvas;
	private PlotPanel _plotPanel;

	
	//the buttons
	private JButton runButton;
	private JButton stopButton;
	private JButton pauseButton;
	private JButton resumeButton;
	private JButton resetButton;

	
	public SimulationPanel(Simulation simulation, JComponent content) {
		setLayout(new BorderLayout(4, 4));
		_simulation = simulation;
		_content = content;
		add(_content, BorderLayout.WEST);
		
		addEast();
		addCenter();
	}
	
	private JPanel insetPanel() {
		JPanel panel = new JPanel() {
			@Override
			public Insets getInsets() {
				Insets def = super.getInsets();
				return new Insets(def.top + 2, def.left + 2, def.bottom + 2,
						def.right + 2);
			}
			
		};
		panel.setLayout(new BorderLayout(4, 4));
		return panel;
	}
	
	private void addCenter() {
		JPanel panel = insetPanel();
		
		Attributes attributes = _simulation.getAttributes();
		String plotTitle = "?";
		String xLabel = "?";
		String yLabel = "?";
		try {
			plotTitle = attributes.getAttribute(Simulation.PLOTTITLE).getString();
			xLabel = attributes.getAttribute(Simulation.XAXISLABEL).getString();
			yLabel = attributes.getAttribute(Simulation.YAXISLABEL).getString();
		} catch (InvalidTargetObjectTypeException e) {
			e.printStackTrace();
		}
		
		_plotCanvas = new PlotCanvas(null, plotTitle, xLabel, yLabel) {
			@Override
			public Dimension getPreferredSize() {
				Dimension d = super.getPreferredSize();
				d.width = 400;
				return d;
			}
		};
		_plotPanel = new PlotPanel(_plotCanvas);

		
		panel.setLayout(new BorderLayout(4, 4));
		panel.add(_plotPanel, BorderLayout.CENTER);

	    add(panel, BorderLayout.CENTER);
		
	}

	//add the east panel
	private void addEast() {
		JPanel panel = insetPanel();
		
		//state label in north
		_stateLabel = new JLabel("State:            ");
	    panel.add(_stateLabel, BorderLayout.NORTH);
	    
		//attributes in center of east panel
		_attributePanel = new AttributePanel(_simulation.getAttributes());
			
	    panel.add(_attributePanel, BorderLayout.CENTER);
		
	    //buttons in south of east panel
	    JPanel bPanel = new JPanel();
	    bPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 2));
	    runButton = makeButton("Run");
	    stopButton = makeButton("Stop");
	    pauseButton = makeButton("Pause");
	    resumeButton = makeButton("Resume");
	    resetButton = makeButton("Reset");

	    
	    bPanel.add(runButton);
	    bPanel.add(pauseButton);
	    bPanel.add(resumeButton);
	    bPanel.add(resetButton);
	    bPanel.add(stopButton);
	    panel.add(bPanel, BorderLayout.SOUTH);
	    
	    add(panel, BorderLayout.EAST);
	    fixPanelState();
	}
	
	//create a buttom
	private JButton makeButton(String label) {
		JButton button = new JButton(label);
		button.addActionListener(this);
		button.setFont(Fonts.smallFont);
		return button;
	}
	
	//fix the states of the buttons
	private void fixPanelState() {
		SimulationState state = _simulation.getSimulationState();
		_stateLabel.setText("State: " + state);
		
		switch (state) {
		case RUNNING:
			runButton.setEnabled(false);
			pauseButton.setEnabled(true);
			resumeButton.setEnabled(false);
			resetButton.setEnabled(false);
			stopButton.setEnabled(true);
			break;
			
		case PAUSED:
			runButton.setEnabled(false);
			pauseButton.setEnabled(false);
			resumeButton.setEnabled(true);
			resetButton.setEnabled(false);
			stopButton.setEnabled(true);
			break;
			
		case STOPPED:
			runButton.setEnabled(true);
			pauseButton.setEnabled(false);
			resumeButton.setEnabled(false);
			resetButton.setEnabled(true);
			stopButton.setEnabled(false);
			break;
		}
	}
	
	@Override
	public Insets getInsets() {
		Insets def = super.getInsets();
		return new Insets(def.top + 2, def.left + 2, def.bottom + 2,
				def.right + 2);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		Object source = e.getSource();
		
		if (source == runButton) {
			_simulation.startSimulation();
		}
		else if (source == pauseButton) {
			_simulation.setSimulationState(SimulationState.PAUSED);
		}
		else if (source == resumeButton) {
			_simulation.setSimulationState(SimulationState.RUNNING);
		}
		else if (source == resetButton) {
			_simulation.reset();
		}
		else if (source == stopButton) {
			_simulation.setSimulationState(SimulationState.STOPPED);
		}

		
		fixPanelState();
	}

}
