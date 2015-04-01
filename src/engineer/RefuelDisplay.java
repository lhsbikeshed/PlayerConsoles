package engineer;

import java.awt.event.KeyEvent;

import jogamp.graph.font.typecast.ot.table.HeadTable;
import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.opengl.PShader;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

public class RefuelDisplay extends Display {
	
	PVector headPos = new PVector(0,0,0);
	PVector headVel	= new PVector(0,0,0);
	PVector headVelTarget = new PVector(0,0,0);
	
	PVector centrePos = new PVector(-304, -447, 400);
	
	PImage overlayImage, targetImage, crosshairImage;
	
	PGraphics targetGraphics;
	PShader damageDistortion;
	PFont kurzFont;
	
	PuzzleState puzzleState = PuzzleState.STATE_PLAYING;
	private int crashTimer;
	private int pullAwayTimer = 0;
	
	public enum PuzzleState {
		STATE_STARTUP, STATE_PLAYING, STATE_CONNECTED, STATE_FUCKED;
	}

	public RefuelDisplay(PlayerConsole parent) {
		super(parent);
		overlayImage = parent.loadImage("engineerconsole/screens/docking/fuelOverlay.png");
		targetImage = parent.loadImage("engineerconsole/screens/docking/target.png");
		crosshairImage = parent.loadImage("engineerconsole/screens/docking/crosshair.png");


		targetGraphics = parent.createGraphics(860,530, PApplet.P3D);
		damageDistortion = targetGraphics.loadShader("common/damageEffects/distort.glsl");
		damageDistortion.set("damage", new Float(30));
		
		kurzFont = parent.loadFont("engineerconsole/kurz.vlw"); 
		headPos = getNewStartPos(0.5f);
	}

	@Override
	public void draw() {
		if(crashTimer > 0){
			crashTimer --;
			damageDistortion.set("damage", 70+parent.random(20));
			if(crashTimer <= 0){
				damageDistortion.set("boom", false);
			}
		}
		
		parent.background(0);
		switch(puzzleState){
		case STATE_STARTUP:
			drawConnection();
			break;
		case STATE_PLAYING:
			doDocking();
			break;
		case STATE_CONNECTED:
			doConnected();
			break;
		case STATE_FUCKED:
			doForceDisconnected();
			break;
		}
		parent.image(overlayImage, 10,15);

	}
	
	/* draw a "LAODING" screen 
	 * 
	 */
	void drawConnection(){
		
	}
	
	/* initiate a connection to the fuel socket
	 * 
	 */
	void connect(){
		ConsoleLogger.log(this, "connection");
		puzzleState = PuzzleState.STATE_CONNECTED;
		headVelTarget = new PVector(0,0,0);
		headVel = new PVector(0,0,0);
	
	}
	
	/* safe disconnect */
	void disconnect(){
		ConsoleLogger.log(this, "Safe disconnection started");
		puzzleState = PuzzleState.STATE_PLAYING;
	}
	
	/* forced disconnect, as in "the player flew off with the line connected */
	void forceDisconnect(){
		puzzleState = PuzzleState.STATE_FUCKED;
	}
	
	/* head crashed into the wall too fast
	 * 
	 */
	void crash(){
		ConsoleLogger.log(this, "BANG");
		crashTimer = 10;
		damageDistortion.set("boom", true);
	}
	
	void doConnected(){
		damageDistortion.set("damage", 40+parent.random(50));
		drawTarget();
		
		//look to see if the player is pulling away with - key
		if(headVelTarget.z < 0){
			pullAwayTimer ++;
			
		} else {
			pullAwayTimer = 0;
		}
		if(pullAwayTimer > 20){
			disconnect();
		}
	}
	
	/* was force disconnected, show static and warning text */
	void doForceDisconnected(){
		damageDistortion.set("damage", parent.random(100));		
		drawTarget();
		
	}
	
