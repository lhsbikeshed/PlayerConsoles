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
import common.util.LerpedFloat;
import common.util.UsefulShit;

public class CollisionRadarDisplay extends RadarDisplay {

	PImage overlayImage;
	PImage[] rockIcon = new PImage[3];
	
	PGraphics pg;
	boolean showCollisionAlert = false;
	
	
	
	public CollisionRadarDisplay(PlayerConsole parent) {
		super(parent);
		silenceNewTargets = true;
		overlayImage = parent.loadImage("pilotconsole/collisionOverlay.png");
		for(int i = 1; i < 4; i++){
			rockIcon[i-1] = parent.loadImage("pilotconsole/rockIcon" + i + ".png");
		}
		pg = parent.createGraphics(1024, 768, PApplet.P3D);
	}

	@Override
	public void draw() {
		showCollisionAlert = false;//this will be set if there are any hightlight targets
									//drawn later on
		parent.background(0);
		pg.beginDraw();
		pg.background(0);
		pg.stroke(255);
		pg.strokeWeight(2);
		
		pg.line(512, 350, 512, 420);
		pg.line(480, 384, 544, 384);
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
						pg.rotateX((rItem.hashCode() + parent.millis() + 100000) * 0.001f);
						pg.rotateY((rItem.hashCode() + parent.millis() + 1000) * 0.001f);
						pg.scale(0.5f);
						//in this view highlighted items need to blink
						//rocks that are on a collision course are tagged as highlighted
						
						if(rItem.targetted){
							showCollisionAlert = true;
							pg.stroke((int)(parent.millis()/5f) % 255, 0,0);
							//pg.stroke(255,0,0);
							pg.strokeWeight(4);
						} else {
							
							pg.stroke(255);
							pg.strokeWeight(1);
						}
						
						pg.noFill(); ; ; pg.sphereDetail(4);
						pg.sphere(105);;
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
		//-- distance marker --
		parent.fill(255);
		float h = (int)parent.getShipState().altitude.getValue(parent.millis());
		if(h < 400f){
			showCollisionAlert = true;
			parent.fill(255,0,0);
		}
		
		int startY = 85;
		int startX = 40;
		parent.stroke(255);
		parent.line(startX, startY, startX, startY + 600);
		int len = 20;
		for(int i = 0; i <= 600 / 20; i++){
			if(i % 5 == 0){
				len = 40;
			} else {
				len = 20;
			}
			parent.line(startX, startY + i * 20, startX + len, startY + i * 20);
			
		}
		float markerY = PApplet.map(h, 2900, 0, startY, startY + 600);
		parent.line(startX, markerY, startX + 50, markerY);
		
		parent.text((int)(parent.getShipState().altitude.getValue(parent.millis()) * 10 )+ "m", 100, markerY);
		
		//=------- collision warning -----
		if(parent.globalBlinker){
			parent.fill(255,0,0);
		} else {
			parent.fill(255);
			
		}
		if(showCollisionAlert && parent.globalBlinker){
			parent.text("COLLISION ALERT", 386,420);
			
			parent.getConsoleAudio().playClip("blip");
			
		}
		
	}

	
	/* incoming osc message are forwarded to the oscEvent method. */
	@Override
	public void oscMessage(OscMessage msg) {

		super.oscMessage(msg);
		
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
