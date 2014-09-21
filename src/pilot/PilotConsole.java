package pilot;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;
import processing.serial.Serial;

import common.ConsoleAudio;
import common.Display;
import common.PlayerConsole;
import common.Rot;
import common.displays.BootDisplay;
import common.displays.CablePuzzleDisplay;
import common.displays.DestructDisplay;
import common.displays.FailureScreen;
import common.displays.RestrictedAreaScreen;
import common.displays.WarpDisplay;

import ddf.minim.Minim;

public class PilotConsole extends PlayerConsole {


	// ---joystick class
	Joystick joy;
	boolean autopilotBanner = false;

	boolean joystickTestMode = true;

	PImage autopilotOverlay;

	long deathTime = 0; // what time did we die?
	// serial stuff
	Serial serialPort;
	String serialBuffer = "";

	String lastSerial = "";

	// mappings from physical buttons to OSC messages
	String[] messageMapping = { "/system/undercarriage/state",
			"/scene/launchland/dockingCompState", "/system/misc/blastShield",
			"/system/propulsion/state", "/system/jump/state",
			"/system/jump/doJump" };
	// -----displays-----
	DropDisplay dropDisplay;
	WarpDisplay warpDisplay;
	RadarDisplay radarDisplay;
	BootDisplay bootDisplay;
	LaunchDisplay launchDisplay;
	CablePuzzleDisplay cablePuzzleDisplay;

	FailureScreen failureScreen;
	int systemPower = 2;

	long heartBeatTimer = -1;

	float lastOscTime = 0;

	void dealWithSerial(String vals) {

		char p = vals.charAt(0);

		if (p == 't') {
			int th = Integer.parseInt(vals.substring(1));
			float t = map(th, 0f, 255f, 0f, 1.0f);
			if (t < 0.1) {
				t = 0;
			}
			// map throttle to 0-1 float, set throttle
			joy.throttle = t;
		} else if (p == 'C' || p == 'c') { // cable connection event
			// C:<plug>:<socket>
			OscMessage msg;
			if (p == 'C') {
				msg = new OscMessage("/system/cablePuzzle/connect");
			} else {
				msg = new OscMessage("/system/cablePuzzle/disconnect");
			}
			String[] parts = vals.split(":");
			int pl = Integer.parseInt(parts[1]);
			int s = Integer.parseInt(parts[2]);
			msg.add(pl);
			msg.add(s);
			oscP5.send(msg, myRemoteLocation);
		} else {

			int sw = Integer.parseInt("" + p);
			int val = Integer.parseInt("" + vals.charAt(1));
			println("sw : " + sw + "  " + val);
			if (shipState.poweredOn == false) {
				return;
			}
			OscMessage myMessage = new OscMessage(messageMapping[sw]);
			if (sw == 4) {
				// jump switch is now inverted
				val = 1 - val;
			}
			myMessage.add(val);
			oscP5.send(myMessage, myRemoteLocation);
		}
	}

	@Override
	public void drawConsole() {

		noSmooth();
		float s = shipState.shipVel.mag();
		shipState.shipVelocity = lerp(shipState.lastShipVel, s,
				(millis() - shipState.lastTransformUpdate) / 250.0f);
		if (!testMode) {
			while (serialPort.available() > 0) {
				char val = serialPort.readChar();
				if (val == ',') {
					// get first char
					dealWithSerial(serialBuffer);
					serialBuffer = "";
				} else {
					serialBuffer += val;
				}
			}
		}

		background(0, 0, 0);

		if (shipState.areWeDead) {
			fill(255, 255, 255);
			if (deathTime + 2000 < millis()) {
				textFont(font, 60);
				text("YOU ARE DEAD", 50, 300);
				textFont(font, 20);
				int pos = (int) textWidth(shipState.deathText);
				text(shipState.deathText, (width / 2) - pos / 2, 340);
			}
		} else {

			// run joystick->osc updates
			joy.update();
			if (shipState.poweredOn) {
				// displayList[currentDisplay].draw();

				currentScreen.draw();
				bannerSystem.draw();
				if (autopilotBanner) {
					image(autopilotOverlay, 244, 594);
				}
			} else {
				if (shipState.poweringOn) {
					bootDisplay.draw();
					if (bootDisplay.isReady()) {
						shipState.poweredOn = true;
						shipState.poweringOn = false;
						/* sync current display to server */
						OscMessage myMessage = new OscMessage(
								"/game/Hello/PilotStation");
						oscP5.send(myMessage, new NetAddress(serverIP, 12000));
						oscP5.send(myMessage, new NetAddress(serverIP, 12000));
					}
				}
			}

		}

		if (heartBeatTimer > 0) {
			if (heartBeatTimer + 400 > millis()) {
				int a = (int) map(millis() - heartBeatTimer, 0, 400, 255, 0);
				fill(0, 0, 0, a);
				rect(0, 0, width, height);
			} else {
				heartBeatTimer = -1;
			}
		}

	}

