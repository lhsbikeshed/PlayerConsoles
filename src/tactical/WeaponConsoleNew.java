package tactical;

import netP5.NetAddress;
import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import tactical.WeaponsConsole.TargetObject;
import common.ConsoleLogger;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;

public class WeaponConsoleNew extends WeaponsConsole {

	
	PImage shipIcon, weaponIcon;
	private PImage weaponStatusPanelImage;
	
	long animationStartTime = 0;
	int animationDirection = 0;
	
	PVector[] weaponIconPositions = new PVector[5];
	float[] weaponIconRotations = new float[5];
	
	PGraphics radarGraphics;
	
	float mouseRadius = 20f;
	
	public WeaponConsoleNew(PlayerConsole parent) {
		super(parent);
		bgImage = parent.loadImage("tacticalconsole/tacticalscreen2.png");
		shipIcon = parent.loadImage("tacticalconsole/weaponsScreen/weaponShip.png");
		weaponIcon = parent.loadImage("tacticalconsole/weaponsScreen/weaponIcon.png");
		weaponStatusPanelImage = parent.loadImage("tacticalconsole/weaponStatusPanel.png");
		
		weaponIconPositions[0] = new PVector(533, 689);
		weaponIconRotations[0] = 0;
		weaponIconPositions[1] = new PVector(616, 710);
		weaponIconRotations[1] = 180f;
		weaponIconPositions[2] = new PVector(545, 668);
		weaponIconRotations[2] = 45f;
		weaponIconPositions[3] = new PVector(622, 682);
		weaponIconRotations[3] = 135f;
		weaponIconPositions[4] = new PVector(565, 724);
		weaponIconRotations[4] = -90f;
		
		radarGraphics = parent.createGraphics(680, 700, PConstants.P3D);
		
		
	}
	
	protected void triggerDeploymentAnimation(int state){
		if(state == ShipState.WEAPON_TRANSIT_OUT ){
			ConsoleLogger.log(this, "deploying weapon anim");
			animationDirection = 1;
			animationStartTime = parent.millis();
		} else if(state == ShipState.WEAPON_TRANSIT_IN ){
			ConsoleLogger.log(this, "retracting weapon anim");
			animationDirection = -1;
			animationStartTime = parent.millis();
		}
	}
	
