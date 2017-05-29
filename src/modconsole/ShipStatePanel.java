package modconsole;

import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.*;
import controlP5.*;

public class ShipStatePanel extends PanelSet {

  //states
  public boolean reactorState = false;
  public boolean rodState = false;
  public float hull = 0;
  public float jumpCharge = 0;
  public float oxygenLevel = 0;
  public int undercarriageState = 0;
  public int failureCount = 0;
  private String[] undercarriageStrings = {
    "up", "down", "Lowering..", "Raising.."
  };

  public boolean canClampBeEnabled = false;
  public boolean canJump = false;
  public boolean reactorOn = false;

  String incomingCallFreq = "";
  boolean incomingCall = false;



  //GUI crap
  DropdownList bgList, lightingList;
  Slider chromaSlider, lowerSlider, upperSlider, thSlider;

  String[] bangList = { 
    "Start\r\nJump", "Damage\r\nShip", "smoke", "Reactor\r\nFailure", "Self\r\nDestruct", "stop SD", "Fix Stuck\r\nBoot?"
  };
  String[] bangMapping = {
    "/system/jump/startJump", "/ship/damage", "/lulz/smoke", "/system/reactor/fail", "/system/reactor/overload", "/system/reactor/overloadinterrupt", 
    "/system/boot/justFuckingBoot"
  };
  /* toggle buttons and their osc messages */
  String[] toggleList = { 
    "Reactor\r\nState", "Propulsion\r\nState", "JumpState", "Weapon State", "BlastShield", "Enable\r\nautopilot", 
    "Tactical power", "Engineer power", "pilot Power", "comms power", 
    "Undercarriage", "Engineer\r\nFailures?", "CablePuzzle", 
  };
  String[] toggleMapping = {
    "/system/reactor/setstate", "/system/propulsion/state", "/system/jump/state", "/system/targetting/changeWeaponState", "/system/misc/blastShield", "/system/control/controlState", 
    "/tactical/powerState", "/engineer/powerState", "/pilot/powerState", "/comms/powerState", "/system/undercarriage/state", "/system/powerManagement/failureState", 
    "/system/cablePuzzle/startPuzzle"
  };
  boolean[] defaultStates = {
    false, false, false, false, true, false, true, 
    false, false, false, false, true, false
  };

  Knob engineerDiffKnob;


  boolean ready = false;
  ModConsole parent;

  public ShipStatePanel(String name, ModConsole parent, OscP5 p5, ControlP5 cp5) {
    super(name, parent, p5, cp5);
    this.parent = parent;
  }

  public void initGui() {

    // docking clamp
    // light
    // prop state
    // jump state
    // jump charge and jump button readyness
    // initiate jump

    //video calling controls
    cp5.addBang("VideoCallStart")
      .setPosition(20, 400)
        .setSize(50, 50)
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("Start Call")     
              ;
    cp5.addBang("VideoCallEnd")
      .setPosition(80, 400)
        .setSize(50, 50)
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("End Call")     
              ;

    cp5.addBang("AudioCall")
      .setPosition(150, 400)
        .setSize(40, 50)
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("AUDIOCALL")     
              ;


    /* interior lighting control 
     char[] lightMap = {'i', 'w', 'r', 'b'}; */
    String[] lightNames  = {
      "Idle", "Warp", "red alert", "briefing"
    };

    lightingList = cp5.addDropdownList("Cabin Lighting")
      .setPosition(880, 480)
        .setSize(120, 100)
          .setItemHeight(20)
            .setBarHeight(20)
              .setColorActive(parent.color(255))
                .setColorForeground(parent.color(255, 100, 0))
                  ;
    for (int i = 0; i < lightNames.length; i++) {
      ListBoxItem lbi = lightingList.addItem(lightNames[i], i);
    }
    cp5.addBang("SetLights")
      .setPosition(810, 460)
        .setSize(50, 25)
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("Set Lights")     
              ;

    cp5.addToggle("LightPower")
      .setPosition(750, 460)
        .setSize(50, 25)
          .setLabel("light power")
            ;

    cp5.addToggle("SeatbeltLight")
      .setPosition(750, 540)
        .setSize(50, 25)
          .setLabel("seatbelts")
            ;

    cp5.addToggle("PrayLight")
      .setPosition(840, 540)
        .setSize(50, 25)
          .setLabel("pray Light")
            ;

    //bang list
    for (int i = 0; i < bangList.length; i++) {
      cp5.addBang(bangList[i])
        .setPosition(20 + i * 55, 500)
          .setSize(35, 35)
            .setTriggerEvent(Bang.RELEASE)
              .setLabel(bangList[i])     
                ;
    }
    int cx = -40;
    int cy = 600;
    for (int i = 0; i < toggleList.length; i++) {
      // system toggles
      cx += 70;
      if (cx > 500) {
        cx = 30;
        cy += 50;
      }
      cp5.addToggle(toggleList[i])
        .setPosition(cx, cy)
          .setSize(50, 20)
            .setState(defaultStates[i])
              ;
    }

    //player killer
    cp5.addTextfield("DeathReason")
      .setPosition(720, 390)
        .setSize(200, 30)
          .setFont(parent.createFont("arial", 12))
            .setAutoClear(false)
              ;

    cp5.addBang("KillShip")
      .setPosition(955, 390)
        .setSize(50, 50)
          .setTriggerEvent(Bang.RELEASE)
            .setLabel("Kill The Ship")     
              ;

    engineerDiffKnob = cp5.addKnob("Engineer\r\nDifficulty")
      .setRange(1, 10)
        .setValue(1)
          .setPosition(920, 650)
            .setRadius(30)
              .setNumberOfTickMarks(10)
                .setTickMarkLength(1)
                  .snapToTickMarks(true)
                    .setColorForeground(parent.color(255))
                      .setColorBackground(parent.color(0, 160, 100))
                        .setColorActive(parent.color(255, 255, 0))
                          .setDragDirection(Knob.HORIZONTAL)

                            ;


    ready = true;
  }

