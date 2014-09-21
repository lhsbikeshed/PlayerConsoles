package common;

import oscP5.OscMessage;
import processing.core.PFont;

public abstract class Display {

	protected PlayerConsole parent;
	protected PFont font;
	public Display(PlayerConsole parent){
		this.parent = parent;
		font = parent.getGlobalFont();
		
	}
	public void keyPressed(char key){
		
	}
	
	public void keyReleased(char key){}
	
	  public abstract void draw();
	  public abstract void oscMessage(OscMessage theOscMessage);
	  public abstract void start();
	  public abstract void stop();
	  public abstract void serialEvent(String content);
	  
}