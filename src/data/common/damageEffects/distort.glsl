#version 130

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform int timer;

uniform sampler2D texture;

uniform vec4 viewport;
uniform ivec2 tiles;

uniform vec2 texOffset;

varying vec4 vertColor;
varying vec4 vertTexCoord;

const int tilesX = 5;
const int tilesY = 5;

out vec4 pixel;

// http://stackoverflow.com/a/4275343/823542
float rand(vec2 co){
	return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453);
}

float desin(float val, float sinnable) {
	sinnable *= 3.141569;
	sinnable *= mod( float(timer), 10);
	return val + sin(sinnable) * 0.1;// ( ( 1 + sin(sinnable) ) / 2 );
}

// 5x5 grid of distortions

///
// This figures out how much of the screen each section takes
//  up and then collapses that to be between 0 and 1.
vec2 sectionSize() {
	float
		w = viewport[2],
		h = viewport[3];
	return vec2( (w / tilesX) / w, (h / tilesY) / h );
}

ivec2 detectScreenSection() {
	vec2 secSize = sectionSize();
	float
		x = vertTexCoord.x,
		y = vertTexCoord.y,
		w = secSize.x,
		h = secSize.y;
	return ivec2(floor(x / w), floor(y / h));
}

bool shouldFlicker() {
	ivec2 section = detectScreenSection();
	bool
		inx = (tiles.x & (1 << section.x)) == 0,
		iny = (tiles.y & (1 << section.y)) == 0;
	return inx && iny;
}

// const float DISTORTION_AMT = 0.02;
vec2 calculateDistortion( vec2 coords ) {
	float
		x = rand(coords.st),
		y = rand(coords.ts);
	// x *= DISTORTION_AMT;
	// y *= DISTORTION_AMT;
	return vec2(x, y);
}


void main() {
	vec2 coords;
	vec4 adjust = vertColor;

	// Sine wave distortion
	coords = vec2( desin( vertTexCoord.s, vertTexCoord.t), vertTexCoord.t );

	// Show "static" in image
	if ( shouldFlicker() ) {
		coords = calculateDistortion( coords );
		adjust = vec4(3, 3, 3, 1);
	}

	// Write it to the screen
	pixel = texture2D(texture, coords) * adjust;
}
