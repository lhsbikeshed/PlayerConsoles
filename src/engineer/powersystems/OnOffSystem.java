package engineer.powersystems;

import common.PlayerConsole;

import processing.core.PImage;
import processing.core.PVector;

public class OnOffSystem extends SubSystem {

  public OnOffSystem(PlayerConsole parent, String name, PVector pos, PImage p) {
    super(parent, name, pos, p);
    maxHealth = 40;
    health = maxHealth;
  }

  public void createFailure() {
    targetState = 1- currentState;
  }

  public String getPuzzleString() {
    if (targetState == 0) {
      return "Turn " + name + " on";
    } 
    else {
      return "Turn " + name + " off";
    }
  }
}