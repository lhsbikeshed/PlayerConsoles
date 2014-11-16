package tactical;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import oscP5.OscMessage;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;

public class PlottingDisplay extends Display {
	PImage backgroundImage;

	ArrayList<MapNode> mapNodes = new ArrayList<MapNode>();
	MapNode currentNode;
	MapNode startNode;
	MapNode destNode;
	boolean routeComplete = false;

	// entered codes
	ArrayList<MapNode> currentRoute = new ArrayList<MapNode>();
	String currentCode = "";

	private String failReason = "";
	int lastPlottedScene = 0;	//scene that the last successful plot was in

	// debug animation stuff
	long lastKeyTime = 0;

	public PlottingDisplay(PlayerConsole parent) {
		super(parent);
		backgroundImage = parent.loadImage("tacticalconsole/plottingBG.png");
		loadMap();
		clearRoute();
	}
	
	@Override
	public void gameReset(){
		lastPlottedScene = 0;
	}

	@Override
	public void draw() {
		parent.image(backgroundImage, 0, 0);
		// TODO Auto-generated method stub
		parent.fill(255);
		parent.textFont(font, 38);

		// current route
		int yOffset = 0;
		for (int i = 0; i < currentRoute.size(); i++) {
			MapNode s = currentRoute.get(i);
			String t = "> " + s.id;
			if (i == currentRoute.size() - 1) {
				t += " - OK";
			}
			parent.text(t, 39, 246 + yOffset);
			yOffset += 40;
		}
		// post-list text
		if (!routeComplete) {
			String t = "> " + currentCode;
			if (parent.globalBlinker) {
				t += "_";
			}
			parent.text(t, 39, 246 + yOffset);
		} else {
			if (parent.globalBlinker) {
				parent.textFont(font, 22);
				parent.fill(0, 255, 0);
				parent.text("ROUTE OK, BEGIN JUMP SEQUENCE", 39, 246 + yOffset);
			}
		}

		// the failure reason
		if (failReason.equals("") == false) {

			parent.fill(255, 0, 0);
			parent.text(failReason, 39, 246 + yOffset + 50);
		}
		parent.noStroke();
		if (routeComplete) {
			if (parent.globalBlinker) {
				parent.fill(0, 255, 0);
			} else {
				parent.fill(0, 100, 0);

			}
			parent.rect(725, 623, 251, 90);
			parent.fill(255);
			parent.textFont(font, 20);
			parent.text("ROUTE OK", 776, 673);
		} else {
			parent.fill(255, 0, 0);
			parent.rect(725, 623, 251, 90);
			parent.fill(255);
			parent.textFont(font, 20);
			parent.text("NO ROUTE", 776, 673);
		}

		// animated debug info
		parent.noFill();
		parent.stroke(255, 255, 0);
		// parent.rect(721, 204, 268, 340);

	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		if (evt.event.equals("KEY")) {
			if (evt.value >= KeyEvent.VK_0 && evt.value <= KeyEvent.VK_9) {
				keyTyped(evt.value);
			} else if (evt.value == KeyEvent.VK_SPACE) {
				codeEntered();
			} else if (evt.value == KeyEvent.VK_BACK_SPACE){
				clearLast();
			}
		} else if (evt.event.equals("KEYPAD")) {
			if (evt.id >= 0 && evt.id <= 9) {
				keyTyped((char) (48 + evt.id));
			} else if (evt.id == TacticalHardwareController.KP_SCAN) {
				codeEntered();
			} else if (evt.id == TacticalHardwareController.KP_STAR){
				clearLast();
			}
		}
	}

	private void loadMap() {
		String[] s = parent.loadStrings("tacticalconsole/map.txt");
		ConsoleLogger.log(this, "loading " + s.length + " mapnodes");

		for (String it : s) {
			if (!it.startsWith("#")) {
				String[] parts = it.split(":");
				int xp = Integer.parseInt(parts[0]);
				int yp = Integer.parseInt(parts[1]);

				String id = parts[2];
				MapNode n = new MapNode();
				
				int routeTag = Integer.parseInt(parts[3]);
				
				n.pos = new PVector(xp, yp);
				n.id = id;
				n.routeTag = routeTag;
				
				mapNodes.add(n);
			}
		}
	}

	private void codeFailed(String reason) {
		currentCode = "";
		failReason = reason;
		parent.getConsoleAudio().playClip("outOfRange");

	}

	private void codeEntered() {
		if (routeComplete)
			return;

		if (currentCode.length() == 4) {
			// check to see if code exists in map
			boolean found = false;
			for (MapNode m : mapNodes) {
				if (m.id.equals(currentCode)) {

					found = true;
					testNode(m);

					break;
				}
			}
			if (!found) {
				codeFailed("INVALID WAYPOINT");
			}

		} else {
			codeFailed("INVALID WAYPOINT");

		}

	}

