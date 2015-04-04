package tactical;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.ho.yaml.Yaml;

import java.util.ArrayList;

import jogamp.opengl.glu.nurbs.Maplist;
import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;

public class PlottingDisplay extends Display {
	PImage backgroundImage;
	
	//the map
	MapNode[] mapNodes;;
	//last node to be entered properly
	MapNode currentNode;
	//start of the route, essentially where the ship is now
	MapNode startNode;
	//are we done here?
	boolean routeComplete = false;

	// entered codes
	ArrayList<MapNode> currentRoute = new ArrayList<MapNode>();
	String currentCode = "";

	private String failReason = "";
	String lastPlottedScene = "";	//scene that the last successful plot was in

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
		lastPlottedScene = "";
		clearRoute();
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
				
				if(currentRoute.size() * 20 > ShipState.instance.fuelTankState[0]/20){
					parent.fill(255, 0, 0);
					parent.text("ROUTE OK, BUT INSUFFICIENT FUEL", 39, 246 + yOffset);					
				} else {
					parent.fill(0, 255, 0);
					parent.text("ROUTE OK, BEGIN JUMP SEQUENCE", 39, 246 + yOffset);
				}
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
		
		parent.pushMatrix();
		parent.translate(25,83);
		parent.text("FUEL\r\nUSAGE", 0,20);
		parent.rect(120,0,500,50);
		
		float amount = ShipState.instance.fuelTankState[0]/20;
		parent.fill(0,255,0);
		parent.noStroke();
		parent.rect(120,0,amount, 25);
		
		
		float required = currentRoute.size() * 20;
		parent.fill(0,255,0);
		parent.noStroke();
		parent.rect(120,25,required, 25);
		
		parent.fill(255);
		parent.text(required + "/" + amount, 400, 40);
		
		parent.popMatrix();
		

	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		if (evt.event.equals("KEY") && evt.value == 1) {
			
			if (evt.id >= KeyEvent.VK_0 && evt.id <= KeyEvent.VK_9) {
				keyTyped(evt.id);
			} else if (evt.id == KeyEvent.VK_SPACE) {
				codeEntered();
			} else if (evt.id == KeyEvent.VK_BACK_SPACE){
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


	//Load the universe map from the yaml file
	private void loadMap() {
		
		ConsoleLogger.log(this, "loading mapnodes..");
		try {
			InputStream in = getClass().getResourceAsStream("/data/tacticalconsole/map.yaml");
			mapNodes = Yaml.loadType(in, MapNode[].class);
			ConsoleLogger.log(this, "..loaded " + mapNodes.length + " nodes");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//yaml cant serialize pvectors, so skim over nodes and setup the positions
		for(MapNode m : mapNodes){
			m.pos = new PVector(m.posx, m.posy);
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
			//ShipState.instance.fuelTankState[0] = 1200;
			//test for fuel
			if((currentRoute.size() + 1) * 400 > ShipState.instance.fuelTankState[0]){
				codeFailed("INSUFFICIENT FUEL");
				return;
			}
			
			
			m.visited = true;
			parent.getConsoleAudio().playClip("codeOk");
			currentRoute.add(m);
			currentCode = "";

			// now do a quick check to see if we are in range of the destination
			// node
			if (m.planned == true && startNode != m) { //we have entered a plannable node, transmit the route to the game
				//find the first interestingThing we encounter on this route, send it to the game
				
				String interestingThing = "";
				for(MapNode routeNode : currentRoute){
					if(routeNode != startNode && routeNode.interestingThing != null){
						//if we arent in the return journey from the warzone and the node is set
						//to not be ignored then its ok. Otherwise ignore it
						if(! (routeNode.ignoreOnReturn && ShipState.instance.returnJourney)){
							interestingThing = routeNode.interestingThing;
							ConsoleLogger.log(this, "found interesting thing:" + interestingThing);
							break;
						}
					}
				}
				
				ConsoleLogger.log(this, "setting next Destination tag " + interestingThing);
						
				// tell the main game which route we're using
				OscMessage msg = new OscMessage("/system/jump/setRoute");
				msg.add(interestingThing);
				msg.add(currentRoute.size());//how much fuel to consume for the jump
				//combine all of the interesting things en route
				parent.getOscClient().send(msg, parent.getServerAddress());
				// fuck yeah, we're there!
				parent.getBannerSystem().setSize(700, 300);
				parent.getBannerSystem().setTitle("ROUTE OK");
				parent.getBannerSystem().setText("JUMP PLOTTING COMPLETE");
				parent.getBannerSystem().displayFor(2000);
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
//		if(lastPlottedScene != parent.getShipState().currentScene){
//			ConsoleLogger.log(this, "last s: " + lastPlottedScene + " cs : " + parent.getShipState().currentScene);
//			clearRoute();
//		}
		//lets see where we are right now
		OscMessage m = new OscMessage("/system/jump/whereAmI");
		parent.getOscClient().send(m, parent.getServerAddress());

		//check to see if the current ship location doesnt match what the first entry in our route table is
		if(currentRoute.size() > 0){
			String currentId = currentRoute.get(0).id;
			
			if( currentId != null && parent.getShipState().currentSceneId.equals(currentId) == false){
				ConsoleLogger.log(this, "last s: " + lastPlottedScene + " cs : " + parent.getShipState().currentScene);
				clearRoute();
			}
		} else {
			clearRoute();
		}


	}
	
	/* clear the last char typed, if char pos it at zero then roll back to prev entry */
	private void clearLast(){
		if(currentCode.length() > 0){
			currentCode = currentCode.substring(0, currentCode.length() -1);
		} else {
			if(currentRoute.size() > 1 ){
				
				currentNode = currentRoute.get(currentRoute.size()-1);
				currentCode = currentNode.id;
				currentCode = currentCode.substring(0, currentCode.length() -1);
				
				
				MapNode removedNode = currentRoute.remove(currentRoute.size()-1);
				removedNode.visited = false;
				if(routeComplete){
					routeComplete = false;
					OscMessage msg = new OscMessage("/system/jump/clearRoute");
					parent.getOscClient().send(msg, parent.getServerAddress());
				}
			} 
		}
	}


	private void clearRoute() {
		ConsoleLogger.log(this, "clearing route..");
		currentRoute = new ArrayList<MapNode>();
		String curSceneId = parent.getShipState().currentSceneId;
		
		//TODO : UNFUCK THIS	
		//problem:
		//i dont want to hardcode the start/end nodes
		startNode = findNodeById(curSceneId);
		
		
		for (MapNode m : mapNodes) {
			m.visited = false;
		}
		currentCode = "";
		failReason = "";
		routeComplete = false;
		if(startNode != null){
			currentRoute.add(startNode);
		}
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

	

}
