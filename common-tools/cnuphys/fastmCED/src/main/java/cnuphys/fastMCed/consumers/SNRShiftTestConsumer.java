package cnuphys.fastMCed.consumers;

import java.util.List;

import org.jlab.clas.physics.PhysicsEvent;
import org.jlab.geom.DetectorId;

import cnuphys.fastMCed.fastmc.HitHolder;
import cnuphys.fastMCed.fastmc.ParticleHits;
import cnuphys.fastMCed.snr.SNRManager;
import cnuphys.fastMCed.streaming.StreamProcessStatus;
import cnuphys.fastMCed.streaming.StreamReason;

public class SNRShiftTestConsumer extends PhysicsEventConsumer {
	
	private SNRManager snr = SNRManager.getInstance();

	String _errStr = "???";

	@Override
	public String getConsumerName() {
		return "SNR Shift Test";
	}

	@Override
	public void streamingChange(StreamReason reason) {
		if (reason == StreamReason.STOPPED) {
		}
	}

	@Override
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {
		HitHolder hholder = particleHits.get(0).getHitHolder(DetectorId.DC);

		for (int supl0 = 0; supl0 < 6; supl0++) {
			if (hholder.sectorUniqueLayerCount(0) > 34) {
				if (hholder.superLayerUniqueLayerCount(0, supl0) > 4) {
					if (!snr.segmentInSuperlayer(0, supl0)) {
						_errStr = "Did not find segments in superlayer " + (supl0 + 1) + " as expected";
						return StreamProcessStatus.FLAG;
					}
				}
			}
		}

		return StreamProcessStatus.CONTINUE;
	}

	@Override
	public void newPhysicsEvent(PhysicsEvent event, List<ParticleHits> particleHits) {
		HitHolder hholder = particleHits.get(0).getHitHolder(DetectorId.DC);
		System.err.println("number unique layers hit in sector 1 = " + hholder.sectorUniqueLayerCount(0));

	}

	@Override
	public String flagExplanation() {
		return _errStr;
	}

}
