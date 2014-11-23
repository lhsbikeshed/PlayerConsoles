package engineer.reactorsim;

import java.awt.Rectangle;

import processing.core.PApplet;
import processing.core.PVector;
import common.ConsoleLogger;
import common.HardwareEvent;
import common.ShipState;
import engineer.reactorsim.ReactorSystem.ReactorResource;

public class PowerDistributionSystem extends ReactorSystem {

	int testVal = 0;
	int currentRoute = 0;
	boolean[] routeUsable = new boolean[3];
	float[] routeHealth = {1.0f, 1.0f, 1.0f};
	
	public PowerDistributionSystem() {
		name = "Power Distribution";
		ReactorResource power = new ReactorResource();
		power.typeTag = "POWER";
		power.maxAmount = 500f;
		resourceStore.put("POWER", power);
		routeUsable[0] = true;
		routeUsable[1] = true;
		routeUsable[2] = true;
	
	}
	
	public float getOutputPower(){
		
		float modifier = 1.0f;
		if(routeUsable[currentRoute] == false){
			modifier = 0.0f;
		} else {
			modifier = routeHealth[currentRoute];
		}
		
		return resourceStore.get("POWER").getAmount() * modifier;
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
		
		
		//calculate how much power to draw off for the 4 subsystems
		int totalPower = 0;
		for(int i = 0; i < 4; i++){
			totalPower += ShipState.instance.powerStates[i];
		}
		float toUse = (totalPower / 48f) * 3f;
		//ConsoleLogger.log(this, "" + toUse + "t: " + totalPower);
		resourceStore.get("POWER").change(-toUse);
		
		//repair slowly
		float repairRate = ShipState.instance.powerStates[ShipState.POWER_DAMAGE] / 120f;
		changeRouteHealth(0, repairRate);
		changeRouteHealth(1, repairRate);
		changeRouteHealth(2, repairRate);
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
		for(int i = 0; i < 3; i++){
			String t = routeUsable[i] == true ? "OK" : "OFFLINE";
			context.text(i + " " + t + " H: " + routeHealth[i], 70, 10 + i * 20);
		}
		
		
		context.fill(255);
		drawResources(context);
		context.popMatrix();
	}

	public void changeRouteHealth(int route, float amount){
		routeHealth[route] += amount / 50f;
		if(routeHealth[route] < 0.0f){
			routeHealth[route] = 0.0f;
		}
		if(routeHealth[route] < 1.0f){
			routeUsable[route] = false;
			
		} else if (routeHealth[route] >= 1.0f){
			routeUsable[route] = true;
			routeHealth[route]  = 1.f;
		}
	}
	
	@Override
	public void applyDamage(float amount) {
		//pick a route and break it
		int randRoute = (int) (Math.random() * 3);
		changeRouteHealth(randRoute, -amount);
		
		

	}

}
