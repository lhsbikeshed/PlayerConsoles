package common;
import java.util.Hashtable;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

public class ConsoleAudio {

	PlayerConsole parent;
  Minim minim;
  Hashtable<String, AudioPlayer> audioList;

  public ConsoleAudio(PlayerConsole parent, Minim minim) {
	  this.parent = parent;
    this.minim = minim;
    loadSounds();
  }

  private void loadSounds() {
    //load sounds from soundlist.cfg
    audioList = new Hashtable<String, AudioPlayer>();
    String lines[] = parent.loadStrings("audio/soundlist.cfg");
    ConsoleLogger.log(this, "Loading " + lines.length + " SFX");
    for (int i = 0 ; i < lines.length; i++) {
      String[] parts = lines[i].split("=");
      if(parts.length == 2 && !parts[0].startsWith("#")){
    	  ConsoleLogger.log(this, "loading: " + parts[1]);
        AudioPlayer s = minim.loadFile("audio/" + parts[1], 512);
        //move to left channel
        s.setPan(-1.0f);
        audioList.put(parts[0], s);
       
      }

    }
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

