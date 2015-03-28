package engineer;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

/*
 * present player with a cross section of the hyperspace warp bubble around the ship
 * it will oscillate and randomly start to change shape
 * their task is to push buttons to keep it circular and prevent it collapsing
 * 
 * for 
 */
public class NewHyperSpaceDisplay extends Display {

	float centreX = 499;
	float centreY = 392;

	// osc
	OscP5 p5;
	String serverIP = "";

	NetAddress myRemoteLocation;
	// state things
	boolean haveFailed = false; // have we failed/
	long failStart = 0; // when fail started

	long failDelay = 0;
	float timeRemaining = 0; // how long until exit


	// assets
	PImage bgImage;

	PImage warningBanner;
	
	//graph things
	float targetLerpRot, targetLerpFreq;
	float currentLerpRot, currentLerpFreq;
	//target details, what the player is aiming for
	float targetRot = 2.0f;
	int targetFreq = 5;
	//player rotation and frequency
	float playerRot = 2.0f;
	int playerFreq = 5; //multiples of 0.5f
	
	float rotationDirection = 0.0f;
	
	float tunnelStability = 2.5f;
	float goodTimer = 0.0f;
	
	long lastUpdateSendTime = 0;
	
	//points to draw
	PVector[] targetPoints, playerPoints;

	
	
	public NewHyperSpaceDisplay(PlayerConsole parent) {
		super(parent);
		this.p5 = parent.getOscClient();
		serverIP = parent.getServerIP();
		myRemoteLocation = new NetAddress(serverIP, 12000);

		// load assets
		bgImage = parent.loadImage("engineerconsole/hyperspace 3.png");
		warningBanner = parent.loadImage("engineerconsole/warpWarning.png");
		
		//configure graph parts
		 targetLerpRot = targetRot;
		 targetLerpFreq = targetFreq * 0.5f;
		  int pts = (int)((PApplet.PI * 2) / 0.005f);
		  targetPoints = new PVector[pts];
		  playerPoints = new PVector[pts];
	}

	@Override
	public void draw() {
		parent.textFont(font, 12);

		
		//update graphs
		targetLerpRot = PApplet.lerp(targetLerpRot, targetRot, 0.1f);
		targetLerpFreq = PApplet.lerp(targetLerpFreq, targetFreq * 0.5f, 0.1f);
		
		//rotate the target on a nice wobbly sin wave
		targetRot = (float) (Math.PI + Math.sin(parent.frameCount / 80f));
		targetRot += (float) (Math.PI + Math.cos(parent.frameCount / 40f));
		targetRot *= 0.4f;
		
		
		
		currentLerpRot = PApplet.lerp(currentLerpRot, playerRot, 0.2f);
		currentLerpFreq = PApplet.lerp(currentLerpFreq, playerFreq * 0.5f, 0.1f);

		updatePoints(targetPoints, targetLerpRot, targetLerpFreq);
		updatePoints(playerPoints, currentLerpRot, currentLerpFreq);
		
		//draw things
		parent.image(bgImage, 0, 0, parent.width, parent.height);

		parent.stroke(255);
		
		//draw peak lines for player
		int peaks = countPeaks(playerFreq* 0.5f);	  
		float angleOffset = getPeakAngle(peaks);
		for(int i =0; i < peaks; i++){
		    PVector p = new PVector(0,1);
		    p.rotate(angleOffset * i - playerRot);
		    p.mult(350);
		  //  parent.line(centreX, centreY, centreX + p.x, centreY + p.y);
		}
		if(tunnelStability < 0.3f && parent.globalBlinker){
			parent.fill(255,0,0,100);
			parent.noStroke();
			parent.ellipse(centreX, centreY, 600, 600);

		}
		
		//draw the graphs
		parent.noStroke();
		parent.fill(180);
		drawShape(targetPoints);
		
		//work out the colour of the graph, red = bad
		// yellow = right number of peaks
		// green  = matches
		float angDiff = angularDifference();
		if(countPeaks(targetFreq * 0.5f) == countPeaks(playerFreq * 0.5f)){
			if(angDiff <= 0.1f){
				parent.fill(0, 255, 0);	
				tunnelStability += 0.005f;
				goodTimer += 0.001f;
			} else {
				parent.fill(255, 255, 0);
				tunnelStability -= 0.002f;;
				goodTimer -= 0.001f;
			}
		}
		else {
			parent.fill(255, 0, 0);
			tunnelStability -= 0.002f;
			goodTimer -= 0.001f;
		}
		if(tunnelStability > 5.0f){
			tunnelStability = 5.0f;
		} else if (tunnelStability < 0.0f){
			tunnelStability = 0.0f;			
		}
		
		/* switch "gears" */
		int tf = (int)PApplet.map(tunnelStability, 5.0f, 0.0f, 1, 8);
		if(tf < targetFreq){
			tunnelStability += 0.5f;
			//ConsoleLogger.log(this, "switch");
		} else if (tf < targetFreq){
			tunnelStability -= 0.5f;
		}
		targetFreq = (int)tf;
		
		
		drawShape(playerPoints);
		
		
		drawStability();
		drawAngleGraph();
		drawFrequencyGraph();
		
		
		/* every 250ms send a tunnel size update to the game, if the tunnel stability < 0
		 * then the game will destroy / damage the ship
		 */
		if(parent.millis() - lastUpdateSendTime > 250){
			lastUpdateSendTime = parent.millis();
			//send an osc update with the current bubble stablility in it
			OscMessage m = new OscMessage("/scene/warp/tunnelStability");
			m.add(tunnelStability);
			parent.getOscClient().send(m, parent.getServerAddress());
		}
		
	}
	
