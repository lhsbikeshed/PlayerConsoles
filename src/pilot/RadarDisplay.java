package pilot;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.Rot;
import common.RotOrder;
import common.ShipState;
import common.UsefulShit;

public class RadarDisplay extends Display {
	Object lock = new Object();
	PImage overlayImage, indicatorImage;
	int sensorPower = 2;
	int propulsionPower = 2;
	RadarObject targetted;
	float zoomLevel = 0.1f;
	float maxDist = 0.0f;

	// HashMap radarList = new HashMap();

	RadarObject[] radarList = new RadarObject[100];
	RadarObject r = new RadarObject();

	// screen space 2d vector representing the direction the pilot should fly to
	// get to the targetted object
	PVector guideVector = new PVector(0, 0);
	PVector tempVec = new PVector(0, 0);
	boolean useGuides = true;
	PImage guideArrow;

	int sectorX, sectorY, sectorZ;

	Rot backgroundRotation = Rot.IDENTITY;
	ShipState shipState;
	


	public RadarDisplay(PlayerConsole parent) {
		super(parent);
		shipState = parent.getShipState();
		overlayImage = parent.loadImage("pilotconsole/overlayImage.png");
		indicatorImage = parent.loadImage("pilotconsole/indicator.png");
		for (int i = 0; i < 100; i++) {
			radarList[i] = new RadarObject();
			radarList[i].active = false;
		}
		guideArrow = parent.loadImage("pilotconsole/guideArrowLeft.png");
		sectorX = sectorY = sectorZ = 0;
	}

	@Override
	public void draw() {
		parent.background(0, 0, 0);
		zoomLevel = 0.5f; // map(mouseY, 0, height, 0.01f, 1.0f);
		drawRadar();
	}

	public void drawAxis(int highlight) {

		// x axis
		parent.stroke(128, 0, 0);
		parent.strokeWeight(1);
		parent.line(-1000, 0, 0, 1000, 0, 0);
		parent.line(1000, 0, -10, 1000, 0, 10);

		parent.pushMatrix();
		parent.rotateX(PApplet.radians(-90));
		parent.noFill();
		parent.strokeWeight(1);
		drawRadarCircle(5, 200, highlight);
		parent.popMatrix();

		// z axis
		parent.stroke(0, 0, 128);
		parent.line(0, 0, -1000, 0, 0, 1000);
		parent.line(-10, 0, 1000, 10, 0, 1000);

		parent.stroke(0, 128, 0);
		// popMatrix();
	}

	public void drawGuides() {
		parent.pushMatrix();
		parent.translate(934, 645);
		parent.noFill();

		parent.stroke(255);
		parent.strokeWeight(2);
		parent.fill(0);
		parent.rect(-50, -50, 100, 130);
		if (guideVector.mag() < 0.05f) {
			parent.fill(0, 125, 0);
		}
		if (!useGuides) {
			parent.fill(0);
		}
		parent.ellipse(0, 0, 80, 80);
		parent.fill(255);
		if (useGuides) {
			parent.line(0, 0, guideVector.x * 100.0f, guideVector.y * 100.0f);
		}
		parent.textFont(font, 12);
		parent.fill(255);
		parent.text(" PILOT\r\nASSIST", -30, 55);
		parent.popMatrix();

		if (useGuides == false) {
			return;
		}
		parent.pushMatrix();

		// left is at 28,326
		if (guideVector.x < 0) {
			parent.tint(255, 255, 255,
					(int) PApplet.map(Math.abs(guideVector.x), 0, 1, 0, 255));

			parent.image(guideArrow, 28, 326);
		} else {
			parent.tint(255, 255, 255,
					(int) PApplet.map(guideVector.x, 0, 1, 0, 255));
			parent.translate(994, 439);
			parent.rotate(PApplet.radians(180));

			parent.image(guideArrow, 0, 0);
		}
		parent.popMatrix();
		parent.pushMatrix();
		if (guideVector.y < 0) {
			parent.tint(255, 255, 255,
					(int) PApplet.map(Math.abs(guideVector.y), 0, 1, 0, 255));
			parent.translate(570, 68);
			parent.rotate(PApplet.radians(90));

			parent.image(guideArrow, 0, 0);
		} else {
			parent.tint(255, 255, 255,
					(int) PApplet.map(guideVector.y, 0, 1, 0, 255));
			parent.translate(455, 742);
			parent.rotate(PApplet.radians(-90));

			parent.image(guideArrow, 0, 0);
		}

		parent.popMatrix();

		parent.noTint();
	}

