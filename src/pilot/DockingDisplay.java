package pilot;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

public class DockingDisplay extends Display {

	PImage bgImage, overlayImage;

	PImage shipImg, feetImg;
	// 22

	boolean landed = false;
	boolean clamped = true;
	boolean bayGravity = true;

	public static final int NO_SIGNAL = 0;
	public static final int BEACON_LOCKING = 1;
	public static final int BEACON_LOCKED = 2;
	int lockingState = NO_SIGNAL;

	boolean speedWarning = false;
	long lastSpeedWarning = 0;

	int[][] warningSpeeds = { { 40, 8 }, { 80, 10 }, { 150, 20 }, { 350, 35 } };
	// distance, speed

	int undercarriageState = 1;
	private String[] undercarriageStrings = { "up", "down", "Lowering..",
			"Raising.." };

	PVector shipPos = new PVector(0, 0, 0);
	PVector shipRot = new PVector(0, 0, 0);

	PVector lastShipPos = new PVector(0, 0, 0);
	long lastPosUpdate = 0;
	float lastRotation = 0;
	// bay img is 600x300, 95,370

	float distanceScale = 0.9f;
	float distance = 100.0f;

	public DockingDisplay(PlayerConsole parent) {
		super(parent);
		bgImage = parent.loadImage("pilotconsole/launchdisplay.png");
		shipImg = parent.loadImage("pilotconsole/shipbehind.png");
		feetImg = parent.loadImage("pilotconsole/shipfeet.png");
		overlayImage = parent.loadImage("pilotconsole/dockingOverlay.png");
	}

	public float CurveAngle(float start, float end, float step) {
		float from = PApplet.radians(start);
		float to = PApplet.radians(end);
		// Ensure that 0 <= angle < 2pi for both "from" and "to"
		while (from < 0) {
			from += PConstants.TWO_PI;
		}
		while (from >= PConstants.TWO_PI) {
			from -= PConstants.TWO_PI;
		}

		while (to < 0) {
			to += PConstants.TWO_PI;
		}
		while (to >= PConstants.TWO_PI) {
			to -= PConstants.TWO_PI;
		}

		if (Math.abs(from - to) < PConstants.PI) {
			// The simple case - a straight lerp will do.
			return PApplet.lerp(from, to, step);
		}

		// If we get here we have the more complex case.
		// First, increment the lesser value to be greater.
		if (from < to) {
			from += PConstants.TWO_PI;
		} else {
			to += PConstants.TWO_PI;
		}

		float retVal = PApplet.lerp(from, to, step);

		// Now ensure the return value is between 0 and 2pi
		if (retVal >= PConstants.TWO_PI) {
			retVal -= PConstants.TWO_PI;
		}
		return retVal;
	}

