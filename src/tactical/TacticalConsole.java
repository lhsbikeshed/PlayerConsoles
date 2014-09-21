package tactical;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.serial.Serial;

import common.ConsoleAudio;
import common.ConsoleLogger;
import common.Display;
import common.PlayerConsole;
import common.displays.BootDisplay;
import common.displays.CablePuzzleDisplay;
import common.displays.DestructDisplay;
import common.displays.FailureScreen;
import common.displays.RestrictedAreaScreen;
import common.displays.WarpDisplay;

import ddf.minim.Minim;

public class TacticalConsole extends PlayerConsole {
	
	// dont change anything past here. Things will break

	// CHANGE ME for testing
	// disables serial port access
	// and sets server to localhost
	boolean testMode = true;
	boolean serialEnabled = false;

	String serverIP = "127.0.0.1";

	// audio
	Minim minim;
	DropDisplay dropDisplay; // display for the drop scene
	WarpDisplay warpDisplay; // warp scene

	WeaponsConsole weaponsDisplay; // tactical weapons display

	BootDisplay bootDisplay; // boot up sequence

	// time (local ms) in which the ship died
	long deathTime = 0;

	// is the decoy blinker on?
	boolean decoyBlinker = false;

	// power for something, not sure what
	int systemPower = -1;
	// serial stuff
	Serial serialPort;

	Serial charlesPort;
	String serialBuffer = "";

	String lastSerial = "";

	long heartBeatTimer = 0;

	boolean decoyLightState = false;

	/*
	 * expected vals: 0-9 from keypad ' ' = scan key 'F' = any of the beam bank
	 * buttons 'm' = decoy button 'X' = conduit puzzle failed 'P' = conduit
	 * puzzle complete 'CX' = cable X connected correctly
	 */
	void dealWithSerial(String vals) {
		// ConsoleLogger.log(this, (vals);

		char c = vals.charAt(0);
		if (c >= '0' && c <= '9') {
			String v = "KEY:" + c;
			consoleAudio.randomBeep();
			currentScreen.serialEvent(v);
		}
		if (c == ' ') {
			currentScreen.serialEvent("KEY:SCAN");
		}
		if (c == 'F') {
			currentScreen.serialEvent("KEY:FIRELASER");
		}
		if (c == 'm') {
			currentScreen.serialEvent("KEY:DECOY");
		}

		if (c == 'C') {

			currentScreen.serialEvent("CONDUITCONNECT:" + vals.charAt(1));
		}
		if (c == 'c') {

			currentScreen.serialEvent("CONDUITDISCONNECT:" + vals.charAt(1));
		}
	}

	void decoyLightState(boolean s) {
		if (serialEnabled == false) {
			return;
		}
		;
		if (s && decoyLightState == false) {
			// ConsoleLogger.log(this, ("poo");
			decoyLightState = true;
			serialPort.write("D,");
		} else if (!s && decoyLightState == true) {
			serialPort.write("d,");
			decoyLightState = false;
		}
	}

