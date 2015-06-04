package common.displays;

import oscP5.OscMessage;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

/* display for the large cable puzzle 
 * just flashes an error code from the game
 */
public class CablePuzzleDisplay extends Display {


	int time;

	PImage bgImage;
	PImage brokenIconImage;
	
	PVector[][] breakLocations = { 	{new PVector(179, 431), new PVector(579,463), new PVector(614, 600) },  
									{new PVector(752, 487), new PVector(568,560), new PVector(466, 607) }, 
									{new PVector(447, 404), new PVector(299,589), new PVector(790, 657) }, 
									{new PVector(476, 534), new PVector(630,706), new PVector(791, 595) },
									{new PVector(299, 588), new PVector(453,497), new PVector(767, 514) }
	};
	
	
	boolean[] connectionState = {false, false, false};
	
	PlugSocketPair[][] pairs = { 	{new PlugSocketPair(12,6), new PlugSocketPair(10,3), new PlugSocketPair(5,14) }, 
									{new PlugSocketPair(10,2), new PlugSocketPair(12,3), new PlugSocketPair(4,11)}, 
									{new PlugSocketPair(12,6), new PlugSocketPair(4,11), new PlugSocketPair(10,14)}, 
									{new PlugSocketPair(10,3), new PlugSocketPair(9,6), new PlugSocketPair(5,14)}, 
									{new PlugSocketPair(5,13), new PlugSocketPair(6,3), new PlugSocketPair(10,13)}
								};
	
	//these are just for test purposes
	String[] labels = { 	"", 
							"",
							"BDB 15",
							"DC P45",
							"DC M32",
							"BDB 14",
							"PDER",
							"XC-01",
							"",
							"XM-23",
							"EPS A",
							"AUX B",
							"AUX A", 
							"EPS B",
							"DF/F"
					
	};
	

	/*
	 * sockets - > plugs 
	 * [14, 6, 3] , [5, 12, 10] 
	 * [2, 3, 11] , [10, 12, 4] 
	 * [6, 11, 14] , [12, 4, 10] 
	 * [3, 6, 14] , [10, 9, 5] 
	 * [13, 3, 14] , [5, 6, 10]
	 */
	protected int selectedPatch = 0;
	

	public CablePuzzleDisplay(PlayerConsole parent) {
		super(parent);
		bgImage = parent.loadImage("data/engineerconsole/screens/cablepuzzle/bg.png");
		brokenIconImage = parent.loadImage("data/engineerconsole/screens/cablepuzzle/broken.png");
	

	}
	
	/* find a plug/socket pair and set its connect/disconnect state */
	private void setConnectionState(int plugId, int socketId, int i) {
		ConsoleLogger.log(this, "plug " + plugId + " - " + socketId);

		for(int ind = 0; ind < 3; ind++){
			if(pairs[selectedPatch][ind].socketId == socketId &&  pairs[selectedPatch][ind].plugId == plugId){
				//found it
				ConsoleLogger.log(this, "found! " + ind);

				if(i == 1){
					connectionState[ind] = true;
				} else {
					connectionState[ind] = false;
				}
				return;
			}
		}
		 
	}
	
	 

	@Override
	public void draw() {
		parent.background(0);
		parent.image(bgImage,0,0);
		//draw breaks
		
		
		
//		selectedPatch = 0;
		PVector[] pv = breakLocations[selectedPatch];
		for(int i = 0; i < 3; i++){
			PVector p = pv[i];
			
			if(connectionState[i]){
				parent.tint(0,255,0);
			} else {
				parent.tint(255,0,0);
			}
			
			
			
//			parent.text(labels[pairs[selectedPatch][i].plugId] + " -> " + labels[pairs[selectedPatch][i].socketId], p.x+10,p.y+10);
//			parent.text(pairs[selectedPatch][i].plugId + " -> " + pairs[selectedPatch][i].socketId, p.x+10,p.y+23);
//			parent.text("" + i, p.x+10,p.y+33);
//			
			parent.noFill();
			parent.noTint();
			if(connectionState[i]){
				if(parent.globalBlinker){
					parent.fill(255,255,0);
				} else {
					parent.fill(255,0,0);
				}
				parent.textFont(parent.getGlobalFont(), 15);
				parent.text("OK", p.x, p.y);
			} else {
				if(parent.globalBlinker){
					parent.stroke(255,255,0);
				} else {
					parent.stroke(255,0,0);
				}
				parent.strokeWeight(2);
				parent.ellipse(p.x+13, p.y+5, 50, 50);
				parent.line(488, 178, p.x+13, p.y+5);
				parent.image(brokenIconImage, p.x, p.y);

			}
		}
		
	}
	

	public void keyPressed() {
	}

	public void keyReleased() {
	}

	@Override
	public void oscMessage(OscMessage msg) {
		//ConsoleLogger.log(this, msg.addrPattern());
		if (msg.checkAddrPattern("/system/cablePuzzle/puzzleComplete")) {

		} else if (msg.checkAddrPattern("/system/cablePuzzle/connectionList")) {
			
			selectedPatch = msg.get(0).intValue();

		} else if (	msg.checkAddrPattern("/system/cablePuzzle/connect")){
			int plugId = msg.get(0).intValue();
			int socketId = msg.get(1).intValue();
			setConnectionState(plugId, socketId, 1);

		} else if (	msg.checkAddrPattern("/system/cablePuzzle/disconnect")){
			int plugId = msg.get(0).intValue();
			int socketId = msg.get(1).intValue();
			setConnectionState(plugId, socketId, 0);
		} else if (msg.checkAddrPattern("/system/cablePuzzle/currentState")){
			String[] vals = msg.get(0).stringValue().split(",");
			
			selectedPatch = Integer.parseInt(vals[0]);
			
			for(int i = 1; i < vals.length; i++){
				connectionState[i-1] = vals[i].equals("1") ? true : false;
			}
			
			ConsoleLogger.log(this, "received status update");
		}

	}

	
	
	@Override
	public void serialEvent(HardwareEvent evt) {
	}

	@Override
	public void start() {
		
		OscMessage msg = new OscMessage("/system/cablePuzzle/getCurrentState");
		parent.getOscClient().send(msg, parent.getServerAddress());
		
		

	}

	@Override
	public void stop() {

	}
	
	private class PlugSocketPair implements Comparable {

		public int plugId = -1;
		public int socketId = -1;
		
		public PlugSocketPair(int plugId, int socketId){
			this.plugId = plugId;
			this.socketId = socketId;
			
		}
		
		@Override
		public int compareTo(Object arg0) {
			PlugSocketPair p = (PlugSocketPair)arg0;
			if(p.plugId == plugId && p.socketId == socketId){
				return 0;
			}
			return -1;
		}
		
		
	}
	
}
