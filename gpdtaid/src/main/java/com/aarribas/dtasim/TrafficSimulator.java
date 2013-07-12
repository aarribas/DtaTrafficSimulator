package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;

import com.aarribas.traffictools.CumulativeBasedCalculator;
import com.aarribas.traffictools.DynamicDijkstra;
import com.aarribas.traffictools.PathFinder;
import com.aarribas.traffictools.PathRepresentation;
import com.aarribas.traffictools.TrafficDataLogger;
import com.aarribas.traffictools.TravelTimeManager;

/**
 * TrafficSimulator encapsulates the logic and the methods to load a network and the OD flows from a .mat, and to 
 * run a either a simple DNL simulation or a complete DTA (default).
 * 
 * @author andresaan
 *
 */

public class TrafficSimulator {


	ArrayList< ArrayList<PathRepresentation>> oldRoutes;
	ArrayList<ArrayList<Double[]>> oldRouteFractions;

	ArrayList< ArrayList<PathRepresentation>> newRoutes;
	ArrayList<ArrayList<Double[]>> newRouteFractions;

	private ArrayList<double[][]> expandedODMatrices = new ArrayList<double[][]>();
	private TrafficData tfData = null;
	private TrafficODPair[] ODPairs = null;
	private double tEnd;
	private double tStep;
	private ArrayList<double[]> linkTravelTimes = new ArrayList<double[]>();
	private ArrayList<double[]> linkSpeeds = new ArrayList<double[]>();
	private ArrayList<double[]> linkSpeedsAtArrival = new ArrayList<double[]>();
	private ArrayList<ArrayList<double[][]>> turningFractions = new ArrayList<ArrayList<double[][]>>();
	private PathFinder pathFinder;
	private double gap, firstGap;
	private int iteration;
	private int timeClicksOfRouteInterval;
	
	boolean saveToFile;
	private TrafficDataLogger tfDatalogger;
	
	public enum VERBOSITY{
		SILENT,
		VERBOSE,
		VERY_VERBOSE
	}
	private VERBOSITY verbosity;
	
	
public TrafficSimulator(String fileName, double tEnd, double tStep, int timeClicksOfRouteInterval, VERBOSITY  verbosity, float netScale, float demandScale){
		
		this(fileName, tEnd, tStep, timeClicksOfRouteInterval, verbosity);
		tfData.scaleNetwork(netScale);
		tfData.scaleDemand(demandScale);
	}
	
	public TrafficSimulator(String fileName, double tEnd, double tStep, int timeClicksOfRouteInterval, VERBOSITY  verbosity){
		
		this(fileName, tEnd, tStep, timeClicksOfRouteInterval);
		this.verbosity = verbosity;
	}

	public TrafficSimulator(String fileName, double tEnd, double tStep, int timeClicksOfRouteInterval){
		this.saveToFile = false; //must be set explicitly
		this.tEnd = tEnd;
		this.tStep = tStep;
		this.timeClicksOfRouteInterval = timeClicksOfRouteInterval;
		
		//by default we are not silent
		this.verbosity = VERBOSITY.SILENT;
		
		TrafficDataLoader loader = new TrafficDataLoader();
		tfData = loader.load(fileName);



	}

	public void runDNLOnly(){
		expandODMatrices();
		computeODPairs();
		computeInitialTravelTimes();
		computeInitialSpeeds();

		oldRoutes = new ArrayList<ArrayList<PathRepresentation>>();
		oldRouteFractions = new ArrayList<ArrayList<Double[]>>();

		pathFinder = new DynamicDijkstra(tfData, ODPairs, tEnd, tStep, 0, timeClicksOfRouteInterval);
		pathFinder.findPath(linkTravelTimes, oldRoutes, oldRouteFractions);

		computeTurningFractions(timeClicksOfRouteInterval, 0, linkSpeeds, oldRoutes, oldRouteFractions);

		LTM ltm = new LTM(expandedODMatrices, tfData, tEnd, tStep);
		ltm.run(turningFractions);

		//		double[][] speeds = CumulativeBasedCalculator.calculateCumulativeToSpeeds(tfData.links, tEnd, tStep);
		//		double[][] flows = CumulativeBasedCalculator.calculateCumulativeToFlows(tfData.links, SIM_FLOW_OPTION.UPSTREAM, tEnd, tStep);
		//		double[][] density = CumulativeBasedCalculator.calculateCumulativeToDensity(tfData.links, tEnd, tStep);
	}
	
