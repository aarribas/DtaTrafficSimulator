package com.aarribas.traffictools;

import java.util.ArrayList;
import java.util.HashSet;

import com.aarribas.dtasim.TrafficData;
import com.aarribas.dtasim.TrafficODPair;

public abstract class PathFinder {
	
	protected ArrayList<ArrayList<PathRepresentation>> routes = null;
	protected ArrayList<ArrayList<Integer[]>>  routeFractions = null;
	protected TrafficData tfData;
	protected TrafficODPair[] ODPairs; 
	protected double tEnd, tStep;
	protected int timeClicksOfRouteInterval, timeClicksOfAdditionalRouteInterval, timeClicksShift;
	protected double[] travelCosts;
	
	enum routeIntervalOption{LAST_OUT, FIRST_IN};

	public PathFinder(TrafficData tfData, TrafficODPair[] ODPairs,double[] travelCosts, double tEnd, double tStep,int timeClicksShift, int timeClicksOfRouteInterval) {
		this.tfData = tfData;
		this.ODPairs = ODPairs;
		this.travelCosts = travelCosts;
		this.tEnd = tEnd;
		this.tStep = tEnd;
		this.timeClicksOfRouteInterval = timeClicksOfRouteInterval;
		//default is option LAST_OUT hence:
		this.timeClicksOfAdditionalRouteInterval = 0;
		this.timeClicksShift = timeClicksShift;
		
		//initialise empty routes and routeFractions
		this.routes = new ArrayList<ArrayList<PathRepresentation>>();
		this.routeFractions = new ArrayList<ArrayList<Integer[]>>();
	}
	
	public PathFinder(TrafficData tfData, TrafficODPair[] ODPairs,double[] travelCosts, double tEnd, double tStep,int timeClicksShift, int timeClicksOfRouteInterval, routeIntervalOption option) {
		this(tfData, ODPairs, travelCosts, tEnd, tStep, timeClicksShift, timeClicksOfRouteInterval);
		switch(option){
		case FIRST_IN:
			timeClicksOfAdditionalRouteInterval = timeClicksOfRouteInterval;
		case LAST_OUT:
			timeClicksOfAdditionalRouteInterval = 0;
		
		}
	}
	
	abstract public void findPath();
	
	private ArrayList<ArrayList<PathRepresentation>> getRoute(){
		return routes;
	}
	
	private ArrayList<ArrayList<Integer[]>> getRouteFractions(){
		return routeFractions;
	}

}