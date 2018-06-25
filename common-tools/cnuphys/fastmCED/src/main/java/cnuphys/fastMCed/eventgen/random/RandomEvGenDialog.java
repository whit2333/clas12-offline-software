package cnuphys.fastMCed.eventgen.random;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import cnuphys.bCNU.dialog.DialogUtilities;
import cnuphys.bCNU.dialog.VerticalFlowLayout;
import cnuphys.bCNU.graphics.ImageManager;

/**
 * A dialog for generating random events
 * 
 * @author heddle
 *
 */
public class RandomEvGenDialog extends JDialog implements IEventGenerator, ActionListener {

	private static String OKSTR = "OK";
	private static String CANCELSTR = "Cancel";
	
	//electron, proton, gamma
	private static int lundIds[] = {11, 2212, 22, -11};

	// the reason the dialog closed.
	private int reason;

	//random number generator
	private Random rand;
	
	// convenient access to south button panel
	private JPanel buttonPanel;
	
	//the particle panels
	private ParticlePanel[] ppanels;

	/**
	 * Create a random event generator
	 * 
	 * @param parent
	 *            the parent frame
	 * @param maxNum the max number of particles
	 */
	public RandomEvGenDialog(JFrame parent, int maxNum, long seed) {
		super(parent, "Random Event Generator", true);

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
		
		//random generator
		if (seed < 1) {
			rand = new Random();
		}
		else {
			rand = new Random(seed);
		}

		setIconImage(ImageManager.cnuIcon.getImage());
		// add components
		createSouthComponent(OKSTR, CANCELSTR);
		createCenterComponent(maxNum);

		pack();

		// center the dialog
		DialogUtilities.centerDialog(this);

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

	/**
	 * Override to create the component that goes in the center. Usually this is
	 * the "main" component.
	 *
	 * @return the component that is placed in the center
	 */
	
	protected void createCenterComponent(int maxNum) {
		JPanel panel = new JPanel();
		
		ppanels = new ParticlePanel[maxNum];
		panel.setLayout(new VerticalFlowLayout());
		
		for (int i = 0; i < maxNum; i++) {
			ppanels[i] = new ParticlePanel(true, lundIds[i % lundIds.length]);
			panel.add(ppanels[i]);
		}
		add(panel, BorderLayout.CENTER);
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

	public static void main(String arg[]) {
		RandomEvGenDialog dialog = new RandomEvGenDialog(null, 4, -1L);
		
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
