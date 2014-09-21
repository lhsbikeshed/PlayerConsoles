package engineer;

import java.util.ArrayList;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
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

import ddf.minim.Minim;

public class EngineerConsole extends PlayerConsole {

	boolean serialEnabled = false;

	// display handling
	Display powerDisplay, wormholeDisplay, jamDisplay, dropDisplay;

	// sound shit
	Minim minim;

	// highlights
	ArrayList<Highlighter> highlightList = new ArrayList(0);

	long deathTime = 0;
	// Peripheral things
	Serial serialPort;
	String serialBuffer = "";

	String lastSerial = "";
	Serial panelPort;
	String panelBuffer = "";

	String lastPanelSerial = "";

	boolean globalBlinker = false;

	long blinkTime = 0;

	long sillinessStartTime = 0;

	int fuelBeepTimer = 0;

	private BootDisplay bootDisplay;

	public void addHighlight(Highlighter h) {
		highlightList.add(h);
	}

	void dealWithSerial(String vals) {

		char p = vals.charAt(0);
		if (p == 'P') { // this is from new panel
			if (vals.substring(0, 2).equals("PS")) { // PS10:1
				// switch
				vals = vals.substring(2);
				String[] sw = vals.split(":");
				consoleAudio.randomBeep();

				String t = "NEWSWITCH:" + sw[0] + ":" + sw[1];

				currentScreen.serialEvent(t);
			} else if (vals.substring(0, 2).equals("PC")) {// probe complete,
															// unmute audio for
															// buttons
				consoleAudio.muteBeeps = false;
			} else {
				// its a dial
				String t = "NEWDIAL:" + vals.substring(1, 2) + ":"
						+ vals.substring(3);

				currentScreen.serialEvent(t);
			}
		} else {

			if (p == 'A' || p == 'B') { // values from the jamming knobs
				int v = Integer.parseInt(vals.substring(1, vals.length()));
				String s = "JAM" + p + ":" + v;
				currentScreen.serialEvent(s);
			} else if (p == 'S') {
				int v = Integer.parseInt(vals.substring(1, vals.length()));
				// ConsoleLogger.log(this,(v);
				if (v == 0) {
					consoleAudio.playClipForce("codeFail");
				} else if (v <= 5) {
					consoleAudio.playClipForce("beepLow");
				} else if (v <= 9) {
					consoleAudio.playClipForce("beepHigh");
				} else {
					consoleAudio.playClipForce("reactorReady");
				}
				if (v > 0) {
					OscMessage myMessage = new OscMessage(
							"/system/reactor/switchState");

					myMessage.add(v);
					OscP5.flush(myMessage, new NetAddress(serverIP, 12000));
				} else {
					OscMessage myMessage = new OscMessage(
							"/system/reactor/setstate");
					myMessage.add(0);
					OscP5.flush(myMessage, new NetAddress(serverIP, 12000));
				}
			} else if (p == 'R') {
				OscMessage myMessage = new OscMessage(
						"/system/reactor/setstate");
				myMessage.add(1);
				OscP5.flush(myMessage, new NetAddress(serverIP, 12000));
			} else if (p == 'p') {
				int v = Integer.parseInt(vals.substring(1, vals.length())) + 1;
				String s = "KEY:" + v;
				consoleAudio.randomBeep();

				currentScreen.serialEvent(s);
			} else if (p == 'L') {

				currentScreen.serialEvent("BUTTON:AIRLOCK");
			} else if (p == 'F') { // fuel gauge stuff
				char nextChar = vals.charAt(1);
				if (nextChar == 'E') {
					// we ran out of fuel
					OscMessage myMessage = new OscMessage(
							"/system/reactor/outOfFuel");

					OscP5.flush(myMessage, new NetAddress(serverIP, 12000));
				}
			}
		}
	}

	@Override
	public void drawConsole() {
		if (shipState.sillinessInProgress
				&& sillinessStartTime + 5000 < millis()) {
			shipState.sillinessInProgress = false;
		}

		noSmooth();
		background(0, 0, 0);
		// serial read
		if (serialEnabled) {
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

			while (panelPort.available() > 0) {
				char val = panelPort.readChar();
				if (val == ',') {
					// get first char
					dealWithSerial(panelBuffer);
					panelBuffer = "";
				} else {
					panelBuffer += val;
				}
			}
		}

		if (shipState.areWeDead) {
			fill(255, 255, 255);
			if (deathTime + 2000 < millis()) {
				textFont(globalFont, 60);
				text("YOU ARE DEAD", 50, 300);
				textFont(globalFont, 20);
				int pos = (int) textWidth(shipState.deathText);
				text(shipState.deathText, (width / 2) - pos / 2, 340);
			}
		} else {

			if (shipState.poweredOn) {
				currentScreen.draw();
				for (int i = highlightList.size() - 1; i > 0; i--) {
					Highlighter h = highlightList.get(i);
					h.update();
					if (h.isDone()) {

						highlightList.remove(h);
					}
				}

				if (shipState.fuelLeaking) {
					fuelBeepTimer--;
					if (fuelBeepTimer <= 0) {
						fuelBeepTimer = 50;
						consoleAudio.playClip("lowFuelBeep");
					}
				}
			} else {
				if (shipState.poweringOn) {
					bootDisplay.draw();
					if (bootDisplay.isReady()) {
						shipState.poweredOn = true;
						shipState.poweringOn = false;
						/* sync current display to server */
						OscMessage myMessage = new OscMessage(
								"/game/Hello/EngineerStation");
						oscP5.send(myMessage, new NetAddress(serverIP, 12000));
					}
				}
			}

			bannerSystem.draw(); // THIS

		}

		

	}

