package cnuphys.fastMCed.eventgen.random;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import cnuphys.bCNU.dialog.VerticalFlowLayout;
import cnuphys.bCNU.util.UnicodeSupport;
import cnuphys.lund.LundComboBox;

public class ParticlePanel extends JPanel {

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

	public ParticlePanel(boolean use, int lundIntId) {
		setLayout(new BorderLayout(20, 4));

		add(addWestPanel(use, lundIntId), BorderLayout.WEST);
		add(addCenterPanel(), BorderLayout.CENTER);
		add(addEastPanel(), BorderLayout.EAST);
		setBorder(BorderFactory.createEtchedBorder());
	}

	public JPanel addWestPanel(boolean use, int lundIntId) {
		JPanel panel = new JPanel();
		panel.setLayout(new VerticalFlowLayout());
		_active = new JCheckBox("use", use);
		_lundComboBox = new LundComboBox(false, 950.0, lundIntId);
		
		panel.add(_active);
		panel.add(_lundComboBox);
		return panel;
	}

	public JPanel addCenterPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new VerticalFlowLayout());
		
		_xoPanel = new VariablePanel("Xo", -1, 1, "cm");
		_yoPanel = new VariablePanel("Yo", -1, 1, "cm");
		_zoPanel = new VariablePanel("Zo", -2, 2, "cm");

		panel.add(_xoPanel);
		panel.add(_yoPanel);
		panel.add(_zoPanel);

		return panel;
	}

	public JPanel addEastPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new VerticalFlowLayout());
		_pPanel = new VariablePanel("P", 1, 12, "GeV/c");
		_thetaPanel = new VariablePanel(UnicodeSupport.SMALL_THETA, 5, 40, "deg");
		_phiPanel = new VariablePanel(UnicodeSupport.SMALL_PHI, -24, 24, "deg");

		panel.add(_pPanel);
		panel.add(_thetaPanel);
		panel.add(_phiPanel);


		return panel;
	}

}
