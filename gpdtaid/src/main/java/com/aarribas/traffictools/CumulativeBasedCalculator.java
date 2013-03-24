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
			if(value>cumulative[cumulativeIndex]){
				tBeforeClick = cumulativeIndex;
			}

			if(tAfterClick ==-1 && value<=cumulative[cumulativeIndex]){
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
			//must be properly taken care of by the caller not to produce a exception
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

		double cumulative;


		//fix click before
		int tClickBefore = Math.min((int)(t/tStep), cumulatives.length-1);
		
		//fix click after
		int tClickAfter = Math.min(cumulatives.length-1, tClickBefore+1);


		//before beginning of time the cumulative is 0
		if(t < 0){
			cumulative = 0;
		}
		else{



			double tempFasterTime = tClickBefore*tStep;
			double tempFasterCumulative = cumulatives[tClickBefore];
			double tInt = (t-tempFasterTime)/((tClickAfter*tStep)-tempFasterTime);

			//TODO verify the code does not fail here due to max value of tempFasterCumulative
			cumulative =  tempFasterCumulative + tInt * (cumulatives[tClickAfter]-tempFasterCumulative);

		}

		return cumulative;

	}



	static public double[][] calculateCumulativeToSpeeds(ArrayList<TrafficLink>links, double tEnd, double tStep){

		double[][] simSpeeds = new double[links.size()][(int)(tEnd/tStep)];
		
		//Initialise to freeSpeed for each instant
		for(int linkIndex = 0; linkIndex<links.size(); linkIndex++){
			Arrays.fill(simSpeeds[linkIndex], links.get(linkIndex).freeSpeed);
		}
		

		for(int timeClick=0; timeClick<(int)(tEnd/tStep); timeClick++){
			for(int linkIndex = 0; linkIndex<links.size(); linkIndex++){
				TrafficLink link = links.get(linkIndex);
				
				double time = calculateCumulativeTime(link.downStreamCumulative, link.upStreamCumulative[timeClick], tEnd, tStep);
				
				//compute simspeed depending on the simulation time
				if(time > tEnd)
				{
					simSpeeds[linkIndex][timeClick] = 
							Math.max(0, 
									simSpeeds[linkIndex][Math.max(0,timeClick-1)] 
											- (simSpeeds[linkIndex][Math.max(0,timeClick-2)] -simSpeeds[linkIndex][Math.max(0,timeClick-1)])
									);
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


	static public double[][] calculateCumulativeToSpeedsAtArrival(ArrayList<TrafficLink>links, double tEnd, double tStep){

		double[][] simSpeeds = new double[links.size()][(int)(tEnd/tStep)];

		for(int timeClick=0; timeClick<(int)(tEnd/tStep); timeClick++){
			for(int linkIndex = 0; linkIndex<links.size(); linkIndex++){
				TrafficLink link = links.get(linkIndex);
				//Initialise to freeSpeed for each instant
				Arrays.fill(simSpeeds[linkIndex], link.freeSpeed);
				double time = calculateCumulativeTime(link.upStreamCumulative, link.downStreamCumulative[timeClick], tEnd, tStep);

				//compute simspeed depending on the simulation time
				if(time > tEnd)
				{
					simSpeeds[linkIndex][timeClick] = Math.max(0, 
							simSpeeds[linkIndex][Math.max(0, timeClick-1)] 
									- (simSpeeds[linkIndex][Math.max(0, timeClick-2)] -simSpeeds[linkIndex][Math.max(0, timeClick-1)])); 
					
				}
				else{
					simSpeeds[linkIndex][timeClick] = link.length/Math.abs(time - timeClick*tStep);
				}

				//adjust the simspeed to feasible values
				if(simSpeeds[linkIndex][timeClick] < 0){
					//limit the speed to the freespeed
					simSpeeds[linkIndex][timeClick] = link.freeSpeed;
				}
				else if(simSpeeds[linkIndex][timeClick] > link.freeSpeed ){
					simSpeeds[linkIndex][timeClick] = link.freeSpeed;
				}
				else if(time == 0){
					//limit the speed to the freespeed
					simSpeeds[linkIndex][timeClick] = link.freeSpeed;
				}


				if(link.upStreamCumulative[Math.min((int)(tEnd/tStep), (int)(timeClick + 10))] - link.upStreamCumulative[Math.max(1, timeClick)]<0.001){
					//very low flow on link => set to freespeed
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
				double t1 = (timeClick*tStep + Math.max(0,timeClick-1)*tStep)/2;
				double t2 = (timeClick*tStep + Math.min((int)(tEnd/tStep)-1,timeClick+1)*tStep)/2;

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
					//t1 time correction required
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
