package engineer.powersystems;

import common.PlayerConsole;

import processing.core.PImage;
import processing.core.PVector;

public class MultiValueSystem extends SubSystem {


  int maxVals = 1;


  public MultiValueSystem(PlayerConsole parent, String name, PVector pos, PImage p, int maxVals) {
    super(parent, name, pos, p);
    this.maxVals = maxVals;
    maxHealth = 120;
    health = maxHealth;
  }

  public void createFailure() {
    int ra = parent.floor(parent.random(maxVals));
    while (ra != targetState) {
      targetState = ra;
      ra = parent.floor(parent.random(maxVals));
    }
  }

  public void toggleState() {
    if (isBroken()) return;
    currentState ++;
    currentState %= maxVals;
  }

  public String getPuzzleString() {
    return "Set " + name + " to " + (targetState + 1);
  }
}