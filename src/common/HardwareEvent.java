package common;

public class HardwareEvent {
	public enum EventSource {
		KEYBOARD, SERIAL
	}

	public String event;

	public Object data;
	public String originalData;
	public String fromDevice;

	public EventSource eventSource;;

}
