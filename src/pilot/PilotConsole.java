package pilot;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PImage;
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
import common.util.Rot;
import ddf.minim.Minim;

public class PilotConsole extends PlayerConsole {

	// ---joystick class
	Joystick joy;
	boolean autopilotBanner = false;

	boolean joystickTestMode = true;

	PImage autopilotOverlay;

	// serial stuff
	//Serial serialPort;
	//String serialBuffer = "";

	String lastSerial = "";

	
	// -----displays-----
	DropDisplay dropDisplay;
	WarpDisplay warpDisplay;
	RadarDisplay radarDisplay;
	LaunchDisplay launchDisplay;
	CablePuzzleDisplay cablePuzzleDisplay;
	SlingshotDisplay slingShotDisplay;

	FailureScreen failureScreen;
	

	float lastOscTime = 0;
	
	PilotHardwareController pilotHardware;

	@Override
	public void drawConsole() {

		noSmooth();
		float s = shipState.shipVel.mag();
		shipState.shipVelocity = lerp(shipState.lastShipVel, s,
				(millis() - shipState.lastTransformUpdate) / 250.0f);
		

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
			pilotHardware.setJumpLightState(false);
				
		} else if (theOscMessage.checkAddrPattern("/ship/jumpStatus") == true) {
			int v = theOscMessage.get(0).intValue();
			if (v == 0) {
				pilotHardware.setJumpLightState(false);
			} else if (v == 1) {
				pilotHardware.setJumpLightState(true);
			}
		} else if (theOscMessage.checkAddrPattern("/control/subsystemstate") == true) {
			
			// displayList[currentDisplay].oscMessage(theOscMessage);
			currentScreen.oscMessage(theOscMessage);
			pilotHardware.setJumpLightState(false);
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
				pilotHardware.setJumpLightState(false);
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
		displayMap.put("collisionradar", new CollisionRadarDisplay(this));
		displayMap.put("slingshot", new SlingshotDisplay(this));
		displayMap.put("landingDisplay", new LandingDisplay(this));
		
		//configure the pilot console hardware stuff
		pilotHardware = new PilotHardwareController("mainconsole", "COM8", 115200, this);
		hardwareControllers.add(pilotHardware);

		// damage stuff
		pilotHardware.setJumpLightState(false);

		//now console is loaded up, load the sound config
		consoleAudio = new ConsoleAudio(this, minim, -1.0f);



		autopilotOverlay = loadImage("pilotconsole/autopilotoverlay.png");

		/* sync to current game screen */
		OscMessage myMessage = new OscMessage("/game/Hello/PilotStation");
		oscP5.send(myMessage, serverAddress);

		// set initial screen
		Display d = displayMap.get("slingshot");
		changeDisplay(d);

	}

	@Override
	protected void shipDamaged(float amount) {
		//nothing to do here for this console

	}

	@Override
	protected void gameReset() {
		super.gameReset();
		currentScreen.stop();
		currentScreen = launchDisplay;
		currentScreen.start();
		shipState.areWeDead = false;
		pilotHardware.setJumpLightState(false);
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

	@Override
	public void hardwareEvent(HardwareEvent h) {
		if(h.event.equals("THROTTLE")){
			joy.setThrottle(h.value);
		}
		
	}
}
