package cnuphys.fastMCed.consumers;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.util.SerialIO;
import cnuphys.fastMCed.eventgen.random.RandomEventGenerator;
import cnuphys.fastMCed.eventio.PhysicsEventManager;
import cnuphys.fastMCed.fastmc.ParticleHits;
import cnuphys.fastMCed.snr.ReducedParticleRecord;
import cnuphys.fastMCed.snr.SNRDictionary;
import cnuphys.fastMCed.snr.SNRManager;
import cnuphys.fastMCed.streaming.StreamProcessStatus;
import cnuphys.fastMCed.streaming.StreamReason;
import cnuphys.lund.GeneratedParticleRecord;
import cnuphys.snr.ExtendedWord;
import cnuphys.snr.NoiseReductionParameters;

public class SNRSector1TestConsumer extends PhysicsEventConsumer {

	private SNRManager snr = SNRManager.getInstance();
	private NoiseReductionParameters params[] = new NoiseReductionParameters[6];
	private ExtendedWord rightSeg[] = new ExtendedWord[6];

	private String errStr = "???";

	private SNRDictionary _dictionary = new SNRDictionary(393241);

	@Override
	public String getConsumerName() {
		// TODO Auto-generated method stub
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
		boolean good = true;
		for (int supl0 = 0; supl0 < 6; supl0++) {
			params[supl0] = snr.getParameters(0, supl0);
			rightSeg[supl0] = params[supl0].getRightSegments();
			if (rightSeg[supl0].isZero()) {
				good = false;
				break;
			}
		}

		if (good) {
			GeneratedParticleRecord gpr = particleHits.get(0).getGeneratedParticleRecord();

			String hash = hashKey();
			_dictionary.put(hash, new ReducedParticleRecord(gpr));
		}
		return StreamProcessStatus.CONTINUE;
	}

	@Override
	public void newPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {
		if (PhysicsEventManager.getInstance().getEventGenerator() instanceof RandomEventGenerator) {

			boolean good = true;
			for (int supl0 = 0; supl0 < 6; supl0++) {
				params[supl0] = snr.getParameters(0, supl0);
				rightSeg[supl0] = params[supl0].getRightSegments();
				if (rightSeg[supl0].isZero()) {
					good = false;
					break;
				}
			}

			if (good) {
				String hash = hashKey();
				ReducedParticleRecord rpr = _dictionary.get(hash);
				if (rpr != null) {
					System.err.println("FOUND REC");
					System.err.println(String.format("%d (%-6.2f, %-6.2f, %-6.2f) (%-6.2f, %-6.2f, %-6.2f) ",
							rpr.charge, rpr.xo, rpr.yo, rpr.zo, rpr.p, rpr.theta, rpr.phi));
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

	private String hashKey() {
		StringBuilder sb = new StringBuilder(128);
		
//		ExtendedWord orWord = new ExtendedWord(112);
//		
//		for (int supl0 = 0; supl0 < 6; supl0 = supl0 + 5) {
//			ExtendedWord.bitwiseOr(rightSeg[supl0], orWord, orWord);
//		}
//		sb.append(Long.toHexString(orWord.getWords()[0]));
//		sb.append('$');
//		sb.append(Long.toHexString(orWord.getWords()[1]));
		
		for (int supl0 = 0; supl0 < 6; supl0++) {
			for (long word : rightSeg[supl0].getWords()) {
				if (sb.length() > 0) {
					sb.append("$");
				}
				if (word != 0) {
					sb.append(Long.toString(word, 16));
				}
			}
		}
		
		
		return sb.toString();
	}

}
