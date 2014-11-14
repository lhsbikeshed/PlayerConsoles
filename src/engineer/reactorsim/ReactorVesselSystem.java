package engineer.reactorsim;

import java.awt.Rectangle;

import processing.core.PApplet;
import processing.core.PVector;
import common.ConsoleLogger;
import common.HardwareEvent;
import engineer.reactorsim.ReactorSystem.ReactorResource;

/* simulate the actual reactor
 * reactor takes fuel, turns it into plasma and generates steam
 * PLASMA:
 * plasma moves around the vessel when ship is damaged or bumped, plasma can heat the walls and break reactor
 * coil field modes can move it left and right in the vessel to prevent it hitting walls
 * 
 * FUEL:
 * fuel is drawn from the fuel system (D/T dials). Rate controls size and awkwardness of plasma
 * 
 * VESSEL:
 * produces steam, this is consumed by the turbines
 */
public class ReactorVesselSystem extends ReactorSystem {
	
	float plasmaPos = 100;
	float plasmaDelta = 0f;
	float plasmaDamageDelta = 0f;
	float plasmaSize = 0f;
	int[] coilMode = new int[2];	//polarity of field coils
	boolean reactorRunning = false;

	public ReactorVesselSystem() {
		name = "Reactor Vessel";
		// configure resources for reactor
		ReactorResource steamResource = new ReactorResource();
		steamResource.typeTag = "STEAM";
		resourceStore.put("STEAM", steamResource);
		
		ReactorResource heatResource = new ReactorResource();
		heatResource.typeTag = "HEAT";
		resourceStore.put("HEAT", heatResource);
		
		ReactorResource coolant = new ReactorResource();
		coolant.typeTag = "COOLANT";
		resourceStore.put("COOLANT", coolant);
		
		ReactorResource structure = new ReactorResource();
		structure.typeTag = "STRUCTURE";
		structure.setAmount(100f);
		resourceStore.put("STRUCTURE", structure);
		
		coilMode[0] = 1;
		coilMode[1] = 1;
	
	}
	
	@Override
	public void tick(){
		//now draw some coolant off of the valves. Consume more if valves are on
		CoolantValveSystem coolantSys = (CoolantValveSystem) inboundConnections.get("Coolant valve");
		if(coolantSys != null){
			float toConsume = 5f;
			float coolantAmount = coolantSys.consumeResource("COOLANT", toConsume);
			resourceStore.get("COOLANT").change(coolantAmount);
		}
		
		if(reactorRunning){
			
			//update the plasma position in the vessel
			//calculate plasma delta, based on size, coil modes		
			if(coilMode[0] != coilMode[1]){
				if(coilMode[0] == 0){
					plasmaDelta = -.05f;
				} else if (coilMode[0] == 1){
					plasmaDelta = .05f;
				}
			} else {
				plasmaDelta = 0f;
				
			}
			//tend plasma damage delta toward zero
			if(plasmaDamageDelta <= -0.1f){
				plasmaDamageDelta += 0.01f;
			} else if (plasmaDamageDelta >= 0.1f){
				plasmaDamageDelta -= 0.01f;
			} else {
				plasmaDamageDelta = 0f;
			}
			//ConsoleLogger.log(this, "elt " + plasmaDamageDelta);
			
			plasmaPos += (plasmaDamageDelta + plasmaDelta) * plasmaSize;	//scale with size, implying that larger ones will move quicker
			
			//test for wall collisions and heat the reactor up if plasma is touching it
			float warmAmount = 0;
			if(plasmaPos <= 0){
				plasmaPos = 0;
				warmAmount = 0.5f * plasmaSize;
			} else if (plasmaPos >= 200){
				plasmaPos = 200;
				warmAmount = 0.5f * plasmaSize;
			}
			warmAmount += 0.1f;	//just heat up for being turned on
			resourceStore.get("HEAT").change(warmAmount);
			
			//damage the reactor based on the heat overload
			float heatLevel = resourceStore.get("HEAT").getAmount();
			if(heatLevel >= 90){
				float damgAmount = heatLevel - 90;
				resourceStore.get("STRUCTURE").change(-damgAmount * 0.02f);
			}
			
			
			//power generated is based on the size of the plasma. If it hits the walls then reduce its size
			//and heat the reactop up
			
			
				
				//trade some of the coolant in the reactor for a bit of heat
			float amt = consumeResource("COOLANT", 0.4f);
			resourceStore.get("HEAT").change(-amt);
			
			
			//generate steam based on plasma size
			
			resourceStore.get("STEAM").change(plasmaSize * 2.0f);
			
			} else {
			
			}
		
	}

	@Override
	public void setScreenPosition(PVector pos){
		this.screenPosition = pos;
		bounds = new Rectangle((int)screenPosition.x, (int)screenPosition.y, 200,200);

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
	public void controlSignal(HardwareEvent e) {
		// TODO Auto-generated method stub
		if(e.event == "MOUSECLICK"){
			int mx = e.value >> 16;
			int my = e.value & 65535;
			if (mx < screenPosition.x + 20){
				coilMode[0] = 1-coilMode[0];
			} else if (mx > screenPosition.x + bounds.width - 20){
				coilMode[1] = 1-coilMode[1];
			} else {
				if(reactorRunning == false){
					startReactor();
				} else {
					stopReactor();
				}
			}
		}

	}

	private void stopReactor() {
		// TODO Auto-generated method stub
		reactorRunning = false;
	}

	private void startReactor() {
		reactorRunning = true;
		
	}

	@Override
	public void draw(PApplet context) {
		context.pushMatrix();
		context.translate(screenPosition.x, screenPosition.y);
		context.noFill();
		context.rect(0,0, 200,200);
		
		
		//field coils
		for(int i = 0; i < 2; i++){
			if(coilMode[i] == 0){
				context.fill(0,0,255);
				
			} else {
				context.fill(255,0,0);
			}
			context.rect(i * 200  -10, 20, 20, 150);
		}
		
		
		//if reactor is running then draw the plasma
		if(reactorRunning){
			context.fill(255);
			//draw plasma
			plasmaSize = 2f;
			context.ellipse(plasmaPos, 75, 14 * plasmaSize,50 * plasmaSize);
		} else {
			context.text("NO PLASMA", 80,120);
		}
		
		
		context.fill(255);
		context.translate(0, 140);
		drawResources(context);
		
		context.popMatrix();
		
	}

	@Override
	public void applyDamage(float amount) {
		plasmaDelta = 1.5f;
	}

}
