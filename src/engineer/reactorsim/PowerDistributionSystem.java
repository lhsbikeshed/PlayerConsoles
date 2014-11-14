package engineer.reactorsim;

import java.awt.Rectangle;

import processing.core.PApplet;
import processing.core.PVector;
import common.HardwareEvent;
import engineer.reactorsim.ReactorSystem.ReactorResource;

public class PowerDistributionSystem extends ReactorSystem {

	int testVal = 0;
	int currentRoute = 0;
	boolean[] routeUsable = new boolean[3];
	
	public PowerDistributionSystem() {
		name = "Power Distribution";
		ReactorResource power = new ReactorResource();
		power.typeTag = "POWER";
		power.maxAmount = 500f;
		resourceStore.put("POWER", power);
		
	
	}
	
	@Override
	public void tick(){
		//steal power from the turbines
		ReactorResource r = resourceStore.get("POWER"); 
		float reqAmount = r.maxAmount - r.getAmount();
		if(reqAmount > 10f){
			reqAmount = 10f;
		}
		
		float source = inboundConnections.get("Turbines").consumeResource("POWER", reqAmount);
		resourceStore.get("POWER").change(source);
		
		resourceStore.get("POWER").change(-1f);
	}
	
	@Override
	public void setScreenPosition(PVector screenPosition) {
		this.screenPosition = screenPosition;
		bounds = new Rectangle((int)screenPosition.x, (int)screenPosition.y, 100,60);

	}
	
	@Override
	public void controlSignal(HardwareEvent e) {

		if(e.event == "MOUSECLICK"){
			currentRoute ++;
			currentRoute %= 3;
		}
	}

	@Override
	public void draw(PApplet context) {
		context.pushMatrix();
		context.translate(screenPosition.x, screenPosition.y);
		context.noFill();
		context.rect(0, 0, 50, 50);
		context.fill(255);
		context.text("route: " + currentRoute, 0,20);
		
		context.fill(255);
		drawResources(context);
		context.popMatrix();
	}

	@Override
	public void applyDamage(float amount) {
		// TODO Auto-generated method stub

	}

}
