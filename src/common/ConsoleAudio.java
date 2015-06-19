package common;

import java.util.Hashtable;

import processing.core.PApplet;
import ddf.minim.AudioOutput;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import ddf.minim.ugens.Oscil;
import ddf.minim.ugens.Waves;

public class ConsoleAudio {

	PlayerConsole parent;
	Minim minim;

	public boolean muteBeeps = false;
	Hashtable<String, AudioPlayer> audioList;

	AudioPlayer[] beepList = new AudioPlayer[4];
	// tone generation
	AudioOutput toneOutput;
	Oscil fm;
	Oscil wave;

	Oscil dialWave = new Oscil(440, 0.8f, Waves.SINE);
	
	float pan = 0.0f;

	public ConsoleAudio(PlayerConsole parent, Minim minim, float pan) {
		this.parent = parent;
		this.minim = minim;
		this.pan = pan;
		loadSounds();

		// set up tone gen
		toneOutput = minim.getLineOut();
		wave = new Oscil(200, 0.8f, Waves.TRIANGLE);
		fm = new Oscil(10, 2, Waves.SINE);
		// set the offset of fm so that it generates values centered around 200
		// Hz
		fm.offset.setLastValue(200);
		// patch it to the frequency of wave so it controls it
		fm.patch(wave.frequency);
		// and patch wave to the output

		wave.patch(toneOutput);
		toneOutput.setPan(1.0f);
		setToneState(false);
	}

	/* does a given name exist? */
	public boolean clipExists(String name) {
		if (audioList.get(name) != null) {
			return true;
		} else {
			return false;
		}
	}

	private void loadSounds() {
		// load sounds from soundlist.cfg
		audioList = new Hashtable<String, AudioPlayer>();
		String lines[] = parent.loadStrings(parent.getConsoleName()
				+ "/audio/soundlist.cfg");
		ConsoleLogger.log(this, "Loading " + lines.length + " SFX from "
				+ parent.getConsoleName() + "/audio/soundlist.cfg");
		for (int i = 0; i < lines.length; i++) {
			String[] parts = lines[i].split("=");
			if (parts.length == 2 && !parts[0].startsWith("#")) {
				ConsoleLogger.log(this, "loading: " + parts[1]);
				AudioPlayer s = minim.loadFile(parent.getConsoleName()
						+ "/audio/" + parts[1], 512);
				// move to left channel
				s.setPan(pan);
				audioList.put(parts[0], s);

			}

		}

		for (int i = 0; i < beepList.length; i++) {

			beepList[i] = minim
					.loadFile("common/audio/buttonBeep" + i + ".wav");
			beepList[i].setPan(pan);
		}

	}
	
	public void setPan(float pan){
		for(AudioPlayer ap : audioList.values()){
			ap.setPan(pan);
		}
		for(AudioPlayer ap : beepList){
			ap.setPan(pan);;
		}
	}
	

	public void playClip(String name) {
		AudioPlayer c = audioList.get(name);
		if (c != null ) {
			if(c.isPlaying() == false){
				c.setPan(pan);
				c.rewind();
				c.play();
				
			} 
		} else {
			ConsoleLogger.log(this, "ALERT: tried to play " + name
					+ " but not found");
		}

	}

	public void playClip(String name, float pan) {
		if (!parent.getShipState().poweredOn) {
			return;
		}
		playClipForce(name, pan);
	}

	/* forces a sound to play even when ship os powered off */
	public void playClipForce(String name) {

		playClipForce(name, 1.0f);
	}

	public void playClipForce(String name, float pan) {
		AudioPlayer c = audioList.get(name);
		if (c != null) {
			c.setPan(pan);
			c.rewind();
			c.play();
		} else {
			ConsoleLogger.log(this, "ALERT: tried to play " + name
					+ " but not found");
		}
	}

	public void randomBeep() {
		if (!parent.getShipState().poweredOn) {
			return;
		}
		if(muteBeeps == false){
			int rand = PApplet.floor(parent.random(beepList.length));
			while (beepList[rand].isPlaying()) {
				rand = PApplet.floor(parent.random(beepList.length));
			}
			beepList[rand].rewind();
			beepList[rand].play();
		}
	}

	public void setToneState(boolean state) {
		if (state) {
			wave.patch(toneOutput);
		} else {
			wave.unpatch(toneOutput);
		}
	}

	// x should range from 220 - 50;
	// y should range from 0.1 - 100
	public void setToneValue(float x, float y) {
		fm.frequency.setLastValue(y);
		fm.amplitude.setLastValue(x);
	}

}
