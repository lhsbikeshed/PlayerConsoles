package engineer.reactorsim;

import java.awt.Rectangle;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;
import common.ConsoleLogger;
import common.HardwareEvent;
import common.ShipState;
import engineer.reactorsim.ReactorManager.ReactorCheck;
import engineer.reactorsim.ReactorSystem.ReactorResource;

public class FuelTankSystem extends ReactorSystem {

	float[] flowRates = { 0.0f, 0.0f};
	float leakRate = 0.0f;
	float leakHealth = 0.0f;
	float fuelRatio = 0f;
	
	ReactorResource fuel;
	
	public FuelTankSystem() {
		name = "Fuel Tank";
		fuel = new ReactorResource();
		fuel.typeTag = "FUEL";
		fuel.maxAmount = 150000f;
		fuel.setAmount(100000f);
		resourceStore.put("FUEL", fuel);
		
	}
	
	@Override
	public void setScreenPosition(PVector pos){
		this.screenPosition = pos;
		bounds = new Rectangle((int)screenPosition.x, (int)screenPosition.y, 200,200);

	}

	@Override
	public void controlSignal(HardwareEvent e) {
		if(e.event == "MOUSECLICK"){
			int mx = e.value >> 16;
			int my = e.value & 65535;
			if (mx < screenPosition.x + 100){
				if(my > screenPosition.y + 50){
					flowRates[0] -= 1;
				
				} else {
					flowRates[0] += 1;
				}
			} else if (mx > screenPosition.x + 100){
				if(my > screenPosition.y + 50){
					flowRates[1] -= 1;
				
				} else {
					flowRates[1] += 1;
				}
			} 
		}

	}

	@Override
	public void tick(){
		super.tick();
		if(leakRate > 0.0f){
			leakHealth += ShipState.instance.powerStates[ShipState.POWER_DAMAGE] / 100.0f + 0.01f;
			if(leakHealth > 10.0f){
				leakRate = 0.0f;
			}
			
			resourceStore.get("FUEL").change(-leakRate);
		}
	}
	
	@Override
	public void draw(PApplet context) {
		context.pushMatrix();
		context.translate(screenPosition.x, screenPosition.y);
		context.fill(0,0,0);
		context.rect(0, 0, 200,100);
		context.fill(0,255,0);
		float w = PApplet.map(fuel.getAmount(), 0, fuel.maxAmount, 0, 200);
		context.rect(0,0,w,100);
		context.fill(255);
		context.text(flowRates[0], 20, 50);
		context.text(flowRates[1], 120, 50);
		
		if(leakRate > 0.0f){
			context.text("LEAKING : " + leakHealth, 0, -20);
		}
		fuelRatio = flowRates[0] / flowRates[1];

		context.text("fuel ratio: " + fuelRatio + "  (keep this at 0.8)", 0, 120);
		context.popMatrix();
	}
	
	public float consumeResource(String resName, float amount){
		ReactorResource res = resourceStore.get(resName);
		if(res == null){
			return 0;
		}
		
		float rateModifier = (flowRates[0] + flowRates[1]) / 20f;
		amount *= rateModifier;
		
		//now modify based on ratio of one to the other
		amount *= 1f - Math.abs((0.8f - fuelRatio));
		//ConsoleLogger.log(this, "ratio: " + ratio);
		
		float amt = (res.getAmount() - amount );
		if(amt <= 0 ){
			amt = amount + amt;
			//in this case may want to punish this system for trying to draw too much
			//of a resource
		} else {
			amt = amount;
		}
		res.change(-amount);
		

		return amt;

}


	@Override
	public void applyDamage(float amount) {
		leakRate = 10.0f;
		leakHealth = 0.0f;
	}

	@Override
	public ArrayList<ReactorCheck> checkForProblems() {
		// TODO Auto-generated method stub
		return null;
	}

}
