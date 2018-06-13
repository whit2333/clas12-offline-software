package cnuphys.bCNU.simanneal;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import cnuphys.bCNU.attributes.AttributePanel;

/**
 * This panel will display the attributes for the simulation, the
 * run and reset buttons, and a plot
 * @author heddle
 *
 */
public class SimulationPanel extends JPanel {
	
	//the underlying simulation
	private Simulation _simulation;
	
	//the content (display_ component
	private JComponent _content;
	
	//the attribute panel
	private AttributePanel _attributePanel;
	
	public SimulationPanel(Simulation simulation, JComponent content) {
		setLayout(new BorderLayout(4, 4));
		_simulation = simulation;
		_content = content;
		add(_content, BorderLayout.CENTER);
		addEast();
	}

	//add the panel
	private void addEast() {
		JPanel panel = new JPanel() {
			@Override
			public Insets getInsets() {
				Insets def = super.getInsets();
				return new Insets(def.top + 2, def.left + 2, def.bottom + 2,
						def.right + 2);
			}
			
		};
		
		panel.setLayout(new BorderLayout(4, 4));
		_attributePanel = new AttributePanel(_simulation.getAttributes());
	    panel.add(_attributePanel, BorderLayout.NORTH);
		
	    JPanel bPanel = new JPanel();
	    bPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 2));
	    JButton runButton = new JButton("Run");
	    runButton.addActionListener(e -> _simulation.start());
	    
	    JButton resetButton = new JButton("Reset");
	    runButton.addActionListener(e -> reset());

	    
	    bPanel.add(runButton);
	    bPanel.add(resetButton);
	    panel.add(bPanel, BorderLayout.SOUTH);
	    
	    add(panel, BorderLayout.EAST);
	}
	
	private void reset() {
//		_simulation.reset();
	}
	
	@Override
	public Insets getInsets() {
		Insets def = super.getInsets();
		return new Insets(def.top + 2, def.left + 2, def.bottom + 2,
				def.right + 2);
	}

}
