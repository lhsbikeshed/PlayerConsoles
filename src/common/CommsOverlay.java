package common;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

public class CommsOverlay {
	
	boolean isActive = false;
	String url = "http://spanners.imakethin.gs/cam.html";
	MJPEGImagethread imgThread;
	
	PlayerConsole parent;
	
	long connectStart = 0;
	PImage commBg;
	
	public CommsOverlay(PlayerConsole p) {
		parent = p;
		commBg = p.loadImage("data/common/commpopup.png");
		
	}

	public void draw(PlayerConsole g){
		g.pushMatrix();
		if(isActive){
			//window draw
			g.noStroke();
			g.fill(100);
			g.image(commBg,0,0);
			if(parent.millis() - connectStart < 1500){
				//show connecting
				g.textFont(g.getGlobalFont(), 13);
				if(g.globalBlinker){
					g.fill(255,255,0);
				} else {
					g.fill(255,0,0);
				}
				
				g.text("INCOMING MESSAGE", 10, 110);
			} else {
				g.textFont(g.getGlobalFont(), 13);
				if(g.globalBlinker){
					g.fill(255,255,0);
				} else {
					g.fill(255,0,0);
				}
				
				g.text(">> CONNECTED <<", 10, 210);
			}
			
		}		
		
		
		g.popMatrix();
		
	}
	
	private void startCall(){
		ConsoleLogger.log(this, "incoming video call");			
		isActive = true;
		parent.getConsoleAudio().playClip("commsBeep");
		connectStart = parent.millis();
	}
	
	private void hangup(){
		ConsoleLogger.log(this, "video call hung up");
		isActive = false;
	}
	
	public void messageReceived(OscMessage msg){
		ConsoleLogger.log(this, "FFS");
		if(msg.checkAddrPattern("/ship/comms/incomingCall")){
			startCall();
			
		} else if (msg.checkAddrPattern("/ship/comms/hangupCall")){
			hangup();
		}
	}
	
}
