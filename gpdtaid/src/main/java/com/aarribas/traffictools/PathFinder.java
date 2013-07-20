package com.aarribas.traffictools;

import java.util.ArrayList;
import com.aarribas.dtasim.TrafficData;
import com.aarribas.dtasim.TrafficODPair;

public abstract class PathFinder {

	protected ArrayList<ArrayList<PathRepresentation>> routes = null;
	protected ArrayList<ArrayList<Double[]>>  routeFractions = null;
	protected TrafficData tfData;
	protected TrafficODPair[] ODPairs; 
	protected double tEnd, tStep;
	protected int timeClicksOfRouteInterval, timeClicksOfAdditionalRouteInterval, timeClicksShift;
	protected ArrayList<double[]> travelCosts;

	enum routeIntervalOption{LAST_OUT, FIRST_IN};

	public PathFinder(TrafficData tfData, TrafficODPair[] ODPairs, double tEnd, double tStep,int timeClicksShift, int timeClicksOfRouteInterval) {
		this.tfData = tfData; 
		this.ODPairs = ODPairs;
		this.tEnd = tEnd;
		this.tStep = tStep;
		this.timeClicksOfRouteInterval = timeClicksOfRouteInterval;
		//default is option LAST_OUT hence:
		this.timeClicksOfAdditionalRouteInterval = 0;
		this.timeClicksShift = timeClicksShift;
		
	}

	public PathFinder(TrafficData tfData, TrafficODPair[] ODPairs, double tEnd, double tStep,int timeClicksShift, int timeClicksOfRouteInterval, routeIntervalOption option) {
		this(tfData, ODPairs, tEnd, tStep, timeClicksShift, timeClicksOfRouteInterval);
		switch(option){
		case FIRST_IN:
			timeClicksOfAdditionalRouteInterval = timeClicksOfRouteInterval;
		case LAST_OUT:
			timeClicksOfAdditionalRouteInterval = 0;

		}
	}

	abstract public void findPath(ArrayList<double[]> travelCosts
			, ArrayList<ArrayList<PathRepresentation>> oldRoutes 
			, ArrayList<ArrayList<Double[]>> oldRouteFRactions);

	public ArrayList<ArrayList<PathRepresentation>> getRoutes(){

		return this.routes;

	}

	public ArrayList<ArrayList<Double[]>> getRouteFractions(){

		return routeFractions;
	}

	public ArrayList<ArrayList<PathRepresentation>> getClonedRoutes(){
		
		return cloneRoutes(routes);
		
	}
	
	protected ArrayList<ArrayList<PathRepresentation>> cloneRoutes(ArrayList<ArrayList<PathRepresentation>> routesToClone){
		
		//the content of the routeFractions must be cloned so that the retrieved routeFractions do not point anymore 
				//to the routeFractions member of PathFinder
				ArrayList<ArrayList<PathRepresentation>> clonedRoutes = new ArrayList<ArrayList<PathRepresentation>>();

				for(int setOfRoutes =0;  setOfRoutes < routesToClone.size(); setOfRoutes++){
					ArrayList<PathRepresentation> rtForOD = new ArrayList<PathRepresentation>();

					for(int rtIndex = 0 ; rtIndex < routesToClone.get(setOfRoutes).size(); rtIndex++ ){
						//clone the full double array
						//usually clone would be used here - had to test without clone -- TBD put clone back
						Integer[] intgn = new Integer[routesToClone.get(setOfRoutes).get(rtIndex).nodeIndexes.length];
						int[] intgl = new int[routesToClone.get(setOfRoutes).get(rtIndex).linkIndexes.length];
						
						for(int  i=0; i<routesToClone.get(setOfRoutes).get(rtIndex).nodeIndexes.length; i++ ){
							
							intgn[i] = routesToClone.get(setOfRoutes).get(rtIndex).nodeIndexes[i];
						}
						
						for(int  i=0; i<routesToClone.get(setOfRoutes).get(rtIndex).linkIndexes.length; i++ ){							
							intgl[i] = routesToClone.get(setOfRoutes).get(rtIndex).linkIndexes[i];
						}
						
						PathRepresentation rt = new PathRepresentation(intgn, intgl);
						rtForOD.add(rt);
					}

					clonedRoutes.add(rtForOD);
				}

				return clonedRoutes;
	}

	public ArrayList<ArrayList<Double[]>> getClonedRouteFractions(){
		
		return cloneRouteFractions(routeFractions);
	
	}
	
	protected ArrayList<ArrayList<Double[]>> cloneRouteFractions(ArrayList<ArrayList<Double[]>> routeFractionsToClone){
	
		//the content of the routeFractions must be cloned so that the retrieved routeFractions do not point anymore 
		//to the routeFractions member of PathFinder
		ArrayList<ArrayList<Double[]>> clonedRouteFractions = new ArrayList<ArrayList<Double[]>>();

		for(int setOfRoutes =0;  setOfRoutes < routeFractionsToClone.size(); setOfRoutes++){
			ArrayList<Double[]> rtFracForOD = new ArrayList<Double[]>();

			for(int rtf = 0 ; rtf < routeFractionsToClone.get(setOfRoutes).size(); rtf++ ){
				//clone the full double array
				Double[] rtFracs = new Double[routeFractionsToClone.get(setOfRoutes).get(rtf).length];
				//usually clone would be used here - had to test without clone -- TBD put clone back
				
				for(int i = 0; i<routeFractionsToClone.get(setOfRoutes).get(rtf).length; i++ ){
					rtFracs[i] = routeFractionsToClone.get(setOfRoutes).get(rtf)[i].doubleValue();
					//routeFractionsToClone.get(setOfRoutes).get(rtf)[i] = null;
				}
				
				rtFracForOD.add(rtFracs);
			}
			clonedRouteFractions.add(rtFracForOD);
		}

		return clonedRouteFractions;
	}
		

	public void setRoutes(ArrayList<ArrayList<PathRepresentation>> routes){
		this.routes = routes;

	}

	public void setRouteFractions(ArrayList<ArrayList<Double[]>> routeFractions){
		this.routeFractions = routeFractions;

	}
	
	public void resetRoutesAndRouteFractions(){
		routes = null;
		routeFractions = null;
	}


}
