package common.util;

import processing.core.PVector;

/* store a Vectr and update witha  given timestamp. 
 * Retrieving the value smooths between the last two updates
 */
public class LerpedVector {

	long updateTime = 0;
	PVector val = new PVector(0,0,0);
	PVector prevVal = new PVector(0,0,0);
	float rate = 250;
	
	
	public LerpedVector(PVector startVal, long startTime, float updateRate) {
		val = startVal;
		prevVal = startVal;
		this.updateTime = startTime;
		this.rate = updateRate;
	}
	
	
	
	public void update(PVector valNew, long updateTime){
		prevVal = val;
		this.val = valNew;
		this.updateTime = updateTime;
	}
	
	public PVector getValue(long currentTime){
		return PVector.lerp(prevVal, val, (currentTime - updateTime) / rate);
	}
	
	

}
