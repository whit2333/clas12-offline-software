package cnuphys.fastMCed.eventgen.random;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;
import java.util.Random;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class VariablePanel extends JPanel implements FocusListener, KeyListener {

	protected static NumberFormat numberFormat = NumberFormat.getNumberInstance();
	static {
		numberFormat.setMaximumFractionDigits(6);
		numberFormat.setMinimumFractionDigits(0);
	}

	private final String _name;
	private final String _units;

	// the text fields
	private JFormattedTextField _minValTF;
	private JFormattedTextField _maxValTF;
	String oldMinText = "";
	String oldMaxText = "";

	// the current values
	private double _minValue;
	private double _maxValue;
	private double _del;
	
	private static int MINTFWIDTH = 80;

	public VariablePanel(final String name, double minVal, double maxVal, final String units) {
		setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));

		_minValTF = new JFormattedTextField(numberFormat);
		_maxValTF = new JFormattedTextField(numberFormat);
		
		Dimension minDim =_minValTF.getPreferredSize();
		minDim.width = MINTFWIDTH;
		_minValTF.setMinimumSize(minDim);
		_minValTF.setPreferredSize(minDim);
		_maxValTF.setMinimumSize(minDim);
		_maxValTF.setPreferredSize(minDim);

		_minValue = minVal;
		_maxValue = maxVal;

		_name = name;
		_units = units;

		_del = _maxValue - _minValue;

		_minValTF.setText("" + _minValue);
		_maxValTF.setText("" + _maxValue);

		_minValTF.addFocusListener(this);
		_maxValTF.addKeyListener(this);

		add(new JLabel(name));
		add(_minValTF);
		add(new JLabel("to"));
		add(_maxValTF);
		add(new JLabel(units));
	}

	// See if the min value has changed.
	protected void checkMinTextChange() {

		try {
			String newText = _minValTF.getText();
			if (newText == null) {
				newText = "";
			}

			if (oldMinText.compareTo(newText) != 0) {
				try {
					_minValue = Double.parseDouble(newText);
					_del = _maxValue - _minValue;
					oldMinText = newText;
					System.err.println(toString());
				} catch (NumberFormatException e) {
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// See if the max value has changed.
	protected void checkMaxTextChange() {

		try {
			String newText = _maxValTF.getText();
			if (newText == null) {
				newText = "";
			}

			if (oldMaxText.compareTo(newText) != 0) {

				try {
					_maxValue = Double.parseDouble(newText);
					_del = _maxValue - _minValue;
					oldMaxText = newText;
					System.err.println(toString());
				} catch (NumberFormatException e) {
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void focusGained(FocusEvent e) {
	}

	@Override
	public void focusLost(FocusEvent e) {
		checkMinTextChange();
		checkMaxTextChange();
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		checkMinTextChange();
		checkMaxTextChange();
	}
	
	/**
	 * Get the minimum value
	 * @return the minimum value
	 */
	public double getMinimumValue() {
		return _minValue;
	}

	/**
	 * Get the maximum value
	 * @return the minimum value
	 */
	public double getMaximumValue() {
		return _maxValue;
	}

	/**
	 * Generate a random value in range
	 * 
	 * @param rand
	 *            the generator
	 * @return a random value in range
	 */
	public double randomValue(Random rand) {
		return _minValue + _del * rand.nextDouble();
	}

	@Override
	public String toString() {
		return _name + " " + _minValue + " to " + _maxValue + " " + _units;
	}
}