	@Override
	public void draw() {
		distanceScale = PApplet.map(distance, 0, 350, 0.5f, 4.0f);

		parent.background(0, 0, 0);
		parent.image(overlayImage, 8, 7);
		parent.stroke(255);
		parent.strokeWeight(1);
		parent.line(parent.width / 2, 85, parent.width / 2, parent.height);
		parent.line(0, parent.height / 2, parent.width, parent.height / 2);

		// image(bgImage, 0, 0, width, height);
		parent.textFont(font, 15);
		parent.fill(255, 255, 0);

		parent.text("Docking Clamp: "
				+ (clamped == true ? "Engaged" : "Disengaged"), 60, 110);
		parent.text("Undercarriage: "
				+ undercarriageStrings[undercarriageState], 60, 130);
		parent.text("Bay Gravity: " + (bayGravity == true ? "On" : "Off"), 60,
				150);
		if (landed) {
			parent.text("Floor Contact", 60, 170);
		}

		parent.noFill();
		for (int i = 1; i < 4; i++) {
			parent.ellipse(parent.width / 2, parent.height / 2, i * 150,
					i * 150);
		}

		if (lockingState != NO_SIGNAL) {
			parent.pushMatrix();

			PVector lerpPos = new PVector(0, 0, 0);
			lerpPos.x = PApplet.lerp(lastShipPos.x, shipPos.x,
					(parent.millis() - lastPosUpdate) / 200.0f);
			lerpPos.y = PApplet.lerp(lastShipPos.y, shipPos.y,
					(parent.millis() - lastPosUpdate) / 200.0f);

			int screenX = (int) PApplet.map(lerpPos.x, 45.0f, -45.0f, 0f,
					parent.width);
			int screenY = (int) PApplet.map(lerpPos.y, 45.0f, -45.0f, 0f,
					parent.height);

			parent.translate(screenX, screenY);
			parent.scale(distanceScale);
			// ship
			parent.strokeWeight(5);
			parent.noFill();

			// calc colour
			float d = Math.abs(lerpPos.mag());
			if (d < 3) {
				parent.stroke(0, 255, 0);
			} else if (d < 6.0) {
				parent.stroke(255, 255, 0);
			} else {
				parent.stroke(255, 0, 0);
			}
			parent.ellipse(0, 0, 100, 100);
			parent.strokeWeight(2);
			parent.line(-50, -50, 50, 50);
			parent.line(-50, 50, 50, -50);

			parent.popMatrix();
		} else {
			parent.textFont(font, 48);
			parent.text("NO SIGNAL", 322, 401);
		}
		parent.textFont(font, 30);
		parent.text("Range: " + distance, 46, 740);
		parent.text("Speed: " + (int) parent.getShipState().shipVelocity, 58,
				183);

		String s = "";
		;
		if (lockingState == NO_SIGNAL) {
			s = "No Docking Beacon Detected";
		} else if (lockingState == BEACON_LOCKING) {
			s = "locking onto beacon..";
		} else if (lockingState == BEACON_LOCKED) {

			s = "LOCKED to beacon";
		}

		parent.text(s, 46, 710);

		if (lockingState != NO_SIGNAL) {
			speedWarning = false;
			int maxSpd = 0;
			for (int i = warningSpeeds.length - 1; i > 0; i--) {
				int[] distSpeeds = warningSpeeds[i];
				if (distance < distSpeeds[0]) {

					maxSpd = distSpeeds[1];
					if (parent.getShipState().shipVelocity > distSpeeds[1]) {
						speedWarning = true;
						break;
					}
				}
			}
			parent.text("Max Speed: " + maxSpd, 44, 678);
			if (speedWarning && lastSpeedWarning + 2000 < parent.millis()) {
				lastSpeedWarning = parent.millis();
				parent.getConsoleAudio().playClip("reduceSpeed");
			}
		} else {
			if (lastSpeedWarning + 5000 < parent.millis()) {
				lastSpeedWarning = parent.millis();
				parent.getConsoleAudio().playClip("searchingBeacon");
			}
		}
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {

		if (theOscMessage.checkAddrPattern("/ship/undercarriage/contact") == true) {
			landed = theOscMessage.get(0).intValue() == 1 ? true : false;
		} else if (theOscMessage.checkAddrPattern("/ship/undercarriage")) {
			undercarriageState = theOscMessage.get(0).intValue();
		} else if (theOscMessage.checkAddrPattern("/system/misc/clampState")) {
			clamped = theOscMessage.get(0).intValue() == 1 ? true : false;
		} else if (theOscMessage
				.checkAddrPattern("/scene/launchland/bayGravity")) {
			bayGravity = theOscMessage.get(0).intValue() == 1 ? true : false;
		} else if (theOscMessage
				.checkAddrPattern("/system/dockingComputer/dockingPosition")) {
			lastShipPos.x = shipPos.x;
			lastShipPos.y = shipPos.y;
			lastShipPos.z = shipPos.z;

			distance = theOscMessage.get(4).floatValue();

			lastPosUpdate = parent.millis();

			shipPos.x = theOscMessage.get(0).floatValue();
			shipPos.y = theOscMessage.get(1).floatValue();
			shipPos.z = theOscMessage.get(2).floatValue();

			// signal locking events
			int newLockingState = theOscMessage.get(5).intValue();
			if (lockingState != newLockingState) {
				if (newLockingState == NO_SIGNAL) {
					parent.getConsoleAudio().playClip("signalLost");
				} else if (newLockingState == BEACON_LOCKING) {
					parent.getConsoleAudio().playClip("searchingBeacon");
				} else if (newLockingState == BEACON_LOCKED) {
					parent.getConsoleAudio().playClip("signalAcquire");
				}
			}

			lockingState = newLockingState;
		}
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
	}

	@Override
	public void start() {
		lockingState = NO_SIGNAL;
	}

	@Override
	public void stop() {
	}
}
