package pilot;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import procontroll.ControllCoolieHat;
import procontroll.ControllDevice;
import procontroll.ControllIO;
import procontroll.ControllSlider;
import procontroll.ControllStick;

import common.PlayerConsole;

public class Joystick {

	ControllIO controll;
	ControllDevice device;
	ControllStick xyStick;
	ControllStick rotThrottleStick;
	ControllCoolieHat cooliehat;

	long lastUpdateTime = 0;
	long updateFreq = 100;
	public float throttle = 0;
	private boolean testMode = true;
	boolean state = true;
	OscP5 oscP5;

	NetAddress myRemoteLocation;
	PlayerConsole parent;

	public Joystick(OscP5 p5, PlayerConsole parent, boolean testing) {
		this.parent = parent;
		oscP5 = p5;
		testMode = testing;
		myRemoteLocation = new NetAddress(parent.getServerIP(), 19999);

		/*
		 * stick setup
		 */
		if (!testMode) {
			controll = ControllIO.getInstance(parent);
			// Dean Camera LUFA Joystick wFFB
			// device = controll.getDevice("Dean Camera LUFA Joystick wFFB");

			device = controll.getDevice("T.Flight Stick X");
			device.setTolerance(0.05f);

			ControllSlider sliderX = device.getSlider("X Axis");
			ControllSlider sliderY = device.getSlider("Y Axis");
			// "Z Rotation
			ControllSlider sliderR = device.getSlider("Z Rotation");
			ControllSlider sliderT = device.getSlider(2);

			xyStick = new ControllStick(sliderX, sliderY);
			xyStick.setTolerance(0.4f);
			rotThrottleStick = new ControllStick(sliderR, sliderT);
			rotThrottleStick.setTolerance(0.2f);

			cooliehat = device.getCoolieHat(12);
		}
	}

	public void setEnabled(boolean state) {
		this.state = state;
		if (state == false) {
			OscMessage myMessage = new OscMessage("/control/joystick/state");

			myMessage.add(0);
			myMessage.add(0);
			myMessage.add(0.0f);

			myMessage.add(0.0f);
			myMessage.add(0.0f);

			myMessage.add(0);

			oscP5.send(myMessage, myRemoteLocation);
		}
	}

	public void update() {
		if (testMode) {
			return;
		}
		if (lastUpdateTime + updateFreq < parent.millis() && state == true) {
			lastUpdateTime = parent.millis();
			OscMessage myMessage = new OscMessage("/control/joystick/state");
			if (testMode) {
				/*
				 * myMessage.add(map(mouseX, 0, width, -1.0, 1.0));
				 * myMessage.add(-map(mouseY, 0, height, -1.0, 1.0));
				 * myMessage.add(0.0f);
				 * 
				 * 
				 * myMessage.add(0.0f); myMessage.add(0.0f);
				 * //myMessage.add(map(rotThrottleStick.getY(), -1.0, 1.0, 1.0,
				 * 0.0)); myMessage.add(throttle);
				 */
			} else {

				myMessage.add(xyStick.getX());
				myMessage.add(-xyStick.getY());
				myMessage.add(-rotThrottleStick.getX());

				myMessage.add(cooliehat.getX());
				myMessage.add(cooliehat.getY());
				// println(cooliehat.getX());
				// myMessage.add(map(rotThrottleStick.getY(), -1.0, 1.0, 1.0,
				// 0.0));
				myMessage.add(throttle);
			}
			oscP5.send(myMessage, myRemoteLocation);
		}
	}
}
