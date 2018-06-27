package cnuphys.fastMCed.eventgen.sweep;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.dialog.DialogUtilities;
import cnuphys.bCNU.graphics.ImageManager;
import cnuphys.fastMCed.eventgen.IEventSource;

/**
 * A dialog for generating random events. The events can have up to four particles
 * 
 * @author heddle
 *
 */
public class SweepEvGenDialog extends JDialog implements ActionListener, IEventSource {

	private static String OKSTR = "OK";
	private static String CANCELSTR = "Cancel";
	
	//steps for each variable
	int steps[] = new int[6];
	
	// the reason the dialog closed.
	private int reason;

	// convenient access to south button panel
	private JPanel buttonPanel;
	
	//total number of steps
	private JLabel _totalLabel;
	
	//the particle panels
	private ParticleSweepPanel ppanel;
	
	//to handle the indexing
	private Odometer odometer;
	
	/**
	 * Create a random event generator
	 * 
	 * @param parent
	 *            the parent frame
	 * @param maxNum the max number of particles
	 */
	public SweepEvGenDialog(JFrame parent) {
		super(parent, "Sweep Event Generator", true);

		// close is like a close
		WindowAdapter wa = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				reason = DialogUtilities.CANCEL_RESPONSE;
				setVisible(false);
			}
		};
		addWindowListener(wa);
		setLayout(new BorderLayout(8, 8));
		

		setIconImage(ImageManager.cnuIcon.getImage());
		// add components
		createSouthComponent(OKSTR, CANCELSTR);
		createCenterComponent();

		_totalLabel = new JLabel("Total number sweep steps:                  ");
		add(_totalLabel, BorderLayout.NORTH);
		
		pack();

		// center the dialog
		DialogUtilities.centerDialog(this);

		fix();
	}

	public void fix() {
		if (_totalLabel != null) {
			_totalLabel.setText("Total number sweep steps: " + totalSteps());
		}
	}

	/**
	 * Get the reason the dialog closed, either DialogUtilities.CANCEL_RESPONSE
	 * or DialogUtilities.OK_RESPONSE
	 * 
	 * @return reason the dialog closed
	 */
	public int getReason() {
		return reason;
	}
		
	// the total number of steps for the sweep
	public long totalSteps() {
		
		if (ppanel == null) {
			return 0;
		}
		return ppanel.getSteps(steps);
	}

	/**
	 * Override to create the component that goes in the center. Usually this is
	 * the "main" component.
	 *
	 * @return the component that is placed in the center
	 */
	
	protected void createCenterComponent() {
		ppanel = new ParticleSweepPanel(this, true, 11);
		add(ppanel, BorderLayout.CENTER);
	}

	/**
	 * Override to create the component that goes in the south.
	 *
	 * @return the component that is placed in the south. The default
	 *         implementation creates a row of closeout buttons.
	 */
	protected void createSouthComponent(String... closeout) {
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPanel.add(Box.createHorizontalGlue());

		int lenm1 = closeout.length - 1;
		for (int index = 0; index <= lenm1; index++) {
			JButton button = new JButton(closeout[index]);
			button.setName("simpleDialog" + closeout[index]);
			button.setActionCommand(closeout[index]);
			button.addActionListener(this);
			buttonPanel.add(button);
			if (index != lenm1) {
				buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
			}
		}

		add(buttonPanel, BorderLayout.SOUTH);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		reason = e.getActionCommand().equals(CANCELSTR) ? DialogUtilities.CANCEL_RESPONSE : DialogUtilities.OK_RESPONSE;
		setVisible(false);
	}
	@Override
	public PhysicsEvent getEvent() {
		PhysicsEvent  event = new PhysicsEvent();
		
		if (odometer == null) {
			totalSteps();
			odometer = new Odometer(steps[0], steps[1], steps[2], steps[3], steps[4], steps[5]);
		}
		else {
			odometer.increment();
		}
		
		
		Particle p = ppanel.createParticle(odometer.x, odometer.y, odometer.z, odometer.p, odometer.theta, odometer.phi);
		event.addParticle(p);
		
		return event;
	}


	public static void main(String arg[]) {
		SweepEvGenDialog dialog = new SweepEvGenDialog(null);
		
		try {
			EventQueue.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					dialog.setVisible(true);
					
					if (dialog.getReason() == DialogUtilities.OK_RESPONSE) {
						System.err.println("OK");
					}
					else {
						System.err.println("Cancelled");
					}
				
					System.exit(1);
				}

			});
		}
		catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
