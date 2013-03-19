package com.aarribas.traffictools;

import java.util.ArrayList;
import java.util.Arrays;

import com.aarribas.dtasim.TrafficLink;

public class CumulativeBasedCalculator {

	public enum SIM_FLOW_OPTION{
		MIDDLE,
		UPSTREAM,
		DOWNSTREAM,
		FORWARD
	}

	static public double calculateCumulativeTime(double[] cumulative, double value, double tEnd, double tStep){
		double time = 0;
		int tBeforeClick = -1;
		int tAfterClick = -1;
		
		for(int cumulativeIndex = 0; cumulativeIndex < cumulative.length; cumulativeIndex++){
			if(value<=cumulative[cumulativeIndex]){
				tBeforeClick = cumulativeIndex;
			}
			
			if(tAfterClick ==-1 && value>cumulative[cumulativeIndex]){
				tAfterClick = cumulativeIndex;
			}
		}
		
		if(tBeforeClick == -1){
			time = 0;
		}
		else if(tAfterClick == -1 && tBeforeClick < (int)(tEnd/tStep)-1){
			double cInt = (value - cumulative[tBeforeClick]) / (cumulative[tBeforeClick+1] - cumulative[tBeforeClick]);
			time = tBeforeClick*tStep + cInt*(tStep*(tBeforeClick+1) - tBeforeClick*tStep);
		}
		else if(tAfterClick == -1){
			time = Double.MAX_VALUE;
		}
		else{
			double cInt = (value - cumulative[tBeforeClick]) / (cumulative[tAfterClick] - cumulative[tBeforeClick]);
			time = tBeforeClick*tStep + cInt*(tStep*(tAfterClick) - tBeforeClick*tStep);
		}
		
		return time;
	}

	static public double calculateCumulativeValue(double cumulatives[], double t, double tStep){

		//Attention: in the matlab code (findcumulative) the calculation o tbefore and tafter is inconsistent with the calculation in findTravelTimes
		//I decided to make it consistent here

		int tClickBefore = (int)(t/tStep);

		if(tClickBefore == cumulatives.length-1){
			//if we have reached the last time click return the last cumulative
			return cumulatives[cumulatives.length-1];
		}

		//if the we are at the very beginning the value is 0
		if(tClickBefore == 0){
			return 0;
		}
		else{
			int tClickAfter = tClickBefore + 1;
			double tempFasterTime = tClickBefore*tStep;
			double tempFasterCumulative = cumulatives[tClickBefore];
			double tInt = (t-tempFasterTime)/((tClickAfter*tStep)-tempFasterTime);
			return tempFasterCumulative + tInt * (cumulatives[tClickAfter]-tempFasterCumulative);

		}

	}



	static public double[][] calculateCumulativeToSpeeds(ArrayList<TrafficLink>links, double tEnd, double tStep){

		double[][] simSpeeds = new double[links.size()][(int)(tEnd/tStep)];
		
		for(int timeClick=0; timeClick<(int)(tEnd/tStep); timeClick++){
			for(int linkIndex = 0; linkIndex<links.size(); linkIndex++){
				TrafficLink link = links.get(linkIndex);
				//Initialise to freeSpeed for each instant
				Arrays.fill(simSpeeds[linkIndex], link.freeSpeed);
				double time = calculateCumulativeTime(link.downStreamCumulative, link.upStreamCumulative[timeClick], tEnd, tStep);
				
				//compute simspeed depending on the simulation time
				if(time > tEnd)
				{
					simSpeeds[linkIndex][timeClick] = 0;
				}
				else{
					simSpeeds[linkIndex][timeClick] = link.length/(time - timeClick*tStep);
				}
				
				//adjust the simspeed to freespeed given limit conditions
				if(simSpeeds[linkIndex][timeClick] <= 0 || link.downStreamCumulative[timeClick] == 0){
					//limit the speed to the freespeed
					simSpeeds[linkIndex][timeClick] = link.freeSpeed;
				}
				else if(simSpeeds[linkIndex][timeClick]>link.freeSpeed){
					//limit the speed to the freespeed
					simSpeeds[linkIndex][timeClick] = link.freeSpeed;
				}
			}

		}
		return simSpeeds;
	}

	static public double[][] calculateCumulativeToFlows(ArrayList<TrafficLink> links, SIM_FLOW_OPTION option, double tEnd, double tStep){

		double[][] simFlows = new double[links.size()][(int)(tEnd/tStep)];


		for(int linkIndex = 0; linkIndex<links.size(); linkIndex++){
			TrafficLink link = links.get(linkIndex);
			for(int timeClick=0; timeClick<(int)(tEnd/tStep); timeClick++){
				//calculate times
				double t1;
				double t2;
				if((timeClick + 1 )< (int)(tEnd/tStep)-1){
					t2 = (timeClick*tStep + (timeClick+1)*tStep)/2;
				}
				else{
					t2 = (timeClick*tStep + (int)(tEnd/tStep)-1)/2;
				}

				if((timeClick-1)<1){
					t1 = (timeClick*tStep)/2;
				}
				else{
					t1 = (timeClick*tStep+ (timeClick-1)*tStep)/2;
				}

				switch(option){
				case MIDDLE: 
					double simFlow1 = (calculateCumulativeValue(link.downStreamCumulative, t2, tStep) 
							- calculateCumulativeValue(link.downStreamCumulative, t1, tStep))/ (t2-t1);
					double simFlow2 = (calculateCumulativeValue(link.upStreamCumulative, t2, tStep) 
							- calculateCumulativeValue(link.upStreamCumulative, t1, tStep))/ (t2-t1);
					simFlows[linkIndex][timeClick] = simFlow1-simFlow2;
					break;
				case UPSTREAM: 
					simFlows[linkIndex][timeClick] = (calculateCumulativeValue(link.upStreamCumulative, t2, tStep) 
							- calculateCumulativeValue(link.upStreamCumulative, t1, tStep))/ (t2-t1);
					break;
				case DOWNSTREAM: 
					simFlows[linkIndex][timeClick] = (calculateCumulativeValue(link.downStreamCumulative, t2, tStep) 
							- calculateCumulativeValue(link.downStreamCumulative, t1, tStep))/ (t2-t1);
					break;
				case FORWARD: 
					//time correction required
					t1 = timeClick*tStep;
					simFlows[linkIndex][timeClick] = (calculateCumulativeValue(link.upStreamCumulative, t2, tStep) 
							- calculateCumulativeValue(link.upStreamCumulative, t1, tStep))/ (t2-t1);
					break;
				}


			}
		}
		return simFlows;
	}

	static public double[][] calculateCumulativeToDensity(ArrayList<TrafficLink> links, double tEnd, double tStep){
		double[][] simDensity = new double[links.size()][(int)(tEnd/tStep)];
		for(int linkIndex = 0; linkIndex<links.size(); linkIndex++){
			TrafficLink link = links.get(linkIndex);
			for(int timeClick=0; timeClick<(int)(tEnd/tStep); timeClick++){
				if(links.get(linkIndex).length>0){
					//density as the difference in streams per unit of length
					simDensity[linkIndex][timeClick] 
							= (link.upStreamCumulative[timeClick]-link.downStreamCumulative[timeClick])
							/link.length;
				}
			}

		}
		return simDensity;
	}

}