	public void draw(){
		sensorPower = parent.getShipState().powerStates[ShipState.POWER_SENSORS];
		
		beamPower = parent.getShipState().powerStates[ShipState.POWER_WEAPONS];
	    int sensorRange = (int) PApplet.map(sensorPower, 0f, 12f, 270,1300);
	    maxBeamRange = (1000 + (beamPower - 1) * 300);
		
		parent.background(0, 0, 0);
		parent.noStroke();
		

		
		//start of 3d section ----------------------
		radarGraphics.beginDraw();
		radarGraphics.background(0);
		radarGraphics.hint(PApplet.DISABLE_DEPTH_TEST);
		radarGraphics.pushMatrix();
		radarGraphics.translate(340, 92, -800);
		radarGraphics.rotateX(parent.radians(41));
		radarGraphics.rotateZ(parent.radians(parent.millis() / 200f));

		
		//parent.image(bgImage, 0, 0);
		radarGraphics.fill(0, 128, 0, 100);
		radarGraphics.noStroke();
		int sensorSize = (int) PApplet.map(sensorPower, 0f, 12f, 270,1450) ;
		radarGraphics.ellipse(0, 0, 1450, 1450);
		radarGraphics.stroke(128);
		
		radarGraphics.pushMatrix();
		
		for(int i=0; i < 24; i++){
			radarGraphics.rotate((float)((Math.PI * 2f) / 24));
			if((i + 1 ) % 3 == 0){
				radarGraphics.line(0, 700, 0, 850);
			} else {
				radarGraphics.line(0, 700, 0, 750);
			
			}
		}
		radarGraphics.popMatrix();

		radarGraphics.ellipse(0, 0, sensorSize, sensorSize);
		radarGraphics.stroke(0,0,255);
		radarGraphics.line(-725,0,0,725,0,0);
		radarGraphics.stroke(255,0,0);
		radarGraphics.line(0,-725,0,0,725,0);
		
		
		
		
		
		radarGraphics.box(30);
		radarTicker += 20;
		radarGraphics.noStroke();
		int alpha = (int) PApplet.map(radarTicker, 0, sensorSize, 45f, 0f );
		radarGraphics.fill(0,255,0, alpha);
		radarGraphics.ellipse(0, 0, radarTicker, radarTicker);
		
		if (radarTicker > sensorRange) {
			radarTicker = 15;
		}

		
		
		drawTargets2();
		
		if (smartBombFireTime + 1000 > parent.millis()) {
			float radius = (parent.millis() - smartBombFireTime) / 1000.0f;
			radarGraphics.noFill();
			radarGraphics.strokeWeight(5);
			radarGraphics.stroke(70, 70, 255);
			radarGraphics.ellipse(0, 0, radius * 900, radius * 900);
			radarGraphics.strokeWeight(1);
		}
		
		radarGraphics.popMatrix();	
		
		drawScreenSpaceTargets();
		
		radarGraphics.endDraw();
		//----------end of 3d section-------------
		parent.image(radarGraphics, 10, 80);
		
		drawSideBar();
		
		drawWeaponStatus();
		parent.stroke(255);
		parent.fill(255);
		
		//draw the mouse targetting cursor
		PVector pos = ((TacticalConsole)parent).mousePosition;
		parent.pushMatrix();
		parent.translate(pos.x, pos.y);		
		parent.noFill();
		parent.line(-20,0, 20,0);
		parent.line(0, 20, 0, -20);
		float scale = parent.sin(parent.millis() / 200f) * 0.2f + 0.8f;
		parent.scale(scale);
		
		parent.ellipse(0, 0,  mouseRadius, mouseRadius);
		
		
		parent.popMatrix();
		
	}
	
	private void drawScreenSpaceTargets() {
		int sensorRange = (int) PApplet.map(sensorPower, 0f, 12f, 270,1450);
		synchronized (targets) {
			for (int i = targets.size() - 1; i >= 0; i--) {
				TargetObject t = targets.get(i);
				float distanceToTarget = t.pos.mag();
				String scanCode = "" + t.scanId;
				
				if (t.scanId < 1000) {
					scanCode = "0" + scanCode;
				}
				
				radarGraphics.pushMatrix();
				radarGraphics.translate(t.screenSpacePos.x, t.screenSpacePos.y);
				if (distanceToTarget < sensorRange * 0.6f) { 
					radarGraphics.textFont(font, 13);
					radarGraphics.fill(255,255,0);
					
					//radarGraphics.rotateX(-0.8f);;
					String h = String.format("%.2f", t.stats[0] * 100);
					radarGraphics.text(t.name + ": " + h + "%", 30, 25);
					radarGraphics.textFont(font, 18);
					radarGraphics.text(scanCode, 30, 5);
					radarGraphics.stroke(255,255,0);
					radarGraphics.line(30, 15, 20, 15);
					radarGraphics.line(0, 0, 20, 15);
					
					
				} else {
					radarGraphics.fill(128);
					StringBuilder s = new StringBuilder(t.name);
					for (int c = 0; c < (int) parent.random(3, s.length()); c++) {
						s.setCharAt((int) parent.random(0, s.length()),
								(char) parent.random(0, 255));
					}
					radarGraphics.text(s.toString(), 10, 0);
				}
				radarGraphics.popMatrix();
			}
		}
		
		
	}

