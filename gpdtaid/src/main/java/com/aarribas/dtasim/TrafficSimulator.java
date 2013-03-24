package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;

import com.aarribas.traffictools.CumulativeBasedCalculator;
import com.aarribas.traffictools.CumulativeBasedCalculator.SIM_FLOW_OPTION;
import com.aarribas.traffictools.DynamicDijkstra;
import com.aarribas.traffictools.PathFinder;
import com.aarribas.traffictools.PathRepresentation;
import com.aarribas.traffictools.TravelTimeManager;

public class TrafficSimulator {

	private ArrayList<double[][]> expandedODMatrices = new ArrayList<double[][]>();
	private TrafficData tfData = null;
	private TrafficODPair[] ODPairs = null;
	private double tEnd;
	private double tStep;
	private ArrayList<double[]> linkTravelTimes = new ArrayList<double[]>();
	private ArrayList<double[]> linkSpeeds = new ArrayList<double[]>();
	private ArrayList<ArrayList<double[][]>> turningFractions;
	private PathFinder pathFinder;

	public TrafficSimulator(String fileName, double tEnd, double tStep){
		this.tEnd = tEnd;
		this.tStep = tStep;

		TrafficDataLoader loader = new TrafficDataLoader();
		tfData = loader.load(fileName);

		expandODMatrices();
		computeODPairs();
		computeInitialTravelTimes();
		computeInitialSpeeds();

		pathFinder = new DynamicDijkstra(tfData, ODPairs, linkTravelTimes, tEnd, tStep, 0, 100);
		pathFinder.findPath();

		computeTurningFractions(100, 0, linkSpeeds);
		
		
//		//code to quick - debug computeTurningFractions
//		int index = 0;
//
//		for(ArrayList<double[][]> turningFraction : turningFractions){
//			System.out.println(index);
//			if(!turningFraction.isEmpty()){
//				System.out.println("NOT EMPTY");
//				int index2 = 0;
//				for(double[][] turFrac: turningFraction){
//					String newS = new String();
//
//					for(double[] turFracR : turFrac){
//						newS = newS.concat(Arrays.toString(turFracR) + index2);
//					}
//					index2++;
//					System.out.println(newS);
//				}
//			}
//			else{
//				System.out.println("EMPTY");
//			}
//
//
//			
//
//				Scanner scan = new Scanner(System.in);
//				scan.nextLine();
//
//			
//			index++;
//
//		}
		
		LTM ltm = new LTM(expandedODMatrices, tfData, tEnd, tStep, turningFractions);
		ltm.run();
		
//		//code to quick - debug cumulatives
//		System.out.println(Arrays.toString(tfData.links.get(0).downStreamCumulative));
//		for(int i= 0; i < tfData.links.get(0).downStreamCumulative.length; i++){
//			System.out.println(i);
//			System.out.println(tfData.links.get(0).downStreamCumulative[i]);
//		}
//		int  i = 0;
//		for(TrafficLink link : tfData.links){
//			System.out.println("link " + i);
//			System.out.println("down max " + link.downStreamCumulativeMax);
//			System.out.println("up max " + link.upStreamCumulativeMax);
//			i++;
//		}
		
		double[][] speeds = CumulativeBasedCalculator.calculateCumulativeToSpeeds(tfData.links, tEnd, tStep);
		double[][] flows = CumulativeBasedCalculator.calculateCumulativeToFlows(tfData.links, SIM_FLOW_OPTION.UPSTREAM, tEnd, tStep);
		double[][] density = CumulativeBasedCalculator.calculateCumulativeToDensity(tfData.links, tEnd, tStep);
		System.out.println("END");
	}