	@Override
	public void keyPressed() {
		consoleAudio.randomBeep();

		currentScreen.keyPressed(key);
		if (key >= '0' && key <= '9') {
			currentScreen.serialEvent("KEY:" + key);
		} else if (key == ' ') {
			currentScreen.serialEvent("BUTTON:AIRLOCK");
		} else if (key == ';') { // change me to something on keyboard
			currentScreen.serialEvent("KEY:" + key);
		} else if (key >= 'a' && key <= 't') {
			currentScreen.serialEvent("KEY:" + key);
		} else if (key == ' ') {
			currentScreen.serialEvent("KEY:" + key);
		}
		if (key == '[') {
			if (shipState.sillinessLevel >= 0 && shipState.poweredOn
					&& shipState.sillinessInProgress == false) {
				OscMessage msg = new OscMessage("/system/reactor/silliness");
				sillinessStartTime = millis();
				switch (shipState.sillinessLevel) {
				case 0:
					shipState.sillinessLevel = 1;
					shipState.sillinessInProgress = true;
					msg.add(0);
					OscP5.flush(msg, new NetAddress(serverIP, 12000));
					bannerSystem.setSize(700, 300);
					bannerSystem.setTitle("!!WARNING!!");
					bannerSystem
							.setText("Please do not push that button again");
					bannerSystem.displayFor(5000);
					consoleAudio.playClip("warning1");
					break;
				case 1:
					shipState.sillinessInProgress = true;
					// shut down
					shipState.sillinessLevel = 2;
					consoleAudio.playClip("warning2");
					msg.add(1);
					OscP5.flush(msg, new NetAddress(serverIP, 12000));
					break;
				case 2:
					shipState.sillinessInProgress = true;
					shipState.sillinessLevel = -1;
					consoleAudio.playClip("warning3");
					msg.add(2);
					OscP5.flush(msg, new NetAddress(serverIP, 12000));
					bannerSystem.setSize(700, 300);
					bannerSystem.setTitle("!!WARNING!!");
					bannerSystem.setText("You Didnt listen, did you?");
					bannerSystem.displayFor(5000);
					break;
				}
			}
		}
	}

	@Override
	public void keyReleased() {
		currentScreen.keyReleased(key);
	}

	@Override
	public void mouseClicked() {
		// ConsoleLogger.log(this,(mouseX + ":" + mouseY);
		if (currentScreen == powerDisplay) {
			((PowerDisplay) currentScreen).mouseClick(mouseX, mouseY);
		}
	}

