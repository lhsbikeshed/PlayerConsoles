package engineer;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.serial.Serial;
import common.ConsoleAudio;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
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
	Display  wormholeDisplay, jamDisplay, dropDisplay;
	PowerDisplay powerDisplay;

	// highlights
	ArrayList<Highlighter> highlightList = new ArrayList(0);

	// Peripheral things
	UpperPanelHardware upperPanel;
	LowerPanelHardware lowerPanel;

	boolean globalBlinker = false;

	long blinkTime = 0;

	long sillinessStartTime = 0;

	int fuelBeepTimer = 0;

	public void addHighlight(Highlighter h) {
		highlightList.add(h);
	}

	

	@Override
	public void drawConsole() {
		if (shipState.sillinessInProgress
				&& sillinessStartTime + 5000 < millis()) {
			shipState.sillinessInProgress = false;
		}

		noSmooth();
		background(0, 0, 0);
		

		if (shipState.areWeDead) {
			drawDeadScreen();
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


	private void doSilliness() {
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



	@Override
	protected void oscEvent(OscMessage theOscMessage) {
		super.oscEvent(theOscMessage);

		if (theOscMessage.checkAddrPattern("/engineer/powerState") == true) {

			if (theOscMessage.get(0).intValue() == 1) {
				shipState.poweredOn = true;
				shipState.poweringOn = false;
				bootDisplay.stop();
				upperPanel.forcePowerMode(true);
			} else {
				shipState.poweredOn = false;
				shipState.poweringOn = false;
				upperPanel.forcePowerMode(false);
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
		
		} else if (theOscMessage.checkAddrPattern("/system/fuelLeakState")) {
			boolean state = theOscMessage.get(0).intValue() == 1 ? true : false;
			if (state) {
				ConsoleLogger.log(this, "fuel leak started");
				shipState.fuelLeaking = true;
				lowerPanel.setFuelRate(50);
			} else {
				ConsoleLogger.log(this, "fuel leak stopped");
				shipState.fuelLeaking = false;
				lowerPanel.setFuelRate(0);
			}

			/*
			 * ---------next section is for routing general display messages to
			 * their right screens
			 */
		} else if (theOscMessage.addrPattern().startsWith(
				"/system/powerManagement")) {
			powerDisplay.oscMessage(theOscMessage);
		
		} else if (theOscMessage.addrPattern().startsWith("/system/jammer/")) {

			jamDisplay.oscMessage(theOscMessage);
		
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
			lowerPanel.probePanel();
			// mute the random beeps in console audio and only unmute when
			// reeiving a probe complete message
			consoleAudio.muteBeeps = true;
		}
	}

	// send a reset to all attached devices
	void resetDevices() {
		lowerPanel.reset();
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

		//now console is loaded up, load the sound config
		consoleAudio = new ConsoleAudio(this, minim, 1.0f);

		upperPanel = new UpperPanelHardware("upperpanel", "COM11", 9600, this);
		lowerPanel = new LowerPanelHardware("lowerpanel", "COM12", 115200, this);
		hardwareControllers.add(upperPanel);
		hardwareControllers.add(lowerPanel);
		

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

	@Override
	protected void gameReset() {
		super.gameReset();
		// reset the entire game
		lowerPanel.reset();
		upperPanel.reset();
		
		
		changeDisplay(displayMap.get("power"));	
		powerDisplay.reset();
		shipState.poweredOn = false;
		shipState.poweringOn = false;
		shipState.areWeDead = false;
		bootDisplay.stop();
		ConsoleLogger.log(this, "Game reset");
		shipState.sillinessLevel = 0;
		
	}

	@Override
	protected void shipDead() {
		ConsoleLogger.log(this, "Ship exploded");
		shipState.fuelLeaking = false;
		deathTime = millis();
		upperPanel.kill();
		
	}

	@Override
	protected void reactorStarted() {
		ConsoleLogger.log(this, "Reactor started");		
	}

	@Override
	protected void reactorStopped() {
		ConsoleLogger.log(this, "Reactor stopped, resetting devices..");
		resetDevices();
		
	}

	@Override
	public void hardwareEvent(HardwareEvent h) {

		if(h.event.equals("KEY")){
			if(h.value == KeyEvent.VK_OPEN_BRACKET){
				doSilliness();
			}
			
		}
		
		currentScreen.serialEvent(h);
	}

}
