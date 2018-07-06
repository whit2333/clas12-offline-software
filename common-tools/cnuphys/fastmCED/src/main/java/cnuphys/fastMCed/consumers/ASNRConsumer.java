package cnuphys.fastMCed.consumers;

import java.io.File;

import cnuphys.bCNU.magneticfield.swim.ISwimAll;
import cnuphys.bCNU.util.Environment;
import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.snr.SNRDictionary;
import cnuphys.fastMCed.snr.SNRManager;
import cnuphys.lund.TrajectoryRowData;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.Solenoid;
import cnuphys.magfield.Torus;

public abstract class ASNRConsumer extends PhysicsEventConsumer {

	protected String errStr = "???";

	protected SNRDictionary _dictionary;

	protected SNRManager snr = SNRManager.getInstance();

	@Override
	public String flagExplanation() {
		return errStr;
	}


	protected void loadOrCreateDictionary() {
		double torusScale = 0;
		double solenoidScale = 0;
		boolean useTorus = MagneticFields.getInstance().hasTorus();
		boolean useSolenoid = MagneticFields.getInstance().hasSolenoid();
		if (useTorus) {
			Torus torus = MagneticFields.getInstance().getTorus();
			torusScale = (torus == null) ? 0 : torus.getScaleFactor();
		}
		if (useSolenoid) {
			Solenoid solenoid = MagneticFields.getInstance().getSolenoid();
			solenoidScale = (solenoid == null) ? 0 : solenoid.getScaleFactor();
		}
		String fileName = SNRDictionary.getFileName(useTorus, torusScale, useSolenoid, solenoidScale);

		String dirPath = Environment.getInstance().getHomeDirectory() + "/dictionaries";

		File file = new File(dirPath, fileName);
		System.err.println("Dictionary file: [" + file.getPath() + "]");
		if (file.exists()) {
			System.err.println("Found dictionary file");
			_dictionary = SNRDictionary.read(dirPath, fileName);

			System.err.println("Number of keys: " + _dictionary.size());
		}

		if (_dictionary == null) {
			_dictionary = new SNRDictionary(useTorus, torusScale, useSolenoid, solenoidScale);
		}
	}
	
	//get the truth information
	protected TrajectoryRowData getTruth() {
		ISwimAll allSwimmer = PhysicsEventManager.getInstance().getAllSwimmer();
		
		TrajectoryRowData trajData = allSwimmer.getRowData().firstElement();
		return trajData;
	}

}
