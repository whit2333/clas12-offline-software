package cnuphys.swimS;

import cnuphys.rk4.IStopper;

public class ZStopper implements IStopper {
	
	private double _targetZ;
	private boolean _normalDirection;
	private double _totalPathLength;
	private double _maxS;
	private double _accuracy;
	private double _currentZ = Double.NaN;
	
	/**
	 * Z stopper that doesn't check max R (does check max path length)
	 * @param s0 starting path length in meters
	 * @param sMax maximal path length in meters
	 * @param targetZ stopping Z in meters
	 * @param accuracy the accuracy in meters
	 * @param normalDirection <code></code> if going smaller to larger z
	 */
	public ZStopper(double s0, double sMax, double targetZ, double accuracy, boolean normalDirection) {
		_targetZ = targetZ;
		_totalPathLength = s0;
		_maxS = sMax;
		_normalDirection = normalDirection;
		_accuracy = accuracy;
	}


	@Override
	public boolean stopIntegration(double s, double[] u) {
		
		_currentZ = u[2];
		_totalPathLength = s;
		
		//independent variable s is the path length
		if (s > _maxS) {
			return true;
		}
		
		//within accuracy?
		if (Math.abs(_currentZ - _targetZ) < _accuracy) {
			return true;
		}
		
		
		if (_normalDirection) {
			if (_currentZ > _targetZ) {
				return true;
			}
		}
		else {
			if (_currentZ < _targetZ) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Get the final path length in meters
	 * 
	 * @return the final path length in meters
	 */
	@Override
	public double getFinalT() {
		return _totalPathLength;
	}

	/**
	 * Is the current z within accuracy
	 * @param z current z
	 * @param accuracy accuracy
	 * @return <code>true</code> if current z with accuracy
	 */
	public boolean withinAccuracy(double z, double accuracy) {
		return Math.abs(z-_targetZ) < accuracy;
	}

	@Override
	public void setFinalT(double finalT) {
		// Do nothing
	}
	
	/**
	 * Generally this is the same as stop integration. So most
	 * will just return stopIntegration(). But sometimes
	 * stop just means we reset and integrate more. For example, with this
	 * fixed Z integrator we "stop" when we cross the z boundary however
	 * we are not done unless we are within tolerance. If we are within
	 * tolerance (on either side) we are really done! 
	 * @param t
	 *            the current value of the independent variable (typically
	 *            pathlength)
	 * @param y
	 *            the current state vector (x, y, z, px/p, py/p, pz/p)
	 * @return <code>true</code> if we should stop now.
	 */
	@Override
	public boolean terminateIntegration(double t, double y[]) {
		return Math.abs(_currentZ - _targetZ) < _accuracy;
	}
	
	public void setS0(double s0) {
		_totalPathLength = s0;
	}
	
	public void setSMax(double sMax) {
		_maxS = sMax;
	}
	
	public void setTargetZ(double targetZ) {
		_targetZ = targetZ;
	}
	
	public void setAccuracy(double accuracy) {
		_accuracy = accuracy;
	}
	
	public void setNormalDirection(boolean normalDirection) {
		_normalDirection = normalDirection;
	}


}
