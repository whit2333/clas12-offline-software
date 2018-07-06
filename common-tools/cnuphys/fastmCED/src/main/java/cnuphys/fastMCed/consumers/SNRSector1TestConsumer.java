package cnuphys.fastMCed.consumers;

import java.io.File;
import java.util.List;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.magneticfield.swim.ISwimAll;
import cnuphys.bCNU.util.Environment;
import cnuphys.fastMCed.eventgen.random.RandomEventGenerator;
import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.fastmc.ParticleHits;
import cnuphys.fastMCed.snr.Dictionary3DPlot;
import cnuphys.fastMCed.snr.SNRDictionary;
import cnuphys.fastMCed.snr.SNRManager;
import cnuphys.fastMCed.streaming.StreamProcessStatus;
import cnuphys.fastMCed.streaming.StreamReason;
import cnuphys.lund.GeneratedParticleRecord;
import cnuphys.lund.TrajectoryRowData;
import cnuphys.lund.TrajectoryTableModel;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.Solenoid;
import cnuphys.magfield.Torus;

public class SNRSector1TestConsumer extends PhysicsEventConsumer {

	private SNRManager snr = SNRManager.getInstance();

	private String errStr = "???";

	private SNRDictionary _dictionary;

	private Dictionary3DPlot _plot3D;

	@Override
	public String getConsumerName() {
		return "SNR Sector 1 Test";
	}

	@Override
	public void streamingChange(StreamReason reason) {
		if (reason == StreamReason.STOPPED) {

			if (_dictionary != null) {
				System.err.println("Num keys " + _dictionary.size());

				File file = _dictionary.write(Environment.getInstance().getHomeDirectory() + "/dictionaries");

				System.err.println("File: [" + file.getPath() + "]  size: " + file.length());
				// make scatter plot
				// makeScatterPlot();
			}
		}
	}

	private void loadOrCreateDictionary() {
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
			_plot3D = Dictionary3DPlot.plotDictionary(_dictionary);
		}

		if (_dictionary == null) {
			_dictionary = new SNRDictionary(useTorus, torusScale, useSolenoid, solenoidScale);
		}
	}

	@Override
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {

		if (snr.segmentsInAllSuperlayers(0, SNRManager.RIGHT)) {
			GeneratedParticleRecord gpr = particleHits.get(0).getGeneratedParticleRecord();

			if (_dictionary == null) {
				loadOrCreateDictionary();
			}

			//test is for sector 1 right leaners only
			String hash = snr.hashKey(0, SNRManager.RIGHT); 
			String gprHash = _dictionary.get(hash);

			// if not there, add
			if (gprHash == null) {
//				System.err.println("Added to dictionary");
				
				gprHash = gpr.hashKey();
				_dictionary.put(hash, gprHash);
				
				// add to plot
				_plot3D.append(gprHash);

			}

		}
		return StreamProcessStatus.CONTINUE;
	}

	@Override
	public void newPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {

		if (_dictionary == null) {
			loadOrCreateDictionary();
		}

		if ((_dictionary != null) && !_dictionary.isEmpty()) {
			if (PhysicsEventManager.getInstance().getEventGenerator() instanceof RandomEventGenerator) {

				//test is for sector 1 right leaners only
				if (snr.segmentsInAllSuperlayers(0, SNRManager.RIGHT)) {
					String hash = snr.hashKey(0, SNRManager.RIGHT); 

					// see if this key is in the dictionary. If it is we'll get
					//  a hash of a GeneratedParticleRec back
					String gprHash = _dictionary.get(hash);

					GeneratedParticleRecord rpr;
					if (gprHash != null) {
						System.err.println("*** Dictionary Match ***");
					} else {
						System.err.println("No dictionary match. Looking for closest");

						String nearestKey = _dictionary.nearestKey(hash);
						System.err.println("COMMON BITS " + SNRDictionary.commonBits(hash, nearestKey));
						gprHash = _dictionary.get(nearestKey);
						System.err.println("Closest Match");
						// add to plot
						_plot3D.append(gprHash);
					}
					
					rpr = GeneratedParticleRecord.fromHash(gprHash);
					System.err.println(rpr.toString());
					TrajectoryRowData trajData = getTruth();
					double dP = Math.abs(trajData.getMomentum() - 1000*rpr.getMomentum());
					double dTheta = Math.abs(trajData.getTheta() - rpr.getTheta());
					double dPhi = Math.abs(trajData.getPhi() - rpr.getPhi());
					double fracdP = dP/trajData.getMomentum();
					System.err.println(String.format("Error dP = %-6.3f MeV dP/P = %-6.3f%% dTheta = %-6.3f deg  dPhi = %-6.3f deg", dP, 100*fracdP, dTheta, dPhi));
				}

			} // random generator
		}
	}

	@Override
	public String flagExplanation() {
		return errStr;
	}
	
	private TrajectoryRowData getTruth() {
		ISwimAll allSwimmer = PhysicsEventManager.getInstance().getAllSwimmer();
		
		TrajectoryRowData trajData = allSwimmer.getRowData().firstElement();
		return trajData;
	}

	// private void makeScatterPlot() {
	// PThetaDialog dialog = new PThetaDialog(null, false, _dictionary);
	// dialog.setVisible(true);
	// }
}
