package engineer.reactorsim;

import java.awt.Rectangle;

import processing.core.PApplet;
import processing.core.PVector;
import common.ConsoleLogger;
import common.HardwareEvent;

public class CoolantMixerSystem extends ReactorSystem {

	boolean powerState = false;
	
	
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
	
	@Override
	public void tick(){
		super.tick();
		ReactorResource heat = resourceStore.get("HEAT");
		ReactorResource coolant = resourceStore.get("COOLANT");
		if(powerState){
			//when turned on (i.e. mixing coolant) increase the heat (implying the mixer gets hot) 
			//and increase the coolant gen rate			
			heat.amountPerTick = 0.05f;			
			coolant.amountPerTick = 30f;
		} else {
			heat.amountPerTick = -1f;
			coolant.amountPerTick = 0f;
			//coolant doesnt change, its only lowered by being consumed by something else
		}
	}

	@Override
	public void controlSignal(HardwareEvent e) {
		// TODO Auto-generated method stub
		if(e.event == "MOUSECLICK"){
			powerState = !powerState;
		}

	}

	@Override
	public void draw(PApplet context) {
		// TODO Auto-generated method stub
		context.pushMatrix();
		context.translate(screenPosition.x, screenPosition.y);
		String textLabel = "OFF";
		if(powerState){
			context.fill(0,255,0);
			textLabel = "ON";
		} else {
			context.fill(255,0,0);
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
		// TODO Auto-generated method stub
		
	}

}
