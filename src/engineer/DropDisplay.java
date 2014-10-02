package engineer;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.UsefulShit;

public class DropDisplay extends Display {

	OscP5 p5;

	// assets
	PImage instructionImage, patchImage, authImage, jumpOverlayImage,
			jumpEnableOverlay, dropFailOverlay, plugImage;

	PImage structFailOverlay;

	boolean structFail = false;

	public int STATE_INST = 0;
	public int STATE_PATCHING = 1;
	public int STATE_AUTH = 2;
	public int STATE_CODEOK = 3;
	int state = STATE_INST;

	String currentAuthCode = "62918";
	// 12345, 62918, 26192
	String[] possibleAuthCodes = { "12345", "62918", "26192" };

	float chargePercent = 0;
	String serverIP = "";

	int curPatch = -1;

	long failTimer = 0;

	long sceneStartTime = 0;
	String authCode = "";
	boolean authResult = false;
	long authDisplayTime = 0;

	// patch panel state
	int[] cableOrder = { 0, 1, 2, 3, 4 };
	// cable pins -> colour map
	// yellow = 0
	// black = 1
	// white = 2
	// blue = 3
	// red = 4
	int[] colorMap = new int[5];

	int currentCable = 0;
	boolean[] cableState = new boolean[5];

	boolean showFailure = false; // show a failure message on screen?

	public DropDisplay(PlayerConsole parent) {
		super(parent);
		this.p5 = parent.getOscClient();
		serverIP = parent.getServerIP();
		instructionImage = parent.loadImage("engineerconsole/dropnew.png");
		patchImage = parent.loadImage("engineerconsole/dropnew.png");
		authImage = parent.loadImage("engineerconsole/dropscene3.png");
		jumpOverlayImage = parent
				.loadImage("engineerconsole/emergencyjump.png");
		jumpEnableOverlay = parent
				.loadImage("engineerconsole/jumpEnableOverlay.png");
		structFailOverlay = parent
				.loadImage("engineerconsole/structuralFailure.png");
		dropFailOverlay = parent
				.loadImage("engineerconsole/dropFailOverlay.png");
		plugImage = parent.loadImage("engineerconsole/dropPlugs.png");

		// setup colours for plugs
		colorMap[0] = UsefulShit.makeColor(255, 255, 0);
		colorMap[1] = UsefulShit.makeColor(0, 0, 0);
		colorMap[2] = UsefulShit.makeColor(255, 255, 255);
		colorMap[3] = UsefulShit.makeColor(0, 0, 255);
		colorMap[4] = UsefulShit.makeColor(255, 0, 0);
	}

	/*
	 * cable was connected, check to see if its the next one in the list if so
	 * then prepare for next one in list if not then set showFailure to true and
	 * stop paying attention to new connects
	 */
	private void cableConnected(int ind) {
		if (ind >= 0 && ind < 5) {
			if (cableState[ind] == true) { // ignore repeated connection events
				return;
			}
			cableState[ind] = true;
			parent.getConsoleAudio().playClip("beepHigh");
		}
		if (cableOrder[currentCable] == ind) {
			// yay! a good connection

			if (currentCable < 4) {
				currentCable++;
				ConsoleLogger.log(this, "Current cable:" + currentCable);
			} else {
				// we're done here, show the auth screen
				state = STATE_AUTH;
				parent.getConsoleAudio().playClip("codeOk");
			}
		} else {
			// wrong cable matey!
			showFailure = true;
			parent.getConsoleAudio().playClip("codeFail");
		}
	}

	/*
	 * cable was unplugged, if all cables are disconnected then turn the failure
	 * off
	 */
	private void cableDisconnected(int ind) {
		if (ind >= 0 && ind < 5) {
			if (cableState[ind] == false) { // ignore repeated disconnection
											// events
				return;
			}
			cableState[ind] = false;
			if (!showFailure) { // generally a disconnected cable will cause a
								// failure
				showFailure = true;
				parent.getConsoleAudio().playClip("codeFail");
			}
		}
		// check to see if all cables are disconnected now
		boolean allClear = true;
		for (int i = 0; i < 5; i++) {
			if (cableState[i] == true) {
				allClear = false;
			}
		}

		if (allClear) {
			showFailure = false;
			currentCable = 0;
		}
	}

