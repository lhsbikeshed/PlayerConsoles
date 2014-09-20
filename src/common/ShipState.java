package common;

import processing.core.PVector;
import common.Rot;


public class ShipState {

  public boolean poweredOn = true;
  public boolean poweringOn = false ;
  public boolean areWeDead = false;
  public String deathText = "";
  public boolean jumpState = false;

  public PVector shipPos = new PVector(0, 0, 0);
  public Rot shipRot = Rot.IDENTITY;
  public Rot lastShipRot = Rot.IDENTITY;
  public PVector shipVel = new PVector(0, 0, 0);

  public float shipVelocity = 0;
  public float lastShipVel = 0;

  public long lastTransformUpdate = 0;


  public ShipState() {
  };

  public void resetState() {
  }
}
