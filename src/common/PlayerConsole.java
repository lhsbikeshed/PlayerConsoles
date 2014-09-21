package common;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

import netP5.NetAddress;
import oscP5.OscP5;
import ddf.minim.Minim;
import processing.core.*;

public abstract class PlayerConsole extends PApplet{
	
	public boolean testMode = false;
	
	//----- global blink state ----
	public boolean globalBlinker;
	long blinkTime = 0;
	
	//---banner overlay class---
	protected BannerOverlay bannerSystem;
		
	
	protected String serverIP = "127.0.0.1";    

	//damage effects
	protected DamageEffect damageEffects;
	
	//---- audio stuff
	protected Minim minim;
	protected ConsoleAudio consoleAudio;
	
	//-----OSC stuff--------
	protected OscP5 oscP5;
	protected NetAddress myRemoteLocation;
	
	//------ ship sate -----
	protected ShipState shipState = new ShipState();  //container for ship data
	
	//----- common assets ----
	protected PFont font;  //default font for game
	
	//----- display control
	protected Hashtable<String, Display> displayMap = new Hashtable<String, Display>();
	protected Display currentScreen;  //screen that is currently being displayed
	
	protected String consoleName = "changeme";
	
	public void setup(){
		size(1024, 768, P3D);
		  frameRate(25);
		  hideCursor();
		bannerSystem = new BannerOverlay(this);
		  damageEffects = new DamageEffect(this);
			font = loadFont("common/HanzelExtendedNormal-48.vlw");

	}
	
	public void draw(){
		//common draw things
		if (blinkTime + 750 < millis()) {
		    blinkTime = millis();
		    globalBlinker = ! globalBlinker;
		  }
		//translate stuff
		damageEffects.startTransform();
		
		//call draw method
		drawConsole();
		
		//post-draw
		damageEffects.stopTransform();
		 damageEffects.draw();
	}
	
	public abstract void drawConsole();

	/* switch to a new display */
	protected void changeDisplay(Display d) {
	  if(currentScreen != null){
	    currentScreen.stop();
	  }
	  currentScreen = d;
	  currentScreen.start();
	}
	
	
	protected void hideCursor() {
		  BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		  Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
		  cursorImg, new Point(0, 0), "blank cursor");
		  frame.setCursor(blankCursor);
		}
		
	
	

	public ConsoleAudio getConsoleAudio() {
		return consoleAudio;
	}

	public void hardwareEvent(HardwareEvent h) {
		// TODO Auto-generated method stub
		
	}

	
	
	//--- getters ----
	public BannerOverlay getBannerSystem() {
		return bannerSystem;
	}
	public String getConsoleName(){
		return consoleName;
	}

	public PFont getGlobalFont(){ return font; }
	public ShipState getShipState() { return shipState; }

	public String getServerIP() {
		return serverIP;
	}
	
	public OscP5 getOscClient(){
		return oscP5;
	}
	
	

}
