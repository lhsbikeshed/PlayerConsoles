package tactical;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
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

public class WeaponsConsole extends Display {

	public class TargetObject implements Comparable<TargetObject> {
		public int hashCode = 0;
		public PVector pos = new PVector(0, 0, 0);
		public PVector lastPos = new PVector(0, 0, 0);
		public long lastUpdateTime = 0;
		public int scanId = 0;
		public boolean trackingPlayer = false;
		public boolean targetted = false;
		public int scanCountDown = -1;
		public boolean beingFiredAt = false;
		public boolean dead = false;
		public String name = "missile";
		public float[] stats = new float[2];
		public String[] statNames = new String[2];
		public PVector screenSpacePos = new PVector(0,0);

		public int fadeTimer = 0;
		
		
		
		protected HashMap<String, Float> statMap = new HashMap<String, Float>();

		public TargetObject() {
			
		}

		public void clearStats() {
			statMap.clear();
		}

		@Override
		public int compareTo(TargetObject other) {
			return (int) (this.pos.mag() - other.pos.mag());
		}

		public Float getStat(String name) {
			Float f = statMap.get(name);
			return f;
		}

		public void setStat(String name, float val) {
			Float f = new Float(val);
			// println("setting stat: " + name);
			statMap.put(name, val);
		}
	}

	public static final int MODE_SCANNER = 0;

	// images etc
	PImage[] banners = new PImage[3];
	PImage bgImage;
	PImage titleImage, hullStateImage;
	PImage beamButton, beamButtonD;
	
	float firingTime = 0; // time a laser firing started

	// current screen mode
	int mode = MODE_SCANNER;
	// targetting crap
	List<TargetObject> targets = Collections
			.synchronizedList(new ArrayList<TargetObject>());

	TargetObject currentTarget;

	int sensorPower = 0;
	int beamPower = 0;
	boolean weaponsDeployed = false;
	
	long smartBombFireTime = 0;
	long missileStartTime = 0;
	float scannerAngle = 0;

	String scanString = "";

	float maxBeamRange = 1300;

	int radarTicker = 175;
	
	// states
	boolean flareEnabled = false;
	boolean offline = false;
	boolean fireEnabled = false;
	boolean blinkenBool = false;

	long blinkenBoolTimer = 0;
	public static final int SCAN_TYPING = 0;
	public static final int SCAN_SCANNING = 1;
	public static final int SCAN_FAILED = 2;
	public static final int SCAN_OK = 3;

	//int scanningState = SCAN_TYPING;

	public boolean hookArmed = false;

	PImage shipIcon, weaponIcon;
	private PImage weaponStatusPanelImage;
	
	long animationStartTime = 0;
	int animationDirection = 0;
	
	PVector[] weaponIconPositions = new PVector[5];
	float[] weaponIconRotations = new float[5];
	
	PGraphics radarGraphics;
	
	float mouseRadius = 20f;
	
	float screenSpaceScaleFactor = 0.0005f;
	
	// sensor power to range mapping
	//int[] sensorRanges = { 370, 580, 900, 1300 };
	OscP5 osc;

	String serverIP = "";

	
	public WeaponsConsole(PlayerConsole parent) {
		super(parent);
		osc = parent.getOscClient();
		serverIP = parent.getServerIP();
		
	
		titleImage = parent.loadImage("tacticalconsole/weaponsTitle.png");
		
		beamButton = parent.loadImage("tacticalconsole/firebeam.png");
		
		beamButtonD = parent.loadImage("tacticalconsole/firebeamD.png");
		

		hullStateImage = parent.loadImage("tacticalconsole/hulldamageoverlay.png");
		
		bgImage = parent.loadImage("tacticalconsole/tacticalscreen2.png");
		shipIcon = parent.loadImage("tacticalconsole/weaponsScreen/weaponShip.png");
		weaponIcon = parent.loadImage("tacticalconsole/weaponsScreen/weaponIcon.png");
		weaponStatusPanelImage = parent.loadImage("tacticalconsole/weaponStatusPanel.png");
		// -441, -11
		weaponIconPositions[0] = new PVector(92, 678);
		weaponIconRotations[0] = 0;
		weaponIconPositions[1] = new PVector(175, 699);
		weaponIconRotations[1] = 180f;
		weaponIconPositions[2] = new PVector(104, 657);
		weaponIconRotations[2] = 45f;
		weaponIconPositions[3] = new PVector(181, 671);
		weaponIconRotations[3] = 135f;
		weaponIconPositions[4] = new PVector(124, 713);
		weaponIconRotations[4] = -90f;
		
		radarGraphics = parent.createGraphics(680, 700, PConstants.P3D);
	
	}

