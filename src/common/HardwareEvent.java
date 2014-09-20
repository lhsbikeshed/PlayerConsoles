package common;

public class HardwareEvent {
	public String event;
	public Object data;
	
	public String originalData;
	public String fromDevice;
	public EventSource eventSource;
	
	public enum EventSource { KEYBOARD, SERIAL };
	
}
