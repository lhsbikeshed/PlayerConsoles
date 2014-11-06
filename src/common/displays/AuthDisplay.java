package common.displays;

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


/* generic authorisation display, ask the player for a code then report back to game with pass and fail
 * configure a text string, expected code and timeout for puzzle
 */
public class AuthDisplay extends Display {

	OscP5 p5;

	// assets
	PImage authImage;

	String promptText = "NEED CODE";
	long puzzleDuration = 10000;
	

	public int STATE_AUTH = 0;
	public int STATE_CODEOK = 1;
	int state = STATE_AUTH;

	String currentAuthCode = "62918";
	
	long failTimer = 0;

	String authCode = "";
	boolean authResult = false;
	long authDisplayTime = 0;

	
	boolean showFailure = false; // show a failure message on screen?

	//osc bits
	String serverIP;

	private int sceneStartTime;
	
	public AuthDisplay(PlayerConsole parent) {
		super(parent);
		this.p5 = parent.getOscClient();
		serverIP = parent.getServerIP();
		
		authImage = parent.loadImage("common/authscreenbg.png");
		
	}

	

	@Override
	public void draw() {
		
		if (state == STATE_AUTH || state == STATE_CODEOK) {
			
			parent.fill(255, 255, 255);
			parent.image(authImage, 0, 0, parent.width, parent.height);
			parent.textFont(font, 40);
			float tw = parent.textWidth(promptText);
			parent.text(promptText, 512 - tw/2f, 303);
			if(authResult){
				parent.fill(0,255,0);
			}
			parent.textFont(font, 80);
			String code = authCode;
			if(parent.globalBlinker){
				code += "_";
			}
			parent.text(code, 158, 495);

			if (authDisplayTime + 1500 > parent.millis()) {
				if (authResult == false) {
					parent.fill(255, 0, 0);
					parent.textFont(font, 40);
					parent.text("CODE FAIL", 357, 369);
				} else {
					parent.fill(0, 255, 0);
					parent.textFont(font, 40);
					parent.text("CODE OK", 266, 573);
				}
			} else if (authDisplayTime + 2500 > parent.millis()
					&& authResult == true) {
				state = STATE_CODEOK;
			}

			
			
			//parent.text(puzzleDuration - (parent.millis() - sceneStartTime), 200,200); 
		}

	}

	

	@Override
	public void oscMessage(OscMessage msg) {
		if(msg.checkAddrPattern("/system/authsystem/authParameters")){
			//read the code to check for, timeout for puzzle (-1 for never) and prompt text
			currentAuthCode = msg.get(0).stringValue();
			puzzleDuration = msg.get(1).longValue();
			promptText = msg.get(2).stringValue();
			ConsoleLogger.log(this, "setting auth params " + currentAuthCode);
		} else if (msg.checkAddrPattern("/system/authsystem/cancelAuth")){
			ConsoleLogger.log(this, "authorisation cancelled, sending ok signal");
			authOk();
		}
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		
		if (state == STATE_AUTH) {
			keyEntered(evt);
		}
	}

	private void keyEntered(HardwareEvent evt) {
		char c = (char)evt.value;
		if (evt.event.equals("KEY")) {
			parent.getConsoleAudio().randomBeep();
			if (authCode.length() < currentAuthCode.length() - 1) {
				authCode += c;
			} else {
				authCode +=c;
				if (authCode.equals(currentAuthCode)) {
					authResult = true;
					parent.getConsoleAudio().playClip("codeOk");
					// tell the main game that auth passed
					authOk();
				} else {
					authFail();
					
				}

				authDisplayTime = parent.millis();
			}
		}
		
	}



	private void authFail() {
		authResult = false;
		parent.getConsoleAudio().playClip("codeFail");
		authCode = "";
		ConsoleLogger.log(this, "Auth code fail");
		OscMessage msg = new OscMessage("/system/authsystem/codeFail");
		parent.getOscClient().send(msg, parent.getServerAddress());
	}



	private void authOk() {
		ConsoleLogger.log(this, "Auth code complete!");
		OscMessage msg = new OscMessage("/system/authsystem/codeOk");
		parent.getOscClient().send(msg, parent.getServerAddress());
	}



	@Override
	public void start() {
		
		currentAuthCode = "62918";

		
		sceneStartTime = parent.millis();
		authCode = "";
		authResult = false;
		authDisplayTime = 0; // start for auth fail/ok display time
		state = STATE_AUTH;
		
		showFailure = false;

	}

	@Override
	public void stop() {
		
		sceneStartTime = parent.millis();
		authCode = "";
		authResult = false;
		authDisplayTime = 0; // start for auth fail/ok display timesplay time
	}
}
