package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


import com.aarribas.traffictools.PathRepresentation;
import com.aarribas.traffictools.TravelTimeManager;
public class TrafficSwappingHeuristicDEC2 extends TrafficSwappingHeuristic{

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
	private  ArrayList<int[]> indexOptimalRtPerOD;

	public TrafficSwappingHeuristicDEC2(double alpha){
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
		
		//can run this one with alpha 1 and/or control the division of delta in the file directly

		//compute costs for all routes
		updateCostsPerRoute();

		//compute number of optimal routes and the minimum cost -so far- per ODPair
		computeNumMinCostAndNumRtsWithMinCost();

		//compute the id for the shortest route per OD and time
		computeOptimalRtPerOD();
		
		ArrayList< ArrayList<Double[]>> tempRouteFractions =  new ArrayList< ArrayList<Double[]>>();

		//compute routeFractions for non optimal routes and normalise routeFractions for optimal routes
		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRoutes.size(); setOfRoutesIndex++){

			ArrayList<Double[]> fractionsForOD = new ArrayList<Double[]>();
			double[] cumulativeOfRouteFractions = new double[(int)(tEnd/tStep)];
			Arrays.fill(cumulativeOfRouteFractions, 0.0);

			//first go through the all routes and compute the new routeFractions for the non optimal or just save an uninitialized array for the optimal
			for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){

				//temp fractions
				Double[] tempFractions = new Double[(int)(tEnd/tStep)];


				if(fractionsIndex < oldRouteFractions.get(setOfRoutesIndex).size()){

					//the following applies to routes already seen 
					for(int fracIndex = 0; fracIndex < (int)(tEnd/tStep); fracIndex++ ){

						//compute correction only if the cost is not minimal for this route and instant
						if(newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] != 1.0){


							PathRepresentation path = oldRoutes.get(setOfRoutesIndex).get(fractionsIndex);
							int startNodeIndex = path.nodeIndexes[0];
							int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];

							double rtFrac = oldRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];
 
							//compute the cost difference between this route and the optimal route
							double normCostDiff = (costPerRoute.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] - 
									costPerRoute.get(setOfRoutesIndex).get(indexOptimalRtPerOD.get(setOfRoutesIndex)[fracIndex])[fracIndex]);
							
							double delta= 0.0;
							
							//if there is an excess cost for a non optimal route we try to fix it
							if(normCostDiff > 0){
							if(expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]!= 0.0 && rtFrac != 0.0){
//							System.out.println("fuck" + "set "  + setOfRoutesIndex + " routeID" +  fractionsIndex + "cost " +  normCostDiff + "time " + fracIndex);
							}
								
								if(cumulativeOfDeltas == null || setOfRoutesIndex >= cumulativeOfDeltas.size()){

									delta = -normCostDiff;
								}
								else{
									if(fractionsIndex >= cumulativeOfDeltas.get(setOfRoutesIndex).size()){
										delta = -normCostDiff; 
									}
									else{

										delta = -normCostDiff - cumulativeOfDeltas.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];
						
									}
								}
								
								if(iteration > 3){
									if(expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]!= 0.0 && rtFrac != 0.0)
									{
//									System.out.println(cumulativeOfDeltas.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex]);
//									System.out.println("cost" + normCostDiff);
									}
								}
								
						
								if(delta > 0.0){
									
									if(expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]!= 0.0 && rtFrac != 0.0){
//									System.out.println("MINED by" + delta );
									}
									delta = 0.0;
								}
								else if(delta < -(rtFrac/propFactor)*expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]){

									//maximum route swapping is the current traffic on that route
									delta = -(rtFrac/propFactor)*expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex];
									if(expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]!= 0.0 && rtFrac != 0.0){
//										System.out.println("MAXED");
									}

								}
								
									
								if(expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]!= 0.0 && rtFrac != 0.0){
//									System.out.println(delta + " " + -(delta+normCostDiff));
									
								}

								//save the new route fraction
								if(expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]!= 0.0){

									tempFractions[fracIndex] = (rtFrac*expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex] + propFactor*delta)
											/ expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex];
								}
								else{
									tempFractions[fracIndex] = rtFrac; //no change
									delta = 0.0;
								}
							}
							else{
								tempFractions[fracIndex] = rtFrac; //no change
								delta = 0.0;
							}
						
							

							saveDeltaForARouteAndTime(setOfRoutesIndex, fractionsIndex, fracIndex, delta/1000.0);
							
							//add current delta to the cumulative of deltas
							
							
//							if(delta!=0.0  && expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex] != 0.0){
//								System.out.println("still trying");
//							}

//							if(delta !=0.0 && expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex] != 0.0 && fracIndex == 201)
//							{
//
//								System.out.println("max delta" + rtFrac/propFactor*expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex]);
//								System.out.println("still trying" + delta);
//								System.out.println("set" + setOfRoutesIndex + "  i:" +  fractionsIndex + " " + fracIndex);
//								System.out.println("rtFrac" + rtFrac + "  cost:" +  normCostDiff );
//								System.out.println("cumu" + (-delta -normCostDiff));
//
//
//							}

