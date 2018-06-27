package cnuphys.fastMCed.eventgen.random;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jlab.clas.physics.Particle;

import cnuphys.bCNU.dialog.VerticalFlowLayout;
import cnuphys.bCNU.util.UnicodeSupport;
import cnuphys.lund.LundComboBox;

public class ParticlePanel extends JPanel implements ItemListener {

	// active checkbox
	private JCheckBox _active;

	// chose a particle
	private LundComboBox _lundComboBox;
	
	//vertex
	private VariablePanel _xoPanel;
	private VariablePanel _yoPanel;
	private VariablePanel _zoPanel;
	
	//momentum
	private VariablePanel _pPanel;
	private VariablePanel _thetaPanel;
	private VariablePanel _phiPanel;
	
	//random generator
	private Random _rand;

	public ParticlePanel(boolean use, int lundIntId, Random rand) {
		_rand = rand;
		setLayout(new BorderLayout(20, 4));

		add(addWestPanel(use, lundIntId), BorderLayout.WEST);
		add(addCenterPanel(), BorderLayout.CENTER);
		add(addEastPanel(), BorderLayout.EAST);
		setBorder(BorderFactory.createEtchedBorder());
		
		fixState();
	}

	public JPanel addWestPanel(boolean use, int lundIntId) {
		JPanel panel = new JPanel();
		panel.setLayout(new VerticalFlowLayout());
		_active = new JCheckBox("use", use);
		_active.addItemListener(this);
		_lundComboBox = new LundComboBox(false, 950.0, lundIntId);
		
		panel.add(_active);
		panel.add(_lundComboBox);
		return panel;
	}

	public JPanel addCenterPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new VerticalFlowLayout());
		
		_xoPanel = new VariablePanel("Xo", 0, 0, "cm");
		_yoPanel = new VariablePanel("Yo", 0, 0, "cm");
		_zoPanel = new VariablePanel("Zo", 0, 0, "cm");

		panel.add(_xoPanel);
		panel.add(_yoPanel);
		panel.add(_zoPanel);

		return panel;
	}
	
	/**
	 * Check whether this panel is active
	 * @return <code>if the panel is active
	 */
	public boolean isActive() {
		return _active.isSelected();
	}

	public JPanel addEastPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new VerticalFlowLayout());
		_pPanel = new VariablePanel("P", 1, 10, "GeV/c");
		_thetaPanel = new VariablePanel(UnicodeSupport.SMALL_THETA, 5, 40, "deg");
		_phiPanel = new VariablePanel(UnicodeSupport.SMALL_PHI, -22, 22, "deg");

		panel.add(_pPanel);
		panel.add(_thetaPanel);
		panel.add(_phiPanel);


		return panel;
	}
	
	//fix sectability
	private void fixState() {
		boolean active = isActive();
		_lundComboBox.setEnabled(active);
		_xoPanel.setEnabled(active);
		_yoPanel.setEnabled(active);
		_zoPanel.setEnabled(active);
		_pPanel.setEnabled(active);
		_thetaPanel.setEnabled(active);
		_phiPanel.setEnabled(active);
	}
	

	@Override
	public void itemStateChanged(ItemEvent e) {
		fixState();
	}
	
	/**
	 * Create a particle to add to an event
	 * @return a particle to add to an event
	 */
	public Particle  createParticle() {
		int pid = _lundComboBox.getSelectedId().getId();
		double p = _pPanel.randomValue(_rand);
		double theta = Math.toRadians(_thetaPanel.randomValue(_rand));
		double phi = Math.toRadians(_phiPanel.randomValue(_rand));
		double pperp = p*Math.sin(theta);
		double px = pperp*Math.cos(phi);
		double py = pperp*Math.sin(phi);
		double pz = p*Math.cos(theta);
		double vx = _xoPanel.randomValue(_rand);
		double vy = _yoPanel.randomValue(_rand);
		double vz = _zoPanel.randomValue(_rand);
		Particle part = new Particle(pid, px, py, pz, vx, vy, vz);
		
		return part;
	}

}