	public void drawRadar() {

		parent.pushMatrix();

		parent.lights();
		parent.ambientLight(255, 255, 255);
		parent.noTint();
		// move the camera --------------------------------------
		parent.translate(parent.width / 2, parent.height / 2);
		parent.rotateX(PApplet.radians(345)); // 326
		parent.rotateY(PApplet.radians(180));

		drawAxis((int) ((parent.millis() % 1750.0f) / 200));

		// draw the background cube ------------------------------
		parent.box(10);
		parent.pushMatrix();
		Rot newRot = Rot.slerp(shipState.lastShipRot,
				shipState.shipRot,
				(parent.millis() - shipState.lastTransformUpdate) / 250.0f,
				false);
		float[] ang = newRot.getAngles(RotOrder.XYZ);
		if (ang != null) {
			parent.rotateX(-ang[0]);
			parent.rotateY(-ang[1]);
			parent.rotateZ(ang[2]);
		}
		// noFill();
		parent.strokeWeight(2);
		parent.stroke(20, 20, 20);
		parent.fill(12, 30, 15);
		parent.box(1500);

		parent.popMatrix();

		// main radar
		parent.strokeWeight(1);
		parent.stroke(0, 0, 0);

		// use this to calculate which target is most distant from the ship,
		// scale the zoom level based on this

		parent.fill(255, 255, 0, 255);
		parent.sphere(1);
		parent.fill(0, 0, 255);
		zoomLevel = PApplet.map(maxDist, 0f, 5000f, 1.8f, 0.2f);
		parent.scale(zoomLevel);
		// println(zoomLevel);
		maxDist = 0;
		float distanceToShip = 0;
		synchronized (lock) {
			for (int i = 0; i < 100; i++) {

				RadarObject rItem = radarList[i];
				if (rItem.active == true) {
					parent.pushMatrix();

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

					// check if this is the farthest target from the ship, used
					// to calculate scaling
					rItem.distance = newPos.mag();
					if (rItem.distance > maxDist) {
						maxDist = rItem.distance;
					}

					// add some random jiggle into the target if its too far
					// away
					if (rItem.distance > 1000) {
						newPos.x += parent.random(-20, 20);
						newPos.y += parent.random(-20, 20);
						newPos.z += parent.random(-20, 20);
					}

					parent.stroke(0, 255, 0);
					// line to base
					// line(-r.position.x, 0, r.position.z, -r.position.x,
					// -r.position.y, r.position.z);
					parent.line(-newPos.x, 0, newPos.z, -newPos.x, -newPos.y,
							newPos.z);
					// circle at base
					parent.pushMatrix();
					parent.translate(-newPos.x, 0, newPos.z);
					parent.rotateX(PApplet.radians(-90));
					parent.fill(0, 50, 0);
					parent.strokeWeight(1);

					// ellipse(0, 0, 20, 20);
					parent.rect(-10, 10, 20, 20);
					parent.popMatrix();

					// sphere and text

					// translate(-r.position.x, -r.position.y, r.position.z);
					rItem.screenPos.x = parent.screenX(-newPos.x, -newPos.y,
							newPos.z);
					rItem.screenPos.y = parent.screenY(-newPos.x, -newPos.y,
							newPos.z);
					parent.translate(-newPos.x, -newPos.y, newPos.z);
					parent.noStroke();
					int alpha = (int) PApplet.lerp(255, 0,
							(parent.millis() - rItem.lastUpdateTime) / 250.0f);
					int c = rItem.displayColor;
					parent.fill(c);

					// sphere(10);
					if (newPos.y >= 0) {

						parent.image(indicatorImage, -16, -16, 32, 32);
					} else {
						parent.scale(1, -1);
						parent.image(indicatorImage, -16, -16, 32, 32);
					}
					parent.popMatrix();

					// workout what needs cleaning

					if (rItem.lastUpdateTime < parent.millis() - 500.0f) {
						// its dead jim
						// removeList.add(new Integer(i));
						ConsoleLogger.log(this, "removing id: " + rItem.id);
						rItem.active = false;
					}
				}
			}
			parent.popMatrix();

			// now do text and other screen space stuff
			targetted = null;
			for (int i = 0; i < 100; i++) {

				RadarObject rItem = radarList[i];
				if (rItem.active) {

					parent.textFont(font, 13);

					if (rItem.distance > 1000) {
						StringBuilder s = new StringBuilder(rItem.name);
						for (int c = 0; c < (int) parent.random(3, s.length()); c++) {
							s.setCharAt((int) parent.random(0, s.length()),
									(char) parent.random(0, 255));
						}

						parent.fill(40);
						parent.text(s.toString(), rItem.screenPos.x + 5,
								rItem.screenPos.y + 10);
					} else {
						parent.fill(rItem.displayColor);
						parent.text(rItem.name, rItem.screenPos.x + 5,
								rItem.screenPos.y + 10);
					}
					// textFont(font, 10);
					// text(r.statusText,r.screenPos.x + 5, r.screenPos.y + 20);

					if (rItem.targetted) {

						targetted = radarList[i];
						parent.noFill();
						parent.stroke(255, 255, 0);
						parent.pushMatrix();
						parent.translate(rItem.screenPos.x, rItem.screenPos.y);
						parent.rotateZ(PApplet.radians((parent.millis() / 10.0f) % 260));
						parent.rect(-15, -15, 30, 30);
						parent.popMatrix();

						int midX = (int) ((660 - rItem.screenPos.x) * 0.33f);
						parent.stroke(255, 255, 0);
						parent.line(660, 190, rItem.screenPos.x + midX, 190);
						parent.line(rItem.screenPos.x + midX, 190,
								rItem.screenPos.x, rItem.screenPos.y);
					}

					// if this target is "pinging" then draw a radiobeacon
					// highlight
					Float f = rItem.getStat("pinging");
					parent.noStroke();
					// strokeWeight(2.0);
					// stroke(255,255,0);
					if (f != null && f > 0.0) {
						int radius = (int) PApplet.map(parent.millis() % 3000,
								0, 3000, 0, 100);
						int alpha = (int) PApplet.map(parent.millis() % 3000,
								0, 3000, 255, 0);
						parent.fill(255, 255, 0, alpha);
						parent.ellipse(rItem.screenPos.x, rItem.screenPos.y,
								radius, radius);

						radius = (int) PApplet.map(
								(parent.millis() + 1500) % 3000, 0, 3000, 0,
								100);
						alpha = (int) PApplet.map(
								(parent.millis() + 1500) % 3000, 0, 3000, 255,
								0);
						parent.fill(255, 255, 0, alpha);
						parent.ellipse(rItem.screenPos.x, rItem.screenPos.y,
								radius, radius);
					}
				}
			}
		}
		// turn on pilot guides if we have a highlighted target
		// turn off if not
		//
		if (targetted != null) {
			useGuides = true;
			tempVec.x = targetted.position.x;
			tempVec.y = targetted.position.z;
			float yRotation = (270 + PApplet.degrees(heading(tempVec.x,
					tempVec.y))) % 360;
			if (yRotation > 0 && yRotation < 180) { // right hand side of ship
				guideVector.x = -PApplet.map(yRotation, 0, 180, 0, 1);
			} else {
				guideVector.x = -PApplet.map(yRotation, 180, 360, -1, 0);
			}
			tempVec.x = targetted.position.z;
			tempVec.y = targetted.position.y;

			float xRotation = PApplet.degrees(heading(tempVec.x, tempVec.y));
			if (xRotation > -90 && xRotation < 0) {
				guideVector.y = -PApplet.map(xRotation, -90, 0, 1, 0);
			} else if (xRotation < 90 && xRotation > 0) {
				guideVector.y = -PApplet.map(xRotation, 90, 0, -1, 0);
			}
		} else {
			useGuides = false;
		}

		// popMatrix();
		parent.noLights();
		parent.hint(PConstants.DISABLE_DEPTH_TEST);
		parent.image(overlayImage, 0, 0, parent.width, parent.height);

		parent.textFont(font, 18);
		parent.fill(0, 255, 255);

		parent.text("Prop: " + (propulsionPower * 33) + "%", 680, 630);

		parent.text("speed: " + (int) shipState.shipVelocity, 680, 660);
		
		
		parent.text("Afterburner: ", 690, 742);
		
		String state = "";
		if(shipState.afterburnerCharging){
			state = "charging";
			parent.fill(255,0,0);
		} else {
			state = "READY";
			if(parent.globalBlinker){
				parent.fill(0,255,0);
			} else {
				parent.fill(0,120,0);
			}
		}
		
		parent.text( state, 857, 742);
		parent.fill(255, 255, 0);
		if (targetted != null) {
			parent.textFont(font, 20);
			parent.text(targetted.name, 675, 70);
			parent.textFont(font, 15);
			parent.text(targetted.statusText, 675, 100);
		}
		parent.text("Sector (" + sectorX + "," + sectorY + "," + sectorZ + ")",
				41, 740);

		drawGuides();
		
		
		
	}