	/* weapon status panel, shows deployment state and health of each*/
	protected void drawWeaponStatus(){
		parent.image(weaponStatusPanelImage, 451, 598);
		
		
		//draw the weapon icons
		
		for(int i = 0; i < weaponIconPositions.length; i++){
			parent.pushMatrix();
			parent.translate(weaponIconPositions[i].x, weaponIconPositions[i].y);
			parent.rotate(PApplet.radians(weaponIconRotations[i]));
			
			float animPos = parent.millis() - animationStartTime;
			if(animPos > 4000){
				animationDirection = 0;
			}
			float xOffset = 0;
			if(parent.getShipState().weaponState == ShipState.WEAPON_TRANSIT_OUT){
				 xOffset = -10;//PApplet.map(animPos, 0, 4000, 0, -10);
				 parent.tint((float) Math.sin(parent.millis() / 100f) * 255 + 128);
			} else if(parent.getShipState().weaponState == ShipState.WEAPON_TRANSIT_IN){
				 xOffset = -10;// PApplet.map(animPos, 0, 4000, -10, 0);
				 parent.tint((float) Math.sin(parent.millis() / 100f) * 255 + 128);
			} else if(parent.getShipState().weaponState == ShipState.WEAPON_DEPLOYED){
				 xOffset = -10;
			} else if(parent.getShipState().weaponState == ShipState.WEAPON_STOWED){
				 xOffset = 0;
			}
			parent.image(weaponIcon, xOffset, 0);
			
			
//			if(parent.getShipState().weaponState == ShipState.WEAPON_DEPLOYED){
//				float health = parent.getShipState().weaponHealth[i];
//				parent.noFill();
//				parent.stroke(255);
//				parent.strokeWeight(1);
//				parent.rect(xOffset - 30f,  + 4f, 25f,  14f);
//				parent.noStroke();
//				parent.fill(255 - (health * 255), health * 255, 0);
//				parent.rect(xOffset - 5f,  + 4f, -PApplet.map(health, 0f, 1.0f, 0f, 25f),  14f);
//			}
			
			parent.popMatrix();
			
		}
		parent.noTint();
		parent.image(shipIcon, 532, 643);
		
		
		
	}
	
	
	