	@Override
	public void drawConsole() {

		noSmooth();
		if (serialEnabled) {
			while (serialPort.available() > 0) {
				char val = serialPort.readChar();
				// ConsoleLogger.log(this, (val);
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

			if (shipState.poweredOn) {
				currentScreen.draw();
			} else {
				if (shipState.poweringOn) {
					bootDisplay.draw();
					if (bootDisplay.isReady()) {
						shipState.poweredOn = true;
						shipState.poweringOn = false;
						/* sync current display to server */
						OscMessage myMessage = new OscMessage(
								"/game/Hello/TacticalStation");
						oscP5.send(myMessage, new NetAddress(serverIP, 12000));
						// oscP5.send(myMessage, new NetAddress(serverIP,
						// 12000));
						bannerSystem.cancel();
						ConsoleLogger.log(this, "BOOTED");
					}
				}
			}
			hint(DISABLE_DEPTH_TEST);
			bannerSystem.draw();

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

		damageEffects.draw();
		// if ( damageTimer + 1000 > millis()) {
		// if (random(10) > 3) {
		// image(noiseImage, 0, 0, width, height);
		// }
		// }
		// if (shipState.poweredOn && shipState.hullState < 20.0f) {
		// if (random(1000) < 10) {
		// if (serialEnabled) {
		// serialPort.write("F,");
		// }
		// }
		// }
	}

	/* these are just for testing when serial devices arent available */
	@Override
	public void keyPressed() {
		if (key >= '0' && key <= '9') {
			consoleAudio.randomBeep();
			currentScreen.serialEvent("KEY:" + key);
		} else if (key == ' ') {
			currentScreen.serialEvent("KEY:SCAN");
		} else if (key == 'm') {
			currentScreen.serialEvent("KEY:FIRELASER");
		} else if (key == 'f') {
			currentScreen.serialEvent("KEY:DECOY");
		} else if (key == 'g') {
			currentScreen.serialEvent("KEY:GRAPPLEFIRE");
		} else if (key == 'h') {
			currentScreen.serialEvent("KEY:GRAPPLERELEASE");
		}
	}

	@Override
	public void mouseClicked() {
		ConsoleLogger.log(this, "mx: " + mouseX + ", my: " + mouseY);
	}

	void oscEvent(OscMessage theOscMessage) {

		if (theOscMessage.checkAddrPattern("/scene/warzone/weaponState") == true) {
			int msg = theOscMessage.get(0).intValue();
			if (msg == 1) {
				if (serialEnabled) {
					serialPort.write("P,");
				}
			} else {
				if (serialEnabled) {

					serialPort.write("p,");
					decoyBlinker = false;
				}
			}

			currentScreen.oscMessage(theOscMessage);
		} else if (theOscMessage
				.checkAddrPattern("/system/reactor/stateUpdate") == true) {
			int state = theOscMessage.get(0).intValue();

			if (state == 0) {
				shipState.poweredOn = false;
				shipState.poweringOn = false;
				bootDisplay.stop();
				bootDisplay.stop();
				bannerSystem.cancel();
				if (serialEnabled) {
					serialPort.write("p,");
					decoyBlinker = false;
					charlesPort.write("R0,");
				}
			} else {

				if (!shipState.poweredOn) {
					shipState.poweringOn = true;

					changeDisplay(bootDisplay);
					if (serialEnabled) {
						serialPort.write("P,");
						charlesPort.write("R1,");
					}
				}
			}
		} else if (theOscMessage.checkAddrPattern("/scene/youaredead") == true) {
			// oh noes we died
			shipState.areWeDead = true;
			deathTime = millis();
			shipState.deathText = theOscMessage.get(0).stringValue();
			if (serialEnabled) {
				serialPort.write("p,");
				charlesPort.write("R0,");
			}
		} else if (theOscMessage.checkAddrPattern("/game/reset") == true) {
			// reset the entire game
			changeDisplay(weaponsDisplay);
			shipState.areWeDead = false;
			shipState.poweredOn = false;
			shipState.poweringOn = false;
			if (serialEnabled) {
				serialPort.write("p,");
			}
			shipState.smartBombsLeft = 6;
		} else if (theOscMessage.checkAddrPattern("/system/subsystemstate") == true) {
			systemPower = theOscMessage.get(1).intValue() + 1;
			currentScreen.oscMessage(theOscMessage);
		} else if (theOscMessage.checkAddrPattern("/tactical/powerState") == true) {

			if (theOscMessage.get(0).intValue() == 1) {
				shipState.poweredOn = true;
				shipState.poweringOn = false;
				bootDisplay.stop();
				OscMessage myMessage = new OscMessage(
						"/game/Hello/TacticalStation");
				oscP5.send(myMessage, new NetAddress(serverIP, 12000));
				if (serialEnabled) {

					serialPort.write("P,");
					charlesPort.write("R1,");
				}
			} else {
				shipState.poweredOn = false;
				shipState.poweringOn = false;
				if (serialEnabled) {

					serialPort.write("p,");
					charlesPort.write("R0,");
				}
			}
		} else if (theOscMessage.checkAddrPattern("/ship/effect/heartbeat") == true) {
			heartBeatTimer = millis();
		} else if (theOscMessage.checkAddrPattern("/ship/damage") == true) {

			// float damage = theOscMessage.get(0).floatValue();
			damageEffects.startEffect(1000);
			if (serialEnabled) {

				serialPort.write("S,");
				charlesPort.write("D1,");
				// serialPort.write("F,");
			}
		} else if (theOscMessage.checkAddrPattern("/control/subsystemstate") == true) {
			int beamPower = theOscMessage.get(3).intValue() - 1; // write charge
																	// rate
			int propPower = theOscMessage.get(0).intValue() - 1;
			int sensorPower = theOscMessage.get(2).intValue() - 1;
			int internalPower = theOscMessage.get(1).intValue() - 1;

			if (serialEnabled) {
				serialPort.write("L" + beamPower + ",");
				charlesPort.write("P" + (propPower + 1));
				charlesPort.write("W" + (beamPower + 1));
				charlesPort.write("S" + (sensorPower + 1));
				charlesPort.write("I" + (internalPower + 1));
			}
			currentScreen.oscMessage(theOscMessage);
		} else if (theOscMessage.checkAddrPattern("/ship/transform") == true) {
			shipState.shipPos.x = theOscMessage.get(0).floatValue();
			shipState.shipPos.y = theOscMessage.get(1).floatValue();
			shipState.shipPos.z = theOscMessage.get(2).floatValue();

			shipState.shipRot.x = theOscMessage.get(3).floatValue();
			shipState.shipRot.y = theOscMessage.get(4).floatValue();
			shipState.shipRot.z = theOscMessage.get(5).floatValue();

			shipState.shipVel.x = theOscMessage.get(6).floatValue();
			shipState.shipVel.y = theOscMessage.get(7).floatValue();
			shipState.shipVel.z = theOscMessage.get(8).floatValue();
		} else if (theOscMessage
				.checkAddrPattern("/clientscreen/TacticalStation/changeTo")) {
			String changeTo = theOscMessage.get(0).stringValue();
			try {
				Display d = displayMap.get(changeTo);
				ConsoleLogger.log(this, "found display for : " + changeTo);
				if (d == null) {
					d = weaponsDisplay;
				}
				changeDisplay(d);
			} catch (Exception e) {
				ConsoleLogger.log(this, "no display found for " + changeTo);
				changeDisplay(weaponsDisplay);
			}
		} else if (theOscMessage.checkAddrPattern("/clientscreen/showBanner")) {
			String title = theOscMessage.get(0).stringValue();
			String text = theOscMessage.get(1).stringValue();
			int duration = theOscMessage.get(2).intValue();

			bannerSystem.setSize(700, 300);
			bannerSystem.setTitle(title);
			bannerSystem.setText(text);
			bannerSystem.displayFor(duration);
		
		} else if (theOscMessage.checkAddrPattern("/control/grapplingHookState")) {

			weaponsDisplay.hookArmed = theOscMessage.get(0).intValue() == 1 ? true
					: false;
			bannerSystem.displayFor(1500);
		} else if (theOscMessage.checkAddrPattern("/ship/stats") == true) {

			shipState.hullState = theOscMessage.get(2).floatValue();
		} else if (theOscMessage.checkAddrPattern("/ship/effect/openFlap")) {
			ConsoleLogger.log(this, "popping panel..");
			if (serialEnabled) {
				serialPort.write("T,");
				serialPort.write("F,");
			}
		} else if (theOscMessage.checkAddrPattern("/ship/effect/flapStrobe")) {
			ConsoleLogger.log(this, "strobe..");
			if (serialEnabled) {
				serialPort.write("F,");
			}
		} else if (theOscMessage.checkAddrPattern("/ship/effect/playSound")) {
			String name = theOscMessage.get(0).stringValue();
			consoleAudio.playClip(name);
		} else {
			if (currentScreen != null) {
				currentScreen.oscMessage(theOscMessage);
			}
		}

	}

	public void probeCableState() {
		if (serialEnabled) {
			serialPort.write("C,");
		} else {
			ConsoleLogger.log(this, "probed cable puzzle state");
		}
	}

	void setDecoyBlinkerState(boolean state) {
		if (!serialEnabled) {
			return;
		}
		if (state != decoyBlinker) {
			if (state) {
				decoyBlinker = true;
				serialPort.write("D,");
			} else {
				decoyBlinker = false;
				serialPort.write("d,");
			}
		}
	}

	// ---------------- main method

	@Override
	public void setup() {
		super.setup();
		consoleName = "tacticalconsole";

		if (testMode) {
			serialEnabled = false;
			serverIP = "127.0.0.1";
			shipState.poweredOn = true;
		} else {
			serialEnabled = true;
			serverIP = "10.0.0.100";
			frame.setLocation(1024, 0);
			serialPort = new Serial(this, "COM7", 9600);
			charlesPort = new Serial(this, "COM5", 9600);
			hideCursor();
		}

		oscP5 = new OscP5(this, 12004);

		font = loadFont("common/HanzelExtendedNormal-48.vlw");

		dropDisplay = new DropDisplay(this);
		// radarDisplay = new RadarDisplay();
		warpDisplay = new WarpDisplay(this);
		weaponsDisplay = new WeaponsConsole(this);

		displayMap.put("weapons", weaponsDisplay);
		displayMap.put("drop", dropDisplay);
		displayMap.put("hyperspace", warpDisplay);
		displayMap.put("selfdestruct", new DestructDisplay(this));
		displayMap.put("cablepuzzle", new CablePuzzleDisplay(this));
		displayMap.put("failureScreen", new FailureScreen(this));
		displayMap.put("restrictedArea", new RestrictedAreaScreen(this));

		// currentScreen = weaponsDisplay;

		bootDisplay = new BootDisplay(this);
		displayMap.put("boot", bootDisplay); // /THIS

		/* power down the tac console panel */
		if (serialEnabled) {
			serialPort.write("p,");
		}

		// audio stuff
		minim = new Minim(this);
		consoleAudio = new ConsoleAudio(this, minim);

		// set initial screen, probably gets overwritten from game shortly
		changeDisplay(displayMap.get("weapons"));

		/* sync to current game screen */
		OscMessage myMessage = new OscMessage("/game/Hello/TacticalStation");
		oscP5.send(myMessage, new NetAddress(serverIP, 12000));

	}

}