	private boolean verbose(){
		return verbosity == VERBOSITY.VERBOSE || verbosity == VERBOSITY.VERY_VERBOSE;
	}

	public void runDTA(int maxIterations, double gapReduction, TrafficSwappingHeuristic heuristic){

		if(verbose()){System.out.println("->NEW DTA BEGINS");}

		iteration = 1;

		expandODMatrices();
		computeODPairs();
		computeInitialSpeeds();
		computeInitialTravelTimes();

		oldRoutes = new ArrayList<ArrayList<PathRepresentation>>();
		oldRouteFractions = new ArrayList<ArrayList<Double[]>>();

		//compute initial paths
		pathFinder = new DynamicDijkstra(tfData, ODPairs, tEnd, tStep, 0, timeClicksOfRouteInterval);
		pathFinder.findPath(linkTravelTimes, oldRoutes, oldRouteFractions);

		//save first routes and routefractions
		oldRoutes = pathFinder.getRoutes();
		oldRouteFractions = pathFinder.getRouteFractions();

		//setup the ltm (note that only the updated turningFractions are passed to run)
		LTM ltm = new LTM(expandedODMatrices, tfData, tEnd, tStep);

		//save references to some basic info at heuristic level
		heuristic.oneTimeSetup(timeClicksOfRouteInterval, tEnd, tStep, tfData, linkSpeeds, expandedODMatrices);

		//main loop
		while(iteration<maxIterations){

			iteration++; //as in original MATLAB code

			if(verbosity == VERBOSITY.VERY_VERBOSE){System.out.println("it: " + iteration);}

			//calculateTurningFRactions
			computeTurningFractions(timeClicksOfRouteInterval, 0, linkSpeedsAtArrival, oldRoutes, oldRouteFractions);

			//update link data calling the LTM
			ltm.run(turningFractions);

			//compute link speeds and link speeds at arrival
			updateSpeeds();

			//compute travelTime
			updateTravelTimes();

			//calculate new routes and routeFractions using dynamic dijkstra
			pathFinder.findPath(linkTravelTimes, oldRoutes, oldRouteFractions);
			newRoutes = pathFinder.getRoutes();
			newRouteFractions = pathFinder.getRouteFractions();

			//recalculate the gap
			setGap(calculateGec());
			if(verbosity == VERBOSITY.VERY_VERBOSE){System.out.println("GAP:" + gap);}
			if(iteration == 2){firstGap = gap;}

			if(checkForConvergence(gapReduction)){
				return;
			}
			
			//save the gap to a file
			if(saveToFile) tfDatalogger.saveGap(iteration, gap);

			//compute routeFractions for next iteration by means of the path swapping heuristic
			heuristic.setup(oldRoutes, oldRouteFractions, newRoutes, newRouteFractions, iteration);
			heuristic.run();


			if(heuristic.getRoutes() == null || heuristic.getRouteFractions() == null){

				//if the heuristic routes/routeFractions are null, the heuristic is telling us to stop
				if(verbose()){System.out.println("->ABORTED");}
				return;
			}
			else{
				//otherwise save the routes and routeFractions as computed by the swapping heuristic prior to new iteration
				oldRoutes = heuristic.getRoutes();
				oldRouteFractions = heuristic.getRouteFractions();
			}

			//move on to next iteration

		}

		//recalculate turningFractions for final iteration
		computeTurningFractions(timeClicksOfRouteInterval, 0, linkSpeedsAtArrival,oldRoutes, oldRouteFractions);

		//obtain final link data using LTM
		ltm.run(turningFractions);

	}

	private boolean checkForConvergence(double gapReduction){
		if(gap < firstGap*gapReduction){
			if(verbose()){System.out.println("->CONVERGED");}
			return true;
		}
		else{
			return false;
		}
	}

