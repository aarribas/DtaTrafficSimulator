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
	
	
	public void scaleNetwork(float scaleFactor){
		
		for(TrafficLink link : links){
			link.length = link.length * (double)scaleFactor;
		}
	}
	
	public void scaleDemand(float scaleFactor){
		
		for(double[][]ODMat : ODMatrices ){
			
			for(int i= 0 ; i < ODMat.length; i++){
				for(int j = 0; j < ODMat[i].length; j++){
					ODMat[i][j] = ODMat[i][j] *(double) scaleFactor;
				}
			}
		}
	}

}
