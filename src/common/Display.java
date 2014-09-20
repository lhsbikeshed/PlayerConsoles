package common;

import oscP5.OscMessage;

public interface Display {

	  public void draw();
	  public void oscMessage(OscMessage theOscMessage);
	  public void start();
	  public void stop();
	  public void serialEvent(String content);
	  
}