	void drawStability(){
		float h = parent.map(tunnelStability, 0.0f, 5.0f, 0.0f, 400f);
		parent.rect(10,500, 20, -h);
		parent.noFill();
		parent.stroke(255);
		parent.rect(10,500,20,-400);
		
		
		int stabVal = (int)(tunnelStability * 20);
		parent.text("Bubble Stability\r\n" + stabVal + "%", 40,500);
		
	}
	
	void drawAngleGraph(){
		
	}
	
	void drawFrequencyGraph(){
		
	}
	
	//get the angular difference between the target and player graphs
	float angularDifference(){

		float targetAngles = getPeakAngle(countPeaks(targetFreq * 0.5f));
		float targetAng = targetRot % targetAngles;
		float angles = getPeakAngle(countPeaks(playerFreq * 0.5f));
		
		float currentAngles = playerRot % angles;
		if(playerRot < 0.0f){
			float p = (float) (Math.PI * 2  +playerRot);
			currentAngles = p % angles;
		}
		parent.text("target phase: " + targetAng, 100,680);
		parent.text("phase diff: " + currentAngles, 100,700);

				
		float angDiff = Math.abs(targetAng - currentAngles);
		return angDiff;
	}
	
	// count the number of peaks for a given frequency graph
	int countPeaks(float freq){
		int v = (int)(freq / 0.5f);
		int peaks = (int)(0.5 * (3 + PApplet.pow(-1, v)) * v); //magic formula from wolfram alpha!
		return peaks;
		
	}
	//calculate radians between peaks
	float getPeakAngle(float peaks){
		
		float r = (2*PConstants.PI) / peaks;
		return r;
	}
	
	//draw a set of points
	void drawShape(PVector[] pts) {
		  for (int i = 0; i < pts.length; i++) {


		    PVector p = pts[i];
		    
		    parent.rect(centreX + p.x, centreY + p.y, 2, 2);
		    
		  }
		}

	
	//wobble the points etc
	void updatePoints(PVector[] pts, float rot, float freq) {
		  float ang = 0.0f;
		  for (int i = 0; i < pts.length; i++) {
		    float rad = (float) (3f * Math.cos(2 *(ang+ rot) * freq));
		
		    PVector p = new PVector(0, 1);
		    p.rotate(ang);
		    p.mult(rad * 100);
		
		    p.mult((float) (1 + Math.sin(parent.frameCount * 0.2f + ang * (10f + 55 * (5.0f-tunnelStability))) * 0.05f));
		    pts[i] = p;
		    ang += 0.005f;
		  }
	}
	

	public void keyPressed() {
		
	}

	public void keyReleased() {
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {

		if (theOscMessage.checkAddrPattern("/scene/warp/updatestats") == true) {
			timeRemaining = (int) theOscMessage.get(1).floatValue();
			
		} else if (theOscMessage.checkAddrPattern("/scene/warp/failjump") == true) {
			haveFailed = true;
			failStart = parent.millis();
			failDelay = theOscMessage.get(0).intValue() * 1000;
		}
	}

	private void sendFailMessage() {
		OscMessage myMessage = new OscMessage("/scene/warp/warpfail");
		OscP5.flush(myMessage, myRemoteLocation);

		
	}

		
	@Override
	public void serialEvent(HardwareEvent evt) {
		if(evt.event.equals("KEY")){
			int c = evt.id;
			if (c == 38) {
			    playerRot -= 0.03f;
			  } 
			  else if (c == 40) {
			    playerRot += 0.03f;
			  } 
			  /*if (currentRot < 0) {
			    currentRot = (float) ((2 * Math.PI) - currentRot);
			  } 
			  else if (currentRot > 2* Math.PI) {
			    currentRot = (float) (currentRot - (2 * Math.PI));
			  }*/

			  //shape
			  if (c == 39) {
			    playerFreq += 1;
			  } 
			  else if (c == 37) {
			    playerFreq -=1;
			  }
		} 
		
	}

	@Override
	public void start() {
		haveFailed = false;
		tunnelStability = 2.5f;

	}

	@Override
	public void stop() {
	}
}