	public void displayRouteFractionPerRouteInterval(){
		
		System.out.println("Route Fractions Per Route Interval");

		//display each available routeFractions per ODPair and route
		for(int setOfRoutesIndex = 0; setOfRoutesIndex< oldRoutes.size(); setOfRoutesIndex++){
			for(int routeIndex = 0; routeIndex< oldRoutes.get(setOfRoutesIndex).size(); routeIndex++){		

				PathRepresentation path = oldRoutes.get(setOfRoutesIndex).get(routeIndex);
				int startNodeIndex = path.nodeIndexes[0];
				int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];;
				
				System.out.println("ODPair: " + setOfRoutesIndex + " Route: " + routeIndex);
				for(int timeClick = 0; timeClick < (int)(tEnd/tStep); timeClick+=timeClicksOfRouteInterval){
					if(expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex] > 0.0){
						System.out.println("Time: " + timeClick + "Frac: " + oldRouteFractions.get(setOfRoutesIndex).get(routeIndex)[timeClick]);
					}
					else{
						System.out.println("Time: " + timeClick + "Frac: "  + "Irrelevant");
					}
				}
			}
		}
}
	
	public void displayRouteTravelTimesPerRoute(){
		
		System.out.println("Route Travel Times Per Route:");
		for(int setOfRoutesIndex = 0; setOfRoutesIndex< oldRoutes.size(); setOfRoutesIndex++){
			System.out.println("ODPair: " + setOfRoutesIndex);
			for(int routeIndex = 0; routeIndex< oldRoutes.get(setOfRoutesIndex).size(); routeIndex++){
				
				System.out.println("routeIndex: " + routeIndex);

				PathRepresentation path = oldRoutes.get(setOfRoutesIndex).get(routeIndex);
				int startNodeIndex = path.nodeIndexes[0];
				int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];
				int[] linkIndexes = path.linkIndexes;
				
				System.out.println("linkIndexes: " + Arrays.toString(linkIndexes));

				for(int timeClick = 0; timeClick < (int)(tEnd/tStep); timeClick+= timeClicksOfRouteInterval){
					double routeTT = 0.0;;
					for(int linkIndex : linkIndexes){
						double[] cost = new double[linkSpeeds.get(linkIndex).length];

						//compute instantaneous cost
						for(int costIndex = 0; costIndex < linkSpeeds.get(linkIndex).length; costIndex++ ){
							cost[costIndex] = tfData.links.get(linkIndex).length / linkSpeeds.get(linkIndex)[costIndex];
						}
						//compute complete routeTT (route cost or travel time)
						routeTT = routeTT + TravelTimeManager.computeTravelTimeForGivenCost(cost,timeClick*tStep + routeTT, tEnd, tStep);
						
						
					}
					if(expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex] != 0.0){
						System.out.println("time: " + timeClick + " tt: " + routeTT  +  " " + expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex] );
					}
					else{
						System.out.println("time: " + timeClick + " tt: " + "Irrelevant" );
					}
				}

			}
		}
		
	}

@SuppressWarnings("unused")
private double calculateGap(){

	//initialise the gap at 0
	double gap = 0;

	for(int setOfRoutesIndex = 0; setOfRoutesIndex< oldRoutes.size(); setOfRoutesIndex++){
		for(int routeIndex = 0; routeIndex< oldRoutes.get(setOfRoutesIndex).size(); routeIndex++){


			PathRepresentation path = oldRoutes.get(setOfRoutesIndex).get(routeIndex);
			int startNodeIndex = path.nodeIndexes[0];
			int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];
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

				if(newRouteFractions.get(setOfRoutesIndex).get(routeIndex)[timeClick] == 1.0){
					//nothing
				}
				else
				{

					//update gap given the previous gap cumulative and the route cost given an OD flow for this route
					gap = gap + oldRouteFractions.get(setOfRoutesIndex).get(routeIndex)[timeClick]* routeTT*expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex];

				}
			}

		}
	}
	return gap;

}