	protected void drawTargets(){
		
		int sensorRange = (int) PApplet.map(sensorPower, 0f, 12f, 270,1300);
		parent.textFont(font, 12);
		fireEnabled = false;
		parent.strokeWeight(1);
		synchronized (targets) {
			for (int i = targets.size() - 1; i >= 0; i--) {
				TargetObject t = targets.get(i);
				
				float distanceToTarget = t.pos.mag();
				float lastDistanceToTarget = t.lastPos.mag();
				// update logic bits
				// if no update received for 280ms then remove this target
				if (parent.millis() - t.lastUpdateTime > 300) {
					if (t.targetted) {
						parent.getConsoleAudio().playClip("targetDestroyed");
						scanningState = SCAN_TYPING;
						scanString = "";
					}
					targets.remove(i);
				}

				float lerpX = PApplet.lerp(t.lastPos.x, t.pos.x,
						(parent.millis() - t.lastUpdateTime) / 250.0f);
				float lerpY = PApplet.lerp(t.lastPos.z, t.pos.z,
						(parent.millis() - t.lastUpdateTime) / 250.0f);
				float lerpZ = PApplet.lerp(t.lastPos.y, t.pos.y,
						(parent.millis() - t.lastUpdateTime) / 250.0f);

				
				PVector p = PVector.fromAngle(t.randomAngle);
				p.mult(75 + distanceToTarget / 3.0f); // new pos
				PVector lp = PVector.fromAngle(t.randomAngle);
				lp.mult(75 + lastDistanceToTarget / 3.0f);

				float screenSpaceScaleFactor = 0.18f;
				
				float x =  lerpX * screenSpaceScaleFactor;
				;
				float y = 420 + lerpY * screenSpaceScaleFactor;

				// set target colour
				float scaleFactor = 1.2f;
				if (distanceToTarget > sensorRange * scaleFactor) {
					parent.fill(100, 100, 100);
					x += parent.random(-5, 5);
					y += parent.random(-5, 5);
				} else if (distanceToTarget < 200) {

					parent.fill(255, 0, 0);
				} else if (distanceToTarget < 500) {
					parent.fill(255, 255, 0);
				} else {
					parent.fill(0, 255, 0);
				}

				// draw the target on the radar
				parent.noStroke();
				parent.ellipse(x, y, 10, 10);
				String scanCode = "" + t.scanId;
				if (t.scanId < 1000) {
					scanCode = "0" + scanCode;
				}
				if (distanceToTarget < sensorRange * scaleFactor) { // grey it
																	// out if
																	// its
																	// outside
																	// of sensor
																	// range, if
																	// not then
																	// draw
					parent.textFont(font, 24);

					String h = String.format("%.2f", t.stats[0] * 100);
					parent.text(t.name + ": " + h + "%", x + 30, y + 5);
					parent.textFont(font, 20);
					parent.text(scanCode, x + 30, y-10);
					parent.stroke(255,255,0);
					parent.line(x+30, y-15, x+20, y-15);
					parent.line(x, y, x+20, y-15);
					
				} else {
					parent.fill(128);
					StringBuilder s = new StringBuilder(t.name);
					for (int c = 0; c < (int) parent.random(3, s.length()); c++) {
						s.setCharAt((int) parent.random(0, s.length()),
								(char) parent.random(0, 255));
					}
					parent.text(s.toString(), x + 10, y);
				}

				if (t.dead) {
					if (t == currentTarget) {
						scanningState = SCAN_TYPING;
					}
					targets.remove(i);

				}

				// scanning stuff
				if (t.scanCountDown > 0) {
					if (t.scanCountDown - 1 > 0) {
						t.scanCountDown--;
					} else {
						// target this motherfucker
						t.scanCountDown--;
						OscMessage myMessage = new OscMessage(
								"/system/targetting/targetObject");
						myMessage.add(t.hashCode);
						osc.send(myMessage, new NetAddress(serverIP, 12000));
						currentTarget = t;
						parent.getConsoleAudio().playClip("targetLocked");
						scanningState = SCAN_OK;
					}
					parent.pushMatrix();
					parent.translate(x, y);
					parent.rotate(PApplet.radians((parent.millis() / 10.0f) % 360));
					parent.noFill();
					parent.stroke(255, 255, 0);
					float scale = PApplet.map(t.scanCountDown, 100, 0, 10, 1);
					parent.rect(-15 * scale, -15 * scale, 30 * scale,
							30 * scale);
					parent.popMatrix();
						
					
					//parent.text("scanning: " + t.scanCountDown, x + 10, y + 10);
				}

				if (t.targetted) {
					parent.stroke(0, 255, 0);
					parent.noFill();
					// rect(x-10, y-10, 20, 20);

					

					parent.pushMatrix();
					parent.translate(x, y);
					parent.rotate(PApplet.radians((parent.millis() / 10.0f) % 360));
					parent.noFill();
					parent.stroke(255, 255, 0);
					float scale = PApplet.map(t.scanCountDown, 100, 0, 10, 1);
					parent.rect(-15, -15, 30, 30);
					parent.popMatrix();
				}

				// draw a beam out to the target if we are firing
				if (!t.beingFiredAt && firingTime + 400 < parent.millis()) {
					parent.stroke(255, 255, 0);
					parent.strokeWeight(2);
					parent.line(0, 0, x, y);
				}
				
			}
		}
	}
	
	

