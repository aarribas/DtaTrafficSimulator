package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


import com.aarribas.traffictools.PathRepresentation;
import com.aarribas.traffictools.TravelTimeManager;
public class TrafficSwappingHeuristicDEC extends TrafficSwappingHeuristic{

	private ArrayList< ArrayList<PathRepresentation>> newRoutes;
	private ArrayList< ArrayList<Double[]>> newRouteFractions;

	private ArrayList< ArrayList<PathRepresentation>> oldRoutes;
	private ArrayList< ArrayList<Double[]>> oldRouteFractions;

	private double iteration;

	private double propFactor;

	//some structures required to pass complex parameters to the GP Context
	private  ArrayList<ArrayList<double[]>> costPerRoute;
	private  ArrayList<ArrayList<double[]>> cumulativeOfDeltas;
	private  ArrayList<int[]>  numRtsWithMinCostPerOD;
	private  ArrayList<double[]> minCostPerOD;

	public TrafficSwappingHeuristicDEC(double alpha){
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

		//compute costs for all routes
		updateCostsPerRoute();

		//compute number of optimal routes and the minimum cost -so far- per ODPair
		computeNumOptimalRtsAndMinRtFrac();

		ArrayList< ArrayList<Double[]>> tempRouteFractions =  new ArrayList< ArrayList<Double[]>>();

		//compute routeFractions for non optimal routes and normalise routeFractions for optimal routes
		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRoutes.size(); setOfRoutesIndex++){

			ArrayList<Double[]> fractionsForOD = new ArrayList<Double[]>();
			double[] cumulativeOfRouteFractions = new double[(int)(tEnd/tStep)];
			Arrays.fill(cumulativeOfRouteFractions, 0.0);

			//first go through the all routes and compute the new routeFractions for the non optimal or just save an uninitialized array for the optimal
			for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){

				//temp fractions
				Double[] tempFractions = new Double[newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length];


				if(fractionsIndex < oldRouteFractions.get(setOfRoutesIndex).size()){

					//the following applies to routes already seen 
					for(int fracIndex = 0; fracIndex < newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length; fracIndex++ ){

						//compute correction only if the cost is not minimal for this route and instant
						if(costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] != minCostPerOD.get(setOfRoutesIndex)[fracIndex]){

							double rtFrac = oldRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];

							//compute the cost difference between this route and the optimal route
							double normCostDiff = costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] - 
									minCostPerOD.get(setOfRoutesIndex)[fracIndex];

//							if(normCostDiff < 0){
//								System.out.println(oldRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] + " " + 
//										newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex]);
//								Scanner scan =new Scanner(System.in);
//								scan.nextLine();
//							}
							//normalise
							//							System.out.println(normCostDiff);
							//							normCostDiff = normCostDiff / costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] ;
							//							System.out.println(" norm:" + normCostDiff);

							double delta= 0.0;
							if(cumulativeOfDeltas == null || setOfRoutesIndex >= cumulativeOfDeltas.size()){

								delta = normCostDiff;
							}
							else{
								if(fractionsIndex >= cumulativeOfDeltas.get(setOfRoutesIndex).size()){
									delta = normCostDiff;
								}
								else{

									delta = normCostDiff + cumulativeOfDeltas.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];
								}
							}

							//	System.out.println(delta + normCostDiff + cumulativeOfDeltas.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] );


							//Limit the correction to some common sense values

							if(delta < 0.0){
//
//								//min route swapping is 0
//								System.out.println(delta  + "HEY" + normCostDiff + " " + (delta - normCostDiff));
								delta = 0.0;
//								System.out.println(delta  + "HEY");


							}
							else if(delta > rtFrac/propFactor){

								//maximum route swapping is the current traffic on that route
								delta = rtFrac/propFactor;

							}

//
//							if(delta !=0){
//								System.out.println("->" + setOfRoutesIndex + " " + fractionsIndex);
//								System.out.println(delta + " " + fracIndex);
//								System.out.println(cumulativeOfDeltas.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex]);
//							}

							//System.out.println(delta  + "HEY");
							//to be compliant with the DEC article
							delta = -delta;



							//add current delta to the cumulative of deltas
							saveDeltaForARouteAndTime(setOfRoutesIndex, fractionsIndex, fracIndex, delta);

							//save the new route fraction
							tempFractions[fracIndex] = rtFrac + propFactor*delta;

