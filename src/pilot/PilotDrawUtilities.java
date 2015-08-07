package pilot;

import common.PlayerConsole;
import common.ShipState;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

public class PilotDrawUtilities {
	PImage overlayImage;
	PlayerConsole parent;
	
	public PilotDrawUtilities(PlayerConsole parent){
		this.parent = parent;
		overlayImage = parent.loadImage("pilotconsole/overlayImage.png");

	}
	

	public  void drawPilotDamageGrid(PlayerConsole parent, int xpos, int ypos){
		parent.pushStyle();
		parent.pushMatrix();
		parent.translate(xpos, ypos);
		parent.textFont(parent.getGlobalFont(), 16);
		parent.fill(255);
		parent.text("SYSTEM STATUS", 0,-10);
		
		parent.noFill();
		
		int w = 300;
		int h = 100;
		parent.rect(0,0,w,h);
		parent.textFont(parent.getGlobalFont(), 10);
		String[] names = {"ENGINE", "  AFTER\r\nBURNER", "LANDING\r\n    GEAR", "ROT/PITCH", "ROT/YAW", "ROT/ROLL"};
		
		boolean[] states = {true, false, true, true, true, false};
		int[] damageOrder = {5,2,3,1,4,0};
		
		float damage = parent.getShipState().hullState;
		int damageCount = (int) PApplet.constrain(PApplet.map(damage, 0, 100, 6, 0), 0, 5);
		
		for(int x = 0; x < w / 100; x++){
			for(int y = 0; y < h / 50; y++){
				int i = x + y * (w/100);
				
				if(i >= damageCount){
					parent.fill(0,255,0);
				} else {
					
					if(parent.globalBlinker){
						parent.fill(255,0,0);
					} else {
						parent.fill(255,255,0);
					}
				
				}
				parent.rect(x*100, y*50, 100, 50);
				parent.fill(0);
				if(i < names.length){
				
					
					parent.text(names[i], x*100+10, y * 50 + 25);
				
				
				}
				
			}	
		}
		
		parent.popMatrix();
		parent.popStyle();
		
	}

	public  void drawPilotBar(PlayerConsole parent) {
		ShipState shipState = parent.getShipState();
		parent.noLights();
		parent.hint(PConstants.DISABLE_DEPTH_TEST);
		parent.image(overlayImage, 0, 0, parent.width, parent.height);

		parent.textFont(parent.getGlobalFont(), 18);
		parent.fill(0, 255, 255);
		int engPower = shipState.powerStates[ShipState.POWER_PROPULSION];
		int power = (int) PApplet.map(engPower, 0f, 12f, 0f, 180f);
		float engColor = PApplet.map(engPower, 0, 12, 0, 255);
		parent.text("Engine power" , 680, 600);
		parent.noFill();
		parent.stroke(255);
		parent.rect(680, 605, 180, 20);
		parent.fill(255 - engColor, engColor,0);
		parent.rect(680, 605, power, 20);
		if(engPower == 0){
			parent.textFont(parent.getGlobalFont(), 13);
			if(parent.globalBlinker){
				parent.fill(255,0,0);
			} else {
				parent.fill(255);
			}
			parent.text("NO POWER", 714, 619);
		}
		parent.textFont(parent.getGlobalFont(),18);
		parent.fill(255);
		parent.text("speed: " + (int) shipState.shipVelocity, 680, 660);

		
		//afterburner charge state
		parent.text("Afterburner: ", 690, 742);
		
		String state = "";
		if(shipState.afterburnerCharging){
			state = "charging";
			parent.fill(255,0,0);
		} else {
			state = "READY";
			if(parent.globalBlinker){
				parent.fill(0,255,0);
			} else {
				parent.fill(0,120,0);
			}
		}
		
		parent.text( state, 857, 742);
		drawPilotDamageGrid(parent, 19, 622);
	
		
	}
}
