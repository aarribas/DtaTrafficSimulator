package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;


import com.aarribas.traffictools.PathRepresentation;
import com.aarribas.traffictools.TravelTimeManager;
public class TrafficSwappingHeuristicDECF extends TrafficSwappingHeuristic{

	private ArrayList< ArrayList<PathRepresentation>> newRoutes;
	private ArrayList< ArrayList<Double[]>> newRouteFractions;

	private ArrayList< ArrayList<PathRepresentation>> oldRoutes;
	private ArrayList< ArrayList<Double[]>> oldRouteFractions;

	@SuppressWarnings("unused")
	private double iteration;

	private double propFactor;

	//some structures required to pass complex parameters to the GP Context
	private  ArrayList<ArrayList<double[]>> costPerRoute;
	private  ArrayList<ArrayList<Double>> cumulativeOfDeltas;
	private  ArrayList<int[]>  numRtsWithMinCostPerOD;
	private  ArrayList<double[]> minCostPerOD;


	public TrafficSwappingHeuristicDECF(double alpha){
		super();
		this.propFactor = alpha;
	}


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
		
		//TODO remove line below!
		int timeClicksOfRouteInterval =50;
		
		//compute costs for all routes
		updateCostsPerRoute();

		//compute number of optimal routes and the minimum cost -so far- per ODPair
		computeNumMinCostAndNumRtsWithMinCost();

		//initiliase the structure to save the routeFractions
		ArrayList< ArrayList<Double[]>> tempRouteFractions = generateEmptyRouteFractions(); 

		//reset the cumulative of deltas
		cumulativeOfDeltas = null;