	/*
	 * test a node to see if its in range of the current node if it is and hasnt
	 * been visited then add it to the waypoint list if it isnt then
	 * codefailed();
	 */
	private void testNode(MapNode m) {
		if (m.visited) {
			codeFailed("INVALID WAYPOINT");
			return;

		} else {

			// check its distance
			PVector src = currentRoute.get(currentRoute.size() - 1).pos;
			PVector dst = m.pos;
			PVector diff = PVector.sub(src, dst);
			if (diff.mag() > 165) { // too far
				codeFailed("DISTANCE TOO GREAT");
				return;
			}
			m.visited = true;
			parent.getConsoleAudio().playClip("codeOk");
			currentRoute.add(m);
			currentCode = "";

			// now do a quick check to see if we are in range of the destination
			// node
			src = m.pos;
			dst = destNode.pos;
			diff = PVector.sub(src, dst);
			if (diff.mag() < 165) { // too far
				
				//look at the last entered waypoint and send the route tag it has
				int routeTag = currentRoute.get(currentRoute.size() - 1).routeTag;
				
				ConsoleLogger.log(this, "setting route tag " + routeTag);
						
				// tell the main game which route we're using
				OscMessage msg = new OscMessage("/system/jump/setRoute");
				msg.add(routeTag);
				parent.getOscClient().send(msg, parent.getServerAddress());
				// fuck yeah, we're there!
				parent.getBannerSystem().setSize(700, 300);
				parent.getBannerSystem().setTitle("ROUTE OK");
				parent.getBannerSystem().setText("JUMP PLOTTING COMPLETE");
				parent.getBannerSystem().displayFor(2000);
				currentRoute.add(destNode);
				routeComplete = true;
				lastPlottedScene = parent.getShipState().currentScene;
				
				

				return;
			}
		}
	}

	// a key was typed
	private void keyTyped(int value) {
		lastKeyTime = 50;
		parent.getConsoleAudio().randomBeep();
		if (currentCode.length() < 4) {
			currentCode += (char) value;
			failReason = "";
		} else {
			currentCode = "" + (char) value;
		}
	}

	/*
	 * clear the current route, set the start and end nodes based on where we
	 * are
	 * TODO: add a clear button to the dashboard rather than clear this when screen changes
	 */
	@Override
	public void start() {
		// if this screen has appeared check to see if the last plot we did was
		// from a different game scene. If it is then clear the route.
		// prevents screen changes from clearing a half plotted route
		if(lastPlottedScene != parent.getShipState().currentScene){
			ConsoleLogger.log(this, "last s: " + lastPlottedScene + " cs : " + parent.getShipState().currentScene);
			clearRoute();
		}
		
	}
	
	/* clear the last char typed, if char pos it at zero then roll back to prev entry */
	private void clearLast(){
		if(currentCode.length() > 0){
			currentCode = currentCode.substring(0, currentCode.length() -1);
		} else {
			if(currentRoute.size() > 1 && routeComplete == false){
				
				currentNode = currentRoute.get(currentRoute.size()-1);
				currentCode = currentNode.id;
				currentCode = currentCode.substring(0, currentCode.length() -1);
				
				
				MapNode removedNode = currentRoute.remove(currentRoute.size()-1);
				removedNode.visited = false;
			} 
		}
	}


	private void clearRoute() {
		currentRoute = new ArrayList<MapNode>();
		int curScene = parent.getShipState().currentScene;
		if (curScene == ShipState.SCENE_LAUNCH
				|| curScene == ShipState.SCENE_LANDING) {
			startNode = findNodeById("STATION 13");
			destNode = findNodeById("TRAINING AREA");
			currentRoute.add(startNode);

		} else if (curScene == ShipState.SCENE_WARZONE) {
			destNode = findNodeById("STATION 13");
			startNode = findNodeById("TRAINING AREA");
			currentRoute.add(startNode);
		} else {
			startNode = new MapNode();
			startNode.pos = new PVector(1000, 1000);
			startNode.id = "MID-ROUTE";
			destNode = new MapNode();
			destNode.pos = new PVector(-1000, -1000);
			destNode.id = "MID-ROUTE";
			currentRoute.add(startNode);
		}
		for (MapNode m : mapNodes) {
			m.visited = false;
		}
		currentCode = "";
		failReason = "";
		routeComplete = false;

		OscMessage msg = new OscMessage("/system/jump/clearRoute");
		parent.getOscClient().send(msg, parent.getServerAddress());
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	private MapNode findNodeById(String id) {
		for (MapNode m : mapNodes) {
			if (m.id.equals(id)) {
				return m;
			}
		}
		return null;
	}

	class MapNode {
		public PVector pos;				//position on map
		public String id;				//id that the user types
		public boolean visited = false;	//have we visited this node already while planning route
		public int routeTag = -1;		//the route tag to transmit when setting route

		public MapNode() {
		}
	}

}
