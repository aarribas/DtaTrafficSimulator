package com.aarribas.traffictools;

import java.util.ArrayList;
import java.util.Scanner;

import com.aarribas.dtasim.TrafficLink;

public class TravelTimeManager {
	
	public static double computeTravelTimeForGivenCost(double[] travelCosts, double t, double tEnd, double tStep){

		double travelCost; 
		
		//index of the timeStep (time click) prior to t
		int indexTimeBefore = ((int)(t/tStep));

		if(indexTimeBefore > travelCosts.length - 1){
			//if we have passed the last time click the travelTime is the last travel time
			//as in matlab code.
			travelCost = travelCosts[travelCosts.length-1];
		}
		else{

			//proportion of time between t and previous time click
			double tBetween  = (t - (indexTimeBefore * tStep)) / tStep;

			//should always work if I have an exception here I made the wrong assumption regarding possible indexTimeBefore

			if(travelCosts[indexTimeBefore] == Double.MAX_VALUE ){
				travelCost =  Double.MAX_VALUE;
			}
			else{
				travelCost = travelCosts[indexTimeBefore] + tBetween * (travelCosts[indexTimeBefore + 1] - travelCosts[indexTimeBefore]);	
			}

		}
		
//		System.out.println(indexTimeBefore);
//		System.out.println(indexTimeBefore + 1);
//		System.out.println(travelCost);
//		Scanner scan = new Scanner(System.in);
//		scan.nextLine();
		return travelCost;
		
	

	}
	
	public static ArrayList<double[]> computeTravelCostsForGivenSpeeds(ArrayList<TrafficLink>  links, ArrayList<double[]> speeds){
		
		ArrayList<double[]> travelCosts = new 	ArrayList<double[]>();
		//for each link compute a travel cost
		for(int i = 0; i<links.size(); i++){
			
			double[] travelCost = speeds.get(i);
			
			for(int j =0; j< travelCost.length; j++){
				if(travelCost[j] == 0){
					
					travelCost[j] = Double.MAX_VALUE;
				}
				else if(travelCost[j] == Double.MAX_VALUE){
					
					travelCost[j] = 0;
				}
				else{
					travelCost[j] = links.get(i).length / travelCost[j];
				}
				
			}
			//add the travelcost
			travelCosts.add(travelCost);
		}
		
		return travelCosts;
	}
	
	public static double computeTravelTimeNode2NodeForGivenCost(ArrayList<double[]> travelCosts, int[] linkIndexes, int startIndex, int endIndex, double timeClick, double tEnd, double tStep){
		
		double travelTime = 0;
		
		//this is taken adapted directly from matlab code. TODO verify the correctness of visiting the route this way.
		for(int i = startIndex; i>endIndex-1; i--){
			int linkIndex = linkIndexes[i];
			
			//get traveltime along the link
			double travelTimeForCost = computeTravelTimeForGivenCost(travelCosts.get(linkIndex), timeClick*tStep - travelTime , tEnd, tStep);
			if(travelTimeForCost == Double.MAX_VALUE){
				travelTime  = Double.MAX_VALUE;
			}
			else{
				travelTime = travelTime + travelTimeForCost;
			}
			
		}
		
		return travelTime;
	}


}