							//round to fix precision mistakes
							if(tempFractions[fracIndex] < 0)
							{
								tempFractions[fracIndex] = 0.0;
							}

							if(tempFractions[fracIndex] > 1)
							{
								tempFractions[fracIndex] = 1.0;
							}

							//update the cumulativeOfRouteFractions
							cumulativeOfRouteFractions[fracIndex] = cumulativeOfRouteFractions[fracIndex] + tempFractions[fracIndex];
						}
					}

				}
				else{
					//COMPLETELY NEW ROUTE!
					//for completely new routes the old fractions is 0 
					//if the route is optimal for some instant then the new rtFraction is obtained via normalisation later on

					for(int fracIndex = 0; fracIndex < newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length; fracIndex++ ){
						//save the new route fraction
						if(newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] == 0){
							tempFractions[fracIndex] = 0.0;
							saveDeltaForARouteAndTime(setOfRoutesIndex, fractionsIndex, fracIndex, 0.0);
						}
					}
				}

				//already save the references to the temporal fractions - note that the tempFractions for optimal routes are uninitialized
				fractionsForOD.add(tempFractions);

			}

			//finally normalise routeFractions for all routes with minimum cost per instant
			for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){

				for(int fracIndex = 0; fracIndex < newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length; fracIndex++ ){
					
					//default rtFrac is 0, this is valid for route Fractions that are completely new
					double rtFrac = 0.0;

					if(fractionsIndex < oldRouteFractions.get(setOfRoutesIndex).size()){
						//otherwise we have to use an old routeFraction
						rtFrac = oldRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];
					}

					fractionsForOD.get(fractionsIndex)[fracIndex] = 1.0 - (cumulativeOfRouteFractions[fracIndex] / (double)(numRtsWithMinCostPerOD.get(setOfRoutesIndex)[fracIndex]));
					//compute the corresponding delta for this route
					double delta = (fractionsForOD.get(fractionsIndex)[fracIndex]  - rtFrac)/propFactor * (double)(numRtsWithMinCostPerOD.get(setOfRoutesIndex)[fracIndex]);
					saveDeltaForARouteAndTime(setOfRoutesIndex, fractionsIndex, fracIndex, delta);

				}

			}


			//save the routeFractions for this OD pair
			tempRouteFractions.add(fractionsForOD);

		}


		//save finalRoutes and finalRouteFractions
		finalRoutes = cloneRoutes(newRoutes);
		finalRouteFractions = tempRouteFractions;

		//		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRoutes.size(); setOfRoutesIndex++){
		//			for(int fractionsIndex = 0; fractionsIndex < tempRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){
		//				System.out.println("OD " + setOfRoutesIndex + " route " + fractionsIndex);
		//				System.out.println(Arrays.toString(tempRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)));
		//				Scanner scan = new  Scanner(System.in);
		//				scan.nextLine();
		//			}
		//		}

	}

	private void computeNumOptimalRtsAndMinRtFrac(){

		//compute min cost per OD and time
		minCostPerOD = new ArrayList<double[]>();

		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRoutes.size(); setOfRoutesIndex++){
			double[] minCosts = new double[(int)(tEnd/tStep)];
			for(int fracIndex = 0; fracIndex < (int)(tEnd/tStep); fracIndex++ ){
				double minCost = costPerRoute.get(setOfRoutesIndex).get(0)[fracIndex];

				for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){

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
			cumulativeOfDeltas = new  ArrayList<ArrayList<double[]>>();
		}

		//required in case of new OD pair
		if(setOfRoutesIndex >= cumulativeOfDeltas.size()){
			cumulativeOfDeltas.add(new ArrayList<double[]>());
		}

		//required in case of new route that was not considered previously
		if(routeIndex >= cumulativeOfDeltas.get(setOfRoutesIndex).size()){
			cumulativeOfDeltas.get(setOfRoutesIndex).add(new double[(int)(tEnd/tStep)]);
			//no changes so far hence all to 0
			Arrays.fill(cumulativeOfDeltas.get(setOfRoutesIndex).get(routeIndex), 0.0);
		}

		//	System.out.println(cumulativeOfDeltas.get(setOfRoutesIndex).get(routeIndex)[timeClick] + " " + delta);

		//add the delta to the cumulative
		cumulativeOfDeltas.get(setOfRoutesIndex).get(routeIndex)[timeClick] = cumulativeOfDeltas.get(setOfRoutesIndex).get(routeIndex)[timeClick] + delta;	


	}

}