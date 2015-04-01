package common;

import java.awt.event.KeyEvent;

import processing.serial.Serial;

/* interface to hardware connected to machine 
 * must override the bufferComplete method 
 * */
public class HardwareController {

	protected Serial serialPort;
	protected boolean isKeyboard = false;
	protected String interfaceName = "";

	protected PlayerConsole parent;

	protected char[] serialBuffer = new char[50];
	protected String finalBufferContents;
	protected int bufPtr = 0;

	public HardwareController(String interfaceName, String port, int rate,
			PlayerConsole parent) {

		if (port.equals("Keyboard")) {
			isKeyboard = true;
		} else {
			if (parent.testMode == false) {
				serialPort = new Serial(parent, port, rate);
			}

		}

		this.interfaceName = interfaceName;
		this.parent = parent;

	}
	
	public void shutDown(){
		if(isKeyboard == false && parent.testMode == false){
			serialPort.stop();
			serialPort.dispose();
			
		}
	}

	/*
	 * take contents of buffer and convert to hardwareevent This needs to be
	 * overriden for each stations hardware as each thing produces its own mess
	 * of data
	 */
	protected void bufferComplete() {
		if (isKeyboard) {
			HardwareEvent h = new HardwareEvent();
			h.event = "KEY";
			h.value = serialBuffer[0];
			h.fromDevice = interfaceName;
			parent.hardwareEvent(h);
		}
	}

	public void keyPressed(KeyEvent ke) {
		if (isKeyboard) {
			HardwareEvent h = new HardwareEvent();
			h.event = "KEY";
			h.id = ke.getKeyCode();
			h.value = 1;
			h.fromDevice = interfaceName;
			parent.hardwareEvent(h);
		}

	}
	
	public void keyReleased(KeyEvent ke){
		if (isKeyboard) {
			HardwareEvent h = new HardwareEvent();
			h.event = "KEY";
			h.id = ke.getKeyCode();
			h.value = 0;
			h.fromDevice = interfaceName;
			parent.hardwareEvent(h);
		}
		
	}

	protected void sendSerial(String toSend) {
		if (parent.testMode == false && serialPort != null) {
			for (int i = 0; i < toSend.length(); i++) {
				serialPort.write(toSend.charAt(i));
			}
			serialPort.write(',');

		} else {
			Messages.println("sending {0} to serial", toSend);
		}
	}

	public void update() {
		if (!isKeyboard && !parent.testMode) {
			while (serialPort.available() > 0) {
				char c = serialPort.readChar();

				if (c == ',') {
					finalBufferContents = (new String(serialBuffer)).substring(0, bufPtr);
					
					bufferComplete();
					
					bufPtr = 0;
				} else {
					serialBuffer[bufPtr] = c;
					bufPtr++;
				}
			}
		}
	}

	public void mouseClicked(int button) {
		ConsoleLogger.log(this, "mx: " + parent.mouseX + " my: " + parent.mouseY);
		HardwareEvent h = new HardwareEvent();
		h.event = "MOUSECLICK";
		h.id = button;
		h.value = parent.mouseX << 16 | parent.mouseY;
		parent.hardwareEvent(h);
		
	}

}
