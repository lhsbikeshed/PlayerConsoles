package tactical;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import pilot.CollisionRadarDisplay;
import processing.core.PVector;
import processing.serial.Serial;
import common.ConsoleAudio;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;
import common.displays.BootDisplay;
import common.displays.CablePuzzleDisplay;
import common.displays.DestructDisplay;
import common.displays.FailureScreen;
import common.displays.RestrictedAreaScreen;
import common.displays.WarpDisplay;
import ddf.minim.Minim;

import java.awt.Point;
import java.awt.Robot;

public class TacticalConsole extends PlayerConsole {

	// dont change anything past here. Things will break

	// CHANGE ME for testing
	// disables serial port access
	// and sets server to localhost
	

	DropDisplay dropDisplay; // display for the drop scene
	WarpDisplay warpDisplay; // warp scene

	WeaponsConsole weaponsDisplay; // tactical weapons display
	PlottingDisplay plottingDisplay;

	// power for something, not sure what
	int systemPower = -1;
	
	//hardware
	TacticalHardwareController mainPanelHardware;
	FanLightHardwareController fanController;

	public PVector mousePosition = new PVector(0,0);
	Robot mouseRobot;
	private boolean ignoreMouse;

	@Override
	public void drawConsole() {

		noSmooth();
		

		background(0, 0, 0);

		if (shipState.areWeDead) {
			drawDeadScreen();
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

		
	}

	/* these are just for testing when serial devices arent available */
	@Override
	public void keyPressed() {
		
	}

	

	@Override
	protected void oscEvent(OscMessage theOscMessage) {
		try {
		super.oscEvent(theOscMessage);
		
		if (theOscMessage.checkAddrPattern("/scene/warzone/weaponState") == true) {
			int msg = theOscMessage.get(0).intValue();
			if (msg == 1) {
				mainPanelHardware.setPowerState(true);
			} else {
				mainPanelHardware.setPowerState(false);
				
			}

			currentScreen.oscMessage(theOscMessage);
		
		
		
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
				mainPanelHardware.setPowerState(true);
				fanController.setPowerState(true);
				
			} else {
				shipState.poweredOn = false;
				shipState.poweringOn = false;
				mainPanelHardware.setPowerState(false);
				fanController.setPowerState(false);
			}
	

		} else if (theOscMessage.checkAddrPattern("/control/subsystemstate") == true) {
			int beamPower = theOscMessage.get(3).intValue()/4 - 1; // write charge
																	// rate
			int propPower = theOscMessage.get(0).intValue()/4 - 1;
			int sensorPower = theOscMessage.get(2).intValue()/4 - 1;
			int internalPower = theOscMessage.get(1).intValue()/4 - 1;

			mainPanelHardware.setChargeRate(beamPower);
			fanController.setPowerLevels(propPower, beamPower, sensorPower,internalPower);
			
			
			currentScreen.oscMessage(theOscMessage);
		
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
		
		} else if (theOscMessage.checkAddrPattern("/ship/effect/openFlap")) {
			mainPanelHardware.popFlap();
		} else if (theOscMessage.checkAddrPattern("/ship/effect/flapStrobe")) {
			mainPanelHardware.strobe();
		}else if (theOscMessage.checkAddrPattern("/ship/weaponState")){
				int state = theOscMessage.get(0).intValue();
				switch(state){
				case ShipState.WEAPON_STOWED:
					getConsoleAudio().playClip("weaponsRetracted");
					mainPanelHardware.setWeaponPanelState(false);
					break;
				case ShipState.WEAPON_DEPLOYED: 
					getConsoleAudio().playClip("weaponsDeployed");
					mainPanelHardware.setWeaponPanelState(true);

					break;
				}
		} else {
			if (currentScreen != null) {
				currentScreen.oscMessage(theOscMessage);
			}
		}
		} catch (Exception e){
			e.printStackTrace();
		}

	}

	

	// ---------------- main method

