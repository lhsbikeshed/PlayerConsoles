package tactical;

import common.ConsoleLogger;
import common.HardwareController;
import common.PlayerConsole;

public class FanLightHardwareController extends HardwareController {

	public FanLightHardwareController(String interfaceName, String port,
			int rate, PlayerConsole parent) {
		super(interfaceName, port, rate, parent);

		ConsoleLogger.log(this, "Starting fan controller on " + port);
	}

	public void setPowerState(boolean b) {
		if(parent.testMode){
			ConsoleLogger.log(this, "changing fan power state to " + b);
			
		} else {
			if(b){
				serialPort.write("R1,");
			} else {
				serialPort.write("R0,");
			}
				
			
		}
		
	}

	public void setPowerLevels(int propPower, int beamPower, int sensorPower,
			int internalPower) {
		if(parent.testMode){
			ConsoleLogger.log(this, "changing power levels");
		} else {
		
			serialPort.write("P" + (propPower / 4));
			serialPort.write("W" + (beamPower / 4));
			serialPort.write("S" + (sensorPower / 4));
			serialPort.write("I" + (internalPower / 4));
		}		
	}

	public void shipDamage() {
		if(parent.testMode){
			ConsoleLogger.log(this, "ship damage effect..");
		} else {
			serialPort.write("D1,");
			
		}		
	}

}