	void drawRadarCircle(int num, int sizing, int highlight) {
		int radius = sizing;
		for (int i = 0; i < num; i++) {
			if (i == highlight) {
				parent.stroke(0, 30, 0);
			} else {
				parent.stroke(0, 30, 0);
			}
			parent.ellipse(0, 0, radius, radius);
			radius += sizing;
		}
		parent.stroke(0, 255, 0);
	}

	public int findRadarItemById(int id) {
		for (int i = 0; i < 100; i++) {
			if (radarList[i].id == id) {
				return i;
			}
		}
		return -1;
	}

	public int getNewRadarItem() {
		for (int i = 0; i < 100; i++) {
			if (radarList[i].active == false) {
				return i;
			}
		}
		return -1;
	}

	// FFUUU P2
	public float heading(float x, float y) {
		float angle = (float) Math.atan2(-y, x);
		return -1 * angle;
	}

	/* incoming osc message are forwarded to the oscEvent method. */
	@Override
	public void oscMessage(OscMessage theOscMessage) {

		/* print the address pattern and the typetag of the received OscMessage */

		if (theOscMessage.checkAddrPattern("/radar/update")) {
			synchronized (lock) {
				// get the id

				int id = theOscMessage.get(0).intValue();
				int rId = findRadarItemById(id);
				boolean newItem = false;
				if (rId == -1) {
					rId = getNewRadarItem();
					ConsoleLogger.log(this, "new item : " + rId + " - " + id);
					if (theOscMessage.get(1).stringValue()
							.equals("INCOMING DEBRIS")) {
						parent.getConsoleAudio().playClip("collisionAlert");
					} else {

						parent.getConsoleAudio().playClip("newTarget");
					}

					newItem = true;
				}

				radarList[rId].id = id;
				radarList[rId].active = true;

				radarList[rId].lastUpdateTime = parent.millis();
				radarList[rId].name = theOscMessage.get(1).stringValue();
				if (newItem) {
					radarList[rId].lastPosition.x = theOscMessage.get(2)
							.floatValue();
					radarList[rId].lastPosition.y = theOscMessage.get(3)
							.floatValue();
					radarList[rId].lastPosition.z = theOscMessage.get(4)
							.floatValue();
					radarList[rId].clearStats();
				} else {
					radarList[rId].lastPosition.x = radarList[rId].position.x;
					radarList[rId].lastPosition.y = radarList[rId].position.y;
					radarList[rId].lastPosition.z = radarList[rId].position.z;
				}

				radarList[rId].position.x = theOscMessage.get(2).floatValue();
				radarList[rId].position.y = theOscMessage.get(3).floatValue();
				radarList[rId].position.z = theOscMessage.get(4).floatValue();
				// println("1:" + radarList[rId].position);
				// println("2:" + radarList[rId].lastPosition);

				String colour = theOscMessage.get(5).stringValue();
				String[] splitColour = colour.split(":");
				radarList[rId].displayColor = UsefulShit.makeColor(
						(int) (Float.parseFloat(splitColour[0]) * 255),
						(int) (Float.parseFloat(splitColour[1]) * 255),
						(int) (Float.parseFloat(splitColour[2]) * 255));
				// radarList[rId].lastUpdateTime = millis();

				radarList[rId].statusText = theOscMessage.get(6).stringValue();
				radarList[rId].targetted = theOscMessage.get(7).intValue() == 1 ? true
						: false;

				// now unpack the stat string
				String statString = theOscMessage.get(8).stringValue();
				String[] pairs = statString.split(",");
				for (String p : pairs) {
					String[] vals = p.split(":");
					radarList[rId].setStat(vals[0], Float.parseFloat(vals[1]));
				}
			}
		} else if (theOscMessage.checkAddrPattern("/control/subsystemstate") == true) {

			sensorPower = theOscMessage.get(2).intValue();
			propulsionPower = theOscMessage.get(0).intValue();
		} else if (theOscMessage.checkAddrPattern("/radar/wayPointReached")) {
			parent.getConsoleAudio().playClip("waypointReached");
		} 
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
	}

	public void setSector(int x, int y, int z) {
		sectorX = x;
		sectorY = y;
		sectorZ = z;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}
}
