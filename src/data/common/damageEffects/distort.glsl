#version 130

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform int timer;
uniform bool boom;
uniform float damage;

uniform vec4 viewport;
uniform sampler2D texture;
uniform vec2 texOffset;

varying vec4 vertColor;
varying vec4 vertTexCoord;

out vec4 pixel;

const float pi = 3.141569;

float gauss(float x, const float height, const float a, const float b, const float offset) {
	return height * exp( - ( pow( x - a, 2 ) / 2 * pow( b, 2 ) ) ) + offset;
}

// http://stackoverflow.com/a/4275343/823542
float rand(vec2 co){
	return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453);
}

float desin(float val, float sinnable) {
	sinnable *= pi;
	sinnable *= mod( float(timer), 10);
	return val + sin(sinnable) * 0.1;
}

void main() {
	vec2 coords;

	coords = vertTexCoord.st;

	vec4 adjust = vertColor;

	const float scandist = 10;
	const float scanAmt = 4;
	float scanloc = mod(float(timer / 40), scandist);
	float cline = mod( (vertTexCoord.t / texOffset.t), scandist );

	// Constant damage screen distortion
	if (damage > 0) {
		float damageAmt = (float(damage) / 100);
		float idamage = 100 - damage;
		float idamageAmt = (float(idamage) / 100);
		// Skew it sideways
		float val = 0;
		// float val = coords.t;

		float height, a, b, offset;

		height = (damageAmt * damageAmt * 0.05) + rand(vec2(0, timer / 100)) * 0.05 * damageAmt;
		a = rand(vec2(1, timer / ( 10 * idamage) ) ) * 4 - 2;
		b = sqrt(.5);
		offset = 0;
		// TODO: Mix this with a much lower level one for a bump spike
		val = gauss( coords.t * 20 - 2, height, a, b, offset );

		height = .1;
		a = rand(vec2(1, timer / ( 10 * idamage) ) ) * 2 + 4;
		b = .2 * damageAmt;
		offset = -0.05;

		val += gauss( coords.t * 20 - 10, height, a, b, offset ) * damageAmt;

		val += ( (1 - (rand(vec2(coords.t, timer / 1000)) * 2)) / 10 ) * (0.02 * damageAmt);
		// val
		// val *= damageAmt;

		coords.s -= val;
	}

	// Active damage moving scanlines
	if ( boom && cline > scanloc - scanAmt && cline < scanloc + scanAmt ) {
		// Endarken the section
		adjust *= 0.8;
		// Skew it sideways
		coords.s -= (2 - coords.t * 4) / 4;
		// Sine wave distortion
		coords.s = desin( coords.s, coords.t);
	}

	// Wrap the coordinates
	if (coords.s < 0)
		coords.s += 1;
	if (coords.s > 1)
		coords.s -= 1;

	// Write it to the screen
	pixel = texture2D(texture, coords) * adjust;
}
