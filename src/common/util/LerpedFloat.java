package common.util;

/* store a float and update witha  given timestamp. 
 * Retrieving the value smooths between the last two updates
 */
public class LerpedFloat {

	long updateTime = 0;
	float val = 0;
	float prevVal = 0;
	float rate = 250;
	
	
	public LerpedFloat(float startVal, long startTime, float updateRate) {
		val = startVal;
		prevVal = startVal;
		this.updateTime = startTime;
		this.rate = updateRate;
	}
	
	
	
	public void update(float valNew, long updateTime){
		prevVal = val;
		this.val = valNew;
		this.updateTime = updateTime;
	}
	
	public float getValue(long currentTime){
		return lerp(prevVal, val, (currentTime - updateTime) / rate);
	}
	
	private float lerp(float start, float stop, float amt) {
	    return start + (stop-start) * amt;
	}

}
