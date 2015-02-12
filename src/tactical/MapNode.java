package tactical;

import processing.core.PVector;

class MapNode {
		public int posx;
		public int posy;
		public PVector pos;				//position on map
		public String id;				//id that the user types
		public String displayName;		//name to show on map screen
		public boolean visited = false;	//have we visited this node already while planning route
		public String interestingThing = "";		//the route tag to transmit when passing through this 
		public boolean planned = false;				//is the interestingThing here planned, i.e. can i set a route to it in the plotter?
		public boolean ignoreOnReturn = false;		//ignore scene event when return journey flag is set

		public MapNode() {
		}
	}