package modconsole;

import procontroll.*;

import java.io.*;

import netP5.*;
import oscP5.*;

public class Joystick {


  ControllIO controll;
  ControllDevice device;
  ControllStick xyStick;
  ControllStick rotThrottleStick;
  ControllCoolieHat cooliehat;


  long lastUpdateTime = 0;
  long updateFreq = 100;
  public float throttle = 0;
  private boolean testMode = false;
  boolean state = true;
  OscP5 oscP5;
  
  float rollRate = 0.0f;
  ControllButton rollL, rollR;
  ControllSlider throtSlider;

  NetAddress myRemoteLocation;
  ModConsole parent;

  public Joystick(OscP5 p5, ModConsole parent, boolean testing) {
    oscP5 = p5;
    this.parent = parent;
    testMode = testing;
    myRemoteLocation = new NetAddress(parent.getServerAddress(), 19999);

    /*stick setup
     */
    if (!testMode) {
      controll = ControllIO.getInstance(parent);

      device = controll.getDevice("Controller (Joytech 360 pad)");
      device.setTolerance(0.05f);

      ControllSlider sliderX = device.getSlider("X Axis");
      ControllSlider sliderY = device.getSlider("Y Axis");
    //"Z Rotation
      ControllSlider sliderR = device.getSlider("X Rotation");
      ControllSlider sliderT = device.getSlider("Y Rotation");
      throtSlider = device.getSlider("Z Axis");

      xyStick = new ControllStick(sliderX, sliderY);
     // xyStick.setTolerance(0.4);
      rotThrottleStick = new ControllStick(sliderR, sliderT);
      //rotThrottleStick.setTolerance(0.2);

      cooliehat = device.getCoolieHat(10);
      
      rollL = device.getButton(4);
      rollR = device.getButton(5);
      
    }
  }

  void setEnabled(boolean state) {
    this.state = state;
    if (state == false) {
      OscMessage myMessage = new OscMessage("/control/joystick/state");

      myMessage.add(0f); 
      myMessage.add(0f);
      myMessage.add(0.0f);


      myMessage.add(0.0f);
      myMessage.add(0.0f);

      myMessage.add(0f);

      oscP5.send(myMessage, myRemoteLocation);
    }
  }

  void update() {
  if(testMode){ return;}
    if (lastUpdateTime + updateFreq < parent.millis() && state == true) {
      lastUpdateTime = parent.millis();
      OscMessage myMessage = new OscMessage("/control/joystick/state");
     
      if(state) {
       
        
        throttle = - throtSlider.getValue();
        if(throttle < 0){ throttle = 0; }
        if(rollR.getValue() > 0){
          rollRate = 1.0f;
        } else if (rollL.getValue() > 0){
          rollRate = -1.0f;
        } else {
          rollRate = 0.0f;
        }
        myMessage.add(xyStick.getX()); 
        myMessage.add(-xyStick.getY());
        myMessage.add(-rollRate);


        myMessage.add(rotThrottleStick.getX());
        myMessage.add(rotThrottleStick.getY());
        //println(cooliehat.getX());
        //myMessage.add(map(rotThrottleStick.getY(), -1.0, 1.0, 1.0, 0.0));
        myMessage.add(throttle);
      }
      oscP5.send(myMessage, myRemoteLocation);
    }
  }
}

