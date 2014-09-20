package common;
import processing.core.*;

public class PlayerConsole extends PApplet{
	PImage p;
	
	public boolean testMode = false;

	public boolean globalBlinker;
	
	
	@Override
	public void setup(){
		size(200,200);
		background(0);
		p = loadImage("data/pilotconsole/destruct.png");
	}
	
	@Override
	public void draw(){
		background(0);
		image(p,0,0,width,height);
		
	}

	public static void main(String[] args) {
		PApplet.main(new String[] { "PlayerConsole" });

	}

	public ConsoleAudio getConsoleAudio() {
		// TODO Auto-generated method stub
		return null;
	}

	public void hardwareEvent(HardwareEvent h) {
		// TODO Auto-generated method stub
		
	}

	public BannerOverlay getBannerSystem() {
		// TODO Auto-generated method stub
		return null;
	}

}