	public Joystick getJoystick() {
		return joy;
	}

	@Override
	public void mouseClicked() {
		println(":" + mouseX + "," + mouseY);
	}

	void oscEvent(OscMessage theOscMessage) {
		lastOscTime = millis();
		// println(theOscMessage);
		if (theOscMessage.checkAddrPattern("/scene/change") == true) {
			setJumpLightState(false);
		} else if (theOscMessage
				.checkAddrPattern("/system/reactor/stateUpdate") == true) {
			int state = theOscMessage.get(0).intValue();
			String flags = theOscMessage.get(1).stringValue();
			String[] fList = flags.split(";");
			// reset flags
			bootDisplay.brokenBoot = false;
			for (String f : fList) {
				if (f.equals("BROKENBOOT")) {
					println("BROKEN BOOT");
					bootDisplay.brokenBoot = true;
				}
			}

			if (state == 0) {
				shipState.poweredOn = false;
				shipState.poweringOn = false;
				bootDisplay.stop();
				bannerSystem.cancel();
			} else {

				if (!shipState.poweredOn) {
					shipState.poweringOn = true;
					changeDisplay(bootDisplay);
				}
			}
		} else if (theOscMessage.checkAddrPattern("/scene/youaredead") == true) {
			// oh noes we died
			shipState.areWeDead = true;
			shipState.deathText = theOscMessage.get(0).stringValue();
			deathTime = millis();
		} else if (theOscMessage.checkAddrPattern("/game/reset") == true) {

			currentScreen.stop();
			currentScreen = launchDisplay;
			currentScreen.start();
			shipState.areWeDead = false;
			setJumpLightState(false);
			shipState.poweredOn = false;
			shipState.poweringOn = false;
		} else if (theOscMessage.checkAddrPattern("/ship/jumpStatus") == true) {
			int v = theOscMessage.get(0).intValue();
			if (v == 0) {
				setJumpLightState(false);
			} else if (v == 1) {
				setJumpLightState(true);
			}
		} else if (theOscMessage.checkAddrPattern("/control/subsystemstate") == true) {
			systemPower = theOscMessage.get(1).intValue() + 1;
			// displayList[currentDisplay].oscMessage(theOscMessage);
			currentScreen.oscMessage(theOscMessage);
			setJumpLightState(false);
		} else if (theOscMessage
				.checkAddrPattern("/system/control/controlState") == true) {
			boolean state = theOscMessage.get(0).intValue() == 0 ? true : false;
			joy.setEnabled(state);
			println("Set control state : " + state);
			if (state == false) {
				autopilotBanner = true;
			} else {
				autopilotBanner = false;
			}
		} else if (theOscMessage.checkAddrPattern("/pilot/powerState") == true) {

			if (theOscMessage.get(0).intValue() == 1) {
				shipState.poweredOn = true;
				shipState.poweringOn = false;
				bootDisplay.stop();
				OscMessage myMessage = new OscMessage(
						"/game/Hello/PilotStation");
				oscP5.send(myMessage, myRemoteLocation);
			} else {
				shipState.poweredOn = false;
				shipState.poweringOn = false;
				setJumpLightState(false);
			}
		} else if (theOscMessage.checkAddrPattern("/ship/effect/heartbeat") == true) {
			heartBeatTimer = millis();
		} else if (theOscMessage.checkAddrPattern("/ship/damage") == true) {
			damageEffects.startEffect(1000);
		} else if (theOscMessage.checkAddrPattern("/ship/transform") == true) {
			shipState.shipPos.x = theOscMessage.get(0).floatValue();
			shipState.shipPos.y = theOscMessage.get(1).floatValue();
			shipState.shipPos.z = theOscMessage.get(2).floatValue();
			/*
			 * shipState.shipRot.x = theOscMessage.get(3).floatValue();
			 * shipState.shipRot.y = theOscMessage.get(4).floatValue();
			 * shipState.shipRot.z = theOscMessage.get(5).floatValue();
			 */
			float w = theOscMessage.get(3).floatValue();
			float x = theOscMessage.get(4).floatValue();
			float y = theOscMessage.get(5).floatValue();
			float z = theOscMessage.get(6).floatValue();
			shipState.lastShipRotQuat = shipState.shipRotQuat;
			shipState.shipRotQuat = new Rot(w, x, y, z, false);
			shipState.shipVel.x = theOscMessage.get(7).floatValue();
			shipState.shipVel.y = theOscMessage.get(8).floatValue();
			shipState.shipVel.z = theOscMessage.get(9).floatValue();

			shipState.lastShipVel = shipState.shipVelocity;
			shipState.lastTransformUpdate = millis();
		} else if (theOscMessage
				.checkAddrPattern("/clientscreen/PilotStation/changeTo")) {
			String changeTo = theOscMessage.get(0).stringValue();
			try {
				Display d = displayMap.get(changeTo);
				println("found display for : " + changeTo);
				changeDisplay(d);
			} catch (Exception e) {
				println("no display found for " + changeTo);
				changeDisplay(radarDisplay);
			}
		} else if (theOscMessage.checkAddrPattern("/clientscreen/showBanner")) {
			String title = theOscMessage.get(0).stringValue();
			String text = theOscMessage.get(1).stringValue();
			int duration = theOscMessage.get(2).intValue();

			bannerSystem.setSize(700, 300);
			bannerSystem.setTitle(title);
			bannerSystem.setText(text);
			bannerSystem.displayFor(duration);
		} else if (theOscMessage.checkAddrPattern("/system/boot/diskNumbers")) {

			int[] disks = { theOscMessage.get(0).intValue(),
					theOscMessage.get(1).intValue(),
					theOscMessage.get(2).intValue() };
			println(disks);

			bootDisplay.setDisks(disks);
		} else if (theOscMessage.checkAddrPattern("/ship/sectorChanged")) {
			radarDisplay.setSector(theOscMessage.get(0).intValue(),
					theOscMessage.get(1).intValue(), theOscMessage.get(2)
							.intValue());
		} else if (theOscMessage.checkAddrPattern("/ship/effect/playSound")) {
			String name = theOscMessage.get(0).stringValue();
			consoleAudio.playClip(name);
		} else {
			if (currentScreen != null) {
				currentScreen.oscMessage(theOscMessage);
			}
		}
	}

