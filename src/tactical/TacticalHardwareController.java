package tactical;

import common.ConsoleLogger;
import common.HardwareController;
import common.HardwareEvent;
import common.PlayerConsole;

/* responsible for:
 * tactical keyboard
 * flap
 * strobe
 * hyperspace power conduit
 * xmas tree lights
 */
public class TacticalHardwareController extends HardwareController {

	public static final int KP_0 = 0;
	public static final int KP_1 = 1;
	public static final int KP_2 = 2;
	public static final int KP_3 = 3;
	public static final int KP_4 = 4;
	public static final int KP_5 = 5;
	public static final int KP_6 = 6;
	public static final int KP_7 = 7;
	public static final int KP_8 = 8;
	public static final int KP_9 = 9;
	
	public static final int KP_SCAN = 10;
	public static final int KP_DECOY = 11;
	public static final int KP_LASER = 12;
	
	boolean decoyLightState = false;

	// is the decoy blinker on?
	boolean decoyBlinker = false;

	//there should only ever be one of these per console
	public static TacticalHardwareController instance;
	
	
	public TacticalHardwareController(String interfaceName, String port,
			int rate, PlayerConsole parent) {
		super(interfaceName, port, rate, parent);
		ConsoleLogger.log(this, "Starting Tactical panel controller on " + port);
		instance = this;
	}

	public void bufferComplete(){
		char c = serialBuffer[0];
		if (c >= '0' && c <= '9') {
			keypadKey();
			
		}
		if (c == ' ') {
			HardwareEvent h = new HardwareEvent();
			h.event = "KEYPAD";
			h.id = KP_SCAN;
			h.value = 1;
			parent.hardwareEvent(h);
		}
		if (c == 'F') {
			HardwareEvent h = new HardwareEvent();
			h.event = "KEYPAD";
			h.id = KP_LASER;
			h.value = 1;
			parent.hardwareEvent(h);
		}
		if (c == 'm') {
			HardwareEvent h = new HardwareEvent();
			h.event = "KEYPAD";
			h.id = KP_DECOY;
			h.value = 1;
			parent.hardwareEvent(h);
		}

		if (c == 'C') {
			HardwareEvent h = new HardwareEvent();
			h.event = "CONDUIT";
			h.id = Integer.parseInt("" + serialBuffer[1]);
			h.value = 1;
			parent.hardwareEvent(h);

		}
		if (c == 'c') {
			HardwareEvent h = new HardwareEvent();
			h.event = "CONDUIT";
			h.id = Integer.parseInt("" + serialBuffer[1]);
			h.value = 0;
			parent.hardwareEvent(h);
			
		}
	}
	
	//decode the buffer as a keypad key
	//these are 0-9 so just parse as Ints for the id part
	void keypadKey(){
		HardwareEvent h = new HardwareEvent();
		h.event = "KEYPAD";
		h.id = Integer.parseInt("" + serialBuffer[0]);
		h.value = 1;
		
		parent.getConsoleAudio().randomBeep();
		parent.hardwareEvent(h);
	}
	
	


	void decoyLightState(boolean s) {
		if (parent.testMode == true) {
			return;
		}
		
		if (s && decoyLightState == false) {
			// ConsoleLogger.log(this, ("poo");
			decoyLightState = true;
			serialPort.write("D,");
		} else if (!s && decoyLightState == true) {
			serialPort.write("d,");
			decoyLightState = false;
		}
	}

	public void setPowerState(boolean b) {
		if(parent.testMode) return;
		if(b){
			/* power up the tac console panel */			
			serialPort.write("P,");			
		} else {
			serialPort.write("p,");
			decoyBlinker = false;
		}
		
	}

	public void setChargeRate(int beamPower) {
		if(parent.testMode) return;
		
		serialPort.write("L" + beamPower + ",");

		
	}

	public void popFlap() {
		if (parent.testMode == false) {
			serialPort.write("T,");
			serialPort.write("F,");
		} else {
			ConsoleLogger.log(this, "Popping flap..");
		}
	}

	public void strobe() {
		if (parent.testMode == false) {
			serialPort.write("F,");
		} else {
			ConsoleLogger.log(this, "starting strobe..");

		}
	}
	
	public void probeCableState() {
		if (parent.testMode == false) {
			serialPort.write("C,");
		} else {
			ConsoleLogger.log(this, "probed cable puzzle state");
		}
	}

	void setDecoyBlinkerState(boolean state) {
		if (parent.testMode == true) {
			ConsoleLogger.log(this, "setting decoy blink to: " + state);
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

	public void shipDamage(float amount) {
		if(parent.testMode){
			ConsoleLogger.log(this, "Damage effect");
		} else {
			if(amount > 0.0f){
				serialPort.write("S,");
			}
		}
	}
}
