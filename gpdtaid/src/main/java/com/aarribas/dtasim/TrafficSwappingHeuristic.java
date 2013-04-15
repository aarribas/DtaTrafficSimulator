package com.aarribas.dtasim;

import java.util.ArrayList;

import com.aarribas.traffictools.PathRepresentation;

public abstract class TrafficSwappingHeuristic {
	
	protected ArrayList< ArrayList<PathRepresentation>> finalRoutes;
	protected ArrayList< ArrayList<Double[]>> finalRouteFractions;
	
	abstract public void setup(ArrayList< ArrayList<PathRepresentation>> oldRoutes,
			ArrayList< ArrayList<Double[]>> oldRouteFractions,
			ArrayList< ArrayList<PathRepresentation>> newRoutes,
			ArrayList< ArrayList<Double[]>> newRouteFractions,
			int iteration);
	
	abstract public void run();
	
	public ArrayList< ArrayList<PathRepresentation>> getRoutes(){
		return this.finalRoutes;
	}
	
	public ArrayList< ArrayList<Double[]>> getRouteFractions(){
		return this.finalRouteFractions;
	}
	
	
	public ArrayList<ArrayList<PathRepresentation>> cloneRoutes(ArrayList<ArrayList<PathRepresentation>> routes){
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

}
