package com.aarribas.traffictools;

import java.util.ArrayList;

import com.aarribas.dtasim.TrafficData;

public abstract class PathFinder {
	
	private ArrayList<ArrayList<Double>> route = null;
	private ArrayList<ArrayList<Double>>  routeFractions = null;
	private TrafficData tfData = null;
	
	public void findPath(){
		
	}
	
	private ArrayList<ArrayList<Double>> getRoute(){
		return route;
	}
	
	private ArrayList<ArrayList<Double>> getRouteFractions(){
		return routeFractions;
	}

}
