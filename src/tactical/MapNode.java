package tactical;

import processing.core.PVector;

class MapNode {
		public int posx;
		public int posy;
		public PVector pos;				//position on map
		public String id;				//id that the user types
		public boolean visited = false;	//have we visited this node already while planning route
		public String routeTag = "";		//the route tag to transmit when setting route

		public MapNode() {
		}
	}