package cnuphys.fastMCed.fastmc.accept;


import java.util.Vector;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.eventio.IPhysicsEventListener;

public class AcceptanceManager {
	
	// list of conditions that are added together
	private static Vector<ACondition> _conditions = new Vector<ACondition>();
		
	private static ElectronCondition _e36Condition;
	private static ProtonCondition _p36Condition;

	private static AcceptanceManager _instance;
	
	private static AcceptanceResult nullResult = new AcceptanceResult(AcceptanceStatus.NULL, null);
	private static AcceptanceResult acceptedResult = new AcceptanceResult(AcceptanceStatus.ACCEPTED, null);
	
	private AcceptanceManager() {
	}

	public ACondition getElectronCondition() {
		return _e36Condition;
	}
	
	public ACondition getProtonCondition() {
		return _p36Condition;
	}
	
	/**
	 * Access to the singleton AcceptanceManager
	 * 
	 * @return the AcceptanceManager
	 */
	public static AcceptanceManager getInstance() {
		if (_instance == null) {
			_instance = new AcceptanceManager();

			_e36Condition = new ElectronCondition(36, false);
			_p36Condition = new ProtonCondition(36, false);
			
			// default condition is 36 hits for electrons
			_conditions.add(_e36Condition);
			_conditions.add(_p36Condition);
		}
		return _instance;
	}


	/**
	 * Check whether the current event is accepted
	 * 
	 */
	public AcceptanceResult testEvent(PhysicsEvent event) {
		if (event == null) {
			return nullResult;
		}

		if ((_conditions == null) || _conditions.isEmpty()) {
			return acceptedResult;
		}

		for (ACondition condition : _conditions) {
			if (condition.isActive() && !condition.pass()) {
				return new AcceptanceResult(AcceptanceStatus.NOTACCEPTED, condition);
			}
		}

		return acceptedResult;
	}

}