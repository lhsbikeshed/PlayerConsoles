package engineer;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import common.ConsoleAudio;
import common.ConsoleLogger;
import common.HardwareController;
import common.HardwareEvent;
import common.PlayerConsole;

/* protocol :
 * S<0-10> : startup sequence level
 * R1 : Reactor start
 * R0 : reactor off
 * A<0-9>, B<0-9> : jamming dials
 * pXXXX : power dial values X = 0-12 value 
 * L  : airlock dump button
 */
public class UpperPanelHardware extends HardwareController {

	public static final int BT_AIRLOCK = 0;

	public UpperPanelHardware(String interfaceName, String port, int rate,
			PlayerConsole parent) {
		super(interfaceName, port, rate, parent);
		ConsoleLogger.log(this, "starting upper panel on " + port);
	}

	
	public void bufferComplete(){
		char p = serialBuffer[0];
		
		if (p == 'A'){	//jamming dial 1			
			jammingDials(0);
		} else if (p == 'B'){	//jamming dial 2
			jammingDials(1);		
		} else if (p == 'S') {		//reactor control switches
			controlSwitches();
		} else if (p == 'R') {		//reactor start signal
			reactorStarted();			
		} else if (p == 'p') {		//power buttons on right of monitor
			powerButtons();
		} else if (p == 'L') {		//airlock button
			airlockButton();
		} 
	}

	
	private void airlockButton() {
		
		HardwareEvent h = new HardwareEvent();
		h.event = "BUTTON";
		h.id = BT_AIRLOCK;
		h.value = 1;
		parent.hardwareEvent(h);
	}
		
	/* h.id = dial id
	 * h.value = value of dial
	 */
	private void powerButtons() {
		//format is:
		// pXXXX
		// where X = power level from 0-12 + 65 (so its a printable char)
		OscMessage msg = new OscMessage("/control/subsystemstate");
		for(int i = 0; i < 4; i++){
			HardwareEvent h = new HardwareEvent();
			h.event = "POWERDIAL";
			h.id = i;
			
			h.value = (int)(finalBufferContents.charAt(1 + i)) - 65;
			parent.hardwareEvent(h);
			msg.add(h.value);
		}
		
		//now tell the game about this
		parent.getOscClient().send(msg, parent.getServerAddress());
		//these values then get pushed back to this console by the main game
		
			
		
	}

	private void reactorStarted() {
		OscMessage myMessage = new OscMessage(
				"/system/reactor/setstate");
		myMessage.add(1);
		parent.getOscClient().send(myMessage, parent.getServerAddress());
	}

	private void controlSwitches() {
		
		//command format is "S<number>"
		String vals = finalBufferContents;
		
		int switchNumber = Integer.parseInt(vals.substring(1, vals.length()));
		// ConsoleLogger.log(this,(v);
		ConsoleAudio consoleAudio = parent.getConsoleAudio();
		if (switchNumber == 0) {
			consoleAudio.playClipForce("codeFail");
		} else if (switchNumber <= 5) {
			consoleAudio.playClipForce("beepLow");
		} else if (switchNumber <= 9) {
			consoleAudio.playClipForce("beepHigh");
		} else {
			consoleAudio.playClipForce("reactorReady");
		}
		if (switchNumber > 0) {
			OscMessage myMessage = new OscMessage(
					"/system/reactor/switchState");

			myMessage.add(switchNumber);
			parent.getOscClient().send(myMessage, parent.getServerAddress());
		} else {
			OscMessage myMessage = new OscMessage(
					"/system/reactor/setstate");
			myMessage.add(0);
			parent.getOscClient().send(myMessage, parent.getServerAddress());
		}
		
	}

	private void jammingDials(int index) {
		String vals = finalBufferContents;
		int value = Integer.parseInt(vals.substring(1, vals.length()));
		
		HardwareEvent h = new HardwareEvent();
		h.event = "JAMDIAL";
		h.id = index;
		h.value = value;
		
		parent.hardwareEvent(h);
		
	}
	
	public void syncPowerLevels(){
		ConsoleLogger.log(this, "Syncing power dials..");
		if(parent.testMode){
			return;
		}
		
		
	}

	public void reset() {
		ConsoleLogger.log(this, "resetting..");

		if(parent.testMode){
			return;
		} 
		serialPort.write('r');
	}

	public void kill() {
		ConsoleLogger.log(this, "Panel killed..");

		if(parent.testMode){
			return;
		} 
			
		serialPort.write('k');
		
		
	}
	
	
}