		//compute routeFractions for non optimal routes and normalise routeFractions for optimal routes
		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRouteFractions.size(); setOfRoutesIndex++){

			int previousTimeClick = 0;
			
			//we start at the interval 0
			int interval = 0;
			int fracIndex = timeClicksOfRouteInterval*interval; //equals timeClickShift of course

			//the following applies to routes already seen 
			while( fracIndex < (int) (tEnd/tStep)){

				double cumulativeOfRouteFractions = 0.0;

				//first go through the all routes and compute the new routeFractions for the non optimal or just save an uninitialized array for the optimal
				for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){


					if(fractionsIndex < oldRouteFractions.get(setOfRoutesIndex).size()){

						//compute correction only if the cost is not minimal for this route and instant
						//						if(newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] != 1.0){
						if(costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] != minCostPerOD.get(setOfRoutesIndex)[fracIndex]){
							//get some path information 
							PathRepresentation path = oldRoutes.get(setOfRoutesIndex).get(fractionsIndex);
							int startNodeIndex = path.nodeIndexes[0];
							int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];
							
						
							//get the rtFrac
							double rtFrac = oldRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];

							//compute the cost difference between this route and the optimal route
							//							double normCostDiff = (costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] - 
							//									costPerRoute.get(setOfRoutesIndex).get(indexOptimalRtPerOD.get(setOfRoutesIndex)[fracIndex])[fracIndex]);
							double normCostDiff = (costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] - 
									minCostPerOD.get(setOfRoutesIndex)[fracIndex]);
							//									normCostDiff = normCostDiff*expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex];

							//=>compute delta starts here
							double delta= 0.0;
							double tempRouteFrac=0.0;

							//if there is an excess cost for a non optimal route we try to fix it
							if(normCostDiff > 0){
								if(cumulativeOfDeltas == null || setOfRoutesIndex >= cumulativeOfDeltas.size()){

									delta = -normCostDiff;
								}
								else{
									if(fractionsIndex >= cumulativeOfDeltas.get(setOfRoutesIndex).size()){
										delta = -normCostDiff; 
									}
									else{

										delta = -normCostDiff - cumulativeOfDeltas.get(setOfRoutesIndex).get(fractionsIndex);

									}
								}

								double origDelta = delta;

								//=>compute delta ends here

								//correct the obtained delta to values that guarantee feasibility
								if(delta > 0.0){

									delta = 0.0;
								}
								else if(delta < -(rtFrac/propFactor)*expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]){

									//maximum route swapping is the current traffic on that route
									delta = -(rtFrac/propFactor)*expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex];

								}
								
								

								//compute the new route fraction if the traffic is not 0
								if(expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]!= 0.0){
									tempRouteFrac = (rtFrac*expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex] + propFactor*delta)
											/ expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex];
								}
								else{
									tempRouteFrac = rtFrac; //no change
									delta = 0.0;
								}
							}
							else{
								//if there is no excess of cost it is ok to keep the assigned traffic as is.
								tempRouteFrac = rtFrac; //no change
								delta = 0.0;
							}
							
					
							saveDeltaForARouteAndTime(setOfRoutesIndex, fractionsIndex, fracIndex, delta);


							//update the cumulativeOfRouteFractions
							cumulativeOfRouteFractions = cumulativeOfRouteFractions + 
									tempRouteFrac;
							
							Arrays.fill(tempRouteFractions.get(setOfRoutesIndex).get(fractionsIndex), previousTimeClick, fracIndex+1, tempRouteFrac);
						}

					}
					else{
						//COMPLETELY NEW ROUTE!
						//for completely new routes the old fraction is 0 
						//if the route is optimal for some instant then the new rtFraction is obtained via normalisation later on
						//otherwise there is no traffic swapping applicable for this route


						//initialise the fraction and the delta
						double tempRouteFrac = 0.0;
						saveDeltaForARouteAndTime(setOfRoutesIndex, fractionsIndex, fracIndex, 0.0);
						Arrays.fill(tempRouteFractions.get(setOfRoutesIndex).get(fractionsIndex), previousTimeClick, fracIndex+1, tempRouteFrac);

					}

				}

				//finally normalise routeFractions for all routes with minimum cost per instant
				for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){

					if(costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] == minCostPerOD.get(setOfRoutesIndex)[fracIndex]){
						//default rtFrac is 0, this is valid for route Fractions that are completely new
						double rtFrac = 0.0;

						if(fractionsIndex < oldRouteFractions.get(setOfRoutesIndex).size()){
							rtFrac = oldRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];
						}

						//compute the new routeFraction for this route - there might be several routes with min cost
						double tempRouteFrac = (1.0 - cumulativeOfRouteFractions)/(double)numRtsWithMinCostPerOD.get(setOfRoutesIndex)[fracIndex];

						Arrays.fill(tempRouteFractions.get(setOfRoutesIndex).get(fractionsIndex), previousTimeClick, fracIndex+1, tempRouteFrac);
					
						//compute the corresponding delta for this route
						PathRepresentation path = newRoutes.get(setOfRoutesIndex).get(fractionsIndex);
						int startNodeIndex = path.nodeIndexes[0];
						int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];

						double delta = (tempRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex]  - rtFrac)/propFactor ;
						delta = delta * expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex];

						saveDeltaForARouteAndTime(setOfRoutesIndex, fractionsIndex, fracIndex, delta);
					}

					
				}
				
				previousTimeClick = fracIndex + 1;
				
				//move to the next route interval
				interval++;
				fracIndex = timeClicksOfRouteInterval*interval; 
			}
			
			//set the route fractions for the last partial interval (from previous time click to end of time)
			//we just extend the last route fraction
			for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){
				Arrays.fill(tempRouteFractions.get(setOfRoutesIndex).get(fractionsIndex), previousTimeClick, (int) (tEnd/tStep), 
						tempRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[previousTimeClick-1]);
			}

		}
		

		//save finalRoutes and finalRouteFractions
		finalRoutes = cloneRoutes(newRoutes);
		finalRouteFractions = tempRouteFractions;

	}




	private ArrayList< ArrayList<Double[]>> generateEmptyRouteFractions(){

		//generate the structure that will contain the routefractions per OD 
		ArrayList< ArrayList<Double[]>> tempRouteFractions = new ArrayList< ArrayList<Double[]>>();

		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRouteFractions.size(); setOfRoutesIndex++){

			ArrayList<Double[]> routeFractionsForOD = new ArrayList<Double[]>();

			for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){				
				routeFractionsForOD.add(new Double[(int)(tEnd/tStep)]);
			}
			tempRouteFractions.add(routeFractionsForOD);
		}

		return tempRouteFractions;	
	}



	private void computeNumMinCostAndNumRtsWithMinCost(){

		//compute min cost per OD and time
		minCostPerOD = new ArrayList<double[]>();

		for(int setOfRoutesIndex = 0; setOfRoutesIndex< costPerRoute.size(); setOfRoutesIndex++){
			double[] minCosts = new double[(int)(tEnd/tStep)];

			for(int fracIndex = 0; fracIndex < (int)(tEnd/tStep); fracIndex++ ){

				double minCost = Double.MAX_VALUE; //fake cost value

				for(int fractionsIndex = 0; fractionsIndex < costPerRoute.get(setOfRoutesIndex).size(); fractionsIndex++ ){

					//check if minimum
					if(costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] < minCost )
					{	
						//save it
						minCost = costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];
					}	
				}
				minCosts[fracIndex] = minCost; 

			}
			minCostPerOD.add(minCosts);

		}



		//compute num routes with min Cost per OD and time
		numRtsWithMinCostPerOD = new ArrayList<int[]>();
		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRoutes.size(); setOfRoutesIndex++){
			//initialize
			numRtsWithMinCostPerOD.add(new int[(int)(tEnd/tStep)]);
			Arrays.fill(numRtsWithMinCostPerOD.get(setOfRoutesIndex),0);

			for(int fracIndex = 0; fracIndex < (int)(tEnd/tStep); fracIndex++ ){

				for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){

					//check if minimum
					if(costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] == minCostPerOD.get(setOfRoutesIndex)[fracIndex] )
					{	

						//increase the counter if this route has min cost
						numRtsWithMinCostPerOD.get(setOfRoutesIndex)[fracIndex] = numRtsWithMinCostPerOD.get(setOfRoutesIndex)[fracIndex] + 1;
					}	
				}
			}

		}
	}


	private void updateCostsPerRoute(){

		costPerRoute =  new ArrayList<ArrayList<double[]>>();

		//for all routes compute and save the cost!
		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRoutes.size(); setOfRoutesIndex++){

			for(int routeIndex = 0; routeIndex< newRoutes.get(setOfRoutesIndex).size(); routeIndex++){

				PathRepresentation path = newRoutes.get(setOfRoutesIndex).get(routeIndex);

				int[] linkIndexes = path.linkIndexes;

				for(int timeClick = 0; timeClick < (int)(tEnd/tStep); timeClick++){
					double routeTT = 0;
					for(int linkIndex : linkIndexes){
						double[] cost = new double[linkSpeeds.get(linkIndex).length];

						//compute instantaneous cost
						for(int costIndex = 0; costIndex < linkSpeeds.get(linkIndex).length; costIndex++ ){
							cost[costIndex] = tfData.links.get(linkIndex).length / linkSpeeds.get(linkIndex)[costIndex];
						}
						//compute complete routeTT (route cost or travel time)
						routeTT = routeTT + TravelTimeManager.computeTravelTimeForGivenCost(cost,timeClick*tStep + routeTT, tEnd, tStep);

					}
					//save the route travel time as cost for this route and time
					saveCostForARouteAndTime(setOfRoutesIndex, routeIndex, timeClick, routeTT);
				}

			}
		}

	}

	private void saveCostForARouteAndTime(int setOfRoutesIndex, int routeIndex, int timeClick, double routeTT){

		//initialisation 
		if(costPerRoute == null){
			costPerRoute = new  ArrayList<ArrayList<double[]>>();
		}

		//required in case of new OD pair
		if(setOfRoutesIndex >= costPerRoute.size()){
			costPerRoute.add(new ArrayList<double[]>());
		}

		//required in case of new route that was not considered previously
		if(routeIndex >= costPerRoute.get(setOfRoutesIndex).size()){
			costPerRoute.get(setOfRoutesIndex).add(new double[(int)(tEnd/tStep)]);
		}

		//save the cost (route travel time)
		costPerRoute.get(setOfRoutesIndex).get(routeIndex)[timeClick] = routeTT;		

	}


	private void saveDeltaForARouteAndTime(int setOfRoutesIndex, int routeIndex, int timeClick, double delta){

		if(cumulativeOfDeltas == null){
			cumulativeOfDeltas = new  ArrayList<ArrayList<Double>>();
		}

		//Required in case of new OD pair
		if(setOfRoutesIndex >= cumulativeOfDeltas.size()){
			cumulativeOfDeltas.add(new ArrayList<Double>());
		}

		//Required in case of new route that was not considered previously
		if(routeIndex >= cumulativeOfDeltas.get(setOfRoutesIndex).size()){
			//produce enough empty deltas
			while(routeIndex >= cumulativeOfDeltas.get(setOfRoutesIndex).size())
			{
				cumulativeOfDeltas.get(setOfRoutesIndex).add(0.0);
			}
		}

		if (timeClick == 0){
			//Initialize the cumulative for this route at 0.0
			cumulativeOfDeltas.get(setOfRoutesIndex).set(routeIndex,0.0);	

		}
		else{
			//Otherwise add the delta
			cumulativeOfDeltas.get(setOfRoutesIndex).set(routeIndex,cumulativeOfDeltas.get(setOfRoutesIndex).get(routeIndex) + delta); 	

		}


	}

}