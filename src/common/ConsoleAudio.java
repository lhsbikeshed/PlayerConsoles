package common;
import java.util.Hashtable;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

public class ConsoleAudio {

	PlayerConsole parent;
  Minim minim;
  
  
  Hashtable<String, AudioPlayer> audioList;
  
  AudioPlayer[] beepList = new AudioPlayer[4];
  
  
  public ConsoleAudio(PlayerConsole parent, Minim minim) {
	  this.parent = parent;
    this.minim = minim;
    loadSounds();
  }

  private void loadSounds() {
    //load sounds from soundlist.cfg
    audioList = new Hashtable<String, AudioPlayer>();
    String lines[] = parent.loadStrings(parent.getConsoleName() + "/audio/soundlist.cfg");
    ConsoleLogger.log(this, "Loading " + lines.length + " SFX from " + parent.getConsoleName() + "/audio/soundlist.cfg");
    for (int i = 0 ; i < lines.length; i++) {
      String[] parts = lines[i].split("=");
      if(parts.length == 2 && !parts[0].startsWith("#")){
    	  ConsoleLogger.log(this, "loading: " + parts[1]);
        AudioPlayer s = minim.loadFile(parent.getConsoleName() + "/audio/" + parts[1], 512);
        //move to left channel
        s.setPan(-1.0f);
        audioList.put(parts[0], s);
       
      }

    }
    
    for(int i = 0; i < beepList.length; i++){
        
        beepList[i] = minim.loadFile(parent.getConsoleName() + "/audio/buttonBeep" + i + ".wav");
        beepList[i].setPan(1.0f);
      }
    
    
  }
  public void randomBeep(){
	    if(!parent.getShipState().poweredOn){
	      return;
	    }
	    int rand = parent.floor(parent.random(beepList.length));
	    while(beepList[rand].isPlaying()){
	      rand = parent.floor(parent.random(beepList.length));
	    }
	    beepList[rand].rewind();
	    beepList[rand].play();
	  }
  public void playClip(String name) {
    AudioPlayer c = audioList.get(name);
    if(c != null){
      c.setPan(-1.0f);
      c.rewind();
      c.play();
    } else {
    	ConsoleLogger.log(this, "ALERT: tried to play " + name + " but not found");
    }
      
  }
}

