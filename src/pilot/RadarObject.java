package pilot;

import java.util.HashMap;

import processing.core.PVector;
import common.util.UsefulShit;

public class RadarObject {
	public boolean active = false;
	public PVector position = new PVector();
	public PVector lastPosition = new PVector();
	public PVector screenPos = new PVector();
	public String name = "";
	public String statusText = "";
	public int id = 0;
	public boolean targetted = false;

	public float bearing = 0.0f;
	public float elevation = 0.0f;
	public float distance = 0.0f;

	public int displayColor = UsefulShit.makeColor(0, 255, 0);

	public long lastUpdateTime = 0;
	protected HashMap<String, Float> statMap = new HashMap<String, Float>();

	public RadarObject() {

	}

	public void clearStats() {
		statMap.clear();
	}

	public Float getStat(String name) {
		Float f = statMap.get(name);
		return f;
	}

	public void setStat(String name, float val) {
		Float f = new Float(val);
		statMap.put(name, val);
	}
}
