package com.aarribas.traffictools;

public class CumulativeBasedCalculator {

	static public double computeCumulativeTime(double t, double tEnd, double tStep){
		return 0.0;
	}

	static public double computeCumulativeValue(double cumulative[], double t, double tEnd, double tStep){

		//Attention: in the matlab code the calculation o tbefore and tafter is inconsistent with the calculation in findTravelTimes
		//I decided to make it consistent here
		
		int tClickBefore = (int)(t/tStep);
		
		if(tClickBefore == cumulative.length-1){
			//if we have reached the last time click return the last cumulative
			return cumulative[cumulative.length-1];
		}
		
		//if the we are at the very beginning the value is 0
		if(tClickBefore == 0){
			return 0;
		}
		else{
			int tClickAfter = tClickBefore + 1;
			double tempFasterTime = tClickBefore*tStep;
			double tempFasterCumulative = cumulative[tClickBefore];
			double tInt = (t-tempFasterTime)/((tClickAfter*tStep)-tempFasterTime);
			return tempFasterCumulative + tInt * (cumulative[tClickAfter]-tempFasterCumulative);
			
		}

	}

	static public double computeCumulativeToSpeeds(){
		//TODO

		return 0.0;
	}

	static public double computeCumulativeToFlows(){

		//TODO
		return 0.0;
	}

	static public double computeCumulativeToDensity(){
		//TODO
		return 0.0;
	}


}
