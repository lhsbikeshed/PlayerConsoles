package common.util;

public class LerpedRot {
	long updateTime = 0;
	Rot val = Rot.IDENTITY;
	Rot prevVal = Rot.IDENTITY;
	float rate = 250;
	
	public LerpedRot(Rot startVal, long startTime, float updateRate) {
		val = startVal;
		prevVal = startVal;
		this.updateTime = startTime;
		this.rate = updateRate;
	}
	
	
	
	public void update(Rot valNew, long updateTime){
		prevVal = val;
		this.val = valNew;
		this.updateTime = updateTime;
	}
	
	public Rot getValue(long currentTime){
		
		return Rot.slerp(prevVal, val, (currentTime - updateTime) / rate, false);
	}
	
	
}