private double calculateGec(){

	ArrayList<ArrayList<HashMap<Integer,Double>>> costPerRoute =  new ArrayList<ArrayList<HashMap<Integer,Double>>>();

	for(int ODPairIndex = 0; ODPairIndex< newRoutes.size(); ODPairIndex++){

		for(int routeIndex = 0; routeIndex< newRoutes.get(ODPairIndex).size(); routeIndex++){

			PathRepresentation path = newRoutes.get(ODPairIndex).get(routeIndex);

			int[] linkIndexes = path.linkIndexes;

			for(int timeClick = 0; timeClick < (int)(tEnd/tStep); timeClick = timeClick + timeClicksOfRouteInterval){
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

				if(costPerRoute == null){
					costPerRoute = new  ArrayList<ArrayList<HashMap<Integer,Double>>>();
				}

				//required in case of new OD pair
				if(ODPairIndex >= costPerRoute.size()){
					costPerRoute.add(new ArrayList<HashMap<Integer,Double>>());
				}

				//required in case of new route that was not considered previously
				if(routeIndex >= costPerRoute.get(ODPairIndex).size()){
					costPerRoute.get(ODPairIndex).add(new HashMap<Integer,Double>());
				}

				//save the cost (route travel time)

				costPerRoute.get(ODPairIndex).get(routeIndex).put(timeClick, routeTT);		
			}

		}
	}



	//compute min cost per OD and time
	ArrayList<HashMap<Integer,Double>> minCostPerOD = new ArrayList<HashMap<Integer,Double>>();

	for(int ODPairIndex = 0; ODPairIndex< costPerRoute.size(); ODPairIndex++){
		HashMap<Integer,Double> minCosts = new HashMap<Integer,Double>();

		for(int timeClick = 0; timeClick < (int)(tEnd/tStep); timeClick = timeClick + timeClicksOfRouteInterval ){

			double minCost = Double.MAX_VALUE; //fake cost value

			for(int routeIndex = 0; routeIndex < costPerRoute.get(ODPairIndex).size(); routeIndex++ ){

				//check if minimum
				if(costPerRoute.get(ODPairIndex).get(routeIndex).get(timeClick) < minCost )
				{	
					//save it
					minCost = costPerRoute.get(ODPairIndex).get(routeIndex).get(timeClick);
				}	
			}
			minCosts.put(timeClick, minCost); 

		}
		minCostPerOD.add(minCosts);
	}

	//initialise the gap at 0
	double gap = 0;

	for(int setOfRoutesIndex = 0; setOfRoutesIndex< oldRoutes.size(); setOfRoutesIndex++){

		for(int routeIndex = 0; routeIndex< oldRoutes.get(setOfRoutesIndex).size(); routeIndex++){


			PathRepresentation path = oldRoutes.get(setOfRoutesIndex).get(routeIndex);
			int startNodeIndex = path.nodeIndexes[0];
			int endNodeIndex = path.nodeIndexes[path.nodeIndexes.length-1];
			int[] linkIndexes = path.linkIndexes;

			//we start at the interval 0
			int interval = 0;
			int timeClick = 0;

			//for all time intervals...
			while( timeClick < (int) (tEnd/tStep)){
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

				if(newRouteFractions.get(setOfRoutesIndex).get(routeIndex)[timeClick] == 1.0){
					//don't count it
				}
				else{

					//compute the duration of the routeInterval for which the gap remains constant
					double routeIntervalInTimeClicks = 0.0;
					if(timeClick  + timeClicksOfRouteInterval < (int)(tEnd/tStep)){

						routeIntervalInTimeClicks = timeClicksOfRouteInterval;
					}
					else{

						routeIntervalInTimeClicks = (int)(tEnd/tStep) - timeClick;
					}

					//update gap given the previous gap cumulative and the route cost given an OD flow for this route for the given time period
					gap = gap + 
							oldRouteFractions.get(setOfRoutesIndex).get(routeIndex)[timeClick]* 
							(routeTT - minCostPerOD.get(setOfRoutesIndex).get(timeClick))*
							expandedODMatrices.get(timeClick)[startNodeIndex][endNodeIndex]*
							routeIntervalInTimeClicks;
				}

				interval++;
				timeClick = interval * timeClicksOfRouteInterval;
			}
		}
	}
	return gap;

}

