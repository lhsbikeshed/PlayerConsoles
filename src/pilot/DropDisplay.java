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

public class DropDisplay extends Display {

	PImage bg, structFailOverlay, fireballImg, turbulenceImg;

	Point[] labelPos = new Point[6];
	float[] temps = new float[6];

	
	long lastUpdate = 0;
	boolean structFail = false;

	PVector fireVec = new PVector(0, 0, 0);
	long turbulenceTime = 0;
	long turbulenceDuration = 0;
	

	public DropDisplay(PlayerConsole parent) {
		super(parent);
		bg = parent.loadImage("pilotconsole/screens/drop/background.png");
		font = parent.getGlobalFont(); // loadFont("HanzelExtendedNormal-48.vlw");
		structFailOverlay = parent
				.loadImage("pilotconsole/structuralFailure.png");
		fireballImg = parent.loadImage("pilotconsole/fireball.png");
		turbulenceImg = parent.loadImage("pilotconsole/turbulence.png");


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
		parent.textFont(font, 30);
		for (int t = 0; t < 6; t++) {
			Point p = labelPos[t];
			if (temps[t] > 200) {
				parent.fill(255, 0, 0);
			} else if (temps[t] > 100 && temps[t] <= 200) {
				parent.fill(255, 255, 0);
			} else {
				parent.fill(0, 255, 0);
			}
			parent.text((int) temps[t] + "c", p.x, p.y);
		}

		if (fireVec.z > 0) {
			parent.tint(255, (int) (fireVec.z * 255));
			int randX = (int) parent.random(fireVec.z * 5);
			int randY = (int) parent.random(fireVec.z * 5);
			parent.image(fireballImg, 707 + randX, 225 + randY,
					fireballImg.width / 2, fireballImg.height / 2);
		}
		if (fireVec.z < 0) {
			parent.tint(255, (int) (Math.abs(fireVec.z * 255)));
			int randX = (int) parent.random(Math.abs(fireVec.z * 5));
			int randY = (int) parent.random(Math.abs(fireVec.z * 5));
			parent.pushMatrix();
			parent.translate(712 + randX, 628+ randY);
			parent.scale(1, -1);
			parent.image(fireballImg, 0, 0, fireballImg.width / 2,
					fireballImg.height / 2);
			parent.popMatrix();
		}
		if (fireVec.x > 0) { // right
			parent.tint(255, (int) (fireVec.x * 255));
			int randX = (int) parent.random(fireVec.x * 5);
			int randY = (int) parent.random(fireVec.x * 5);
			parent.pushMatrix();
			parent.translate(969 + randX, 357 + randY);
			parent.rotate(PApplet.radians(90));
			parent.image(fireballImg, 0, 0, fireballImg.width / 2,
					fireballImg.height / 2);
			parent.popMatrix();
		}

		if (fireVec.x < 0) { // left
			parent.tint(255, Math.abs((int) (fireVec.x * 255)));
			int randX = Math.abs((int) parent.random(fireVec.x * 5));
			int randY = Math.abs((int) parent.random(fireVec.x * 5));
			parent.pushMatrix();
			parent.translate(607+ randX, 520 + randY);
			parent.rotate(PApplet.radians(-90));
			parent.image(fireballImg, 0, 0, fireballImg.width / 2,
					fireballImg.height / 2);
			parent.popMatrix();
		}
		if (fireVec.y > 0) { // top
			parent.tint(255, (int) (fireVec.y * 255));
			int randX = (int) parent.random(fireVec.y * 5);
			int randY = (int) parent.random(fireVec.y * 5);
			parent.pushMatrix();
			parent.translate(371 + randX, 249 + randY);
			// rotate(radians(90));
			parent.image(fireballImg, 0, 0, fireballImg.width / 2,
					fireballImg.height / 2);
			parent.popMatrix();
		}

		if (fireVec.y < 0) { // top
			parent.tint(255, Math.abs((int) (fireVec.y * 255)));
			int randX = Math.abs((int) parent.random(fireVec.y * 5));
			int randY = Math.abs((int) parent.random(fireVec.y * 5));
			parent.pushMatrix();
			parent.translate(538 + randX, 603 + randY);
			parent.rotate(PApplet.radians(-180));
			parent.image(fireballImg, 0, 0, fireballImg.width / 2,
					fireballImg.height / 2);
			parent.popMatrix();
		}
		parent.noTint();

		if (turbulenceTime < parent.millis()
				&& parent.millis() < turbulenceTime + turbulenceDuration) {

			parent.image(turbulenceImg, 155, 410);

		}

		if (structFail) { // show the "structural failure" warning

			parent.image(structFailOverlay, 128, 200);
		}
		
		
		//alt meters
		parent.stroke(255);
		parent.textFont(parent.getGlobalFont(), 15);
		for(int i = 0; i < 7; i++){
			int h = 300 + (int) ((i * 50) + (alt % 50));
			
			parent.line(94,h, 204,  h);
			
			//parent.text(i, 50, h);
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
			fireVec.x = theOscMessage.get(7).floatValue();
			fireVec.y = theOscMessage.get(8).floatValue();
			fireVec.z = theOscMessage.get(9).floatValue();

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