	@Override
	protected void oscEvent(OscMessage theOscMessage) {
		super.oscEvent(theOscMessage);

		// ConsoleLogger.log(this,(theOscMessage);
		if (theOscMessage.checkAddrPattern("/system/reactor/stateUpdate") == true) {
			int state = theOscMessage.get(0).intValue();

			// reset flags
			bootDisplay.brokenBoot = false;

			if (state == 0) {
				shipState.poweredOn = false;
				shipState.poweringOn = false;
				bootDisplay.stop();
				bannerSystem.cancel();
				resetDevices();
			} else {

				if (!shipState.poweredOn) {
					shipState.poweringOn = true;
					changeDisplay(bootDisplay);
				}
			}
			currentScreen.oscMessage(theOscMessage);
		} else if (theOscMessage.checkAddrPattern("/scene/youaredead") == true) {
			// oh noes we died
			shipState.areWeDead = true;
			shipState.deathText = theOscMessage.get(0).stringValue();
			shipState.fuelLeaking = false;
			deathTime = millis();
			if (serialEnabled) {
				serialPort.write('k');
			}
		} else if (theOscMessage.checkAddrPattern("/game/reset") == true) {
			// reset the entire game
			if (serialEnabled) {
				serialPort.write('r');
				panelPort.write('R');
			}
			changeDisplay(displayMap.get("power"));	
			shipState.poweredOn = false;
			shipState.poweringOn = false;
			shipState.areWeDead = false;
			bootDisplay.stop();
			ConsoleLogger.log(this, "Game reset");
			shipState.sillinessLevel = 0;
		} else if (theOscMessage.checkAddrPattern("/engineer/powerState") == true) {

			if (theOscMessage.get(0).intValue() == 1) {
				shipState.poweredOn = true;
				shipState.poweringOn = false;
				bootDisplay.stop();
			} else {
				shipState.poweredOn = false;
				shipState.poweringOn = false;
			}
		
		} else if (theOscMessage
				.checkAddrPattern("/clientscreen/EngineerStation/changeTo")) {
			if (!shipState.poweredOn) {
				return;
			}
			String changeTo = theOscMessage.get(0).stringValue();
			try {
				Display d = displayMap.get(changeTo);
				ConsoleLogger.log(this, "found display for : " + changeTo);
				changeDisplay(d);
			} catch (Exception e) {
				ConsoleLogger.log(this, "no display found for " + changeTo);
				e.printStackTrace();
				changeDisplay(displayMap.get("power"));
			}
		} else if (theOscMessage.checkAddrPattern("/clientscreen/showBanner")) {
			String title = theOscMessage.get(0).stringValue();
			String text = theOscMessage.get(1).stringValue();
			int duration = theOscMessage.get(2).intValue();

			bannerSystem.setSize(700, 300);
			bannerSystem.setTitle(title);
			bannerSystem.setText(text);
			bannerSystem.displayFor(duration);
		} else if (theOscMessage.checkAddrPattern("/system/fuelLeakState")) {
			boolean state = theOscMessage.get(0).intValue() == 1 ? true : false;
			if (state) {
				ConsoleLogger.log(this, "fuel leak started");
				shipState.fuelLeaking = true;
				if (serialEnabled) {

					panelPort.write('F');
					char c = 50;
					panelPort.write(c);
				}
			} else {
				ConsoleLogger.log(this, "fuel leak stopped");
				shipState.fuelLeaking = false;
				if (serialEnabled) {

					panelPort.write('X');
				}
			}

			/*
			 * ---------next section is for routing general display messages to
			 * their right screens
			 */
		} else if (theOscMessage.addrPattern().startsWith(
				"/system/powerManagement")) {
			powerDisplay.oscMessage(theOscMessage);
		} else if (theOscMessage.addrPattern().startsWith(
				"/engineer/wormholeStatus/")) {

			wormholeDisplay.oscMessage(theOscMessage);
		} else if (theOscMessage.addrPattern().startsWith("/system/jammer/")) {

			jamDisplay.oscMessage(theOscMessage);
		} else if (theOscMessage.checkAddrPattern("/ship/effect/playSound")) {
			String name = theOscMessage.get(0).stringValue();
			consoleAudio.playClip(name);
		} else {
			if (currentScreen != null) {
				currentScreen.oscMessage(theOscMessage);
			}
		}
	}

	/* send a probe to engineer arduino panel to get the current state */
	void probeEngPanel() {
		if (serialEnabled) {
			ConsoleLogger.log(this, "Probing engineer panel for state...");
			panelPort.write('P');
			// mute the random beeps in console audio and only unmute when
			// reeiving a probe complete message
			consoleAudio.muteBeeps = true;
		}
	}

	// send a reset to all attached devices
	void resetDevices() {
		if (!serialEnabled) {
			return;
		}
		serialPort.write('r');
	}

	// ---------------- main method

	@Override
	public void setup() {
		super.setup();
		consoleName = "engineerconsole";
		if (testMode) {
			serialEnabled = false;
			serverIP = "127.0.0.1";
			shipState.poweredOn = true;
		} else {
			serialEnabled = true;
			serverIP = "10.0.0.100";
			shipState.poweredOn = false;
		}

		if (serialEnabled) {
			serialPort = new Serial(this, "COM11", 9600);
			panelPort = new Serial(this, "COM12", 115200);
		}

		oscP5 = new OscP5(this, 12001);
		serverAddress = new NetAddress(serverIP, 12000);

		powerDisplay = new PowerDisplay(this);
		jamDisplay = new JamDisplay(this);
		displayMap.put("power", powerDisplay);
		displayMap.put("drop", new DropDisplay(this));
		displayMap.put("hyperspace", new HyperSpaceDisplay(this));
		displayMap.put("jamming", jamDisplay);
		displayMap.put("airlockdump", new AirlockDisplay(this));
		displayMap.put("selfdestruct", new DestructDisplay(this));

		displayMap.put("cablepuzzle", new CablePuzzleDisplay(this));
		displayMap.put("failureScreen", new FailureScreen(this));
		displayMap.put("restrictedArea", new RestrictedAreaScreen(this));

		bootDisplay = new BootDisplay(this);
		displayMap.put("boot", bootDisplay);

		// setup sound
		minim = new Minim(this);
		consoleAudio = new ConsoleAudio(this, minim);

		// set initial screen, probably gets overwritten from game shortly
		changeDisplay(displayMap.get("power"));

		/* sync to current game screen */
		OscMessage myMessage = new OscMessage("/game/Hello/EngineerStation");
		oscP5.send(myMessage, serverAddress);
	}

	@Override
	protected void shipDamaged(float amount) {
		if (currentScreen == powerDisplay) {
			((PowerDisplay) powerDisplay).shipDamaged(amount);
		}

	}

}
