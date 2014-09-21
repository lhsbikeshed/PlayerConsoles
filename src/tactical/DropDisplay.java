package tactical;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import common.Display;
import common.PlayerConsole;
import processing.core.*;

/** during the drop scene
 * show warning that systems are offline
 * show references to flight manual for patching emergency jump power in
 */

public class DropDisplay extends Display {

  PImage bg ;
  PImage repairedBg;
  PImage structFailOverlay;
  PImage offlineBlinker;
  PImage damagedIcon;

  PFont font;

  boolean fixed = false;
  boolean structFail = false;
  boolean jumpCharged = false;



  int curStep = -1;

  public DropDisplay(PlayerConsole parent) {
	super(parent);
    bg = parent.loadImage("tacticalconsole/dropBackground.png");


    repairedBg = parent.loadImage("tacticalconsole/dropscenefixed.png");
    structFailOverlay = parent.loadImage("tacticalconsole/structuralFailure.png");
    damagedIcon = parent.loadImage("tacticalconsole/dropDamage.png");
    offlineBlinker = parent.loadImage("tacticalconsole/dropOffline.png");
   
  }

  public void start() {
    fixed = false;
    structFail = false;
    jumpCharged = false;
    //probe for current cable state
    ((TacticalConsole)parent).probeCableState();
  }

  public void stop() {
  }

  public void draw() {
    parent.background(0);
    parent.fill(255, 255, 255);
    parent.image(bg, 0, 0, parent.width, parent.height);
    if (fixed) {
      parent.strokeWeight(8);
      parent.stroke(0, parent.map(parent.millis() % 1250, 0, 1250, 0, 255), 0);
      parent.line(462, 280, 585, 280);
      parent.textFont(font, 30);
      parent.fill(0, 255, 0);
      if (jumpCharged) {

        parent.text("JUMP SYSTEM CHARGING", 10, 148);
        parent.textFont(font, 20);

        parent.text("CHARGING", 61, 440);
      } 
      else {
        parent.text("JUMP SYSTEM READY", 10, 148);
        parent.textFont(font, 20);

        parent.text("READY", 61, 440);
      }
    } 
    else {
      parent.strokeWeight(8);
      parent.stroke(parent.map(parent.millis() % 250, 0, 250, 0, 255), 0, 0);
      parent.line(462, 280, 585, 280);
      parent.image(damagedIcon, 530, 204);

      //if (globalBlinker) {
      parent.textFont(font, 30);
      parent.fill(parent.map(parent.millis() % 800, 0, 800, 0, 255), 0, 0);
      parent.text("JUMP SYSTEM OFFLINE", 10, 148);
      parent.textFont(font, 20);
      parent.text("OFFLINE", 61, 440);
      // }
    }

    if (structFail) { //show the "structural failure" warning

      parent.image(structFailOverlay, 128, 200);
    }
  }

  public void oscMessage(OscMessage theOscMessage) {
    //   println(theOscMessage);
    if (theOscMessage.checkAddrPattern("/scene/drop/structuralFailure")==true) {
      structFail = true;
    } 
    else if (theOscMessage.checkAddrPattern("/ship/jumpStatus") == true) {
      int v = theOscMessage.get(0).intValue();
      if (v == 0) {
        jumpCharged = false;
      } 
      else if (v == 1) {
        jumpCharged = true;
      }
    }
  }

  public void serialEvent(String evt) {
    String[] evtData = evt.split(":");
    
    if (evtData[0].equals("CONDUITCONNECT")) {

      char c = evtData[1].charAt(0);
      if (c >= '0' && c < '9') {
        OscMessage myMessage = new OscMessage("/scene/drop/conduitConnect");
        myMessage.add(Integer.parseInt(evtData[1]));
        OscP5.flush(myMessage, new NetAddress(parent.getServerIP(), 12000));
      }
    } 
    else if (evtData[0].equals("CONDUITDISCONNECT")) {

      char c = evtData[1].charAt(0);
      if (c >= '0' && c < '9') {
        OscMessage myMessage = new OscMessage("/scene/drop/conduitDisconnect");
        myMessage.add(Integer.parseInt(evtData[1]));
        OscP5.flush(myMessage, new NetAddress(parent.getServerIP(), 12000));
      }
    } 
   
  }
}

