package com.aarribas.dtasim;

import java.util.ArrayList;

import com.aarribas.traffictools.PathRepresentation;

public abstract class TrafficSwappingHeuristic {
	
	protected ArrayList< ArrayList<PathRepresentation>> finalRoutes;
	protected ArrayList< ArrayList<Double[]>> finalRouteFractions;
	
	abstract public void run();
	
	public ArrayList< ArrayList<PathRepresentation>> getRoutes(){
		return this.finalRoutes;
	}
	
	public ArrayList< ArrayList<Double[]>> getRouteFractions(){
		return this.finalRouteFractions;
	}
	

}