	@Override
	public void draw() {
		if (state == STATE_INST) {
			parent.image(instructionImage, 0, 0, parent.width, parent.height);

			state = STATE_PATCHING;
		} else if (state == STATE_PATCHING) {
			parent.image(patchImage, 0, 0, parent.width, parent.height);
			parent.fill(0, 255, 0);
			parent.textFont(font, 20);
			parent.noStroke();
			for (int i = 0; i < 5; i++) {
				// draw the plug colour
				parent.fill(colorMap[cableOrder[i]]);
				parent.rect(350, 300 + 88 * i, 85, 62);
				parent.rect(614, 300 + 88 * i, 85, 62);

				// and now mask it with cleverness
				parent.image(plugImage, 350, 299 + 88 * i);

				// and connection state
				if (i < currentCable) {
					if (cableState[cableOrder[i]] == true) {
						parent.fill(0, 255, 0); // connected and correct
					} else {
						parent.fill(250, 0, 0); // connected and wrong
					}
				} else {
					if (i == currentCable) {
						int c = (int) PApplet.map(
								PApplet.sin(parent.millis() / 100.0f), -1.0f,
								1.0f, 0, 255);
						parent.fill(c, c, 0);
					} else {
						parent.fill(120, 0, 0); // not connected
					}
				}
				parent.rect(438, 301 + 88 * i, 174, 62);
			}

			if (showFailure) {
				parent.image(dropFailOverlay, 97, 471);
			}
		} else if (state == STATE_AUTH || state == STATE_CODEOK) {
			parent.fill(255, 255, 255);
			parent.image(authImage, 0, 0, parent.width, parent.height);
			parent.textFont(font, 20);
			parent.text(authCode + "_", 266, 445);

			if (authDisplayTime + 1500 > parent.millis()) {
				if (authResult == false) {
					parent.fill(255, 0, 0);
					parent.textFont(font, 40);
					parent.text("CODE FAIL", 266, 573);
				} else {
					parent.fill(0, 255, 0);
					parent.textFont(font, 40);
					parent.text("CODE OK", 266, 573);
				}
			} else if (authDisplayTime + 2500 > parent.millis()
					&& authResult == true) {
				state = STATE_CODEOK;
			}

			if (state == STATE_CODEOK) {
				// show an overlay that the jump engine is on and charging
				parent.image(jumpOverlayImage, 64, 320);
				parent.rect(125, 469,
						PApplet.map(chargePercent, 0, 1.0f, 0, 480), 48);
				if (chargePercent >= 1.0f) {
					parent.image(jumpEnableOverlay, 173, 237);
				}
			}
		}

		if (structFail) { // show the "structural failure" warning

			parent.image(structFailOverlay, 128, 200);
		}
	}

	

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		if (theOscMessage.checkAddrPattern("/ship/stats") == true) {
			chargePercent = theOscMessage.get(0).floatValue();
		}
		if (theOscMessage.checkAddrPattern("/scene/drop/panelRepaired") == true) {

			state = STATE_AUTH;
		} else if (theOscMessage.checkAddrPattern("/scene/drop/conduitConnect") == true) {
			int val = theOscMessage.get(0).intValue();
			cableConnected(val);
		} else if (theOscMessage
				.checkAddrPattern("/scene/drop/conduitDisconnect") == true) {
			int val = theOscMessage.get(0).intValue();
			cableDisconnected(val);
			// println(curPatch);
		} else if (theOscMessage.checkAddrPattern("/scene/drop/conduitFail") == true) {
			curPatch = -1;
			showFailure = true;
			failTimer = parent.millis();
			parent.getConsoleAudio().playClip("codeFail");
		} else if (theOscMessage
				.checkAddrPattern("/scene/drop/structuralFailure") == true) {
			structFail = true;
		}
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		
		if (state == STATE_AUTH) {
			char c = (char)evt.value;
			if (evt.event.equals("KEY")) {
				parent.getConsoleAudio().randomBeep();
				if (authCode.length() < 4) {
					authCode += c;
				} else {
					authCode +=c;
					if (authCode.equals(currentAuthCode)) {
						authResult = true;
						parent.getConsoleAudio().playClip("codeOk");
						// tell the main game that auth passed
						OscMessage myMessage = new OscMessage(
								"/scene/drop/droppanelrepaired");
						myMessage.add(2);
						p5.send(myMessage, new NetAddress(serverIP, 12000));
					} else {
						authResult = false;
						parent.getConsoleAudio().playClip("codeFail");
						authCode = "";
					}

					authDisplayTime = parent.millis();
				}
			}
		}
	}

	@Override
	public void start() {
		structFail = false;
		currentAuthCode = possibleAuthCodes[1];

		chargePercent = 0;
		sceneStartTime = parent.millis();
		authCode = "";
		authResult = false;
		authDisplayTime = 0; // start for auth fail/ok display time
		state = STATE_INST;
		curPatch = -1;
		currentCable = 0;
		showFailure = false;

		// randomise the order
		for (int i = 4; i > 0; i--) {
			int rand = PApplet.floor(parent.random(i + 1));
			if (rand != i) {
				int t = cableOrder[i];
				cableOrder[i] = cableOrder[rand];
				cableOrder[rand] = t;
			}
		}
	}

	@Override
	public void stop() {
		chargePercent = 0;
		sceneStartTime = parent.millis();
		authCode = "";
		authResult = false;
		authDisplayTime = 0; // start for auth fail/ok display timesplay time
	}
}
