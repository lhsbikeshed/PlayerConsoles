#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform int timer;

uniform sampler2D texture;

uniform vec2 texOffset;

varying vec4 vertColor;
varying vec4 vertTexCoord;

float desin(float val, float sinnable) {
	sinnable *= 3.141569;
	sinnable *= timer % 10;
	return val + sin(sinnable) * 0.1;// ( ( 1 + sin(sinnable) ) / 2 );
}

void main() {
	// gl_FragColor = vertTexCoord;
	vec2 coord = vec2( desin( vertTexCoord.s, vertTexCoord.t), vertTexCoord.t );
	// gl_FragColor = vec4(coord.x, 0, vertTexCoord.s, 0);
	gl_FragColor = texture2D(texture, coord) * vertColor;
}
