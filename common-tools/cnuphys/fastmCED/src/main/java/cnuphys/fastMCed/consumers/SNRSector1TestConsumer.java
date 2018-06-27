package cnuphys.fastMCed.consumers;

import java.util.List;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.util.SerialIO;
import cnuphys.fastMCed.eventgen.random.RandomEventGenerator;
import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.fastmc.ParticleHits;
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
			
		}
	}

	@Override
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {

		if (snr.segmentsInAllSuperlayers(0)) {
			GeneratedParticleRecord gpr = particleHits.get(0).getGeneratedParticleRecord();

			String hash = snr.hashKey(0);  //test if for sector 1 only
			_dictionary.put(hash, gpr.hashKey());
		}
		return StreamProcessStatus.CONTINUE;
	}

	@Override
	public void newPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {
		if (PhysicsEventManager.getInstance().getEventGenerator() instanceof RandomEventGenerator) {


			if (snr.segmentsInAllSuperlayers(0)) {
				String hash = snr.hashKey(0);  //test if for sector 1 only
				
				//see if this key is in the dictionary. If it is we'll get a
				//hash of a GeneratedParticleRec back
				String gprHash = _dictionary.get(hash);
				
	
				if (gprHash != null) {
					System.err.println("FOUND REC");
					GeneratedParticleRecord rpr = GeneratedParticleRecord.fromHash(gprHash);
					System.err.println(String.format("%d (%-6.2f, %-6.2f, %-6.2f) (%-6.2f, %-6.2f, %-6.2f) ",
							rpr.getCharge(),
							rpr.getVertexX(), rpr.getVertexY(), rpr.getVertexX(), 
							rpr.getMomentum(), rpr.getTheta(), rpr.getPhi()));
				} else {
					System.err.println("NO REC");
				}
			}

		}
	}
	

	@Override
	public String flagExplanation() {
		return errStr;
	}


}
