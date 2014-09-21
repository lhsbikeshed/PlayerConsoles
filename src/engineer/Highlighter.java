package engineer;
import common.PlayerConsole;

import processing.core.*;

public class Highlighter {
  int time = 10;
  PVector startSize = new PVector(400, 400);
  PVector endSize = new PVector (100, 100);
  PVector centre = new PVector(10, 10);

  boolean running = false;
  int hangRoundTime = 1000;

  long startTime = 0;
  PlayerConsole parent;
  public Highlighter(PlayerConsole parent, PVector centre, PVector startSize, PVector endSize, int time, int hang) {
	 this.parent = parent;
    this.centre = centre;
    this.startSize = startSize;
    this.endSize = endSize;
    this.time = time;
    this.hangRoundTime = hang;

    running = true;
    startTime = parent.millis();
  }

  public boolean isDone() {
    if (startTime + time + hangRoundTime < parent.millis()) {
      return true;
    } 
    else {
      return false;
    }
  }

  public void update() {
    if (running) {
      int w = 0;
      int h = 0;

      if (startTime + time < parent.millis()) {
        w = (int)endSize.x;
        h = (int)endSize.y;
      } 
      else if (startTime + time > parent.millis()) {

        float t = (parent.millis() - startTime) / (float)time;
        w = (int)parent.lerp( startSize.x, endSize.x, t);
        h = (int)parent.lerp( startSize.y, endSize.y, t);
      } 
      parent.noFill();
      int r = (int)parent.map(parent.sin(parent.millis() / 100.0f), -1.0f, 1.0f, 120, 255);
      parent.stroke(r,0,0);
      parent.strokeWeight(4);
      for(int i = 0; i < 4; i++){
        parent.rect( centre.x - w/2 - i * 5, centre.y - h/2 -  i * 5, w + i * 10, h + i * 10);
      }
    }
  }
}