	@Override
	public void start() {
		offline = false;
		targets = new ArrayList<TargetObject>();
		OscMessage msg = new OscMessage("/system/ship/getPowerLevels");
		OscP5.flush(msg, new NetAddress(serverIP, 12000));
	}

	@Override
	public void stop() {
		offline = false;
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

	private void fireDecoy(){
		parent.getShipState().smartBombsLeft--;
		smartBombFireTime = parent.millis();
		
	}

	private void fireSmartBomb() {
		if (parent.getShipState().smartBombsLeft > 0) {
			if (smartBombFireTime + 1000 < parent.millis()) {
	
				OscMessage myMessage = new OscMessage(
						"/system/targetting/fireFlare");
				osc.send(myMessage, new NetAddress(serverIP, 12000));
	
				TacticalHardwareController.instance.setPowerLevel(3, 80); //force power levels to always be charged
				
			}
		} else {
			// warn we have no flares left
		}
		
	}

	private void fireLaser(int bank) {
		OscMessage myMessage = new OscMessage(
				"/system/targetting/fireAtTarget");
		osc.send(myMessage, new NetAddress(serverIP, 12000));
		if (currentTarget != null && currentTarget.pos.mag() < maxBeamRange) {
			parent.getConsoleAudio().playClip("firing");
			//clear the power level on that bank
			TacticalHardwareController.instance.setPowerLevel(bank, 0);
		} else {
			parent.getConsoleAudio().playClip("outOfRange");
		}
	
		ConsoleLogger.log(this, "Fire at target");
		return;
		
	}

	@Override
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
	
		
		//radar base
		radarGraphics.fill(0, 128, 0, 100);
		radarGraphics.noStroke();
		int sensorSize = (int) PApplet.map(sensorPower, 0f, 12f, 270,1450) ;
		radarGraphics.ellipse(0, 0, 1450, 1450);
		radarGraphics.stroke(128);
		
		radarGraphics.pushMatrix();
		
		//angle pips
		for(int i=0; i < 24; i++){
			radarGraphics.rotate((float)((Math.PI * 2f) / 24));
			if((i + 1 ) % 3 == 0){
				radarGraphics.line(0, 700, 0, 850);
			} else {
				radarGraphics.line(0, 700, 0, 750);
			
			}
		}
		radarGraphics.popMatrix();
	
		//radar range
		radarGraphics.ellipse(0, 0, sensorSize, sensorSize);
		
		//cross lines
		radarGraphics.stroke(0,0,255);
		radarGraphics.line(-725,0,0,725,0,0);
		radarGraphics.stroke(255,0,0);
		radarGraphics.line(0,-725,0,0,725,0);
		
		
		
		
		//ship
		radarGraphics.box(30);
		
		//radar pulse
		radarTicker += 20;
		radarGraphics.noStroke();
		int alpha = (int) PApplet.map(radarTicker, 0, sensorSize, 45f, 0f );
		radarGraphics.fill(0,255,0, alpha);
		//radarGraphics.ellipse(0, 0, radarTicker, radarTicker);
		radarGraphics.sphereDetail(20);
		radarGraphics.sphere(radarTicker/2f);
		if (radarTicker > sensorRange) {
			radarTicker = 15;
		}
	
		
		
		drawTargets();
		
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

	void drawSideBar() {
		parent.image(titleImage, 7, 5);
		// draw sidebar stuff
		parent.fill(255, 255, 255);
		parent.textFont(font, 56);
		parent.text(parent.getShipState().smartBombsLeft, 908, 670);
		// power gauges in bottom left
		parent.noStroke();
		int f = (int) PApplet.map(beamPower, 0f, 12f, 255f, 0f);
		parent.fill(f, 255-f, 0);
		parent.rect(801, 708, 25, PApplet.map(beamPower, 0f, 12f, 0f, -90));
		f = (int) PApplet.map(sensorPower, 0f, 12f, 255f, 0f);
		parent.fill(f, 255-f, 0);
		parent.rect(743, 708, 25, PApplet.map(sensorPower, 0f, 12f, 0f, -90));

		// the target list on the right hand side
		parent.textFont(font, 14);
		int sensorRange = (int) PApplet.map(sensorPower, 0f, 12f, 270,1300); //sensorRanges[sensorPower / 4];
		synchronized (targets) {
			Collections.sort(targets); // sorted by distance from ship
			int ypos = 144;
			for (TargetObject t : targets) {
				if (t.targetted) {
					parent.fill(255, 0, 0);
				} else {
					if (t.pos.mag() < sensorRange) {
						parent.fill(0, 255, 0);
					} else {
						parent.fill(100, 100, 100);
					}
				}
				if (t.pos.mag() > sensorRange) {
					parent.text("???", 710, ypos);
				} else {
					parent.text(t.scanId, 710, ypos);
				}
				String h = String.format("%.0f", t.pos.mag());
				parent.text(h, 780, ypos);

				String name = t.name;
				if (name.length() > 12) {
					name = name.substring(0, 12) + "..";
				}
				if (t.pos.mag() > sensorRange) {
					name = "???";
				}
				parent.text(name, 855, ypos);

				if (ypos + 20 > 400) {
					break;
				} else {
					ypos += 20;
				}
			}
		}

		// text in the scanning ID field
		// set its colour based on what its doing
		parent.fill(255);
		parent.textFont(font, 30);
		
		parent.fill(255);

		if (blinkenBool && fireEnabled) {
			parent.image(beamButton, 714, 431);
		}

	}

	/* weapon status panel, shows deployment state and health of each451, 598  delta: -441, -11*/
	protected void drawWeaponStatus(){
		parent.image(weaponStatusPanelImage, 10, 587);
		
		
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
		parent.image(shipIcon, 91, 632);
		
		
		
	}

	/* stuff that needs to be drawn in screen space*/
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
					radarGraphics.fill(255,255,0, 155+ t.fadeTimer *5);
					
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

	/* draw targets in 3d space */
	protected void drawTargets(){
		
		int sensorRange = (int) PApplet.map(sensorPower, 0f, 12f, 270,1450);
		parent.textFont(font, 12);
		fireEnabled = false;
		parent.strokeWeight(1);
		synchronized (targets) {
			for (int i = targets.size() - 1; i >= 0; i--) {
				TargetObject t = targets.get(i);
				
				float distanceToTarget = t.pos.mag();
				t.fadeTimer--;
				if(t.fadeTimer <= 0){
					t.fadeTimer = 0;
				}
	
				// update logic bits
				// if no update received for 280ms then remove this target
				if (parent.millis() - t.lastUpdateTime > 300) {
					if (t.targetted) {
						parent.getConsoleAudio().playClip("targetDestroyed");
				
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
	
					radarGraphics.fill(255, 0, 0, t.fadeTimer*2);
				} else if (distanceToTarget < 500) {
					radarGraphics.fill(255, 255, 0, t.fadeTimer*2);
				} else {
					radarGraphics.fill(0, 255, 0, t.fadeTimer*2);
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

	// find a target by hashcode
	protected TargetObject findTargetById(int id) {
		for (TargetObject t : targets) {
			if (t.hashCode == id) {
				return t;
			}
		}
		return null;
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {

		if (theOscMessage
				.checkAddrPattern("/tactical/weapons/targetUpdate")) {

			updateTarget(theOscMessage);
		} else if (theOscMessage
				.checkAddrPattern("/tactical/weapons/targetRemove")) {
			removeTarget(theOscMessage);
		} else if (theOscMessage
				.checkAddrPattern("/tactical/weapons/firingAtTarget")) {
			firingAtTarget(theOscMessage);
		} else if (theOscMessage.checkAddrPattern("/system/targetting/smartBombOk")){
			fireDecoy();
			
		} else if (theOscMessage.checkAddrPattern("/ship/weaponState")){
			int state = theOscMessage.get(0).intValue();
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

	protected void firingAtTarget(OscMessage theOscMessage) {
		synchronized (targets) {
			int tgtHash = theOscMessage.get(0).intValue();
			TargetObject t = findTargetById(tgtHash);
			if (t != null) {
				t.beingFiredAt = true;
				firingTime = parent.millis();
			}
		}
		
	}

	protected void removeTarget(OscMessage theOscMessage) {
		synchronized (targets) {
			int tgtHash = theOscMessage.get(0).intValue();
			TargetObject t = findTargetById(tgtHash);
			if (t != null) {
				t.dead = true;
				t.targetted = false;
				ConsoleLogger.log(this, "target removed");
				if (t.targetted) {
					parent.getConsoleAudio().playClip("targetDestroyed");
				
				}
				
			}
		}
		
	}

	protected void updateTarget(OscMessage theOscMessage) {
		int tgtHash = theOscMessage.get(0).intValue();
		synchronized (targets) {
			TargetObject t = findTargetById(tgtHash);
			boolean doNotInterpolate = false;
			if (t == null) {
				ConsoleLogger.log(this, "new target: " + tgtHash);
				t = new TargetObject();
				t.hashCode = tgtHash;
				doNotInterpolate = true;
				targets.add(t);
				parent.getConsoleAudio().playClip("newTarget");
				t.fadeTimer = 0;
			}
			
			//check for the donotinterpolate flag
			if(theOscMessage.get(11).intValue() == 1){
				doNotInterpolate = true;
			}
			
			t.scanId = theOscMessage.get(1).intValue();
			t.trackingPlayer = theOscMessage.get(2).intValue() == 1 ? true
					: false;
			t.targetted = theOscMessage.get(3).intValue() == 1 ? true
					: false;
			float x = theOscMessage.get(4).floatValue();
			float y = theOscMessage.get(5).floatValue();
			float z = theOscMessage.get(6).floatValue();
			if (doNotInterpolate) {
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
			
			if(t.pos.mag() < radarTicker / 2f && t.fadeTimer <= 0){
				t.fadeTimer = 40;
			}
			
		}
	
		
	
	}

	void scanTarget(String scanId) {

		currentTarget = null;
		ConsoleLogger.log(this, "scan start");

		int sId = 0;
		try {
			sId = Integer.parseInt(scanId);
			// find what were scanning
			boolean targetFound = false;
			synchronized (targets) {
				for (TargetObject t : targets) {
					if (sId == t.scanId) {
						t.scanCountDown = 25;//(13 - sensorPower) * 5;
						targetFound = true;
					} else {
						if (t.targetted) {
							t.scanCountDown = -1;
							t.targetted = false;
							t.beingFiredAt = false;
							OscMessage myMessage = new OscMessage(
									"/system/targetting/untargetObject");
							myMessage.add(t.hashCode);
							osc.send(myMessage, new NetAddress(serverIP,
									12000));
						}
					}
				}

				if (targetFound) {
					parent.getConsoleAudio().playClip("targetting");
				
				} else {
					parent.getConsoleAudio().playClip("outOfRange");
					
				}
			}
		} catch (NumberFormatException e) {
		}
		// scanString = "";
	}

	//pick a target under the mouse cursor and target it
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
	
	@Override
	public void serialEvent(HardwareEvent evt) {
		if(evt.event.equals("KEYPAD")){

			int action = evt.id;
			if (action == TacticalHardwareController.KP_LASER) {
				int bank = evt.value;
				fireLaser(bank);
			}
	
			if (action == TacticalHardwareController.KP_DECOY) {
				fireSmartBomb();
				
				return;
			}
	
			
		} else if (evt.event.equals("KEY")){
			if(evt.id == KeyEvent.VK_F){
				fireLaser(0);
			} else if (evt.id == KeyEvent.VK_M){
				fireSmartBomb();
			}
			
		} else if(evt.event.equals("MOUSECLICK")){
			
			pickTarget();
		}
	}
}
