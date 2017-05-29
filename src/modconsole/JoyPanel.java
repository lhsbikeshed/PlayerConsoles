package modconsole;


import oscP5.*;
import netP5.*;

import java.awt.event.*;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;



public class JoyPanel {


	long lastUpdateTime = 0;
	long updateFreq = 100;

	boolean lastMouse = false;

	OscP5 oscP5;
	NetAddress myRemoteLocation;

	float rotZ = 0;
	float throt = 0;
	float zrot = 0;
	boolean leftDown, rightDown;

	boolean[] keyStates = new boolean[6];

	PVector translate = new PVector(0, 0, 0);

	PVector screenPos, screenSize;
	PFont font;

	ModConsole parent;

	public JoyPanel(ModConsole parent, int x, int y, int w, int h) {
		this.parent = parent;
		screenPos = new PVector(x, y);
		screenSize = new PVector(w, h);
		oscP5 = new OscP5(this, 12009);
		font = parent.getGlobalFont();

		myRemoteLocation = new NetAddress(parent.getServerAddress(), 19999);
	}




	void keyPressed(char key) {

		if (key == 'w') {
			throt += 0.1;
		} 
		else if (key == 's') {
			throt -= 0.1;
		} 
		else if (key == 'q') {
			leftDown = true;
		} 
		else if (key == 'e') {
			rightDown = true;
		} 
		else if (key == 'i') {
			translate.y = 1.0f;
		}  
		else if (key == 'k') {
			translate.y = -1.0f;
		}  
		else if (key == 'j') {
			translate.x = 1.0f;
		}  
		else if (key == 'l') {
			translate.x = -1.0f;
		} else if (key == 'f'){
			OscMessage myMessage = new OscMessage("/system/propulsion/afterburner");
			oscP5.send(myMessage, myRemoteLocation);
			
		}
	}

	void keyReleased(char key) {
		if (key == 'q') {
			leftDown = false;
		} 
		else if (key == 'e') {
			rightDown = false;
		} 
		else if (key == 'i') {
			translate.y = 0.0f;
		}  
		else if (key == 'k') {
			translate.y = 0.0f;
		}  
		else if (key == 'j') {
			translate.x = 0;
		}  
		else if (key == 'l') {
			translate.x = 0;
		}
	}

	void draw(PApplet p) {

		p.pushMatrix();
		p.translate(screenPos.x, screenPos.y);
		p.noFill();
		p.stroke(255, 255, 255);
		p.rect(0, 0, screenSize.x, screenSize.y);
		p.line(screenSize.x / 2, 0, screenSize.x / 2, screenSize.y);
		p.line(0, screenSize.x / 2, screenSize.x, screenSize.y / 2);

		p.textFont(font, 12);
		p.text(throt, 50, 50);

		if (lastUpdateTime + updateFreq < p.millis() ) {
			lastUpdateTime = p.millis();
			OscMessage myMessage = new OscMessage("/control/joystick/state");
			zrot = 0.0f; 
			if (leftDown) { 
				zrot = 1.0f;
			}
			if (rightDown) { 
				zrot = -1.0f;
			}


			if (p.mousePressed && checkBounds(p.mouseX, p.mouseY)) {
				myMessage.add(PApplet.map(p.mouseX -screenPos.x, 0f, screenSize.x, -1.0f, 1.0f)); 
				myMessage.add(-PApplet.map(p.mouseY - screenPos.y, 0f, screenSize.y, -1.0f, 1.0f));
				myMessage.add(zrot);
				myMessage.add(-translate.x);
				myMessage.add(-translate.y);
				myMessage.add(throt);
				lastMouse = true;
				oscP5.send(myMessage, myRemoteLocation);
			} 
			else {
				if (lastMouse ) {
					myMessage.add(0f); 
					myMessage.add(0f);
					myMessage.add(0f);
					myMessage.add(0f);
					myMessage.add(0f);
					myMessage.add(0f);
					oscP5.send(myMessage, myRemoteLocation);
				}
				lastMouse = false;
			}
		}
		if (checkBounds(p.mouseX, p.mouseY)) {
			p.ellipse(p.mouseX - screenPos.x, p.mouseY - screenPos.y, 5, 5);
			p.stroke(255, 255, 255);
		}


		p.popMatrix();
	}

	private boolean checkBounds(int x, int y) {
		if (x - screenPos.x > 0 && x - screenPos.x < screenSize.x) {
			if (y - screenPos.y > 0 && y - screenPos.y < screenSize.y) {
				return true;
			}
		} 
		return false;
	}
}

