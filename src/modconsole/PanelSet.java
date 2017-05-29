package modconsole;

import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import controlP5.ControlEvent;
import controlP5.ControlP5;

/* change this scene to show the altitude and predicted death time*/
public abstract class PanelSet {

	ModConsole parent;
	OscP5 oscP5;
	ControlP5 cp5;
	String name;
	public String sceneTag = "";

	public PanelSet(String name, ModConsole parent, OscP5 p5, ControlP5 cp5) {
		this.parent = parent;
		this.oscP5 = p5;
		this.name = name;
		this.cp5 = cp5;
	}

	public String getName(){
		return name;
	}

	public void reset() {
	}
	public void initGui() {
	}
	public void draw(ModConsole p) {
	}
	public void oscMessage(OscMessage msg) {
	}
	public void controlEvent(ControlEvent theControlEvent) {
	}

}