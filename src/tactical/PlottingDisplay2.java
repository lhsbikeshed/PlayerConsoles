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
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;

public class PlottingDisplay2 extends Display {
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
	

	private String failReason = "";
	String lastPlottedScene = "";	//scene that the last successful plot was in

	// debug animation stuff
	long lastKeyTime = 0;
	
	PVector mapOffset = new PVector(0,0);
	
	PGraphics mapGraphics;

	public PlottingDisplay2(PlayerConsole parent) {
		super(parent);
		backgroundImage = parent.loadImage("tacticalconsole/plottingBG.png");
		loadMap();
		clearRoute();
		mapGraphics = parent.createGraphics(parent.width, parent.height);
	}
	
	@Override
	public void gameReset(){
		lastPlottedScene = "";
		clearRoute();
	}

	@Override
	public void draw() {
		parent.clear();
		mapGraphics.beginDraw();
		mapGraphics.clear();
		
		PVector mousePos = ((TacticalConsole)parent).mousePosition;
		PVector centre = new PVector (parent.width/2f, parent.height/2f);
		float dist = PVector.dist(mousePos,  centre);
		mapOffset.x = mousePos.x - centre.x ;
		mapOffset.y = mousePos.y - centre.y;
		
		// TODO Auto-generated method stub
		mapGraphics.strokeWeight(1);
		mapGraphics.noStroke();
		mapGraphics.fill(0,40,0);
		mapGraphics.rect(0, 0, 1015, 757);
		
		//a grid
		int stepX = 102;
		int stepY = 100;
		mapGraphics.stroke(0,20,0);
		stepX = 200;
		stepY = 200;
		for (int x = -400; x < parent.width; x += stepX){
			for (int y = 0; y < parent.width; y += stepY){
				mapGraphics.line(x - mapOffset.x * 0.5f, 0, x - mapOffset.x * 0.5f, parent.height);
				mapGraphics.line(0, y - mapOffset.y * 0.5f, parent.width, y - mapOffset.y * 0.5f);
			}
		}
		stepX = 102;
		stepY = 100;
		mapGraphics.stroke(0,200,0);
		for (int x = -400; x < parent.width; x += stepX){
			for (int y = 0; y < parent.width; y += stepY){
				mapGraphics.line(x - mapOffset.x, 0, x - mapOffset.x, parent.height);
				mapGraphics.line(0, y - mapOffset.y, parent.width, y - mapOffset.y);
			}
		}
		
		
		if(routeComplete == false){
			MapNode lastNode = currentRoute.get(currentRoute.size() - 1);
			mapGraphics.pushMatrix();
			float mouseDist = PVector.dist(PVector.sub(lastNode.pos, mapOffset), mousePos);
			float a = parent.map(mouseDist, 0f, 165f, 255f, 0f);
			a = parent.constrain(a,0.0f, 255.0f);
			float modVal = 0f;
			if (mouseDist > 165f) {
				modVal = (PApplet.sin(parent.frameCount * 0.5f) + 1.0f) /2f;
				mapGraphics.stroke(255 * modVal,0,0);
			} else {
				mapGraphics.stroke(200,200,0);
				
			}
			mapGraphics.strokeWeight(3);
			//mapGraphics.translate(-mapOffset.x, -mapOffset.y);
			mapGraphics.line(lastNode.posx-mapOffset.x, lastNode.posy-mapOffset.y, mousePos.x, mousePos.y);
			mapGraphics.popMatrix();
		}
		
		mapGraphics.fill(255);
		mapGraphics.textFont(font, 18);
		
		// ------------- map nodes ---------
		for(MapNode m : mapNodes){
			mapGraphics.stroke(0,200,0);
			mapGraphics.pushMatrix();
			mapGraphics.translate(-mapOffset.x, -mapOffset.y);
			mapGraphics.translate(m.posx, m.posy);
			if(parent.getShipState().currentSceneId.equals(m.id)){
				mapGraphics.fill(255,0,0);
				mapGraphics.text(m.id + " <current>", 10, 0);
			
			} else {
				mapGraphics.fill(255);
				mapGraphics.text(m.id, 10, 0);

			}
			mapGraphics.ellipse(0,0,10,10);
			mapGraphics.popMatrix();
		}
		
		// ---------- plotting info
		
		for(int i = currentRoute.size() - 1 ; i > 0; i--){
			MapNode m1 = currentRoute.get(i);
			MapNode m2 = currentRoute.get(i-1);
			mapGraphics.strokeWeight(2);
			float col = (-PApplet.sin(parent.frameCount * 0.3f - i) + 1.0f ) * 0.5f;
			mapGraphics.stroke(0,0 , 50 + 200 * col);
			mapGraphics.pushMatrix();
			mapGraphics.translate(-mapOffset.x, -mapOffset.y);
			mapGraphics.line(m1.posx , m1.posy, m2.posx, m2.posy);
			mapGraphics.popMatrix();
		}
		
		
		
		
		// ---------- mouse cursor ----------------
		
		PVector pos = ((TacticalConsole)parent).mousePosition;
		mapGraphics.pushMatrix();
		mapGraphics.translate(pos.x, pos.y);
		mapGraphics.noFill();
		mapGraphics.stroke(255);
		mapGraphics.line(-20,0, 20,0);
		mapGraphics.line(0, 20, 0, -20);
		float scale = parent.sin(parent.millis() / 200f) * 0.2f + 0.8f;
		mapGraphics.scale(scale);
		
		mapGraphics.ellipse(0, 0,  30, 30);
		
		mapGraphics.popMatrix();

		
		mapGraphics.text(failReason, 100,100);
		mapGraphics.endDraw();
		
		parent.image(mapGraphics,0,0);
		parent.image(backgroundImage, 0, 0);
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		if(evt.event == "MOUSECLICK"){
			
			ConsoleLogger.log(this, evt.event);
			clickedAt(((TacticalConsole)parent).mousePosition);
			
		
		} else if (evt.event.equals("KEY")) {
			if(evt.id == 520){
				clearLast();
			}
		}
		ConsoleLogger.log(this, ""+evt.id);
	}

	private void clickedAt(PVector pos){
		//find out where we clicked
		MapNode clicked = null;
		for(MapNode m : mapNodes){
			PVector mp = pos;
			PVector nodePos = new PVector(m.posx - mapOffset.x,m.posy - mapOffset.y);
			
			float dist = PVector.dist(mp, nodePos);
			if(dist < 20f){
				clicked = m;
				break;
			}
		}
		
		if(clicked == null) return;
		ConsoleLogger.log(this, "clicked node: " + clicked.id);
		testNode(clicked);
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
	
		failReason = reason;
		parent.getConsoleAudio().playClip("outOfRange");

	}


	/*
	 * test a node to see if its in range of the current node if it is and hasnt
	 * been visited then add it to the waypoint list if it isnt then
	 * codefailed();
	 */
	private void testNode(MapNode m) {
		if (m.visited) {
			codeFailed("ALREADY SELECTED");
			return;

		} else {

			// check its distance
			int testP = currentRoute.size() - 1;
			if(testP < 0){
				return;
			}
			PVector src = currentRoute.get(testP).pos;
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
		
		if(currentRoute.size() > 1 ){
			
			currentNode = currentRoute.get(currentRoute.size()-1);
		
			MapNode removedNode = currentRoute.remove(currentRoute.size()-1);
			removedNode.visited = false;
			if(routeComplete){
				routeComplete = false;
				OscMessage msg = new OscMessage("/system/jump/clearRoute");
				parent.getOscClient().send(msg, parent.getServerAddress());
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
