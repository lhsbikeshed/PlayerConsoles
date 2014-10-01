package common;

import oscP5.OscMessage;
import processing.core.PFont;

/* abstract class representing a single screen to display to the players
 * responsible for input, drawing, serial events and osc messages
 */
public abstract class Display {

	protected PlayerConsole parent;
	protected PFont font;

	public Display(PlayerConsole parent) {
		this.parent = parent;
		font = parent.getGlobalFont();

	}

	/* called at 24fps to draw the screen and update its state */
	public abstract void draw();

	
	public void keyPressed(char key) {

	}

	public void keyReleased(char key) {
	}

	/* received an osc message from server */
	public abstract void oscMessage(OscMessage theOscMessage);

	/* received a serialEvent from the hardware connected to machine */
	public abstract void serialEvent(HardwareEvent evt);

	/* called when display is shown */
	public abstract void start();

	/* called when display is hidden */
	public abstract void stop();

}