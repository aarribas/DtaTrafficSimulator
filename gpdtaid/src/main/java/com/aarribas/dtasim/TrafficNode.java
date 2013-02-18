package com.aarribas.dtasim;

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
	public double outgoingLinks;
	public double incomingLinks;

}
