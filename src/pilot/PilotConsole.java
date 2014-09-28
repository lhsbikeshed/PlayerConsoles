package pilot;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PImage;
import processing.serial.Serial;
import common.ConsoleAudio;
import common.ConsoleLogger;
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
	LaunchDisplay launchDisplay;
	CablePuzzleDisplay cablePuzzleDisplay;

	FailureScreen failureScreen;
	int systemPower = 2;

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
			oscP5.send(msg, serverAddress);
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
			oscP5.send(myMessage, serverAddress);
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
			drawDeadScreen();
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

	

	}

	public Joystick getJoystick() {
		return joy;
	}

	@Override
	public void mouseClicked() {
		println(":" + mouseX + "," + mouseY);
	}

	@Override
	protected void oscEvent(OscMessage theOscMessage) {
		super.oscEvent(theOscMessage);

		lastOscTime = millis();
		// println(theOscMessage);
		if (theOscMessage.checkAddrPattern("/scene/change") == true) {
			setJumpLightState(false);
				
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
				oscP5.send(myMessage, serverAddress);
			} else {
				shipState.poweredOn = false;
				shipState.poweringOn = false;
				setJumpLightState(false);
			}

		
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
		
		} else if (theOscMessage.checkAddrPattern("/ship/sectorChanged")) {
			radarDisplay.setSector(theOscMessage.get(0).intValue(),
					theOscMessage.get(1).intValue(), theOscMessage.get(2)
							.intValue());
		
		} else if (theOscMessage.checkAddrPattern("/system/propulsion/afterburnerCharged")){
			shipState.afterburnerCharging = false;
		} else if (theOscMessage.checkAddrPattern("/system/propulsion/afterburnerCharging")){
			shipState.afterburnerCharging = true;
		
			
			
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
		serverAddress = new NetAddress(serverIP, 12000);
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

		

		// damage stuff
		setJumpLightState(false);

		//now console is loaded up, load the sound config
		consoleAudio = new ConsoleAudio(this, minim, -1.0f);



		autopilotOverlay = loadImage("pilotconsole/autopilotoverlay.png");

		/* sync to current game screen */
		OscMessage myMessage = new OscMessage("/game/Hello/PilotStation");
		oscP5.send(myMessage, serverAddress);

		// set initial screen
		Display d = displayMap.get("radar");
		changeDisplay(d);

	}

	@Override
	protected void shipDamaged(float amount) {
		//nothing to do here for this console

	}

	@Override
	protected void gameReset() {

		currentScreen.stop();
		currentScreen = launchDisplay;
		currentScreen.start();
		shipState.areWeDead = false;
		setJumpLightState(false);
		shipState.poweredOn = false;
		shipState.poweringOn = false;
	}

	@Override
	protected void shipDead() {
		ConsoleLogger.log(this, "Ship died");
		
	}

	@Override
	protected void reactorStarted() {
		ConsoleLogger.log(this, "Reactor started");
		
	}

	@Override
	protected void reactorStopped() {
		ConsoleLogger.log(this, "Reactor stopped");
		
	}
}