	void doDocking(){
		damageDistortion.set("damage", 20+parent.random(40));
		headVel = PVector.lerp(headVel, headVelTarget, 0.02f);
		if(headPos.z + headVel.z > 400){	//test for collision
			
			if(headVel.z >= 0.3f){
				crash();
			} else {
				//test for a connection
				if(PVector.dist(headPos, centrePos) < 8f){
					connect();
				} else {
					crash();
				}
				
				
			}
			headVel.z = -headVel.z;
		} else if (headPos.z + headVel.z < 0){
			headVel.z = -headVel.z;
			ConsoleLogger.log(this, "DISCONNECT");
		}
	
		headPos.add(headVel);
		headPos.x = PApplet.constrain(headPos.x, -600, 0);
		headPos.y = PApplet.constrain(headPos.y, -946, 0);
		drawTarget();
		
		
		
	}
	
	void drawTarget(){
		targetGraphics.beginDraw();
		targetGraphics.background(0);
		
		targetGraphics.pushMatrix();
		targetGraphics.translate(headPos.x, headPos.y, headPos.z);
		if(puzzleState == PuzzleState.STATE_FUCKED){
			targetGraphics.fill(0);
			targetGraphics.rect(0,0,targetGraphics.width, targetGraphics.height);
		} else {
			targetGraphics.image(targetImage,0, 0);
		}
		targetGraphics.popMatrix();
		
		
		
		targetGraphics.hint(PApplet.DISABLE_DEPTH_TEST);

		targetGraphics.image(crosshairImage,-124, -90);
		
		
		
		
		//----TEXT SECTION----
		targetGraphics.fill(255);
		targetGraphics.textFont(kurzFont, 58);
		targetGraphics.text("ПРИЧАЛ ПРИЧАЛ",60,50);
		
		targetGraphics.text("Шx " + (int)(headVel.x * 10f), 680,50);
		targetGraphics.text("Шy " + (int)(headVel.y * 10f), 680,80);
		String zText = "Шz " + (int)(headVel.z * 10f);
		if(headVel.z >= 0.3f && parent.globalBlinker){
			zText = ">>" + zText + "<<";
			
		}
	
		targetGraphics.text(zText, 680,110);
		
		targetGraphics.text("Цx " + (headPos.x - centrePos.x), 680,160);
		targetGraphics.text("Цy " + (headPos.y - centrePos.y), 680,190);
		targetGraphics.text("Цz " + (headPos.z - centrePos.z), 680,220);
		
		targetGraphics.textFont(kurzFont, 90);
		String connectText = ">>ОТСОЕДИНЕННОМ<<";
		if(puzzleState == PuzzleState.STATE_CONNECTED){
			if(parent.globalBlinker){
				connectText = "<< ПОДКЛЮЧЕНИЕ >>";
			} else {
				connectText = "";
			}
		} else if (puzzleState == PuzzleState.STATE_FUCKED){
			connectText = "!! НЕТ СОЕДИНЕНИЯ !!";
			
		}
		targetGraphics.text(connectText, 156,494); //connected = 
		targetGraphics.endDraw();

		
		targetGraphics.filter(damageDistortion);
		parent.image(targetGraphics, 90, 148);
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		
		if(evt.event.equals("KEY")){
			
			switch(evt.id){
			case KeyEvent.VK_UP:
				headVelTarget.y = 1f;
				break;
			case KeyEvent.VK_DOWN:
				headVelTarget.y = -1f;
				break;
			case KeyEvent.VK_RIGHT:
				headVelTarget.x = -1f;
				break;
			case KeyEvent.VK_LEFT:
				headVelTarget.x = 1f;
				break;
			case KeyEvent.VK_ADD:
				headVelTarget.z = 1f;
				break;
			case KeyEvent.VK_SUBTRACT:
				headVelTarget.z = -1f;
				break;
			
			}
		}
	}

	PVector getNewStartPos(float accuracy){
		//tl : -178,0
		//br : -600, -946
		//centre -304, -447
		
		
		int randX = (int) -( parent.random(600));
		int randY = (int) -parent.random(946);
		
		return new PVector(-304, -447, 300);//PVector(randX, randY, 0);
	}
	
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}