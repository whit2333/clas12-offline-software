package cnuphys.fastMCed.consumers;

import java.util.List;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.util.SerialIO;
import cnuphys.bCNU.view.PlotView;
import cnuphys.fastMCed.eventgen.PThetaDialog;
import cnuphys.fastMCed.eventgen.random.RandomEventGenerator;
import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.fastmc.ParticleHits;
import cnuphys.fastMCed.frame.FastMCed;
import cnuphys.fastMCed.snr.SNRDictionary;
import cnuphys.fastMCed.snr.SNRManager;
import cnuphys.fastMCed.streaming.StreamProcessStatus;
import cnuphys.fastMCed.streaming.StreamReason;
import cnuphys.lund.GeneratedParticleRecord;

public class SNRSector1TestConsumer extends PhysicsEventConsumer {

	private SNRManager snr = SNRManager.getInstance();

	private String errStr = "???";

	private SNRDictionary _dictionary = new SNRDictionary(393241);

	@Override
	public String getConsumerName() {
		return "SNR Sector 1 Test";
	}

	@Override
	public void streamingChange(StreamReason reason) {
		if (reason == StreamReason.STOPPED) {
			byte[] bytes = SerialIO.serialWrite(_dictionary);
			System.err.println("Num keys " + _dictionary.size());
			System.err.println("serialization size: " + bytes.length);
			
			//make scatter plot
			makeScatterPlot();
		}
	}

	@Override
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {

		if (snr.segmentsInAllSuperlayers(0, SNRManager.RIGHT)) {
			GeneratedParticleRecord gpr = particleHits.get(0).getGeneratedParticleRecord();

			String hash = snr.hashKey(0, SNRManager.RIGHT);  //test is for sector 1 right only
			_dictionary.put(hash, gpr.hashKey());
			
		}
		return StreamProcessStatus.CONTINUE;
	}

	@Override
	public void newPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {
		if (PhysicsEventManager.getInstance().getEventGenerator() instanceof RandomEventGenerator) {


			if (snr.segmentsInAllSuperlayers(0, SNRManager.RIGHT)) {
				String hash = snr.hashKey(0, SNRManager.RIGHT);  //test if for sector 1 right leaners only
				
//				System.err.println("COMMON BITS " + SNRDictionary.commonBits(hash, hash));
				
				//see if this key is in the dictionary. If it is we'll get a
				//hash of a GeneratedParticleRec back
				String gprHash = _dictionary.get(hash);
				
	
				if (gprHash != null) {
					System.err.println("Dictionary Match");
					GeneratedParticleRecord rpr = GeneratedParticleRecord.fromHash(gprHash);
					System.err.println(rpr.toString());
				} else {
					System.err.println("No dictionary match. Looking for closest");
					
					String nearestKey = _dictionary.nearestKey(hash);
					System.err.println("COMMON BITS " + SNRDictionary.commonBits(hash, nearestKey));
					gprHash = _dictionary.get(nearestKey);
					System.err.println("Closest Match");
					GeneratedParticleRecord rpr = GeneratedParticleRecord.fromHash(gprHash);
					System.err.println(rpr.toString());
				}
			}

		}
	}
	

	@Override
	public String flagExplanation() {
		return errStr;
	}


	private void makeScatterPlot() {
		PThetaDialog dialog = new PThetaDialog(null, false, _dictionary);
		dialog.setVisible(true);
	}
}