	@Override
	public void setup() {
		super.setup();
		consoleName = "tacticalconsole";

		if (testMode) {
			ConsoleLogger.log(this, "running test mode tactical");
			serverIP = "127.0.0.1";
			shipState.poweredOn = true;
		} else {
			ConsoleLogger.log(this, "running LIVE mode tactical");
			serverIP = "10.0.0.100";
			frame.setLocation(1024, 0);
			hideCursor();
		}

		oscP5 = new OscP5(this, 12004);
		serverAddress = new NetAddress(serverIP, 12000);
		
		globalFont = loadFont("common/HanzelExtendedNormal-48.vlw");

		dropDisplay = new DropDisplay(this);
		// radarDisplay = new RadarDisplay();
		warpDisplay = new WarpDisplay(this);
		weaponsDisplay = new WeaponConsoleNew(this);

		displayMap.put("weapons", weaponsDisplay);
		displayMap.put("drop", dropDisplay);
		displayMap.put("hyperspace", warpDisplay);
		displayMap.put("selfdestruct", new DestructDisplay(this));
		displayMap.put("cablepuzzle", new CablePuzzleDisplay(this));
		displayMap.put("failureScreen", new FailureScreen(this));
		displayMap.put("restrictedArea", new RestrictedAreaScreen(this));
		displayMap.put("plottingDisplay", new PlottingDisplay(this));
		
		

		
		mainPanelHardware = new TacticalHardwareController("mainPanel", "COM7", 115200, this);
		hardwareControllers.add(mainPanelHardware);
		mainPanelHardware.setPowerState(false);

		fanController = new FanLightHardwareController("fanController", "COM5", 9600, this);
		fanController.setPowerState(false);

		
		
		shipState.smartBombsLeft = 6;
		
		
		//now console is loaded up, load the sound config
		consoleAudio = new ConsoleAudio(this, minim, 1.0f);
		

		// set initial screen, probably gets overwritten from game shortly
		changeDisplay(displayMap.get("weapons"));

		/* sync to current game screen */
		OscMessage myMessage = new OscMessage("/game/Hello/TacticalStation");
		oscP5.send(myMessage, new NetAddress(serverIP, 12000));

		try{
			mouseRobot = new Robot();
			mouseRobot.mouseMove(getLocationOnScreen().x+512, getLocationOnScreen().y + 384); 
		} catch (Exception e){
			ConsoleLogger.log(this, "there was a problem setting up the robot class. Game will fail");
			
		}
	}
	
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
		if(mouseRobot!= null && ignoreMouse == false){
			mouseRobot.mouseMove(getLocationOnScreen().x+512, getLocationOnScreen().y + 384);  
			mousePosition.x += e.getX() - 512;
			mousePosition.y += e.getY() - 384;
			if(mousePosition.x < 0){
				mousePosition.x = 0;
			} else if (mousePosition.x > 1024){
				mousePosition.x = 1024;
			}
			if(mousePosition.y < 0){
				mousePosition.y = 0;
			} else if (mousePosition.y > 768){
				mousePosition.y = 768;
			}
		}
	}
	
	
	@Override
	protected void shipDamaged(float amount) {
		//flash the lights attached to this console
		mainPanelHardware.shipDamage(amount);
		fanController.shipDamage();

	}

	@Override
	protected void gameReset() {
		super.gameReset();
		// reset the entire game
		changeDisplay(weaponsDisplay);
		shipState.areWeDead = false;
		shipState.poweredOn = false;
		shipState.poweringOn = false;
		mainPanelHardware.reset();
		
		shipState.smartBombsLeft = 6;
	}

	@Override
	protected void shipDead() {
		ConsoleLogger.log(this, "Ship exploded");
		mainPanelHardware.setPowerState(false);
		fanController.setPowerState(false);
	}

	@Override
	protected void reactorStarted() {
		ConsoleLogger.log(this, "Reactor started");
		mainPanelHardware.setPowerState(true);
		fanController.setPowerState(true);

		
	}

	@Override
	protected void reactorStopped() {
		ConsoleLogger.log(this, "Reactor stopped");
		mainPanelHardware.setPowerState(false);
		fanController.setPowerState(false);

		
	}

	@Override
	public void hardwareEvent(HardwareEvent h) {
		currentScreen.serialEvent(h);
		
		if(h.event.equals("KEY")){
			if(h.value == 83){
				ignoreMouse = !ignoreMouse;
			} else if (h.value == KeyEvent.VK_P){
				OscMessage m = new OscMessage("/control/screenSelection");
				m.add("TacticalStation");
				m.add("plottingDisplay");
				getOscClient().send(m, getServerAddress());
			}
		} else if (h.event.equals("MOUSECLICK")){
			ConsoleLogger.log(this, "mx: " + mousePosition.x + " y: " + mousePosition.y);
		}
		
	}

}