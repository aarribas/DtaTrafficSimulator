package com.aarribas.dtasim;

import java.util.ArrayList;

import com.aarribas.traffictools.PathRepresentation;

public abstract class TrafficSwappingHeuristic {
	
	protected ArrayList< ArrayList<PathRepresentation>> finalRoutes;
	protected ArrayList< ArrayList<Double[]>> finalRouteFractions;
	protected ArrayList<double[][]> expandedODMatrices;
	
	protected double tEnd, tStep;
	protected TrafficData tfData;
	protected ArrayList<double[]> linkSpeeds;
	protected int timeClicksOfRouteInterval;
	
	public void oneTimeSetup(int timeClicksOfRouteInterval, double tEnd, double tStep, TrafficData tfData, ArrayList<double[]> linkSpeeds, ArrayList<double[][]> expandedODMatrices){
		
		this.tEnd = tEnd;
		this.tStep = tStep;
		this.tfData = tfData;
		this.linkSpeeds = linkSpeeds;
		this.expandedODMatrices = expandedODMatrices;
		this.timeClicksOfRouteInterval = timeClicksOfRouteInterval;
		
	}
	
	abstract public void setup(ArrayList< ArrayList<PathRepresentation>> oldRoutes,
			ArrayList< ArrayList<Double[]>> oldRouteFractions,
			ArrayList< ArrayList<PathRepresentation>> newRoutes,
			ArrayList< ArrayList<Double[]>> newRouteFractions,
			int iteration);
	
	abstract public void run();
	
	public ArrayList< ArrayList<PathRepresentation>> getRoutes(){
		return finalRoutes;
	}
	
	public ArrayList< ArrayList<Double[]>> getClonedRouteFractions(){
		ArrayList< ArrayList<Double[]>> rtf = cloneRouteFractions(finalRouteFractions);
		return rtf;
		
	}
	

	public ArrayList< ArrayList<Double[]>> getRouteFractions(){
		return finalRouteFractions;
		
	}
//	
//	public void reset(){
//		resetRouteFrac(finalRouteFractions);
//		
//	}
//	
//	protected void resetRouteFrac(ArrayList< ArrayList<Double[]>> routeFractions){
//		
//
//		//display each available routeFractions per ODPair and route
//		for(int setOfRoutesIndex = 0; setOfRoutesIndex< routeFractions.size(); setOfRoutesIndex++){
//			for(int routeIndex = 0; routeIndex< routeFractions.get(setOfRoutesIndex).size(); routeIndex++){		
//
//				for(int  i = 0; i<routeFractions.get(setOfRoutesIndex).get(routeIndex).length; i++){
//					routeFractions.get(setOfRoutesIndex).get(routeIndex)[i] = null;
//				}
//				routeFractions.get(setOfRoutesIndex).set(routeIndex, null);
//
//			}
//		}
//		
//	}
	
	
	
	protected ArrayList<ArrayList<PathRepresentation>> cloneRoutes(ArrayList<ArrayList<PathRepresentation>> routes){
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
	

	protected ArrayList<ArrayList<Double[]>> cloneRouteFractions(ArrayList<ArrayList<Double[]>> routeFractionsToClone){
	
		//the content of the routeFractions must be cloned so that the retrieved routeFractions do not point anymore 
		//to the routeFractions member of PathFinder
		ArrayList<ArrayList<Double[]>> clonedRouteFractions = new ArrayList<ArrayList<Double[]>>();

		for(int setOfRoutes =0;  setOfRoutes < routeFractionsToClone.size(); setOfRoutes++){
			ArrayList<Double[]> rtFracForOD = new ArrayList<Double[]>();

			for(int rtf = 0 ; rtf < routeFractionsToClone.get(setOfRoutes).size(); rtf++ ){
				//clone the full double array
				
				Double[] rtFracs = new Double[routeFractionsToClone.get(setOfRoutes).get(rtf).length];
				
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

}
