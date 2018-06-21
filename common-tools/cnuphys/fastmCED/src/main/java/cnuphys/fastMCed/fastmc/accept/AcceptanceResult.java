package cnuphys.fastMCed.fastmc.accept;

public class AcceptanceResult {

	public final AcceptanceStatus status;
	public final ACondition condition;
	
	/**
	 * A result of an acceptance test
	 * @param status the status of the test
	 * @param condition if failed, this is the first condition tested that failed
	 */
	public AcceptanceResult(AcceptanceStatus status, ACondition condition) {
		this.status = status;
		this.condition = condition;
	}
}
