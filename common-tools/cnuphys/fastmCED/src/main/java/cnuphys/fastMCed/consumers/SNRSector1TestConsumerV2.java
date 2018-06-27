package cnuphys.fastMCed.consumers;

import java.util.ArrayList;
import java.util.Collections;
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
import cnuphys.fastMCed.snr.SegmentSet;
import cnuphys.fastMCed.streaming.StreamProcessStatus;
import cnuphys.fastMCed.streaming.StreamReason;
import cnuphys.lund.GeneratedParticleRecord;
import cnuphys.snr.ExtendedWord;
import cnuphys.snr.NoiseReductionParameters;

public class SNRSector1TestConsumerV2 extends PhysicsEventConsumer {

	private SNRManager snr = SNRManager.getInstance();
	private NoiseReductionParameters params[] = new NoiseReductionParameters[6];
	private ExtendedWord rightSeg[] = new ExtendedWord[6];

	private String errStr = "???";
	private ArrayList<SegmentSet> roads = new ArrayList<SegmentSet>();

	@Override
	public String getConsumerName() {
		// TODO Auto-generated method stub
		return "SNR Sector 1 Test V2";
	}

	@Override
	public void streamingChange(StreamReason reason) {
		if (reason == StreamReason.STOPPED) {
			byte[] bytes = SerialIO.serialWrite(roads);
			System.err.println("Num roads " + roads.size());
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
			SegmentSet sset = new SegmentSet(rightSeg[0], rightSeg[1], rightSeg[2], rightSeg[3], rightSeg[4],
					rightSeg[5]);

			int index = Collections.binarySearch(roads, sset);

			if (roads.isEmpty()) {
				sset.setReducedParticleRecord(new ReducedParticleRecord(gpr));
				roads.add(sset);
			} else {

				if (index >= 0) { // duplicate
					// System.err.println("duplicate index");
				} else {
					index = -(index + 1); // now the insertion point.
					sset.setReducedParticleRecord(new ReducedParticleRecord(gpr));
					roads.add(index, sset);
				}
			}

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
				GeneratedParticleRecord gpr = particleHits.get(0).getGeneratedParticleRecord();
				SegmentSet sset = new SegmentSet(rightSeg[0], rightSeg[1], rightSeg[2], rightSeg[3], rightSeg[4],
						rightSeg[5]);
				int index = Collections.binarySearch(roads, sset);
				if (index >= 0) { // found match
					System.err.println("found match");
					ReducedParticleRecord rpr = roads.get(index).getReducedParticleRecord();
					System.err.println(String.format("%d (%-6.2f, %-6.2f, %-6.2f) (%-6.2f, %-6.2f, %-6.2f) ",
							rpr.charge, rpr.xo, rpr.yo, rpr.zo, rpr.p, rpr.theta, rpr.phi));


				} else {
					index = -(index + 1); // now the insertion point.
					index = index - 1; // nearest entry
					if (index < 0) {
						index = 0;
					}
					System.err.println("found closest");
					ReducedParticleRecord rpr = roads.get(index).getReducedParticleRecord();
					System.err.println(String.format("%d (%-6.2f, %-6.2f, %-6.2f) (%-6.2f, %-6.2f, %-6.2f) ",
							rpr.charge, rpr.xo, rpr.yo, rpr.zo, rpr.p, rpr.theta, rpr.phi));

				}

			}

		}
	}

	@Override
	public String flagExplanation() {
		return errStr;
	}

}
