package com.aarribas.dtasim;

/**
 * @author andresaan
 *
 * This class represents the basic data for a traffic link
 * All members are publicly accessible as speed is the priority.
 * All member variables are doubles because this is the format from the .mat files
 */

public class TrafficLink {
	
	public double id;
	public double startNode;
	public double endNode;
	public double type;
	public double kJam;
	public double capacity;
	public double freeSpeed;
	public double length;

}