//							if(tempFractions[fracIndex]< 0.00000000001){
//								tempFractions[fracIndex] = 0.0;
//							}
//							else if(tempFractions[fracIndex]>0.9999999999){
//
//								tempFractions[fracIndex] = 1.0;
//
//							}
							//update the cumulativeOfRouteFractions
							cumulativeOfRouteFractions[fracIndex] = cumulativeOfRouteFractions[fracIndex] + tempFractions[fracIndex];
						}
					}


				}
				else{
					//COMPLETELY NEW ROUTE!
					//for completely new routes the old fraction is 0 
					//if the route is optimal for some instant then the new rtFraction is obtained via normalisation later on
					//otherwise there is no traffic swapping applicable for this route

					for(int fracIndex = 0; fracIndex < (int)(tEnd/tStep); fracIndex++ ){
						//initialise the fraction and the delta
						tempFractions[fracIndex] = 0.0;
						saveDeltaForARouteAndTime(setOfRoutesIndex, fractionsIndex, fracIndex, 0.0);
					}
				}

				//already save the references to the temporal fractions - note that the tempFractions for optimal routes are uninitialized
				fractionsForOD.add(tempFractions);

			}

			//finally normalise routeFractions for all routes with minimum cost per instant
			for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){

				for(int fracIndex = 0; fracIndex < newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length; fracIndex++ ){
					if(newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] == 1.0){
						//default rtFrac is 0, this is valid for route Fractions that are completely new
						double rtFrac = 0.0;

						if(fractionsIndex < oldRouteFractions.get(setOfRoutesIndex).size()){
							rtFrac = oldRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex];
						}


						fractionsForOD.get(fractionsIndex)[fracIndex] = (1.0 - cumulativeOfRouteFractions[fracIndex]);
						//compute the corresponding delta for this route

						PathRepresentation path = newRoutes.get(setOfRoutesIndex).get(fractionsIndex);
						int startNodeIndex = path.nodeIndexes[0];
						int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];

						double delta = (fractionsForOD.get(fractionsIndex)[fracIndex]  - rtFrac)/propFactor ;
						delta = delta * expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex];
//
//						if(delta !=0.0 && expandedODMatrices.get(fracIndex)[startNodeIndex][endNodeIndex] != 0.0 && fracIndex == 201)
//						{
//							System.out.println("fixed");
//							System.out.println("set" + setOfRoutesIndex + "  i:" +  fractionsIndex + " " + fracIndex);
//							System.out.println("still trying" + delta);
//
//						}



						saveDeltaForARouteAndTime(setOfRoutesIndex, fractionsIndex, fracIndex, delta/1000.0);
					}
				}

			}


			//save the routeFractions for this OD pair
			tempRouteFractions.add(fractionsForOD);

		}


		//save finalRoutes and finalRouteFractions
		finalRoutes = cloneRoutes(newRoutes);
		finalRouteFractions = tempRouteFractions;
		//			for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRoutes.size(); setOfRoutesIndex++){
		//
		//
		//				for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){
		//
		//					for(int fracIndex = 0; fracIndex < newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length; fracIndex++ ){
		//						if(fracIndex == 201){
		//							System.out.println("set " + setOfRoutesIndex + " frac:" + fractionsIndex);
		//							System.out.println(finalRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex]);
		//						}
		//					}
		//
		//				}
		//			}

	}

	private void computeOptimalRtPerOD(){
		
		indexOptimalRtPerOD = new ArrayList<int[]>();

		for(int setOfRoutesIndex = 0; setOfRoutesIndex< newRoutes.size(); setOfRoutesIndex++){

			int[] indexOptRt = new int[(int)(tEnd/tStep)];

			//compute for this OD pair the number of optimal routes per instant
			for(int fractionsIndex = 0; fractionsIndex < newRouteFractions.get(setOfRoutesIndex).size(); fractionsIndex++ ){
				for(int fracIndex = 0; fracIndex < newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex).length; fracIndex++ ){

					if(newRouteFractions.get(setOfRoutesIndex).get(fractionsIndex)[fracIndex] == 1.0){

						indexOptRt[fracIndex] = fractionsIndex;

					}
				}

			}
			indexOptimalRtPerOD.add(indexOptRt);
		}
	}


	private void computeNumMinCostAndNumRtsWithMinCost(){

		//compute min cost per OD and time
		minCostPerOD = new ArrayList<double[]>();

		for(int setOfRoutesIndex = 0; setOfRoutesIndex< costPerRoute.size(); setOfRoutesIndex++){
			double[] minCosts = new double[(int)(tEnd/tStep)];

			for(int fracIndex = 0; fracIndex < (int)(tEnd/tStep); fracIndex++ ){

				double minCost = 100.0;

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