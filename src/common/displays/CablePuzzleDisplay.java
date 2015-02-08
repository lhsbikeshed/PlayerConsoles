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
	
	/*
	 * sockets - > plugs [8, 6, 3] , [5, 12, 10] [2, 3, 11] , [10, 12, 4] [6,
	 * 11, 8] , [12, 4, 10] [3, 6, 8] , [10, 9, 5] [13, 3, 8] , [5, 6, 10]
	 */
	protected int selectedPatch = 0;
	

	public CablePuzzleDisplay(PlayerConsole parent) {
		super(parent);
		bgImage = parent.loadImage("data/engineerconsole/screens/cablepuzzle/bg.png");
		brokenIconImage = parent.loadImage("data/engineerconsole/screens/cablepuzzle/broken.png");
	

	}

	@Override
	public void draw() {
		parent.background(0);
		parent.image(bgImage,0,0);
		//draw breaks
		
		
		selectedPatch = 2;
		PVector[] pv = breakLocations[selectedPatch];
		for(PVector p : pv){
			parent.tint(255,0,0);
			parent.image(brokenIconImage, p.x, p.y);
			parent.noFill();
			parent.noTint();
			if(parent.globalBlinker){
				parent.stroke(255,255,0);
			} else {
				parent.stroke(255,0,0);
			}
			parent.strokeWeight(2);
			parent.ellipse(p.x+13, p.y+5, 50, 50);
			parent.line(488, 178, p.x+13, p.y+5);
			
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

		}
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {

	}
}
