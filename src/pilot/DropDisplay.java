package pilot;

import java.awt.Point;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PImage;
/* change this scene to show the altitude and predicted death time*/
import processing.core.PVector;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.util.LerpedVector;

public class DropDisplay extends Display {

	PImage bg, structFailOverlay, fireballImg, turbulenceImg;
	PImage overlayImage;

	Point[] labelPos = new Point[6];
	float[] temps = new float[6];

	
	long lastUpdate = 0;
	boolean structFail = false;

	LerpedVector fireVec = new LerpedVector(new PVector(0,0,0),0,250f);
	long turbulenceTime = 0;
	long turbulenceDuration = 0;
	

	public DropDisplay(PlayerConsole parent) {
		super(parent);
		bg = parent.loadImage("pilotconsole/screens/drop/background.png");
		font = parent.getGlobalFont(); // loadFont("HanzelExtendedNormal-48.vlw");
		structFailOverlay = parent
				.loadImage("pilotconsole/structuralFailure.png");
		fireballImg = parent.loadImage("pilotconsole/screens/drop/fireball.png");
		turbulenceImg = parent.loadImage("pilotconsole/turbulence.png");
		overlayImage = parent.loadImage("pilotconsole/screens/drop/overlay.png");

		labelPos[0] = new Point(647,508);
		labelPos[1] = new Point(422, 519);
		labelPos[2] = new Point(423, 350);
		labelPos[3] = new Point(885, 518);
		labelPos[4] = new Point(764, 322);
		labelPos[5] = new Point(765, 584);
		
	}

	@Override
	public void draw() {
		parent.background(0);
		parent.noTint();
		parent.fill(255, 255, 255);
		parent.image(bg, 0, 0, parent.width, parent.height);
		parent.fill(255, 255, 255);
		parent.textFont(font, 20);
		float alt = parent.getShipState().altitude.getValue(parent.millis());
		parent.text((int) alt + "m", 100,623 );
		
		if(alt < 2000 && parent.globalBlinker){
			parent.getConsoleAudio().playClip("terrainAlert");
		}

		
		
		
		//alt meters
		parent.stroke(255);
		parent.textFont(parent.getGlobalFont(), 15);
		for(int i = 0; i < 7; i++){
			int h = 300 + (int) ((i * 50) + (alt % 50));
			
			parent.line(94,h, 204,  h);
			
			//parent.text(i, 50, h);
		}
		
		
		
		
		
		
		
		
		parent.pushMatrix();
		PVector p = PVector.mult(fireVec.getValue(parent.millis()), 100f);
		p.x += 643;
		p.y += 444;
		parent.translate(p.x, p.y, p.z);
		parent.fill(128,128,0);
		parent.noStroke();
		parent.rotate(parent.millis()*0.001f);
		parent.scale(1f + parent.random(10)/80f);
		parent.image(fireballImg, -50,-50,100,100);
		parent.popMatrix();
		
		parent.pushMatrix();
		parent.translate(643,444);
		parent.stroke(255);
		parent.line(-250,0,250,0);
		parent.line(0,-250,0,250);
		parent.noFill();
		parent.ellipse(0, 0, 100,100);
		parent.ellipse(0, 0, 250,250);

		parent.popMatrix();
		
		
		parent.hint(PApplet.DISABLE_DEPTH_TEST);
		parent.image(overlayImage, 270,141);
		//temp text
		parent.textFont(font, 20);
		parent.fill(255);
		//left
		parent.pushMatrix();
		parent.translate(346,490);
		parent.rotate(parent.radians(-90));
		parent.text(String.format(" LEFT\n%.2fc", temps[2]), 0,0);
		parent.popMatrix();
		//right
		parent.pushMatrix();
		parent.translate(926,400);
		parent.rotate(parent.radians(90));
		parent.text(String.format("RIGHT\n%.2fc", temps[3]), 0,0);
		parent.popMatrix();
		
		//top
		parent.pushMatrix();
		parent.translate(600,161);		
		parent.text(String.format(" TOP\n%.2fc", temps[0]), 0,0);
		parent.popMatrix();
		
		//bottom
		parent.pushMatrix();
		parent.translate(580,711);		
		parent.text(String.format("BOTTOM\n  %.2fc", temps[0]), 0,0);
		parent.popMatrix();

		if (structFail) { // show the "structural failure" warning
			
			parent.image(structFailOverlay, 128, 200);
		}
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		// println(theOscMessage);
		if (theOscMessage.checkAddrPattern("/scene/drop/statupdate") == true) {
			lastUpdate = parent.millis();
		
			for (int t = 0; t < 6; t++) {
				temps[t] = theOscMessage.get(1 + t).floatValue();
			}
			PVector temp = new PVector(	theOscMessage.get(7).floatValue(),
										-theOscMessage.get(8).floatValue(),
										theOscMessage.get(9).floatValue());
			fireVec.update(temp, parent.millis());

		} else if (theOscMessage
				.checkAddrPattern("/scene/drop/structuralFailure") == true) {
			structFail = true;
		} else if (theOscMessage
				.checkAddrPattern("/scene/drop/turbulenceWarning")) {
			turbulenceTime = parent.millis();
			turbulenceDuration = (long) (theOscMessage.get(0).floatValue() * 1000l);
			parent.getConsoleAudio().playClip("bannerPopup");
		}
	}

	@Override
	public void serialEvent(HardwareEvent content) {
	}

	@Override
	public void start() {
		structFail = false;

	}

	@Override
	public void stop() {
	}
}
