package engineer;

import oscP5.OscMessage;
import common.ConsoleLogger;
import common.HardwareController;
import common.HardwareEvent;
import common.PlayerConsole;

public class LowerPanelHardware extends HardwareController {
	
	public static final int STICK_UP = 14;
	public static final int STICK_DOWN = 16;
	public static final int STICK_LEFT = 15;
	public static final int STICK_RIGHT = 13;
	
	public static final int BTN_DNP = 12;
	public static final int BTN_JAM = 11;
	
	
	public int fuelValveDirection = 0; //0 to space, 1/2/3 are for the other tanks
	public boolean fuelValveState = false;	//state of the main fuel transfer valve
									
	

	public LowerPanelHardware(String interfaceName, String port, int rate,
			PlayerConsole parent) {
		super(interfaceName, port, rate, parent);
		ConsoleLogger.log(this,"Starting lower panel hardware on " + port);
		
	}
	
	public void bufferComplete(){
		char p = serialBuffer[0];
		
		if (p == 'P') { // this switch from new panel
			String vals = finalBufferContents;
			if (vals.substring(0, 2).equals("PS")) { // PS10:1
				// chop off first two chars, split on the : character
				vals = vals.substring(2);
				String[] sw = vals.split(":");

				HardwareEvent h = new HardwareEvent();
				h.event = "NEWSWITCH";
				h.id = Integer.parseInt(sw[0]);
				h.value = Integer.parseInt(sw[1]);
				parent.hardwareEvent(h);
				
				if(h.value < 13 && h.value > 14){
					parent.getConsoleAudio().randomBeep();	//no beeps for stick buttons
				}
				
				if(h.id == 9){	//9 is hardcoded to the jump system toggle
					//send that message now but pass it on to rest of console in case something wants it
					OscMessage m = new OscMessage("/system/jump/state");
					m.add(h.value);
					parent.getOscClient().send(m, parent.getServerAddress());
	
				}
				
			} else if (vals.substring(0, 2).equals("PC")) {// probe complete,
															// unmute audio for
															// buttons
				parent.getConsoleAudio().muteBeeps = false;
			} else if (p == 'F'){
				//fuel was pumped, next character should be an integer from 0-10 based on how rapidly its pumped
				int c = Integer.parseInt(vals.substring (1,2));
				if(c >= 0 && c <= 9){
					OscMessage m = new OscMessage("/system/reactor/pumpRate");
					m.add(c);
					parent.getOscClient().send(m, parent.getServerAddress());
	
				}
			} else if (p == 'V'){
				setFuelValveState(true);
			} else if (p == 'v'){
				setFuelValveState(false);
			} else {
				// its a dial
				
				int switchNum = Integer.parseInt(vals.substring(1,2));
				int value = Integer.parseInt(vals.substring(3));
				HardwareEvent h = new HardwareEvent();
				h.event = "NEWDIAL";
				h.id = switchNum;
				h.value = value;
				
				
				
				
				parent.hardwareEvent(h);

			}
		} 
	}
	
	
	
	public void setFuelValveState(boolean b) {
		fuelValveState = b;
		OscMessage m = new OscMessage("/system/reactor/setFuelValveState");
		m.add(b  ? 1 : 0);
		parent.getOscClient().send(m, parent.getServerAddress());
		
	}

	public void reset() {
		ConsoleLogger.log(this, "resetting..");

		if(parent.testMode){
			return;
		} 
		serialPort.write('R');
		serialPort.write("D0");

		
	}
	
	public void setDNPBlink(boolean state){
		if(parent.testMode){
			ConsoleLogger.log(this, "setting DNP blink to " + state);
			return;
		}
		
		if(state ){
			serialPort.write("D1");
			
		} else {
			serialPort.write("D0");
		}
	}
	
	public void powerOff(){
		if(parent.testMode){
			ConsoleLogger.log(this, "powering off..");
			return;
		} 
		
		serialPort.write("D0");

	}
	
	public void powerOn(){}

	public void setFuelRate(int i) {
		ConsoleLogger.log(this, "Setting fuel leak rate to " + i);

		if(parent.testMode){
			return;
		} 
		if(i == 0){
			serialPort.write('X');

		} else {

			serialPort.write('F');
			char c = (char) i;
			serialPort.write(c);
		}
				
	}

	public void probePanel() {
		ConsoleLogger.log(this, "probing panel for state");

		if(parent.testMode){
			return;
		} 
		parent.getConsoleAudio().muteBeeps = true;
		serialPort.write('P');
	
		
	}


}