	private void expandODMatrices(){

		//start at the first timeSlice
		int timeSliceNumber = 0;

		//a little error Tolerance is required in order to compare too doubles (foreseen representation errors of floating point)
		double doubleErrorTolerance = 0.0000001d;

		//compute all odmatrices - note that we expect the tEnd to be a multiple of tStep
		//TO DO: add a check and possibly throw an exception
		for(int i = 0; i < (int)(tEnd/tStep); i++){

			double ctime = tStep * i;

			RealMatrix expandedODMatrix;

			//if we have reached the last timeslice we know that it is safe to add the remaining odmatrix
			if(timeSliceNumber == tfData.timeSlices.size()-1){
				//save the od matrix as read from the .mat file for the corresponding timeslice
				expandedODMatrix = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber));
				//scale in time to obtain flows
				expandedODMatrix = expandedODMatrix.scalarMultiply(tStep);
			}
			else{

				//check if we change timeslice at a time not multiple of the tStep
				//the first timeSlice is expected to be 0 this is a mistake in the .mat
				//TO DO change .mat or adapt code to more reasonable values
				if((ctime + tStep) > tfData.timeSlices.get(timeSliceNumber+1) + doubleErrorTolerance && ctime !=0 ){

					//part of the flow in the matrix is due to OD prior to timeSlice change
					expandedODMatrix = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber));
					expandedODMatrix = expandedODMatrix.scalarMultiply(tfData.timeSlices.get(timeSliceNumber) - ctime);

