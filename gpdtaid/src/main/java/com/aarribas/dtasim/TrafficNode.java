package com.aarribas.dtasim;

import java.util.ArrayList;

/**
 * @author andresaan
 *
 * This class represents the basic data for a traffic node.
 * All members are publicly accessible as speed is the priority.
 * All member variables are doubles because this is the format from the .mat files
 */


public class TrafficNode {
	public double id;
	public double x;
	public double y;
	public ArrayList<TrafficLink> outgoingLinks;
	public ArrayList<TrafficLink> incomingLinks;

}
