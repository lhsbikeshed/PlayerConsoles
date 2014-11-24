
package engineer.reactorsim;

import java.awt.Rectangle;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;
import common.HardwareEvent;
import engineer.reactorsim.ReactorManager.ReactorCheck;
import engineer.reactorsim.ReactorSystem.ReactorResource;

/* ooolant valves draw from the mixer and produce coolant when turned on */
public class CoolantValveSystem extends ReactorSystem {

	boolean powerState = false;
	
	boolean[] valveStates = new boolean[3];
	int testValveState = 0;
	
	public CoolantValveSystem() {
		super();
		name = "Coolant valve";
		ReactorResource coolant = new ReactorResource();
		coolant.typeTag = "COOLANT";
		
		resourceStore.put("COOLANT", coolant);
	}

	@Override
	public void controlSignal(HardwareEvent e) {
		// TODO Auto-generated method stub
		
		if(e.event == "MOUSECLICK"){
			int mx = (int) ((e.value >> 16) - screenPosition.x);
			int my = (int) ((e.value & 65535) - screenPosition.y);
			for(int i = 0; i < 3; i++){

				if(mx >= i * 30 && mx <= i * 30 + 20){
					valveStates[i] = !valveStates[i];
				}
				
				
			}
		}
	}
	
	public int getNumberInState(boolean state){
		int ct = 0;
		for(int i = 0; i < 3; i++){
			if(valveStates[i] == state){
				ct++;
			}
		}
		return ct;
	}
	
	@Override
	public void tick(){
		//count how many valves are on
		int ct = getNumberInState(true);
		
		
	}
	
	public void setScreenPosition(PVector screenPosition) {
		this.screenPosition = screenPosition;
		bounds = new Rectangle((int)screenPosition.x, (int)screenPosition.y, 100,30);

	}

	@Override
	public void draw(PApplet context) {
		context.pushMatrix();
		context.translate(screenPosition.x, screenPosition.y);
		for(int i = 0; i < 3; i++){
			if(valveStates[i]){
				context.fill(0,255,0);
			} else {
				context.fill(255,0,0);
			}
			context.stroke(255);
			context.rect(i * 30, 0, 20,20);
			context.fill(255);
			context.text(i, 5 + i * 30, 15);
		}
		int p = 0;
		for(String k : resourceStore.keySet()){
			ReactorResource r = resourceStore.get(k);
			context.text(r.typeTag + " - " + (int)r.getAmount() + "/" + r.maxAmount, 0, 35+p);
			p+= 20;
		}
		context.popMatrix();
	}

	@Override
	public float consumeResource(String resName, float amount) {
		ReactorResource res = resourceStore.get(resName);
		if(res == null){
			return 0;
		}
		if(getNumberInState(true) == 0){
			return 0;
		}
		float amt = amount / (4 - getNumberInState(true));
		float retAmt = inboundConnections.get("Coolant mixer").consumeResource("COOLANT", amt);
		return retAmt;
		
		
		
	}

	@Override
	public void applyDamage(float amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<ReactorCheck> checkForProblems() {
		// TODO Auto-generated method stub
		return null;
	}

}