  @Override
  public void draw(ModConsole parent) {
   
    //y=380
    parent.textFont(parent.getGlobalFont(), 12);
    parent.pushMatrix();
    parent.translate(10, 800);
    parent.text("reactor On?: " + reactorState, 128, 670);
    parent.text("Can jump? : " + canJump, 128, 690);

    parent.text("Hull Health: " + hull, 0, 0);
    parent.text("o2 Level: " + oxygenLevel, 0, 20);
    parent.text("Jump Charge: " + jumpCharge, 0, 40);
    parent.text("Undercarriage: " + undercarriageStrings[undercarriageState], 0, 60);
    parent.text("can clamp be used: " + canClampBeEnabled, 0, 80);
    if (failureCount >= 6) {
    	parent.fill(255, 0, 0);
    } 
    else {
    	parent.fill(255, 255, 255);
    }
    parent.text("failed reactor systems: " + failureCount, 300, 0);
    parent.popMatrix();

    if (incomingCall) {
    	parent.text("Incoming Call: " + incomingCallFreq + "Hz", 190, 431);
    }
  }


  public void oscMessage(OscMessage msg) {

    if (msg.checkAddrPattern("/display/captain/dialRequest")) {
      incomingCall = true;
      incomingCallFreq = msg.get(0).stringValue();
    } 
    else if (msg.checkAddrPattern("/display/captain/dialNoResponse")) {
      incomingCall = false;
    }
  }

  public void controlEvent(ControlEvent theControlEvent) {
    if (!ready) { 
      return;
    }
    String name = theControlEvent.getName();

    /* do the toggle list first */
    try {
      Toggle t = (Toggle)theControlEvent.getController();

      for (int i = 0; i < toggleList.length; i++) {
        if (toggleList[i].equals(name)) {
          //get the state of the toggle
          int state = (int)theControlEvent.getValue();
          OscMessage msg = new OscMessage(toggleMapping[i]);

          msg.add(state);
          oscP5.send(msg, parent.getNetAddress());
        }
      } 

      if (name.equals("LightPower")) {
        boolean state = (int)theControlEvent.getValue() == 1 ? true : false;
        //setLightState(state);
        OscMessage msg = new OscMessage("/system/effect/lightingPower");

        msg.add(state == true ? 1 : 0);
        oscP5.send(msg, parent.getNetAddress());
      }
      if (name.equals("PrayLight")) {
        boolean state = (int)theControlEvent.getValue() == 1 ? true : false;
        //setLightState(state);
        OscMessage msg = new OscMessage("/system/effect/prayLight");

        msg.add(state == true ? 1 : 0);
        oscP5.send(msg, parent.getNetAddress());
      }
      if (name.equals("SeatbeltLight")) {
        boolean state = (int)theControlEvent.getValue() == 1 ? true : false;
        //setLightState(state);
        OscMessage msg = new OscMessage("/system/effect/seatbeltLight");

        msg.add(state == true ? 1 : 0);
        oscP5.send(msg, parent.getNetAddress());
      }
    } 
    catch (ClassCastException e) {
    }

    /* do the bang list first */
    try {
      Bang t = (Bang)theControlEvent.getController();

      for (int i = 0; i < bangList.length; i++) {
        if (bangList[i].equals(name)) {          
          OscMessage msg = new OscMessage(bangMapping[i]);
          oscP5.send(msg, parent.getNetAddress());
        }
      } 

      if (name.equals("KillShip") ) {
        OscMessage msg = new OscMessage("/game/KillPlayers");

        msg.add(cp5.get(Textfield.class, "DeathReason").getText());
        oscP5.send(msg, parent.getNetAddress());
      } 
      else if (name.equals("SetLights")) {
        int ind =(int) lightingList.getValue();
        //setLightMode(ind);
        OscMessage msg = new OscMessage("/system/effect/lightingMode");

        msg.add(ind);
        oscP5.send(msg, parent.getNetAddress());
      } 
      else if (name.equals("VideoCallStart")) {
       
        OscMessage msg = new OscMessage("/clientscreen/CommsStation/incomingCall");

        msg.add(0);  //0 for video
        oscP5.send(msg, parent.getNetAddress());
      } 
      else if (name.equals("VideoCallEnd")) {
        OscMessage msg = new OscMessage("/clientscreen/CommsStation/hangUp");
        oscP5.send(msg, parent.getNetAddress());
        
      } else if (name.equals("AudioCall")){
        OscMessage msg = new OscMessage("/clientscreen/CommsStation/incomingCall");

        msg.add(1);   //for audio
        oscP5.send(msg, parent.getNetAddress());
      }
    } 
    catch (ClassCastException e) {
    }



    try {
      Knob b = (Knob)theControlEvent.getController();
      //  println((int)b.value());
      OscMessage msg = new OscMessage("/system/powerManagement/failureSpeed");

      msg.add((int)b.value());
      oscP5.send(msg, parent.getNetAddress());
    }
    catch (ClassCastException e) {
    }
  }
}

