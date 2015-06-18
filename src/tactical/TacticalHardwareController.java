package tactical;

import oscP5.OscMessage;
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
	
	public static final int KP_SCAN = 35;		//move the scan key to the # key for now
	public static final int KP_DECOY = 11;
	public static final int KP_LASER = 12;
	public static final int KP_SCREENCHANGE = 13;
	public static final int KP_HASH = 35;
	public static final int KP_STAR = 42;
	public static final int KP_A = 65;
	public static final int KP_B = 66;
	public static final int KP_C = 67;
	public static final int KP_D = 68;
	
	
	/* for screen switching using hardware, at some point this will be moved out of here */
	String[] screenNames = {"weapons", "plottingDisplay"};
	int screenIndex = 1;
	
	String[] weaponNames = {"CANNON", "CANNON", "CANNON", "EMP"};
	
	
	boolean decoyLightState = false;
	boolean previousWeaponLightState = false;
	boolean weaponLightState = false;

	// is the decoy blinker on?
	boolean decoyBlinker = false;
	private boolean weaponPanelState;
	private boolean preshutdownWeaponPanelState;

	//there should only ever be one of these per console
	public static TacticalHardwareController instance;
	
	
	public TacticalHardwareController(String interfaceName, String port,
			int rate, PlayerConsole parent) {
		super(interfaceName, port, rate, parent);
		ConsoleLogger.log(this, "Starting Tactical panel controller on " + port);
		instance = this;
		
		setBankName(0, weaponNames[0]);
		setBankName(1, weaponNames[1]);
		setBankName(2, weaponNames[2]);
		setBankName(3, weaponNames[3]);
		setPowerLevel(3, 80);	//emp is always charged for now, we limit the number they can fire instead
		
		
		
	}

	private void setBankName(int bank, String name) {
		ConsoleLogger.log(this, "Setting bank name " + bank + " to " + name);

		if(parent.testMode){
			return;
		}
		
		name = "n" + bank + name + ",";
		serialPort.write(name);		
		
	}

	public void bufferComplete(){
		char c = serialBuffer[0];
		if(c == 'K'){		//keypad key	
			char val = serialBuffer[1];
			keypadKey(val);
				
			
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
			int bank = serialBuffer[1] - '0';
			if(bank >= 0 && bank < 3){		//banks 0,1,2 are lasers
				h.id = KP_LASER;
			} else if (bank == 3){
				h.id = KP_DECOY;
			}
			h.event = "KEYPAD";
			h.value = bank;
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
		/* weapons toggling commands */
		if ( c == 'w'){
			sendWeaponChange(false);
			
		} else if ( c == 'W'){
			sendWeaponChange(true);
		} else if (c == 'S'){
			//TODO: control for this will be switched with a toggle soon, its value determins which
			//display to use
			cycleScreen();
		}
		
		//trackball buttons
		if(c == 'B'){
			if(serialBuffer[1] == '0'){
				HardwareEvent h = new HardwareEvent();
				h.event = "MOUSECLICK";
				h.id = 0;
				h.value = 1;
				parent.hardwareEvent(h);
			}
		}
	}

	protected void sendWeaponChange(boolean b) {
		//toggle weapons on
		if(parent.getShipState().poweredOn == true){
			OscMessage m = new OscMessage("/system/targetting/changeWeaponState");
			m.add(b == true ? 1 : 0);
			parent.getOscClient().send(m, parent.getServerAddress());
		}
	}

	public void cycleScreen() {
		if(parent.getShipState().poweredOn == true){

			screenIndex ++;
			screenIndex %= screenNames.length;
			
			OscMessage m = new OscMessage("/control/screenSelection");
			m.add("TacticalStation");
			m.add(screenNames[screenIndex]);
			parent.getOscClient().send(m, parent.getServerAddress());
		}
	}
	
	//decode the buffer as a keypad key
	//these are 0-9 so just parse as Ints for the id part
	//other keys just pass out the character value, can be compared with the constants define
	//at the top of this file
	void keypadKey(char val){
		HardwareEvent h = new HardwareEvent();
		h.event = "KEYPAD";
		if(val >= '0' && val <= '9'){
			h.id = Integer.parseInt("" + val);
		} else if (val == '#'){
			h.id = KP_HASH;
		} else {
			h.id = (int)val;
		}
		h.value = 1;
		
		parent.getConsoleAudio().randomBeep();
		parent.hardwareEvent(h);
	}
	
	void setWeaponsArmedLight(boolean state){
		ConsoleLogger.log(this, "setting weapons armed light to " + state);
		if(parent.testMode) return;
		
		if(state){
			weaponLightState = true;
			serialPort.write("A,");
			
		} else {
			weaponLightState = false;
			serialPort.write("a,");
		}
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
	
	public void setWeaponPanelState(boolean powerOn){
		ConsoleLogger.log(this, "setting weapon panel state to " + powerOn);
		if(parent.testMode) return;
		
		if(powerOn){
			setBankName(0, weaponNames[0]);
			setBankName(1, weaponNames[1]);
			setBankName(2, weaponNames[2]);
			setBankName(3, weaponNames[3]);
			serialPort.write("W,");
			weaponPanelState = true;
		} else {
			serialPort.write("w,");
			weaponPanelState = false;
		}
	}

	public void setPowerState(boolean b) {
		if(parent.testMode) return;
		if(b){
			/* power up the tac console panel */			
			serialPort.write("P,");	
			setWeaponPanelState(preshutdownWeaponPanelState);
			setWeaponsArmedLight(previousWeaponLightState);
		} else {
			serialPort.write("p,");
			
			decoyBlinker = false;
			previousWeaponLightState = weaponLightState;
			preshutdownWeaponPanelState = weaponPanelState;
			setWeaponPanelState(false);
			setWeaponsArmedLight(false);
		}
		
	}

	public void setChargeRate(int beamPower) {
		
		if(parent.testMode) return;
		//charge rates are set from char '0' upwards, setting emp charge rate has no effect as the charge level is
		//permanently held at full, it should always flash "READY FIRE"
		for(int i = 0; i < 4; i++){
			int c = '0' + beamPower;
			serialPort.write("L" + i + c + ",");
			
		}
		
		

		
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

	public void reset() {
		setWeaponsArmedLight(false);
		setPowerState(false);
		
	}

	public void setPowerLevel(int bank, int i) {
		ConsoleLogger.log(this, "setting charge level of bank " + bank + "to " + i);
		if(parent.testMode) return;
		
		String msg = "x" + bank + ",";
		serialPort.write(msg);
		
	}
}