private void updateSpeeds(){

	//obtain simulated speeds
	double [][] speeds = CumulativeBasedCalculator.calculateCumulativeToSpeeds(tfData.links, tEnd, tStep);
	double [][] speedsAtArrival = CumulativeBasedCalculator.calculateCumulativeToSpeedsAtArrival(tfData.links, tEnd, tStep);

	//update linkSpeeds and linkSpeedsAtArrival
	for(int linkIndex = 0; linkIndex < linkSpeeds.size(); linkIndex++){
		linkSpeeds.set(linkIndex, speeds[linkIndex]);
		linkSpeedsAtArrival.set(linkIndex, speedsAtArrival[linkIndex]);
	}
}

private void updateTravelTimes(){

	double[][] travelTime  = new double[linkTravelTimes.size()][(int)(tEnd/tStep)];

	//update travelTime per link as length/speed (instantaneous)
	for(int linkIndex = 0; linkIndex < linkSpeeds.size(); linkIndex++){
		for(int timeClick = 0; timeClick < (int)(tEnd/tStep); timeClick++){
			travelTime[linkIndex][timeClick] =  tfData.links.get(linkIndex).length / linkSpeeds.get(linkIndex)[timeClick];
		}
		linkTravelTimes.set(linkIndex, travelTime[linkIndex]);
	}

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

			if((ctime + tStep) > tfData.timeSlices.get(timeSliceNumber+1) + doubleErrorTolerance && ctime !=0 ){

				//part of the flow in the matrix is due to OD prior to timeSlice change
				expandedODMatrix = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber));
				expandedODMatrix = expandedODMatrix.scalarMultiply(tfData.timeSlices.get(timeSliceNumber+1) - ctime);

				//the remaining flow is due to the OD in the next timeSlice
				RealMatrix temp = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber + 1));
				temp = temp.scalarMultiply((ctime+tStep) - tfData.timeSlices.get(timeSliceNumber+1));
				expandedODMatrix = expandedODMatrix.add(temp);

			}
			else{
				//if there is no timeslice change it is safe to add the contribution of the ODMatrix for that timeSlice
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
		double[] speedsAtArrival = new double[(int)(tEnd/tStep)];

		//we initialize speepds to the linkfreespeed
		java.util.Arrays.fill(speeds, link.freeSpeed);	
		java.util.Arrays.fill(speedsAtArrival, link.freeSpeed);	

		//add the corresponding array of freespeeds
		linkSpeeds.add(speeds);
		linkSpeedsAtArrival.add(speedsAtArrival);

	}


}

private void computeTurningFractions(int timeClicksOFTurningFractionsInterval, 
		int timeClicksShift, 
		ArrayList<double[]> currentLinkSpeeds,
		ArrayList< ArrayList<PathRepresentation>> oldRoutes,
		ArrayList<ArrayList<Double[]>> oldRouteFractions){

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


			for(int setOfRoutesIndex = 0; setOfRoutesIndex < oldRoutes.size(); setOfRoutesIndex++){

				//extract set of routes 

				ArrayList<PathRepresentation> setOfRoutes = oldRoutes.get(setOfRoutesIndex);

				//visit each route in the set of routes
				for(int routeIndex = 0; routeIndex < setOfRoutes.size(); routeIndex++){

					PathRepresentation route = setOfRoutes.get(routeIndex);

					//obtain the index in the route of the nodeIndex
					int indexNodeInRoute = -1;
					indexNodeInRoute = route.findIndexInPathOfNodeIndex(nodeIndex);

					if(indexNodeInRoute == -1 || oldRouteFractions.get(setOfRoutesIndex).get(routeIndex)[timeClick] == 0 || route.isBorderNode(nodeIndex)){

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
											+ oldRouteFractions.get(setOfRoutesIndex).get(routeIndex)[timeClick]
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

public double getGap() {
	return gap;
}

public void setGap(double gap) {
	this.gap = gap;
}


public int getIteration() {
	return iteration;
}

public void setSaveToFile(boolean saveToFile){
	this.saveToFile = saveToFile;
	tfDatalogger = new TrafficDataLogger();
	
}

}
