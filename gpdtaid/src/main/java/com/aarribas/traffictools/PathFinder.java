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

		//initialise empty routes and routeFractions
		this.routes = new ArrayList<ArrayList<PathRepresentation>>();
		this.routeFractions = new ArrayList<ArrayList<Double[]>>();
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

	abstract public void findPath(ArrayList<double[]> travelCosts);

	public ArrayList<ArrayList<PathRepresentation>> getRoutes(){

		//the content of the routeFractions must be cloned so that the retrieved routeFractions do not point anymore 
		//to the routeFractions member of PathFinder
		ArrayList<ArrayList<PathRepresentation>> clonedRoutes = new ArrayList<ArrayList<PathRepresentation>>();
		
		for(int setOfRoutes =0;  setOfRoutes < routes.size(); setOfRoutes++){
			ArrayList<PathRepresentation> rtForOD = new ArrayList<PathRepresentation>();

			for(int rtIndex = 0 ; rtIndex < routes.get(setOfRoutes).size(); rtIndex++ ){
				//clone the full double array
				PathRepresentation rt = routes.get(setOfRoutes).get(rtIndex).clone();
				rtForOD.add(rt);
			}
			
			clonedRoutes.add(rtForOD);
		}

		return clonedRoutes;
	}

	public ArrayList<ArrayList<Double[]>> getRouteFractions(){

		//the content of the routeFractions must be cloned so that the retrieved routeFractions do not point anymore 
		//to the routeFractions member of PathFinder
		ArrayList<ArrayList<Double[]>> clonedRouteFractions = new ArrayList<ArrayList<Double[]>>();

		for(int setOfRoutes =0;  setOfRoutes < routeFractions.size(); setOfRoutes++){
			ArrayList<Double[]> rtFracForOD = new ArrayList<Double[]>();

			for(int rtf = 0 ; rtf < routeFractions.get(setOfRoutes).size(); rtf++ ){
				//clone the full double array
				Double[] rtFracs = routeFractions.get(setOfRoutes).get(rtf).clone();
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


}
