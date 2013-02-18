package com.aarribas.dtasim;

import java.util.ArrayList;

/**
 * @author andresaan
 *
 * This class represents the whole basic data.
 * All members are publicly accessible as speed is the priority.
 */

public class TrafficData {
	
	public ArrayList<Double> timeSlices = new ArrayList<Double>();
	public ArrayList<TrafficNode> nodes = new ArrayList<TrafficNode>();
	public ArrayList<TrafficLink> links = new ArrayList<TrafficLink>();
	public ArrayList<double[][]> ODMatrices = new ArrayList<double[][]>();

}
