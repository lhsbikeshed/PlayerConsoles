package common;

import common.util.LerpedFloat;
import common.util.Rot;
import oscP5.OscMessage;
import processing.core.PVector;

public class ShipState {
	
	public static final int POWER_PROPULSION = 0;
	public static final int POWER_DAMAGE = 1;
	public static final int POWER_SENSORS = 2;
	public static final int POWER_WEAPONS = 3;
	
	public static final int FUEL_FUCKED = 2;
	public static final int FUEL_CONNECTED = 1;
	public static final int FUEL_DISCONNECTED = 0;	
	public int fuelLineConnectionState = FUEL_DISCONNECTED;
	
	
	
	//power handling
	//is the ship on?
	public boolean poweredOn = true;
	//is the ship booting?
	public boolean poweringOn = false;
	//are we exploded?
	public boolean areWeDead = false;
	//text to display on the death screen
	public String deathText = "";
	//can the ship jump at the moment? i.e. are all the reqs met
	public boolean jumpState = false;

	//ship position in unity world space. 
	public PVector shipPos = new PVector(0, 0, 0);
	//ships current rotation and last frame rotation (for interpolation)
	public Rot shipRot = Rot.IDENTITY;
	public Rot lastShipRot = Rot.IDENTITY;
	//ships world velocity
	public PVector shipVel = new PVector(0, 0, 0);
	//ships world velocity magnitude
	public float shipVelocity = 0;
	public float lastShipVel = 0;

	public long lastTransformUpdate = 0;
	
	//how many emp blasts left
	public int smartBombsLeft;
	//hull state
	public float hullState = 100;
	//is the fuel tank leaking
	public boolean fuelLeaking;
	//is the engineer silliness in progress
	public boolean sillinessInProgress;
	//how silly is the engineer?
	public int sillinessLevel;
	
	//is the afterburner ready for use?
	public boolean afterburnerCharging = true;
	
	//power levels of ship systems
	public int[] powerStates = new int[4];
	
	//weapon deployment states
	public static final int WEAPON_STOWED = 0;
	public static final int WEAPON_DEPLOYED = 1;
	public static final int WEAPON_TRANSIT_OUT = 2;
	public static final int WEAPON_TRANSIT_IN = 3;
	public int weaponState = 0;
	public float[] weaponHealth = {1f, 1f,1f,1f, 1f};
	
	// map handling
	public String currentScene = "";
	public String currentSceneId = "8593";
	public boolean returnJourney = false;	//are we on the return leg of the route home? if so 
											//we can ignore certain events
	
	//is the thrust reverser currently deployed?
	public boolean thrustReverser = false;
	
	//ships altitude above something
	public LerpedFloat altitude = new LerpedFloat(2000f, 0, 250);
	private PlayerConsole parent;
	
	public static ShipState instance;
	
	public ShipState(PlayerConsole parent) {
		this.parent = parent;
		this.instance = this;
		powerStates[0] = 6;
		powerStates[1] = 6;
		powerStates[2] = 6;
		powerStates[3] = 6;
	};

	public void resetState() {
		powerStates[0] = 6;
		powerStates[1] = 6;
		powerStates[2] = 6;
		powerStates[3] = 6;
		returnJourney = false;
		
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
			currentScene = msg.get(0).stringValue();
			currentSceneId = msg.get(1).stringValue();
			ConsoleLogger.log(this, "new scene node: " + currentSceneId);
		} else if(msg.checkAddrPattern("/system/propul.sion/setThrustReverser")){
			thrustReverser  = msg.get(0).intValue() == 1 ? true : false;
		} else if (msg.checkAddrPattern("/ship/state/altitude")){
			altitude.update(msg.get(0).floatValue(), parent.millis());
		} else if (msg.checkAddrPattern("/ship/state/currentLocationId")){
			ConsoleLogger.log(this, "new scene node: " + currentSceneId);
			currentSceneId = msg.get(0).stringValue();
		} else if (msg.checkAddrPattern("/ship/state/setReturnJourney")){
			returnJourney = msg.get(0).intValue() == 1;
			if(returnJourney){
				ConsoleLogger.log(this, "Congrats chaps! Youre on the return leg");
			}
		
		} else if (msg.checkAddrPattern("/ship/transform")) {
			//ships position in unity coords relative to centre of game space
			
			shipPos.x = msg.get(0).floatValue();
			shipPos.y = msg.get(1).floatValue();
			shipPos.z = msg.get(2).floatValue();
			
			//rotation as a quaternion
			float w = msg.get(3).floatValue();
			float x = msg.get(4).floatValue();
			float y = msg.get(5).floatValue();
			float z = msg.get(6).floatValue();
			lastShipRot = shipRot;
			shipRot = new Rot(w, x, y, z, false);
			
			//ships velocity
			shipVel.x = msg.get(7).floatValue();
			shipVel.y = msg.get(8).floatValue();
			shipVel.z = msg.get(9).floatValue();

			lastShipVel = shipVelocity;
			
			lastTransformUpdate = parent.millis();
		} else if (msg.checkAddrPattern("/ship/state/setFuelConnectionState")){
			fuelLineConnectionState = msg.get(0).intValue();
		}
	}
}
