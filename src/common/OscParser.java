package common;

import common.util.Rot;

import oscP5.OscMessage;
import processing.core.PVector;

public class OscParser {

	public static Rot parseQuaternion(OscMessage m, int startIndex) {

		String s = m.get(startIndex).stringValue();
		String[] parts = s.split(":");
		if (parts[0].equals("quat")) {

			float w = Float.parseFloat(parts[1]);
			float x = Float.parseFloat(parts[2]);
			float y = Float.parseFloat(parts[3]);
			float z = Float.parseFloat(parts[4]);
			return new Rot(w, x, y, z, false);
		} else {
			System.out.println("NULL QUAT");
			return null;
		}
	}

	public static PVector parseVector(OscMessage m, int startIndex) {
		String s = m.get(startIndex).stringValue();
		String[] parts = s.split(":");
		if (parts[0].equals("vec3")) {

			float x = Float.parseFloat(parts[1]);
			float y = Float.parseFloat(parts[2]);
			float z = Float.parseFloat(parts[3]);
			return new PVector(x, y, z);
		} else {

			return null;
		}
	}
}
