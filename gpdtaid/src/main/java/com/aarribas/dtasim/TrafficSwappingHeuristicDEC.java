package com.aarribas.dtasim;

import java.util.ArrayList;

import com.aarribas.traffictools.PathRepresentation;

public class TrafficSwappingHeuristicDEC extends TrafficSwappingHeuristic{

	private ArrayList< ArrayList<PathRepresentation>> newRoutes;
	private ArrayList< ArrayList<Double[]>> newRouteFractions;

	private ArrayList< ArrayList<PathRepresentation>> oldRoutes;
	private ArrayList< ArrayList<Double[]>> oldRouteFractions;

	private double iteration;

	public void setup(ArrayList< ArrayList<PathRepresentation>> oldRoutes,
			ArrayList< ArrayList<Double[]>> oldRouteFractions,
			ArrayList< ArrayList<PathRepresentation>> newRoutes,
			ArrayList< ArrayList<Double[]>> newRouteFractions,
			int iteration){
		this.oldRoutes  = oldRoutes;
		this.oldRouteFractions = oldRouteFractions;
		this.newRoutes = newRoutes;
		this.newRouteFractions = newRouteFractions;
		this.iteration = (double) iteration;

	}

	public void run(){

		ArrayList< ArrayList<Double[]>> tempRouteFractions =  new ArrayList< ArrayList<Double[]>>();

		
		//TODO VERIFY OLDROUTES VS NEWROUTES BELOW!!!
		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRoutes.size(); setOfRoutesIndex++){
			
			ArrayList<Double[]> fractionsForOD = new ArrayList<Double[]>();
			
			for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){

				//temp fractions
				Double[] tempFractions = new Double[newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length];

				if(fractionsIndex < oldRouteFractions.get(setOfRoutesIndex).size()){

					for(int fracIndex = 0; fracIndex < newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length; fracIndex++ ){

						//msa as oldRouteFrac + 1/k of the difference between new routefracs and old ones
						tempFractions[fracIndex] = oldRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] 
								+ (1/iteration)*(newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] 
										-oldRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex]);

					}

				}
				else{

					for(int fracIndex = 0; fracIndex < newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length; fracIndex++ ){

						//msa for 0 old route fractions
						tempFractions[fracIndex] =  (1/iteration)*newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];

					}

				
				}
				//update fractions
				fractionsForOD.add(tempFractions);
				
			}
			tempRouteFractions.add(fractionsForOD);
		}
		
		//save finalRoutes and finalRouteFractions
		finalRoutes = cloneRoutes(newRoutes);
		finalRouteFractions = tempRouteFractions;
		

	}
	

}
