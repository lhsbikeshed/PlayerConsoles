package modconsole;

import netP5.NetAddress;
import controlP5.*;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
/* controls for the launch scene */

public class LaunchControl extends PanelSet {

  boolean gravOn = true;
  boolean clampOn = true;
  boolean grabberState = false;
  
  NetAddress myRemoteLocation;

  public LaunchControl(String name, ModConsole parent, OscP5 p5, ControlP5 cp5) {
    super(name, parent, p5, cp5);
     sceneTag = "launch";
     myRemoteLocation = parent.getNetAddress();
  }

  public void draw(ModConsole p) {
    p.textFont(p.getGlobalFont(), 12);
    p.text("docking grabber can be used? : " + grabberState, 100, 200);
    p.text("LAUNCH", 90, 60);
    p.text("LAND", 350, 60);
    p.line(329, 70, 329, 170);
  }


  public void initGui() {
   

    //controls for:

    // front door open 
    cp5.addBang("BayDoors")
      .setPosition(220, 220)
        .setSize(50, 50)
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("Open Bay Doors")
              .moveTo(sceneTag)
                ;
    //gravity
    cp5.addToggle("Bay Gravity")
      .setPosition(290, 220)
        .setSize(50, 50)
          .setLabel("Bay Gravity")
            .setValue(1.0f)
              .moveTo(sceneTag)
                ;

    //LAUNCH

    cp5.addBang("StartLaunch")
      .setPosition(40, 90)
        .setSize(50, 50)
          .setLabel("Start launch\r\nsequence")
            .setValue(1.0f)
              .moveTo(sceneTag)
                ;

    cp5.addBang("DockingClamp")
      .setPosition(110, 90)
        .setSize(50, 50)
          .setLabel("Docking\r\nClamp")
            .setValue(1.0f)
              .moveTo(sceneTag)
                ;
   cp5.addBang("LaunchOtherShip")
      .setPosition(180, 90)
        .setSize(50, 50)
          .setLabel("launch\r\nother ship")
            .setValue(1.0f)
              .moveTo(sceneTag)
                ;
   
    cp5.addToggle("SpawnMissile")
      .setPosition(250, 90)
        .setSize(50, 50)
          .setLabel("Spawn Training\r\nMissiles?")
            .setValue(0.0f)
              .moveTo(sceneTag)
                ;
                
    //landing
    
     cp5.addBang("GameWin")
     .setPosition(350, 90)
     .setSize(50, 50)
     .setLabel("Win Game")
     .moveTo(sceneTag);
     
      cp5.addToggle("DockingComp")
     .setPosition(410, 90)
     .setSize(50, 50)
     .setLabel("DockingComp")     
     .moveTo(sceneTag);

    /* player names */
    cp5.addTextfield("PilotName")
      .setPosition(680, 80)
        .setSize(200, 30)
          .setFont(parent.getSmallFont())
            .setAutoClear(false)
              .moveTo(sceneTag)
                ;
    cp5.addTextfield("TacticalName")
      .setPosition(680, 130)
        .setSize(200, 30)
          .setFont(parent.getSmallFont())
            .setAutoClear(false)
              .moveTo(sceneTag)
                ;
    cp5.addTextfield("EngineerName")
      .setPosition(680, 180)
        .setSize(200, 30)
          .setFont(parent.getSmallFont())
            .setAutoClear(false)
              .moveTo(sceneTag)
                ;

    cp5.addTextfield("CaptainName")
      .setPosition(680, 230)
        .setSize(200, 30)
          .setFont(parent.getSmallFont())
            .setAutoClear(false)
              .moveTo(sceneTag)
                ;
    cp5.addTextfield("GmName")
      .setPosition(680, 280)
        .setSize(200, 30)
          .setFont(parent.getSmallFont())
            .setAutoClear(false)
              .moveTo(sceneTag)
                ;

    cp5.addBang("SetNames")
      .setPosition(900, 302)
        .setSize(50, 50)
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("Set")
              .moveTo(sceneTag)
                ;
                
                
        
  }

  public void oscMessage(OscMessage msg) {

    if (msg.checkAddrPattern("/scene/launchland/grabberState") == true) {
      grabberState = msg.get(0).intValue() == 1 ? true : false;
    }
  }
  public void controlEvent(ControlEvent theControlEvent) {
    if (theControlEvent.getName().equals("BayDoors")) {
      // /scene/dockingBay 1
      OscMessage m  = new OscMessage("/scene/launchland/dockingBay");
      m.add(1);
      oscP5.send(m, myRemoteLocation);
    } 
    else if (theControlEvent.getName().equals("Bay Gravity")) {
      OscMessage m  = new OscMessage("/scene/launchland/bayGravity");
      m.add( (int)theControlEvent.getValue() );
      oscP5.send(m, myRemoteLocation);
    } 
    else if (theControlEvent.getName().equals("StartLaunch")) {
      OscMessage m  = new OscMessage("/scene/launchland/startLaunch");
      oscP5.send(m, myRemoteLocation);
    } 
    else if (theControlEvent.getName().equals("DockingClamp")) {
      OscMessage m  = new OscMessage("/scene/launchland/releaseClamp");

      oscP5.send(m, myRemoteLocation);
    } 
    else if (theControlEvent.getName().equals("SpawnMissile")) {
      OscMessage m  = new OscMessage("/scene/launchland/trainingMissiles");
      m.add( (int)theControlEvent.getValue() );
      oscP5.send(m, myRemoteLocation);
    } 
    else if (theControlEvent.getName().equals("HighlightGate")) {
      OscMessage m  = new OscMessage("/scene/launchland/targetGate");
      m.add( (int)theControlEvent.getValue() );
      oscP5.send(m, myRemoteLocation);
    } 
    else if (theControlEvent.getName().equals("LaunchOtherShip")) {

      OscMessage m  = new OscMessage("/scene/launchland/launchOtherShip");

      oscP5.send(m, myRemoteLocation);
    } 
    else if (theControlEvent.getName().equals("otherShipToGate")) {

      OscMessage m  = new OscMessage("/scene/launchland/otherShipToGate");

      oscP5.send(m, myRemoteLocation);
    } 
    else if (theControlEvent.getName().equals("otherShipHyperspace")) {

      OscMessage m  = new OscMessage("/scene/launchland/otherShipHyperspace");

      oscP5.send(m, myRemoteLocation);
    } else if(theControlEvent.getName().equals("GameWin")){
      OscMessage m  = new OscMessage("/game/gameWin");
      oscP5.send(m, myRemoteLocation);
    } else if(theControlEvent.getName().equals("DockingComp")){
      OscMessage m  = new OscMessage("/scene/launchland/dockingCompState");
      m.add( (int)theControlEvent.getValue() );
      oscP5.send(m, myRemoteLocation);
    } else if (theControlEvent.getName().equals("SetNames")) {
      OscMessage m = new OscMessage("/game/setNames");
      m.add(cp5.get(Textfield.class, "PilotName").getText());
      m.add(cp5.get(Textfield.class, "TacticalName").getText());
      m.add(cp5.get(Textfield.class, "EngineerName").getText());
      m.add(cp5.get(Textfield.class, "CaptainName").getText());
      m.add(cp5.get(Textfield.class, "GmName").getText());
      oscP5.send(m, myRemoteLocation);
    }
  }
}

