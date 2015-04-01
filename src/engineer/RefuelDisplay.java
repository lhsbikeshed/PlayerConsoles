package engineer;

import java.awt.event.KeyEvent;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.opengl.PShader;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

public class RefuelDisplay extends Display {
	
	PVector headPos = new PVector(0,0,0);
	PVector headVel	= new PVector(0,0,0);
	PVector headVelTarget = new PVector(0,0,0);
	
	PImage overlayImage, targetImage;
	
	PGraphics targetGraphics;


	public RefuelDisplay(PlayerConsole parent) {
		super(parent);
		overlayImage = parent.loadImage("engineerconsole/screens/docking/fuelOverlay.png");
		targetImage = parent.loadImage("engineerconsole/screens/docking/target.png");

		targetGraphics = parent.createGraphics(860,530, PApplet.P3D);

	}

	@Override
	public void draw() {
		headVel = PVector.lerp(headVel, headVelTarget, 0.02f);
		
		headPos.add(headVel);
		
		parent.background(0);
		
		targetGraphics.beginDraw();
		targetGraphics.background(0);
		targetGraphics.translate(headPos.x, headPos.y, headPos.z);
		
		targetGraphics.image(targetImage,0, 0);

		targetGraphics.endDraw();
		

		parent.image(targetGraphics, 90, 148);
		
		parent.image(overlayImage, 10,15);

	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		if(evt.event.equals("KEY")){
			switch(evt.id){
			case KeyEvent.VK_UP:
				headVelTarget.y = 1f * evt.value;
				break;
			case KeyEvent.VK_DOWN:
				headVelTarget.y = -1f * evt.value;
				break;
			case KeyEvent.VK_RIGHT:
				headVelTarget.x = -1f * evt.value;
				break;
			case KeyEvent.VK_LEFT:
				headVelTarget.x = 1f * evt.value;
				break;
			case KeyEvent.VK_ADD:
				headVelTarget.z = 1f * evt.value;
				break;
			case KeyEvent.VK_SUBTRACT:
				headVelTarget.z = -1f * evt.value;
				break;
			
			}
		}
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
