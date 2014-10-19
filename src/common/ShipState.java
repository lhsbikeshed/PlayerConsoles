package common;

import oscP5.OscMessage;
import processing.core.PVector;

public class ShipState {
	
	public static final int POWER_PROPULSION = 0;
	public static final int POWER_DAMAGE = 1;
	public static final int POWER_SENSORS = 2;
	public static final int POWER_WEAPONS = 3;
	
	public static final int SCENE_LAUNCH = 0;
	public static final int SCENE_HYPERSPACE = 1;
	public static final int SCENE_MARSDROP = 2;
	public static final int SCENE_WARZONE = 3;
	public static final int SCENE_LANDING = 4;
	public static final int SCENE_DEAD = 5;
	public static final int SCENE_COMET = 6;


	public boolean poweredOn = true;
	public boolean poweringOn = false;
	public boolean areWeDead = false;
	public String deathText = "";
	public boolean jumpState = false;

	public PVector shipPos = new PVector(0, 0, 0);
	//public PVector shipRot = new PVector(0, 0, 0);
	//public PVector lastShipRot = new PVector(0, 0, 0);
	public Rot shipRot = Rot.IDENTITY;
	public Rot lastShipRot = Rot.IDENTITY;

	public PVector shipVel = new PVector(0, 0, 0);

	public float shipVelocity = 0;
	public float lastShipVel = 0;

	public long lastTransformUpdate = 0;
	public int smartBombsLeft;
	public float hullState;
	public boolean fuelLeaking;
	public boolean sillinessInProgress;
	public int sillinessLevel;
	
	public boolean afterburnerCharging = true;
	public int[] powerStates = new int[4];

	
	public static final int WEAPON_STOWED = 0;
	public static final int WEAPON_DEPLOYED = 1;
	public static final int WEAPON_TRANSIT_OUT = 2;
	public static final int WEAPON_TRANSIT_IN = 3;
	public int weaponState = 0;
	public float[] weaponHealth = {1f, 1f,1f,1f, 1f};
	
	public int currentScene = 0;
	public boolean thrustReverser = false;
	
	public ShipState() {
	};

	public void resetState() {
		powerStates[0] = 6;
		powerStates[1] = 6;
		powerStates[2] = 6;
		powerStates[3] = 6;
		
	}
	
	public void processOSCMessage(OscMessage msg){
		if (msg.checkAddrPattern("/system/ship/powerLevels")){
			powerStates[POWER_PROPULSION] = msg.get(0).intValue(); //engines
			powerStates[POWER_DAMAGE] = msg.get(1).intValue(); //damage
			powerStates[POWER_SENSORS] = msg.get(2).intValue();	//sensors
			powerStates[POWER_WEAPONS] = msg.get(3).intValue(); //weapons
			
		
		} else if (msg.checkAddrPattern("/ship/weaponState")){
			weaponState = msg.get(0).intValue();
		} else if (msg.checkAddrPattern("/ship/weaponHealth")){
			for(int i = 0; i < 5; i++){
				weaponHealth[i] = msg.get(i).floatValue();
			}
			
		} else if(msg.checkAddrPattern("/scene/change")){
			currentScene = msg.get(0).intValue();
		} else if(msg.checkAddrPattern("/system/propulsion/setThrustReverser")){
			thrustReverser  = msg.get(0).intValue() == 1 ? true : false;
		}
	}
}