	protected void drawTargets2(){
		
		int sensorRange = (int) PApplet.map(sensorPower, 0f, 12f, 270,1450);
		parent.textFont(font, 12);
		fireEnabled = false;
		parent.strokeWeight(1);
		synchronized (targets) {
			for (int i = targets.size() - 1; i >= 0; i--) {
				TargetObject t = targets.get(i);
				
				float distanceToTarget = t.pos.mag();

				// update logic bits
				// if no update received for 280ms then remove this target
				if (parent.millis() - t.lastUpdateTime > 300) {
					if (t.targetted) {
						parent.getConsoleAudio().playClip("targetDestroyed");
						scanningState = SCAN_TYPING;
						scanString = "";
						if(currentTarget == t){
							currentTarget = null;
							
						}
					}
					targets.remove(i);
				}

				float lerpX = PApplet.lerp(t.lastPos.x, t.pos.x,
						(parent.millis() - t.lastUpdateTime) / 250.0f);
				float lerpY = PApplet.lerp(t.lastPos.z, t.pos.z,
						(parent.millis() - t.lastUpdateTime) / 250.0f);
				float lerpZ = PApplet.lerp(t.lastPos.y, t.pos.y,
						(parent.millis() - t.lastUpdateTime) / 250.0f);

				float screenSpaceScaleFactor = 0.0005f;
				
				float x =  lerpX * screenSpaceScaleFactor;
				float y =  lerpY * screenSpaceScaleFactor;
				float z =  lerpZ * screenSpaceScaleFactor;

				
				t.screenSpacePos.x = radarGraphics.screenX(lerpX, lerpY, lerpZ);
				t.screenSpacePos.y = radarGraphics.screenY(lerpX, lerpY, lerpZ);
				
				// set target colour
				float scaleFactor = 0.6f;
				if (distanceToTarget > sensorRange * scaleFactor) {
					radarGraphics.fill(100, 100, 100);
					x += parent.random(-5, 5);
					y += parent.random(-5, 5);
				} else if (distanceToTarget < 200) {

					radarGraphics.fill(255, 0, 0);
				} else if (distanceToTarget < 500) {
					radarGraphics.fill(255, 255, 0);
				} else {
					radarGraphics.fill(0, 255, 0);
				}

				// draw the target on the radar
				radarGraphics.pushMatrix();
				radarGraphics.noStroke();
				
				radarGraphics.translate(lerpX, lerpY, lerpZ);
				radarGraphics.sphereDetail(2);
				radarGraphics.sphere(10);
				if(t == currentTarget){
					radarGraphics.stroke(255);
				} else {
					radarGraphics.stroke(128);
				}
				radarGraphics.line(0, 0, 0, 0,0,-lerpZ);;
				//radarGraphics.line(-lerpX, -lerpY, -lerpZ, 0,0,-lerpZ);;
				
				
				

				radarGraphics.popMatrix();
				
				if (t.dead) {
					if (t == currentTarget) {
						scanningState = SCAN_TYPING;
					}
					targets.remove(i);

				}

				// scanning stuff
				if (t.scanCountDown > 0) {
					if (t.scanCountDown - 1 > 0) {
						t.scanCountDown--;
					} else {
						// target this motherfucker
						t.scanCountDown--;
						OscMessage myMessage = new OscMessage(
								"/system/targetting/targetObject");
						myMessage.add(t.hashCode);
						osc.send(myMessage, new NetAddress(serverIP, 12000));
						currentTarget = t;
						parent.getConsoleAudio().playClip("targetLocked");
						scanningState = SCAN_OK;
					}
					radarGraphics.pushMatrix();
					radarGraphics.translate(lerpX, lerpY, lerpZ);
					radarGraphics.rotate(PApplet.radians((parent.millis() / 10.0f) % 360));
					radarGraphics.noFill();
					radarGraphics.stroke(255, 255, 0);
					float scale = PApplet.map(t.scanCountDown, 100, 0, 20, 1);
					for(int i1 = 0; i1 < 4; i1++){
						scale *= 0.7f;
						radarGraphics.rect(-15 * scale, -15 * scale, 30 * scale,
								30 * scale);
					}
					
					
					radarGraphics.popMatrix();
						
					
					//parent.text("scanning: " + t.scanCountDown, x + 10, y + 10);
				}

				if (t.targetted) {
					radarGraphics.stroke(0, 255, 0);
					radarGraphics.noFill();
					// rect(x-10, y-10, 20, 20);

					

					radarGraphics.pushMatrix();
					radarGraphics.translate(lerpX, lerpY, lerpZ);
					radarGraphics.rotate(PApplet.radians((parent.millis() / 10.0f) % 360));
					radarGraphics.noFill();
					radarGraphics.stroke(255, 255, 0);
					radarGraphics.rect(-30, -30, 60, 60);
					radarGraphics.popMatrix();
				}

				// draw a beam out to the target if we are firing
				if (t.beingFiredAt && firingTime + 400 > parent.millis()) {
					radarGraphics.stroke(255, 255, 0);
					radarGraphics.strokeWeight(2);
					radarGraphics.line(0, 0, 0, lerpX, lerpY, lerpZ);
				}
				// draw a beam to the ship if the target is firing at us
				Float f = t.getStat("firing");
				if (f != null && f > 0.0f) {
					radarGraphics.stroke(255, 0, 0);
					radarGraphics.strokeWeight(4);
					radarGraphics.line(x, y, 364, 707);
				}
			}
		}
		
	
	}
	
