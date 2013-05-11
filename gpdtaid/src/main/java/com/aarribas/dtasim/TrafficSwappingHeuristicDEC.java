package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;


import com.aarribas.traffictools.PathRepresentation;
import com.aarribas.traffictools.TravelTimeManager;
public class TrafficSwappingHeuristicDEC extends TrafficSwappingHeuristic{

	private ArrayList< ArrayList<PathRepresentation>> newRoutes;
	private ArrayList< ArrayList<Double[]>> newRouteFractions;

	private ArrayList< ArrayList<PathRepresentation>> oldRoutes;
	private ArrayList< ArrayList<Double[]>> oldRouteFractions;

	@SuppressWarnings("unused")
	private double iteration;

	private double alpha;

	//some structures required to pass complex parameters to the GP Context
	private  ArrayList<ArrayList<double[]>> costPerRoute;
	private  ArrayList<ArrayList<Double>> cumulativeOfDeltas;
	private  ArrayList<int[]>  numRtsWithMinCostPerOD;
	private  ArrayList<double[]> minCostPerOD;


	public TrafficSwappingHeuristicDEC(double alpha){
		super();
		this.alpha = alpha;
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



		//compute costs for all routes
		updateCostsPerRoute();

		//compute number of optimal routes and the minimum cost -so far- per ODPair
		computeNumMinCostAndNumRtsWithMinCost();

		//initiliaze the structure to save the routeFractions
		ArrayList< ArrayList<Double[]>> tempRouteFractions = generateEmptyRouteFractions(); 

		//reset the cumulative of deltas
		cumulativeOfDeltas = null;

		//compute routeFractions for non optimal routes and normalise routeFractions for optimal routes
		for(int ODPairIndex = 0; ODPairIndex< newRouteFractions.size(); ODPairIndex++){

			int previousTimeClick = 0;

			//we start at the interval 0
			int interval = 0;
			int timeClick = timeClicksOfRouteInterval*interval; //equals timeClickShift of course

			//the following applies to routes already seen 
			while( timeClick < (int) (tEnd/tStep)){

				double cumulativeOfRouteFractions = 0.0;

				//first go through the all routes and compute the new routeFractions for the non optimal or just save an uninitialized array for the optimal
				for(int routeIndex = 0; routeIndex < newRouteFractions.get(ODPairIndex).size(); routeIndex++ ){


					if(routeIndex < oldRouteFractions.get(ODPairIndex).size()){

						//compute correction only if the cost is not minimal for this route and instant
						//						if(newRouteFractions.get(ODPairIndex).get(routeIndex)[timeClick] != 1.0){
						if(costPerRoute.get(ODPairIndex).get(routeIndex)[timeClick] != minCostPerOD.get(ODPairIndex)[timeClick]){
							//get some path information 
							PathRepresentation path = oldRoutes.get(ODPairIndex).get(routeIndex);
							int startNodeIndex = path.nodeIndexes[0];
							int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];


							//get the rtFrac
							double rtFrac = oldRouteFractions.get(ODPairIndex).get(routeIndex)[timeClick];

							//compute the cost difference between this route and the optimal route
							//							double normCostDiff = (costPerRoute.get(ODPairIndex).get(routeIndex)[timeClick] - 
							//									costPerRoute.get(ODPairIndex).get(indexOptimalRtPerOD.get(ODPairIndex)[timeClick])[timeClick]);
							double normCostDiff = (costPerRoute.get(ODPairIndex).get(routeIndex)[timeClick] - 
									minCostPerOD.get(ODPairIndex)[timeClick]);
							//									normCostDiff = normCostDiff*expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex];

							//=>compute delta starts here
							double delta= 0.0;
							double tempRouteFrac=0.0;

							//if there is an excess cost for a non optimal route we try to fix it
							if(normCostDiff > 0){
								if(cumulativeOfDeltas == null || ODPairIndex >= cumulativeOfDeltas.size()){

									delta = -normCostDiff;
								}
								else{
									if(routeIndex >= cumulativeOfDeltas.get(ODPairIndex).size()){
										delta = -normCostDiff; 
									}
									else{

										delta = -normCostDiff - cumulativeOfDeltas.get(ODPairIndex).get(routeIndex);

									}
								}
								//=>compute delta ends here

								//correct the obtained delta to values that guarantee feasibility
								if(delta > 0.0){

									delta = 0.0;
								}
								else if(delta < -(rtFrac/alpha)*expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex]){

									//maximum route swapping is the current traffic on that route
									delta = -(rtFrac/alpha)*expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex];

								}



								//compute the new route fraction if the traffic is not 0
								if(expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex]!= 0.0){
									tempRouteFrac = (rtFrac*expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex] + alpha*delta)
											/ expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex];
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


							saveDeltaForARouteAndTime(ODPairIndex, routeIndex, timeClick, delta);


							//update the cumulativeOfRouteFractions
							cumulativeOfRouteFractions = cumulativeOfRouteFractions + 
									tempRouteFrac;

							Arrays.fill(tempRouteFractions.get(ODPairIndex).get(routeIndex), previousTimeClick, timeClick+1, tempRouteFrac);
						}

					}
					else{
						//COMPLETELY NEW ROUTE!
						//for completely new routes the old fraction is 0 
						//if the route is optimal for some instant then the new rtFraction is obtained via normalisation later on
						//otherwise there is no traffic swapping applicable for this route


						//initialise the fraction and the delta
						double tempRouteFrac = 0.0;
						saveDeltaForARouteAndTime(ODPairIndex, routeIndex, timeClick, 0.0);
						Arrays.fill(tempRouteFractions.get(ODPairIndex).get(routeIndex), previousTimeClick, timeClick+1, tempRouteFrac);

					}

				}

				//finally normalize routeFractions for all routes with minimum cost per instant
				for(int routeIndex = 0; routeIndex < newRouteFractions.get(ODPairIndex).size(); routeIndex++ ){

					if(costPerRoute.get(ODPairIndex).get(routeIndex)[timeClick] == minCostPerOD.get(ODPairIndex)[timeClick]){
						//default rtFrac is 0, this is valid for route Fractions that are completely new
						double rtFrac = 0.0;

						if(routeIndex < oldRouteFractions.get(ODPairIndex).size()){
							rtFrac = oldRouteFractions.get(ODPairIndex).get(routeIndex)[timeClick];
						}

						//compute the new routeFraction for this route - there might be several routes with min cost
						double tempRouteFrac = (1.0 - cumulativeOfRouteFractions)/(double)numRtsWithMinCostPerOD.get(ODPairIndex)[timeClick];

						Arrays.fill(tempRouteFractions.get(ODPairIndex).get(routeIndex), previousTimeClick, timeClick+1, tempRouteFrac);

						//compute the corresponding delta for this route
						PathRepresentation path = newRoutes.get(ODPairIndex).get(routeIndex);
						int startNodeIndex = path.nodeIndexes[0];
						int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];

						double delta = (tempRouteFractions.get(ODPairIndex).get(routeIndex)[timeClick]  - rtFrac)/alpha ;
						delta = delta * expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex];

						saveDeltaForARouteAndTime(ODPairIndex, routeIndex, timeClick, delta);
					}


				}

				previousTimeClick = timeClick + 1;

				//move to the next route interval
				interval++;
				timeClick = timeClicksOfRouteInterval*interval; 
			}

			//set the route fractions for the last partial interval (from previous time click to end of time)
			//we just extend the last route fraction
			for(int routeIndex = 0; routeIndex < newRouteFractions.get(ODPairIndex).size(); routeIndex++ ){
				Arrays.fill(tempRouteFractions.get(ODPairIndex).get(routeIndex), previousTimeClick, (int) (tEnd/tStep), 
						tempRouteFractions.get(ODPairIndex).get(routeIndex)[previousTimeClick-1]);
			}

		}


		//save finalRoutes and finalRouteFractions
		finalRoutes = cloneRoutes(newRoutes);
		finalRouteFractions = tempRouteFractions;

	}




	private ArrayList< ArrayList<Double[]>> generateEmptyRouteFractions(){

		//generate the structure that will contain the routefractions per OD 
		ArrayList< ArrayList<Double[]>> tempRouteFractions = new ArrayList< ArrayList<Double[]>>();

		for(int ODPairIndex = 0; ODPairIndex< newRouteFractions.size(); ODPairIndex++){

			ArrayList<Double[]> routeFractionsForOD = new ArrayList<Double[]>();

			for(int routeIndex = 0; routeIndex < newRouteFractions.get(ODPairIndex).size(); routeIndex++ ){				
				routeFractionsForOD.add(new Double[(int)(tEnd/tStep)]);
			}
			tempRouteFractions.add(routeFractionsForOD);
		}

		return tempRouteFractions;	
	}



	private void computeNumMinCostAndNumRtsWithMinCost(){

		//compute min cost per OD and time
		minCostPerOD = new ArrayList<double[]>();

		for(int ODPairIndex = 0; ODPairIndex< costPerRoute.size(); ODPairIndex++){
			double[] minCosts = new double[(int)(tEnd/tStep)];

			for(int timeClick = 0; timeClick < (int)(tEnd/tStep); timeClick++ ){

				double minCost = Double.MAX_VALUE; //fake cost value

				for(int routeIndex = 0; routeIndex < costPerRoute.get(ODPairIndex).size(); routeIndex++ ){

					//check if minimum
					if(costPerRoute.get(ODPairIndex).get(routeIndex)[timeClick] < minCost )
					{	
						//save it
						minCost = costPerRoute.get(ODPairIndex).get(routeIndex)[timeClick];
					}	
				}
				minCosts[timeClick] = minCost; 

			}
			minCostPerOD.add(minCosts);
		}



		//compute num routes with min Cost per OD and time
		numRtsWithMinCostPerOD = new ArrayList<int[]>();
		for(int ODPairIndex = 0; ODPairIndex< newRoutes.size(); ODPairIndex++){
			//initialize
			numRtsWithMinCostPerOD.add(new int[(int)(tEnd/tStep)]);
			Arrays.fill(numRtsWithMinCostPerOD.get(ODPairIndex),0);

			for(int timeClick = 0; timeClick < (int)(tEnd/tStep); timeClick++ ){

				for(int routeIndex = 0; routeIndex < newRouteFractions.get(ODPairIndex).size(); routeIndex++ ){

					//check if minimum
					if(costPerRoute.get(ODPairIndex).get(routeIndex)[timeClick] == minCostPerOD.get(ODPairIndex)[timeClick] )
					{	

						//increase the counter if this route has min cost
						numRtsWithMinCostPerOD.get(ODPairIndex)[timeClick] = numRtsWithMinCostPerOD.get(ODPairIndex)[timeClick] + 1;
					}	
				}
			}

		}
	}


	private void updateCostsPerRoute(){

		costPerRoute =  new ArrayList<ArrayList<double[]>>();

		//for all routes compute and save the cost!
		for(int ODPairIndex = 0; ODPairIndex< newRoutes.size(); ODPairIndex++){

			for(int routeIndex = 0; routeIndex< newRoutes.get(ODPairIndex).size(); routeIndex++){

				PathRepresentation path = newRoutes.get(ODPairIndex).get(routeIndex);

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
					saveCostForARouteAndTime(ODPairIndex, routeIndex, timeClick, routeTT);
				}

			}
		}

	}

	private void saveCostForARouteAndTime(int ODPairIndex, int routeIndex, int timeClick, double routeTT){

		//initialisation 
		if(costPerRoute == null){
			costPerRoute = new  ArrayList<ArrayList<double[]>>();
		}

		//required in case of new OD pair
		if(ODPairIndex >= costPerRoute.size()){
			costPerRoute.add(new ArrayList<double[]>());
		}

		//required in case of new route that was not considered previously
		if(routeIndex >= costPerRoute.get(ODPairIndex).size()){
			costPerRoute.get(ODPairIndex).add(new double[(int)(tEnd/tStep)]);
		}

		//save the cost (route travel time)
		costPerRoute.get(ODPairIndex).get(routeIndex)[timeClick] = routeTT;		

	}


	private void saveDeltaForARouteAndTime(int ODPairIndex, int routeIndex, int timeClick, double delta){

		if(cumulativeOfDeltas == null){
			cumulativeOfDeltas = new  ArrayList<ArrayList<Double>>();
		}

		//Required in case of new OD pair
		if(ODPairIndex >= cumulativeOfDeltas.size()){
			cumulativeOfDeltas.add(new ArrayList<Double>());
		}

		//Required in case of new route that was not considered previously
		if(routeIndex >= cumulativeOfDeltas.get(ODPairIndex).size()){
			//produce enough empty deltas
			while(routeIndex >= cumulativeOfDeltas.get(ODPairIndex).size())
			{
				cumulativeOfDeltas.get(ODPairIndex).add(0.0);
			}
		}

		if (timeClick == 0){
			//Initialize the cumulative for this route at 0.0
			cumulativeOfDeltas.get(ODPairIndex).set(routeIndex,0.0);	

		}
		else{
			//Otherwise add the delta
			cumulativeOfDeltas.get(ODPairIndex).set(routeIndex,cumulativeOfDeltas.get(ODPairIndex).get(routeIndex) + delta); 	

		}


	}

}