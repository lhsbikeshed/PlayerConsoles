package pilot;

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
import common.UsefulShit;

public class CollisionRadarDisplay extends RadarDisplay {

	PImage overlayImage;
	PImage[] rockIcon = new PImage[3];
	
	PGraphics pg;
	
	public CollisionRadarDisplay(PlayerConsole parent) {
		super(parent);
		overlayImage = parent.loadImage("pilotconsole/collisionOverlay.png");
		for(int i = 1; i < 4; i++){
			rockIcon[i-1] = parent.loadImage("pilotconsole/rockIcon" + i + ".png");
		}
		pg = parent.createGraphics(1024, 768, PApplet.P3D);
	}

	@Override
	public void draw() {
		parent.background(0);
		pg.beginDraw();
		pg.background(0);
		pg.stroke(255);
		pg.strokeWeight(2);
		
		pg.line(512, 0, 512, 1024);
		pg.line(0, 384, 1024, 384);
		pg.pushMatrix();
		
		pg.translate(512, 384 , 100);
		pg.fill(20,20,20,120);
	    pg.hint(PConstants.DISABLE_DEPTH_MASK);
		for(int i = 0; i < 4; i++){
			pg.pushMatrix();
			pg.translate(0,0,-i * 100);
			pg.rect(-250,-250,500,500);
			pg.popMatrix();
		}
		pg.noStroke();
		pg.fill(128,128,128,128);
		pg.ellipse(0,0,40,40);
		//parent.rotateX(parent.radians(10f));
 	    pg.hint(PConstants.ENABLE_DEPTH_MASK);
 	    pg.textFont(font, 22);
		synchronized (lock) {
			for (int i = 0; i < 100; i++) {

				RadarObject rItem = radarList[i];
				if (rItem.active == true) {
					PVector newPos = rItem.lastPosition;

					newPos.x = PApplet.lerp(rItem.lastPosition.x,
							rItem.position.x,
							(parent.millis() - rItem.lastUpdateTime) / 250.0f);
					newPos.y = PApplet.lerp(rItem.lastPosition.y,
							rItem.position.y,
							(parent.millis() - rItem.lastUpdateTime) / 250.0f);
					newPos.z = PApplet.lerp(rItem.lastPosition.z,
							rItem.position.z,
							(parent.millis() - rItem.lastUpdateTime) / 250.0f);
					
					pg.pushMatrix();
					pg.translate(newPos.x  *22f, -newPos.y *22f, -newPos.z * 22f);
					//parent.rotateX(parent.PI / 2f);
					
					if(-newPos.z * 22f < 0){
						//parent.tint(parent.map(newPos.z, 0, 20, 128, 0));
						//parent.ellipse(0,0,50,50);
						pg.fill(255);
						
						pg.pushMatrix();
						pg.rotate((rItem.hashCode() + parent.millis() + 100000) * 0.001f);
						pg.scale(0.5f);
						if(newPos.mag() < 4.0f){
							pg.tint(255,0,0);
						} else {
							pg.noTint();
						}
						PImage rock = rockIcon[Math.abs(rItem.id / 10) % 3];
						pg.image(rock, -rock.width/2, -rock.height/2);
						pg.popMatrix();
						pg.fill(255);
						pg.text(newPos.mag(), 28, 70);
						
					}
					pg.popMatrix();
					
					clearDeadItems(rItem);
				}
			}
		}
		
		pg.popMatrix();
		pg.endDraw();
		parent.image(pg, 0, 0);;
		parent.image(overlayImage, 0, 0);
		parent.textFont(font, 20);
		if(parent.globalBlinker){
			parent.fill(255,0,0);
		} else {
			parent.fill(255);
			
		}
		parent.text("ROTATION CONTROL OFFLINE", 286, 743);
	}

	
	/* incoming osc message are forwarded to the oscEvent method. */
	@Override
	public void oscMessage(OscMessage theOscMessage) {

		super.oscMessage(theOscMessage);
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