					//the remaining flow is due to the OD in the next timeSlice
					RealMatrix temp = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber + 1));
					temp = temp.scalarMultiply(tStep - tfData.timeSlices.get(timeSliceNumber));
					expandedODMatrix = expandedODMatrix.add(temp);

				}
				else{
					//if there is no timeslice change it is save to add the contribution of the ODMatrix for that timeSlice
					expandedODMatrix = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber));
					expandedODMatrix = expandedODMatrix.scalarMultiply(tStep);
				}

				//increase the timeSliceNumber, in case the current time moves on to a new timeSlice
				if(ctime + tStep >= tfData.timeSlices.get(timeSliceNumber+1)){
					timeSliceNumber++;
				}
			}
			expandedODMatrices.add(expandedODMatrix.getData());
		}

	}

	private void computeODPairs(){

		HashSet<TrafficODPair> tempODPairs = new HashSet<TrafficODPair>();

		for(int i=0; i<tfData.ODMatrices.size(); i++){

			//set current ODMatrix and dimensions
			double[][] currMatrix = tfData.ODMatrices.get(i);
			int NumColumns = currMatrix[0].length;
			int NumRows = currMatrix.length;

			//visit all cells
			for(int row=0; row<NumRows; row++  ){
				for(int column=0; column<NumColumns; column++){

					//if cell is empty add the od pair to the set (avoids duplicates)
					if(currMatrix[row][column] != 0){						
						//save to the set of tuples / same ODPair can only be added once
						tempODPairs.add(new TrafficODPair(tfData.nodes.get(row).id, tfData.nodes.get(column).id));

					}

				}
			}

		}

		//save ODPairs to array.
		ODPairs = new TrafficODPair[tempODPairs.size()];
		tempODPairs.toArray(ODPairs);

	}

	private void computeInitialTravelTimes(){

		for(TrafficLink link : tfData.links){
			double[] travelTimes = new double[(int)(tEnd/tStep)];

			//we initialize traveltimes to length/freespeed
			java.util.Arrays.fill(travelTimes, link.length/link.freeSpeed);	

			//add the corresponding array of traveltimes
			linkTravelTimes.add(travelTimes);
		}


	}

	private void computeInitialSpeeds(){

		for(TrafficLink link : tfData.links){
			double[] speeds = new double[(int)(tEnd/tStep)];

			//we initialize traveltimes to the linkfreespeed
			java.util.Arrays.fill(speeds, link.freeSpeed);	

			//add the corresponding array of freespeeds
			linkSpeeds.add(speeds);
		}


	}

	private void computeTurningFractions(int timeClicksOFTurningFractionsInterval, int timeClicksShift, ArrayList<double[]> currentLinkSpeeds){

		//prepare the turningFractions structure of size nodes x time steps
		turningFractions= new ArrayList<ArrayList<double[][]>>();

		for(int tfIndex = 0; tfIndex< tfData.nodes.size(); tfIndex++){
			turningFractions.add(new ArrayList<double[][]>((int) (tEnd/tStep)));
		}

		//intialise the previous time click to 0
		int prevTimeClick = 0;

		//we start at the interval 0
		int interval = 0;
		int timeClick = timeClicksShift + timeClicksOFTurningFractionsInterval*interval; //equals timeClickShift. Whole equation for consistency.

		//prepare the maps of outgoing and incoming links (index based) per node
		HashMap<Double, ArrayList<Integer>> outgoingLinkIndexes = new HashMap<Double, ArrayList<Integer>>();
		HashMap<Double, ArrayList<Integer>> incomingLinkIndexes = new HashMap<Double, ArrayList<Integer>>();
		computeIncomingAndOutgoingLinksPerNode(outgoingLinkIndexes, incomingLinkIndexes);

		//prepare the travelcosts matrix (divides links length per current link speed.
		ArrayList<double[]> currTravelCosts = TravelTimeManager.computeTravelCostsForGivenSpeeds(tfData.links, currentLinkSpeeds);


		while( timeClick < (int) (tEnd/tStep)){
			

			for(int nodeIndex = 0; nodeIndex < tfData.nodes.size(); nodeIndex++){
				TrafficNode currNode = tfData.nodes.get(nodeIndex);

				//if there are incoming links and outgoing links for this node, initialise the turning fractions accordingly
				if(incomingLinkIndexes.containsKey(currNode.id) && outgoingLinkIndexes.containsKey(currNode.id))
				{
					double[][] turningFraction = new double[incomingLinkIndexes.get(currNode.id).size()][outgoingLinkIndexes.get(currNode.id).size()];
					for(double[] row : turningFraction){
						Arrays.fill(row, 0);
					}

					//fill from the previous timeclick to current timeclick - 1 with turningFraction
					for(int j = prevTimeClick+1; j <timeClick; j++){
						turningFractions.get(nodeIndex).add(turningFraction);
					}
					//fill the current timeclick with turningFraction
					turningFractions.get(nodeIndex).add(turningFraction);

				}


				for(int setOfRoutesIndex = 0; setOfRoutesIndex < pathFinder.getRoutes().size(); setOfRoutesIndex++){
					
					//extract set of routes 
					ArrayList<PathRepresentation> setOfRoutes = pathFinder.getRoutes().get(setOfRoutesIndex);

					//visit each route in the set of routes
					for(int routeIndex = 0; routeIndex < setOfRoutes.size(); routeIndex++){

						PathRepresentation route = setOfRoutes.get(routeIndex);

						//obtain the index in the route of the nodeIndex
						int indexNodeInRoute = -1;
						indexNodeInRoute = route.findIndexInPathOfNodeIndex(nodeIndex);

						if(indexNodeInRoute == -1 || pathFinder.getRouteFractions().get(setOfRoutesIndex).get(routeIndex)[timeClick] == 0 || route.isBorderNode(nodeIndex)){

							//nothing							
						}
						else{

							//compute current travel time node 2 node
							double travelTime = TravelTimeManager.computeTravelTimeNode2NodeForGivenCost(currTravelCosts, route.linkIndexes, indexNodeInRoute -1, 1, timeClick, tEnd, tStep);

							//compute starting time for the corresponding OD flow
							double timeStartODFlow = tStep*timeClick - travelTime;
							int timeClickStartODFlow = (int)(timeStartODFlow/tStep);
							if(timeStartODFlow < 0){
								//nothing to do 
								//flow can only get here if it started before the start of the simulation
							}
							else{

								//get the index of the incoming link in the route
								int incomingLinkPath = route.linkIndexes[indexNodeInRoute-1];
								int incomingLinkPathIndex = -1;
								for(int i = 0;  i < incomingLinkIndexes.get(currNode.id).size(); i++){
									if(incomingLinkIndexes.get(currNode.id).get(i) == incomingLinkPath){
										incomingLinkPathIndex = i;
										break;
									}
								}

								//get the index for the outgoing link in the route
								int outgoingLinkPath = route.linkIndexes[indexNodeInRoute];
								int outgoingLinkPathIndex = -1;
								for(int i = 0;  i < outgoingLinkIndexes.get(currNode.id).size(); i++){
									if(outgoingLinkIndexes.get(currNode.id).get(i) == outgoingLinkPath){
										outgoingLinkPathIndex = i;
										break;
									}
								}

								//compute turningFractions for the current node and time click
								//Remark: turning Fraction = given turning fractino + route Fraction + flow according to ODMatrix
								//TODO Review in Cascetta's book

								turningFractions.get(nodeIndex).get(timeClick)[incomingLinkPathIndex][outgoingLinkPathIndex] 
										= turningFractions.get(nodeIndex).get(timeClick)[incomingLinkPathIndex][outgoingLinkPathIndex]
												+ pathFinder.getRouteFractions().get(setOfRoutesIndex).get(routeIndex)[timeClick]
														* expandedODMatrices.get(timeClickStartODFlow)[ODPairs[setOfRoutesIndex].getIndexStartNode(tfData.nodes)][ODPairs[setOfRoutesIndex].getIndexEndNode(tfData.nodes)];

							}


						}

					}//end for visiting routes

				}//end for visiting sets of routes

				//set turning fractions from previous click to current based on current - only if the node is not a border node (hence not empty)
				if(!turningFractions.get(nodeIndex).isEmpty()){
					RealMatrix tempMatrix = new Array2DRowRealMatrix(turningFractions.get(nodeIndex).get(timeClick));

					
					for(int row = 0; row< turningFractions.get(nodeIndex).get(timeClick).length; row++){
						double sumTF = 0;
						//compute the sum of turningFraction for a row
						for(int column = 0; column<turningFractions.get(nodeIndex).get(timeClick)[row].length; column++){

							sumTF = sumTF + turningFractions.get(nodeIndex).get(timeClick)[row][column];
						}

						if(sumTF>0){
							//save the normalised row
							RealVector vect= tempMatrix.getRowVector(row);
							tempMatrix.setRowVector(row, vect.mapDivide(sumTF));
						}
						else{
							//use the row from the previous fraction if normalisation was not required - as in original MATLAB code.
							double[] prevRow = turningFractions.get(nodeIndex).get(prevTimeClick)[row];
							tempMatrix.setRow(row, prevRow);
						}

					}
					
					//udpate turning fractions from previous time click to current
					for(int j = prevTimeClick+1; j<=timeClick; j++){
						turningFractions.get(nodeIndex).set(j, tempMatrix.getData());
					}
					

				}


			}//end for visiting nodes

			//save current time click as prevTimeClick
			prevTimeClick = timeClick;

			///move to the next route interval
			interval++;
			timeClick = timeClicksShift + timeClicksOFTurningFractionsInterval*interval; 

		}//end of while

		//for all  time clicks between prev and current timeclick fix the turningFractions to the ones at prevTimeClick
		//this is required to fix the turning fractions for the final routeInterval (from preTimeClick till end of time)
		for(int tClick=prevTimeClick+1; tClick<(int)(tEnd/tStep); tClick++){

			for(int nodeIndex = 0; nodeIndex < turningFractions.size(); nodeIndex++){

				if(!turningFractions.get(nodeIndex).isEmpty()){
					turningFractions.get(nodeIndex).add(turningFractions.get(nodeIndex).get(prevTimeClick));
				}
			}

		}


	}


	private void computeIncomingAndOutgoingLinksPerNode(HashMap<Double, ArrayList<Integer>> outgoingLinkIndexes, HashMap<Double, ArrayList<Integer>> incomingLinkIndexes){

		for(int linkIndex = 0; linkIndex < tfData.links.size(); linkIndex++){

			double startNodeId = tfData.links.get(linkIndex).startNode;
			double endNodeId = tfData.links.get(linkIndex).endNode;

			//if no entry for the start node in the outgoing, prepare to add
			if(!outgoingLinkIndexes.containsKey(startNodeId)){
				outgoingLinkIndexes.put(startNodeId, new ArrayList<Integer>());
			}

			//if no entry for the end node in the incoming, prepare to add
			if(!incomingLinkIndexes.containsKey(endNodeId)){
				incomingLinkIndexes.put(endNodeId, new ArrayList<Integer>());
			}

			//add the link index to each map
			outgoingLinkIndexes.get(startNodeId).add(linkIndex);
			incomingLinkIndexes.get(endNodeId).add(linkIndex);
		}
	}
	
	private void runDTA(){
		
		//init LTM
		int iteration = 1;
		int maxIterations = 30;
		
		//main loop
		while(iteration<maxIterations){
			
			//calculateTurningFRactions
			
			//update link data calling the LTM
			
			//obtain simulated speeds
			
			//obtain simulated speeds at arrival
			
			//compute travelTime
			
			//calculate new routes and routeFractions using dijkstra
			
			//recompute the gap
			
			//update routeFractions by means of the path swapping heuristic
			
			//move on to next iteration
			iteration++;
		}
		
		//recalculate turningFractions for final iteration
		
		//obtain final link data using LTM
		
	}

}
