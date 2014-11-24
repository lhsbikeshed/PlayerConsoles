package engineer.reactorsim;

import java.awt.Rectangle;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;
import common.ConsoleLogger;
import common.HardwareEvent;
import engineer.reactorsim.ReactorManager.ReactorCheck;

public class CoolantMixerSystem extends ReactorSystem {

	boolean powerState = false;
	
	
	int lowCoolantTime = 0;	//how many ticks has the coolant pressure been low for?
	
	
	public CoolantMixerSystem() {
		// TODO Auto-generated constructor stub
		ReactorResource heat = new ReactorResource();
		heat.typeTag = "HEAT";
		ReactorResource coolant = new ReactorResource();
		coolant.typeTag = "COOLANT";
		coolant.maxAmount = 500f;
		
		resourceStore.put("HEAT", heat);
		resourceStore.put("COOLANT", coolant);
		name = "Coolant mixer";
	}
	public void setScreenPosition(PVector screenPosition) {
		this.screenPosition = screenPosition;
		bounds = new Rectangle((int)screenPosition.x, (int)screenPosition.y, 100,30);

	}
	
	/* generate coolant if turned on, if off or in cooldown state then drop pressure and heat
	 * 
	 */
	@Override
	public void tick(){
		super.tick();
		ReactorResource heat = resourceStore.get("HEAT");
		ReactorResource coolant = resourceStore.get("COOLANT");
		if(runningState[0] == PowerState.STATE_ON){
			//when turned on (i.e. mixing coolant) increase the heat (implying the mixer gets hot) 
			//and increase the coolant gen rate			
			heat.amountPerTick = 0.08f;			
			coolant.amountPerTick = 30f;
			if(coolant.getAmount() <= 10.01f){
				heat.amountPerTick += 1.08f;		
			}
			
		} else if (runningState[0] == PowerState.STATE_OFF){
			heat.amountPerTick = -1f;
			coolant.amountPerTick = 0f;
		
		} else if (runningState[0] == PowerState.STATE_COOLING){
			heat.amountPerTick = -4f;
			coolant.amountPerTick = 0f;
			if (heat.getAmount() <= 5.1f){
				runningState[0] = PowerState.STATE_OFF;
			}
		}
		//check to see if we overheated. If we did then go into cooldown state
		if(heat.getAmount() >= heat.maxAmount){
			runningState[0] = PowerState.STATE_COOLING;
			
		}
	}

	@Override
	public void controlSignal(HardwareEvent e) {
		// TODO Auto-generated method stub
		if(e.event == "MOUSECLICK"){
			if(runningState[0] != PowerState.STATE_COOLING){
				if(runningState[0] == PowerState.STATE_OFF){
					runningState[0] = PowerState.STATE_ON;
				} else {
					runningState[0] = PowerState.STATE_OFF;
					
				}
			}
		}

	}

	@Override
	public void draw(PApplet context) {
		// TODO Auto-generated method stub
		context.pushMatrix();
		context.translate(screenPosition.x, screenPosition.y);
		String textLabel = "OFF";
		switch(runningState[0]){
		case STATE_ON:
			context.fill(0,255,0);
			textLabel = "ON";
			break;
		case STATE_OFF:
		
			context.fill(255,0,0);
			break;
		case STATE_COOLING:
			context.fill(255,255,0);
			textLabel = "COOLING";
			break;
		}
		context.rect(0,0,100,30);
		context.fill(255);
		context.text(textLabel, 10, 15);
		drawResources(context);
		
		context.popMatrix();
	}

	@Override
	public float consumeResource(String resName, float amount) {
		ReactorResource res = resourceStore.get(resName);
		if(res == null){
			return 0;
		}
		
		float amt = res.getAmount() - amount;
		if(amt < 0 ){
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
		
		
		
	}
	@Override
	public ArrayList<ReactorCheck> checkForProblems() {
		ReactorResource heat = resourceStore.get("HEAT");
		ReactorResource coolant = resourceStore.get("COOLANT");
		ArrayList<ReactorCheck> returnProblems = new ArrayList<ReactorCheck>();
		
		if(heat.getAmount() > heat.maxAmount * 0.66f){
			ReactorCheck r = new ReactorCheck("MIXER_OVERHEAT", false);
			r.setMessage("COOLANT MIXER OVERHEAT, TURN OFF");
			returnProblems.add(r);
		} else {
			ReactorCheck r = new ReactorCheck("MIXER_OVERHEAT", true);
			returnProblems.add(r);

		}
		
		if(coolant.getAmount() < 40f){
			lowCoolantTime += 1;
			if(lowCoolantTime > 5){
				
				if(runningState[0] != PowerState.STATE_ON){
					ReactorCheck r = new ReactorCheck("MIXER_LOWCOOLANT_OFF", false);
					r.setMessage("LOW COOLANT PRESSURE, TURN ON PRESSURISER");	
					returnProblems.add(r);

				} else {
					ReactorCheck r = new ReactorCheck("MIXER_LOWCOOLANT_VALVE", false);
					r.setMessage("LOW COOLANT PRESSURE, TURN OFF COOLANT VALVES");
					returnProblems.add(r);

				}
				
			}
		} else {
			if(lowCoolantTime > 5){
				ReactorCheck r = new ReactorCheck("MIXER_LOWCOOLANT_OFF", true);
				returnProblems.add(r);
				r = new ReactorCheck("MIXER_LOWCOOLANT_VALVE", true);
				returnProblems.add(r);
			}
			lowCoolantTime = 0;
		}
		
		
		
		return returnProblems;
	}

}
