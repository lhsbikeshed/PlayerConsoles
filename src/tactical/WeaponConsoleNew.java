package tactical;

import netP5.NetAddress;
import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import tactical.WeaponsConsole.TargetObject;
import common.ConsoleLogger;
import common.PlayerConsole;
import common.ShipState;

public class WeaponConsoleNew extends WeaponsConsole {

	
	PImage shipIcon, weaponIcon;
	private PImage weaponStatusPanelImage;
	
	long animationStartTime = 0;
	int animationDirection = 0;
	
	PVector[] weaponIconPositions = new PVector[5];
	float[] weaponIconRotations = new float[5];
	

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
		if (mode == MODE_SCANNER) {
			parent.image(bgImage, 0, 0);
			parent.fill(0, 128, 0, 100);
			int sensorSize = (int) PApplet.map(sensorPower, 0f, 12f, 270,650) ;
			parent.ellipse(351, 420, sensorSize, sensorSize);
			radarTicker += 10;
			parent.noStroke();
			int alpha = (int) PApplet.map(radarTicker, 0, sensorSize, 45f, 0f );
			parent.fill(0,255,0, alpha);
			parent.ellipse(351, 420, radarTicker, radarTicker);
			if (radarTicker > sensorRange) {
				radarTicker = 15;
			}

			
			
			drawTargets();
		}
		drawSideBar();
		
		drawWeaponStatus();
		parent.stroke(255);
		parent.fill(255);
		
		
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
				 xOffset = PApplet.map(animPos, 0, 4000, 0, -10);
			} else if(parent.getShipState().weaponState == ShipState.WEAPON_TRANSIT_IN){
				 xOffset = PApplet.map(animPos, 0, 4000, -10, 0);
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
				
				float x = 351 + lerpX * screenSpaceScaleFactor;
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
					parent.textFont(font, 14);

					String h = String.format("%.2f", t.stats[0] * 100);
					parent.text(t.name + ": " + h + "%", x + 30, y + 5);
					parent.textFont(font, 20);
					parent.text(scanCode, x + 30, y-10);
					parent.stroke(255,255,0);
					parent.line(x+30, y-15, x+20, y-15);
					parent.line(x, y, x+20, y-15);
					// are there any extended stats on this?
					Float f = t.getStat("scanning");
					if (f != null && f > 0.0f) {
						// draw a scanning effect around the target
						int maxSize = 70;
						int size = (int) PApplet.map(parent.millis() % 2000, 0,
								2000, 0, maxSize);
						parent.noFill();
						parent.strokeWeight(2);
						parent.stroke(0, 128, 255,
								PApplet.map(size, 0, maxSize, 255, 0));
						parent.ellipse(x, y, size, size);
						size = (int) PApplet.map(
								(parent.millis() + 1000) % 2000, 0, 2000, 0,
								maxSize);
						parent.stroke(0, 128, 255,
								PApplet.map(size, 0, maxSize, 255, 0));
						parent.ellipse(x, y, size, size);
					}
					f = t.getStat("chargingWeapons");
					if (f != null && f > 0.0f) {
						// warn the player that the target is charging its
						// weapons
					}
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
				if (t.beingFiredAt && firingTime + 400 > parent.millis()) {
					parent.stroke(255, 255, 0);
					parent.strokeWeight(2);
					parent.line(364, 707, x, y);
				}
				// draw a beam to the ship if the target is firing at us
				Float f = t.getStat("firing");
				if (f != null && f > 0.0f) {
					parent.stroke(255, 0, 0);
					parent.strokeWeight(4);
					parent.line(x, y, 364, 707);
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
