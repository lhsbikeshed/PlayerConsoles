package pilot;

import com.jogamp.opengl.math.Quaternion;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;
import common.util.LerpedRot;
import common.util.LerpedVector;
import common.util.Rot;
import common.util.RotOrder;
import common.util.UsefulShit;

public class LandingDisplay extends Display {
	

	long lastQuaternionTime = 0;
	
	PVector shipScreenSpace = new PVector();
	
	PImage overlayImage;
	
	PGraphics landingGraphics;
	
	long distancePingTime = 0;
	
	public LandingDisplay(PlayerConsole parent) {
		super(parent);
		overlayImage = parent.loadImage("data/pilotconsole/landing_overlay.png");
		landingGraphics = parent.createGraphics(parent.width, parent.height, PConstants.P3D);
		
	}

	@Override
	public void draw() {
		//testCode();
		
		
		landingGraphics.beginDraw();
		landingGraphics.background(0,30,0);
		landingGraphics.hint(PApplet.ENABLE_DEPTH_TEST);

		long now = parent.millis();
		landingGraphics.textFont(parent.getGlobalFont(), 20);
	
		landingGraphics.pushMatrix();
		
		landingGraphics.noStroke();
		landingGraphics.translate(landingGraphics.width / 2, landingGraphics.height / 2);
		landingGraphics.rotateX(PApplet.radians(55)); // 326
		
		
		//landingGraphics.rotateY(PApplet.radians(-180));
		
		
		landingGraphics.fill(0,50,0);
		landingGraphics.rect(-400,-400,800,800);
		
		landingGraphics.noFill();
		landingGraphics.strokeWeight(3);
		for(int i = 0; i < 5; i++){
			landingGraphics.stroke(0,128,0);
			if((parent.millis() % 1000 ) / 200 == (4 - i)){
				landingGraphics.stroke(0,200,0);	//pulse inward
			}
			landingGraphics.ellipse(0, 0, 150 + i * 120, 150 + i * 120);
		}
		
		//crosshair - highlight axis separately based on closeness to centre
		
		LerpedVector shipOffset = parent.getShipState().dockingOffset;
		LerpedRot localRotation = parent.getShipState().dockingRotation;
		
		if(PApplet.abs(shipOffset.getValue(now).y) < 5){
			landingGraphics.stroke(0,255,0);
		} else {
			landingGraphics.stroke(0,128,0);

		}
		landingGraphics.line(-400,0,400,0);
		if(PApplet.abs(shipOffset.getValue(now).x) < 5){
			landingGraphics.stroke(0,255,0);
		} else {
			landingGraphics.stroke(0,128,0);

		}
		landingGraphics.line(0, -400,0,400);

		
		landingGraphics.stroke(0,255,0);
		landingGraphics.fill(0,128,0);
		PVector pos = shipOffset.getValue(now);
		pos.mult(10f);	//slight fudge
		
		//save ship screen space coords for drawing gui elements later
		shipScreenSpace.x = landingGraphics.screenX(pos.x, pos.y, pos.z);
		shipScreenSpace.y = landingGraphics.screenY(pos.x, pos.y, pos.z);


		landingGraphics.pushMatrix();
		landingGraphics.translate(pos.x, pos.y, pos.z);
		
		
		landingGraphics.line(0,0,0, 0,0, -pos.z);
		landingGraphics.pushMatrix();
		landingGraphics.translate(0,0,-pos.z +1);
		landingGraphics.ellipse(0,0,40,40);
		landingGraphics.popMatrix();
		
		
		float[] ang = localRotation.getValue(now).getAngles(RotOrder.XYZ);
		if (ang != null) {
			landingGraphics.rotateX(ang[0]);
			landingGraphics.rotateY(ang[1]);
			landingGraphics.rotateZ(ang[2]);
		}
		
		/*
		landingGraphics.stroke(255,0,0);
		landingGraphics.line(0,0,0,-100,0,0);
		landingGraphics.stroke(0,255,0);
		landingGraphics.line(0,0,0,0,100,0);
		landingGraphics.stroke(0,0,100);
		landingGraphics.line(0,0,0,0,0,100);
		*/
		
		//---------draw the ship body
		landingGraphics.strokeWeight(1);
		landingGraphics.pushMatrix();
		landingGraphics.translate(0,9.5f,0);
		landingGraphics.scale(1, 0.7f, 1.3f);
		landingGraphics.box(50);
		landingGraphics.popMatrix();
		
		//legs
		for(int x = -1; x < 1; x++){
			
			for(int y = -1; y < 1; y++){
				
				landingGraphics.pushMatrix();
				landingGraphics.translate(30+x*60,-6,25+ y*40);
				landingGraphics.box(10);
				landingGraphics.popMatrix();
			}
		}
		//tail
		landingGraphics.pushMatrix();
		landingGraphics.translate(0,31,-30);
		landingGraphics.scale(0.4f, 1.4f, 2.3f);
		landingGraphics.box(10);
		landingGraphics.popMatrix();
		
		//-----------body done
		
		//---- screen space stuff
		landingGraphics.popMatrix();
		
		landingGraphics.popMatrix();
		landingGraphics.hint(PApplet.DISABLE_DEPTH_TEST);
		landingGraphics.textFont(font, 18);
		landingGraphics.fill(255);
		landingGraphics.translate(shipScreenSpace.x, shipScreenSpace.y);
		PVector sp = shipOffset.getValue(now);
		landingGraphics.text(String.format("X:%.2f\r\nY:%.2f\r\nZ:%.2f", sp.x, sp.y, sp.z),20,50);

		if(ShipState.instance.shipDocked){
			if(parent.globalBlinker){
				landingGraphics.fill(255,0,0);
				
			} else {
				landingGraphics.fill(255);
				
			}
			landingGraphics.text("*DOCKED*", 0,110);
		}
		
		landingGraphics.endDraw();
		
		parent.image(landingGraphics, 0,-100);

		//parent.image(overlayImage, 10,10);
		
		//stats in bottom left
		parent.textFont(font, 14);
		ShipState shipState = parent.getShipState();
		String ls = ShipState.undercarriageStrings[ shipState.undercarriageState];
		
		((PilotConsole)parent).drawUtils.drawPilotBar(parent);

		
		if(!ShipState.instance.shipDocked){
			if(distancePingTime <= 0){
				distancePingTime = Math.abs((long) (1000 * (shipOffset.getValue(parent.millis()).mag() / 20f)));
				parent.getConsoleAudio().playClip("shortBlip");
	
			}
			distancePingTime -= 50;	
		}
		
	}

	private void testCode() {
		
		
	}

	@Override
	public void oscMessage(OscMessage msg) {
		

	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		// TODO Auto-generated method stub

	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
