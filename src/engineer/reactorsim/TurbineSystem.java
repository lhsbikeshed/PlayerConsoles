package engineer.reactorsim;

import java.awt.Rectangle;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;
import common.ConsoleLogger;
import common.HardwareEvent;
import engineer.reactorsim.ReactorManager.ReactorCheck;
import engineer.reactorsim.ReactorSystem.ReactorResource;

public class TurbineSystem extends ReactorSystem {
	
	//boolean[] powerStates = new boolean[2];
	int testVal = 0;
	float[] temperature = new float[2];
	

	public TurbineSystem() {
		name = "Turbines";
		ReactorResource heat = new ReactorResource();
		heat.typeTag = "HEAT";
		heat.maxAmount = 200;
		resourceStore.put("HEAT", heat);
		
		ReactorResource power = new ReactorResource();
		power.typeTag = "POWER";
		resourceStore.put("POWER", power);
		runningState = new PowerState[2];
		runningState[0] = PowerState.STATE_OFF;
		runningState[1] = PowerState.STATE_OFF;
		
		
	}
	public int getNumberInState(PowerState state){
		int ct = 0;
		for(int i = 0; i < 2; i++){
			if(runningState[i] == state){
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
			
			for(int i = 0; i < 2; i++){
				if(runningState[i] == PowerState.STATE_ON){
					temperature[i] += 0.04f;
					
				} else {
					temperature[i] -= 0.8f;
				}
				
				if(temperature[i] <= 0){
					temperature[i] = 0;
					if(runningState[i] == PowerState.STATE_COOLING){
						runningState[i] = PowerState.STATE_OFF;

					}

				} else if (temperature[i] >= 100f){
					temperature[i] = 100;
					runningState[i] = PowerState.STATE_COOLING;
				}
				
				
			}
		
			resourceStore.get("HEAT").setAmount(temperature[0] + temperature[1]);
		}
		
		
	}
	
	public float consumeResource(String resName, float amount) {
		if(resName.equals("POWER")){
			
			
			ReactorVesselSystem rs = (ReactorVesselSystem) inboundConnections.get("Reactor Vessel");
			if(rs != null){
				//take steam from reactor, add to our pool, then draw from that to make power
				float steamAmount =  getNumberInState(PowerState.STATE_ON) * 1.95f;

				float powerGenerated = rs.consumeResource("STEAM", steamAmount) * .99f;
				
				return powerGenerated;
			}
			return 0;
	
		} else {
			return super.consumeResource(resName, amount);
		}
	}

	@Override
	public void controlSignal(HardwareEvent e) {
		// TODO Auto-generated method stub
		if(e.event == "MOUSECLICK"){
			int mx = (int) (( e.value >> 16 ) - screenPosition.x);
			int my = (int) (( e.value & 65535 ) - screenPosition.y);
			
			PowerState newState  = PowerState.STATE_OFF;
			for (int i = 0; i < 2; i++){
				
				if(my >= i * 30 && my <= i * 30 + 30){
				
					if(runningState[i] == PowerState.STATE_OFF){
						newState = PowerState.STATE_ON;
					} else if(runningState[i] == PowerState.STATE_ON){
						newState= PowerState.STATE_OFF;
					}
				
					if(runningState[i] != PowerState.STATE_COOLING){
						runningState[i] = newState;
					}
				}
			}
			
		}

	}

	@Override
	public void draw(PApplet context) {
		context.pushMatrix();
		context.translate(screenPosition.x, screenPosition.y);
		
		for(int i = 0; i < runningState.length; i++){
			String text = "OFF  ";
			switch (runningState[i]){
			case STATE_ON:
				context.fill(0,255,0);
				text = "ON  ";
				break;
			case STATE_OFF:
				context.fill(255,0,0);
				text = "OFF";
				break;
			case STATE_COOLING:
				context.fill(255,255,0);
				text = "COOLDOWN";
				
			}
			
			text += " " + (int)temperature[i];
			context.rect(0,i * 30, 100, 30);
			context.fill(255);
			context.text(text, 10, 20+i*30);
		}
		
		drawResources(context);
		
		context.popMatrix();
	}
	@Override
	public void applyDamage(float amount) {
		ConsoleLogger.log(this, "TURBINE DAMAGE");
		//damaged turbines overheat quicker?
		
		
	}
	@Override
	public ArrayList<ReactorCheck> checkForProblems() {
		//check that one of the two turbines is turned on
		ArrayList<ReactorCheck> returnProblems = new ArrayList<ReactorCheck>();

		
		if(getNumberInState(PowerState.STATE_ON) == 0 ){
			ReactorCheck r = new ReactorCheck("TURBINE_OFF", false);
			r.setMessage("NO TURBINES RUNNING, NO POWER WILL GENERATE");
			returnProblems.add(r);
		} else {
			ReactorCheck r = new ReactorCheck("TURBINE_OFF", true);
			returnProblems.add(r);
		}
		
		//do temps
		for(int i = 0; i < 2; i++){
			ReactorCheck r;
			if(temperature[i] > 55){
				r = new ReactorCheck("TURBINE_TEMP_" + i, false);
				r.setMessage("TURBINE #" + (i+1) + " RISKS OVERHEATING");
			} else {
				r = new ReactorCheck("TURBINE_TEMP_" + i, true);

			}
			returnProblems.add(r);
		}
		
		
		return returnProblems;
	}

}
