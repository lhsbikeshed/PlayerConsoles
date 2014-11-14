package engineer.reactorsim;

import java.awt.Rectangle;

import processing.core.PApplet;
import processing.core.PVector;
import common.HardwareEvent;
import engineer.reactorsim.ReactorSystem.ReactorResource;

public class TurbineSystem extends ReactorSystem {
	
	boolean[] powerStates = new boolean[2];
	int testVal = 0;
	float[] temperature = new float[2];
	

	public TurbineSystem() {
		name = "Turbines";
		ReactorResource steam = new ReactorResource();
		steam.typeTag = "STEAM";
		resourceStore.put("STEAM", steam);
		
		ReactorResource heat = new ReactorResource();
		heat.typeTag = "HEAT";
		heat.maxAmount = 200;
		resourceStore.put("HEAT", heat);
		
		ReactorResource power = new ReactorResource();
		power.typeTag = "POWER";
		resourceStore.put("POWER", power);
		
		
	}
	public int getNumberInState(boolean state){
		int ct = 0;
		for(int i = 0; i < 2; i++){
			if(powerStates[i] == state){
				ct++;
			}
		}
		return ct;
	}

	@Override
	public void setScreenPosition(PVector screenPosition) {
		this.screenPosition = screenPosition;
		bounds = new Rectangle((int)screenPosition.x, (int)screenPosition.y, 100,60);

	}
	
	@Override
	public void tick(){
		ReactorVesselSystem rs = (ReactorVesselSystem) inboundConnections.get("Reactor Vessel");
		if(rs != null){
			//take steam from reactor, add to our pool, then draw from that to make power
			float steamAmount =  4f;
			float steamConsumed = rs.consumeResource("STEAM", steamAmount);
			resourceStore.get("STEAM").change(steamConsumed);
			
			//now consume from our pool
			steamAmount = getNumberInState(true) * 1.5f;
			steamConsumed = consumeResource("STEAM", steamAmount);
			resourceStore.get("POWER").change(steamConsumed);
			
			
			
			
			float warmAmount = -1f;
			if(steamConsumed > 0){
				for(int i = 0; i < 2; i++){
					if(powerStates[i]){
						temperature[i] += 0.02f;
						
					} else {
						temperature[i] -= 0.8f;
					}
					if(temperature[i] <= 0){
						temperature[i] = 0;
					} else if (temperature[i] >= 100f){
						temperature[i] = 100;
					}
				}
			}
			resourceStore.get("HEAT").setAmount(temperature[0] + temperature[1]);
		}
		
		
	}
	
	

	@Override
	public void controlSignal(HardwareEvent e) {
		// TODO Auto-generated method stub
		if(e.event == "MOUSECLICK"){
			testVal ++;
			testVal %= 4;
			
			powerStates[0] = (testVal & 1) > 0 ? true : false;
			powerStates[1] = (testVal & 2) > 0 ? true : false;

		}

	}

	@Override
	public void draw(PApplet context) {
		context.pushMatrix();
		context.translate(screenPosition.x, screenPosition.y);
		
		for(int i = 0; i < powerStates.length; i++){
			String text = "OFF  ";
			if(powerStates[i]){
				context.fill(0,255,0);
				text = "ON  ";
			} else {
				context.fill(255,0,0);
			}
			text += (int)temperature[i];
			context.rect(0,i * 30, 100, 30);
			context.fill(255);
			context.text(text, 10, 20+i*30);
		}
		
		drawResources(context);
		
		context.popMatrix();
	}
	@Override
	public void applyDamage(float amount) {
		// TODO Auto-generated method stub
		
	}

}
