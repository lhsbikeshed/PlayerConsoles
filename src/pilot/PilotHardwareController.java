package pilot;

import oscP5.OscMessage;
import processing.core.PApplet;
import common.ConsoleLogger;
import common.HardwareController;
import common.HardwareEvent;
import common.PlayerConsole;

public class PilotHardwareController extends HardwareController {

	public static final int SW_LANDINGGEAR = 0;
	public static final int SW_LIGHTSWITCH = 1;
	public static final int SW_BLASTSHIELD = 2;
	public static final int SW_ENGINES = 3;
	public static final int SW_JUMPSWITCH = 4;
	public static final int SW_JUMPBUTTON = 5;
	
	public static final int SW_THROTTLE = 10;
	
	// mappings from physical buttons to OSC messages
	String[] messageMapping = { "/system/undercarriage/state",
			"/scene/launchland/dockingCompState", "/system/misc/blastShield",
			"/system/propulsion/state", "/system/nothing/nothing",
			"/system/jump/doJump" };

	public PilotHardwareController(String interfaceName, String port, int rate,
			PlayerConsole parent) {
		super(interfaceName, port, rate, parent);
		ConsoleLogger.log(this, "Started pilot hardware on " + port);
	}
	
	
	protected void bufferComplete(){
		
		//split the buffer up, format for pilot controls is
		// <switch number><state>,
		//except throttle which is t<number>,
		// and cable connections which are either C:<number>:<number2> or c:<number>:<number2>
		
		char commandChar = serialBuffer[0];
		if(commandChar == 't'){
			HardwareEvent h = new HardwareEvent();
			h.event = "THROTTLE";
			h.id = SW_THROTTLE;
			String buffer = finalBufferContents;
			
			int th = Integer.parseInt(buffer.substring(1));
			
			
			h.value = th;
			parent.hardwareEvent(h);
			
		} else if (commandChar == 'c' || commandChar == 'C'){
			// cable connection event 
			// C:<plug>:<socket>
			OscMessage msg;
			if (commandChar == 'C') {
				msg = new OscMessage("/system/cablePuzzle/connect");
			} else {
				msg = new OscMessage("/system/cablePuzzle/disconnect");
			}
			String vals = finalBufferContents;
			String[] parts = vals.split(":");
			int plugId = Integer.parseInt(parts[1]);
			int socketId = Integer.parseInt(parts[2]);
			msg.add(plugId);
			msg.add(socketId);
			parent.getOscClient().send(msg, parent.getServerAddress());
			
			//pass it up to any consoles that are interested
			HardwareEvent h = new HardwareEvent();
			h.event = commandChar == 'C' ? "CABLECONNECT" : "CABLEDISCONNECT";
			h.id = plugId;
			h.value = socketId;
			parent.hardwareEvent(h);
			
		} else {
			int switchNumber = Integer.parseInt("" + serialBuffer[0]);
			int state = Integer.parseInt("" + serialBuffer[1]);
			
			HardwareEvent h = new HardwareEvent();
			h.event = "BUTTON";
	
			h.event = "SWITCH";
			h.id = switchNumber;
			h.value = state;
			h.fromDevice = interfaceName;
			parent.hardwareEvent(h);
			
			if (parent.getShipState().poweredOn == true) {
					
				//find out what osc message this maps to and send it to the server
				OscMessage myMessage = new OscMessage(messageMapping[switchNumber]);
				if (switchNumber == 4) {
					// jump switch is now inverted
					state = 1 - state;
				}
				myMessage.add(state);
				parent.getOscClient().send(myMessage, parent.getServerAddress());
			}
		}
		
		
	}
	
	void setJumpLightState(boolean state) {
		if (state == true && parent.getShipState().jumpState == false) {
			serialPort.write('B');
			parent.getShipState().jumpState = true;
		} else if (state == false && parent.getShipState().jumpState == true) {
			serialPort.write('b');
			parent.getShipState().jumpState = false;
		}
	}

}
