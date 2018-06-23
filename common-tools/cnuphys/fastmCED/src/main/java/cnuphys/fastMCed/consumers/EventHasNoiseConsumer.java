package cnuphys.fastMCed.consumers;

import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.fastMCed.snr.SNRManager;
import cnuphys.fastMCed.streaming.StreamProcessStatus;
import cnuphys.fastMCed.streaming.StreamReason;

/**
 * This is a test consumer that will flag an event that SNR determined has noise
 * @author heddle
 *
 */
public class EventHasNoiseConsumer extends PhysicsEventConsumer {

	@Override
	public String getConsumerName() {
		return "Find Noise Test Consumer";
	}

	@Override
	public void streamingChange(StreamReason reason) {
		//ignore
	}

	@Override
	public StreamProcessStatus streamingPhysicsEvent(PhysicsEvent event) {
		if (SNRManager.getInstance().getNoiseCount() > 0) {
			return StreamProcessStatus.FLAG;
		}
		else {
			return StreamProcessStatus.CONTINUE;
		}
	}

	@Override
	public void newPhysicsEvent(PhysicsEvent event) {
		System.err.println(getConsumerName() + "  got a new event via next event");

	}

	@Override
	public String flagExplanation() {
		return "Found an event with noise.";
	}

}