	void setJumpLightState(boolean state) {
		if (state == true && shipState.jumpState == false) {
			serialPort.write('B');
			shipState.jumpState = true;
		} else if (state == false && shipState.jumpState == true) {
			serialPort.write('b');
			shipState.jumpState = false;
		}
	}

	// ---------------- main method

	@Override
	public void setup() {
		super.setup();
		consoleName = "pilotconsole";

		if (testMode) {
			serverIP = "127.0.0.1";
			joystickTestMode = true;
			shipState.poweredOn = true;
		} else {
			serverIP = "10.0.0.100";
			joystickTestMode = false;
			shipState.poweredOn = false;
			frame.setLocation(0, 0);
			serialPort = new Serial(this, "COM8", 115200);
		}

		oscP5 = new OscP5(this, 12002);
		myRemoteLocation = new NetAddress(serverIP, 12000);
		dropDisplay = new DropDisplay(this);
		radarDisplay = new RadarDisplay(this);
		warpDisplay = new WarpDisplay(this);
		launchDisplay = new LaunchDisplay(this);

		joy = new Joystick(oscP5, this, joystickTestMode);

		displayMap.put("radar", radarDisplay);
		displayMap.put("drop", dropDisplay);
		displayMap.put("docking", launchDisplay);
		displayMap.put("hyperspace", warpDisplay);
		displayMap.put("selfdestruct", new DestructDisplay(this));
		displayMap.put("dockingtest", new DockingDisplay(this));
		displayMap.put("cablepuzzle", new CablePuzzleDisplay(this));
		displayMap.put("failureScreen", new FailureScreen(this));
		displayMap.put("restrictedArea", new RestrictedAreaScreen(this));

		bootDisplay = new BootDisplay(this);
		displayMap.put("boot", bootDisplay);

		font = loadFont("common/HanzelExtendedNormal-48.vlw");

		// damage stuff
		setJumpLightState(false);

		// SOUND!
		minim = new Minim(this);
		consoleAudio = new ConsoleAudio(this, minim);
		// consoleAudio.playClip("bannerPopup");
		// consoleAudio.playClip("newTarget");

		autopilotOverlay = loadImage("pilotconsole/autopilotoverlay.png");

		/* sync to current game screen */
		OscMessage myMessage = new OscMessage("/game/Hello/PilotStation");
		oscP5.send(myMessage, myRemoteLocation);

		// set initial screen
		Display d = displayMap.get("radar");
		changeDisplay(d);

	}
}