	public void serialEvent(HardwareEvent h){
		super.serialEvent(h);
		if(h.event.equals("MOUSECLICK")){
			
			pickTarget();
		}
	}
	
	protected void pickTarget(){
		mouseRadius = 30.0f;
		for(TargetObject t : targets){
			PVector m = ((TacticalConsole)parent).mousePosition;
			
			PVector s = new PVector(t.screenSpacePos.x+10, t.screenSpacePos.y+80);
		
			if(PVector.sub(m,s).mag() < mouseRadius && t.targetted == false){
				//check the range
				float scaleFactor = 0.6f;
				float distanceToTarget = t.pos.mag();
				int sensorRange = (int) PApplet.map(sensorPower, 0f, 12f, 270,1450);

				if (distanceToTarget < sensorRange * scaleFactor) {
					ConsoleLogger.log(this, "picked target " + t.scanId);
					scanString = "" + t.scanId;
					scanTarget(scanString);
					
					break;
				}
			}
		}
		
	}
	

	protected void updateTarget(OscMessage theOscMessage) {
		int tgtHash = theOscMessage.get(0).intValue();
		synchronized (targets) {
			TargetObject t = findTargetById(tgtHash);
			boolean newTarget = false;
			if (t == null) {
				ConsoleLogger.log(this, "new target: " + tgtHash);
				t = new TargetObject();
				t.hashCode = tgtHash;
				newTarget = true;
				targets.add(t);
				parent.getConsoleAudio().playClip("newTarget");
			}
			t.scanId = theOscMessage.get(1).intValue();
			t.trackingPlayer = theOscMessage.get(2).intValue() == 1 ? true
					: false;
			t.targetted = theOscMessage.get(3).intValue() == 1 ? true
					: false;
			float x = theOscMessage.get(4).floatValue();
			float y = theOscMessage.get(5).floatValue();
			float z = theOscMessage.get(6).floatValue();
			if (newTarget) {
				t.lastPos.x = x;
				t.lastPos.y = y;
				t.lastPos.z = z;
				
			} else {

				t.lastPos.x = t.pos.x;
				t.lastPos.y = t.pos.y;
				t.lastPos.z = t.pos.z;
			}
			t.lastUpdateTime = parent.millis();
			
			t.pos = new PVector(x, y, z);
			
			
			t.stats[0] = theOscMessage.get(7).floatValue();
			// t.stats[1] = theOscMessage.get(8).floatValue();
			t.statNames[0] = theOscMessage.get(8).stringValue();
			// t.statNames[1] = theOscMessage.get(10).stringValue();
			t.name = theOscMessage.get(9).stringValue();

			// now unpack the stat string
			String statString = theOscMessage.get(10).stringValue();
			String[] pairs = statString.split(",");
			for (String p : pairs) {
				String[] vals = p.split(":");
				t.setStat(vals[0], Float.parseFloat(vals[1]));
			}
		}
		
	}
	
	public void oscMessage(OscMessage msg){
		super.oscMessage(msg);
		if (msg.checkAddrPattern("/ship/weaponState")){
			int state = msg.get(0).intValue();
			switch(state){
			
			case ShipState.WEAPON_TRANSIT_IN:
				triggerDeploymentAnimation(state);
				break;
			case ShipState.WEAPON_TRANSIT_OUT:
				triggerDeploymentAnimation(state);
				break;
			}
			
		}
		
	}
	
}
