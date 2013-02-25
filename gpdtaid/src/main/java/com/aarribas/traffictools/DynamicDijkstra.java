package com.aarribas.traffictools;

import com.aarribas.dtasim.TrafficData;
import com.aarribas.dtasim.TrafficLink;
import com.aarribas.dtasim.TrafficODPair;

import java.util.ArrayList;
import java.util.Arrays;

public class DynamicDijkstra extends PathFinder {


	public DynamicDijkstra(TrafficData tfData, TrafficODPair[] ODPairs,double[] travelCosts, double tEnd, double tStep, int timeClicksShift, int timeClicksOfRouteInterval) {
		super(tfData, ODPairs, travelCosts, tEnd, tStep, timeClicksShift, timeClicksOfRouteInterval);
	}

	@Override
	public void findPath(){

		if(routes.isEmpty()){

			//look for a path for each od pair per routeInterval
			for(int odIndex = 0; odIndex < ODPairs.length; odIndex++){

				//we start at the interval 0
				int interval = 0;
				int timeClick = timeClicksShift + timeClicksOfRouteInterval*interval; //equals timeClickShift of course

				while( timeClick < (int) (tEnd/tStep)){

					double departureTime = (timeClick - timeClicksOfAdditionalRouteInterval) * tStep;
					int indexStartNode = ODPairs[odIndex].getIndexStartNode(tfData.nodes);
					int indexEndNode = ODPairs[odIndex].getIndexEndNode(tfData.nodes);

					//compute parents with source the start Node, departure time the 
					int[] parents = computeParents(indexStartNode, departureTime );

					//compute node indexes of nodes that form the path
					Integer[] pathNodeIndexes = (Integer[]) computeNodeIndexesInPath(indexStartNode, indexEndNode, parents).toArray();

					//compute link indexes of links that form the path
					int[] pathLinkIndexes = computeLinkIndexesInPath(pathNodeIndexes);

					//save the corresponding path representation
					addPathRepresentation(odIndex,new PathRepresentation(pathNodeIndexes, pathLinkIndexes));

					//save the routefraction too
					Integer[] routeFraction = new Integer[(int) (tEnd/tStep)];
					Arrays.fill(routeFraction, 1);
					addRouteFraction(odIndex, routeFraction);

					//move to the next route interval
					interval++;
					timeClick = timeClicksShift + timeClicksOfRouteInterval*interval; 
				}

			}

		}
		else{

			//look for a path for each od pair per routeInterval
			for(int odIndex = 0; odIndex < ODPairs.length; odIndex++){

				int previousTimeClick = 0;
				//we start at the interval 0
				int interval = 0;
				int timeClick = timeClicksShift + timeClicksOfRouteInterval*interval; //equals timeClickShift of course
				int shortestRouteIndex = 0;
				
				while( timeClick < (int) (tEnd/tStep)){

					double departureTime = (timeClick - timeClicksOfAdditionalRouteInterval) * tStep;
					int indexStartNode = ODPairs[odIndex].getIndexStartNode(tfData.nodes);
					int indexEndNode = ODPairs[odIndex].getIndexEndNode(tfData.nodes);

					//compute parents with source the start Node, departure time the 
					int[] parents = computeParents(indexStartNode, departureTime );

					//compute node indexes of nodes that form the path
					Integer[] pathNodeIndexes = (Integer[]) computeNodeIndexesInPath(indexStartNode, indexEndNode, parents).toArray();

					//compute link indexes of links that form the path
					int[] pathLinkIndexes = computeLinkIndexesInPath(pathNodeIndexes);
					
					boolean routeAlreadyFound = false;
					
					if(routes.get(odIndex).isEmpty()){
						//save the corresponding path representation
						addPathRepresentation(odIndex,new PathRepresentation(pathNodeIndexes, pathLinkIndexes));

						//save the routefraction too
						Integer[] routeFraction = new Integer[(int) (tEnd/tStep)];
						Arrays.fill(routeFraction, 1);
						addRouteFraction(odIndex, routeFraction);
					}
					else{
						PathRepresentation tempPathRepresentation = new PathRepresentation(pathNodeIndexes, pathLinkIndexes);
						
						//compare to each route for that odpair
						for(int routeIndex = 0; routeIndex < routes.get(odIndex).size(); routeIndex++){
							if(tempPathRepresentation.equals(routes.get(odIndex).get(routeIndex))){
								routeAlreadyFound = true;
								shortestRouteIndex = routeIndex;
							}
							else{
								routeAlreadyFound = false;
							}
						}
						if(!routeAlreadyFound){
							//we found a new route, add the route to the list of routes
							addPathRepresentation(odIndex, tempPathRepresentation);
							
							//move the index pointing to the shortest route
							shortestRouteIndex = routes.get(odIndex).size() -1;
							
							//add routefractions to 0
							Integer[] routeFraction = new Integer[(int) (tEnd/tStep)];
							Arrays.fill(routeFraction, 0);
							addRouteFraction(odIndex, routeFraction);
						}
						
						//updated routeFractions accordingly
						for(int routeFractionIndex = 0; routeFractionIndex<routeFractions.get(odIndex).size(); routeFractionIndex++){
							if(routeFractionIndex == shortestRouteIndex){
								Arrays.fill(routeFractions.get(odIndex).get(routeFractionIndex), previousTimeClick, timeClick, 1);
							}
							else{
								Arrays.fill(routeFractions.get(odIndex).get(routeFractionIndex), previousTimeClick, timeClick, 0);
							}
						}
						//move to the next relevant time click in term of routeIntervals
						previousTimeClick = timeClick + 1;	
					}
					
					//fix route fractions till last timeclick
					for(int routeFractionIndex = 0; routeFractionIndex<routeFractions.get(odIndex).size(); routeFractionIndex++){
						if(routeFractionIndex == shortestRouteIndex){
							Arrays.fill(routeFractions.get(odIndex).get(routeFractionIndex), previousTimeClick, (int) (tEnd/tStep), 1);
						}
						else{
							Arrays.fill(routeFractions.get(odIndex).get(routeFractionIndex), previousTimeClick, (int) (tEnd/tStep), 0);
						}
					}

					//move to the next route interval
					interval++;
					timeClick = timeClicksShift + timeClicksOfRouteInterval*interval; 
				}
			}
		}

	}

	private void addPathRepresentation(int odIndex, PathRepresentation pathRepresentation){

		//TODO improve this part - it should be possible to use one single add.

		//if no routes, prepare to add the first one
		if(routes.isEmpty())
		{
			routes.add(new ArrayList<PathRepresentation>());
		}

		//if no path Representations for this ODPair yet, add the first pathrepresentation
		if(routes.get(odIndex).isEmpty()){
			routes.get(odIndex).add(pathRepresentation);
		}
		else{
			//otherwise just add one more representation
			routes.get(odIndex).add(pathRepresentation);
		}

	}

	private void addRouteFraction(int odIndex, Integer[] routeFraction){

		//TODO improve this part - it should be possible to use one single add.

		//if no routeFractions, prepare to add the first one
		if(routeFractions.isEmpty())
		{
			routeFractions.add(new ArrayList<Integer[]>());
		}

		//if no path routeFraction add the first one
		if(routeFractions.get(odIndex).isEmpty()){
			routeFractions.get(odIndex).add(routeFraction);
		}
		else{
			//otherwise just add one more representation
			routeFractions.get(odIndex).add(routeFraction);
		}

	}


	private int[] computeParents(int source, double departureTime){

		int numNodes = tfData.nodes.size();
		int numLinks = numNodes + 1;

		//initialise arrays.
		boolean[] visited = new boolean[numNodes];
		double[] distances = new double[numNodes];
		int[] parents = new int[numNodes];

		//initialization to default values
		Arrays.fill(visited, false);
		Arrays.fill(parents, 0);
		Arrays.fill(distances, Double.MAX_VALUE);

		distances[source] = 0;

		//in dynamic dijsktra apparently it is sufficient to iterate numNodes -1
		for(int iteration = 0; iteration<numNodes - 1; iteration++ ){

			double[] tempDistances = new double[numNodes];

			//for all nodes set preliminary distances
			for(int node =0; node<numNodes; node++){
				//if not visited yet as a source, consider the shortest distance so far.
				if(visited[node] == false){
					tempDistances[node] = distances[node];
				}
				else{
					//otherwise consider the maximum distance
					tempDistances[node] = Double.MAX_VALUE;
				}
			}

			//find the node at minimum distance
			double minDistance = tempDistances[0];
			int indexMinNode = 0;
			for(int indexCandidateMin = 0; indexCandidateMin<numNodes; indexCandidateMin++){
				if(tempDistances[indexCandidateMin]<minDistance){
					minDistance = tempDistances[indexCandidateMin];
					indexMinNode = indexCandidateMin;
				}
			}

			//flag it as visited
			visited[indexMinNode] = true;


			//check for connecting links -- note that the current code only works for fully connected networks.
			for(int linkIndex = 0; linkIndex < numLinks; linkIndex++){
				TrafficLink link = tfData.links.get(linkIndex);

				//if it is a connecting link then proceed
				if(link.startNode == tfData.nodes.get(indexMinNode).id){

					//TODO add an exception here for unexpected situation where no corresponding node is found!
					//obtain the index for the end node
					int neighborgNodeIndex=0;
					for(int i = 0; i< tfData.nodes.size(); i++){
						if(tfData.nodes.get(i).id == link.endNode){
							neighborgNodeIndex = i;
							break;
						}	

						//compute the complete travel time at time departureTime + minDistance
						double completeTravelTime = computeTravelTime(departureTime + distances[indexMinNode]);

						if((completeTravelTime + distances[indexMinNode]) < distances[neighborgNodeIndex]){
							distances[neighborgNodeIndex] = distances[indexMinNode];
							parents[neighborgNodeIndex] = indexMinNode;
						}

					}
				}

			}

		}   

		return parents;
	}

	private ArrayList<Integer> computeNodeIndexesInPath(int indexStartNode, int indexEndNode, int[] parents){

		ArrayList<Integer> nodeIndexes = new ArrayList<Integer>();

		if(indexEndNode == indexStartNode){
			//only one node 
			nodeIndexes.add(indexStartNode);

		}
		else{
			//extract path betweem startNode and parent of endNoded
			nodeIndexes = computeNodeIndexesInPath(indexStartNode, parents[indexEndNode], parents);
			//add end node to complete the path
			nodeIndexes.add(indexEndNode);

		}

		return nodeIndexes;		
	}

	private int[] computeLinkIndexesInPath(Integer[] nodeIndexes){

		int[] linkIndexes = new int[nodeIndexes.length - 1];
		boolean[] extracted = new boolean[tfData.links.size()]; 
		Arrays.fill(extracted, false);

		//note that if there is only one node we return the empty array as expected.

		for(int i = 0; i<nodeIndexes.length - 1; i++){

			//visist all links, if the endNode and startNode match the nodes referred by indexes i+1 and i, save the index of the link
			for(int linkIndex = 0; linkIndex <tfData.links.size(); linkIndex++){

				//only look at links that have not been extracted so far / makesthis slightly more efficient
				if(!extracted[linkIndex]){
					if((tfData.links.get(linkIndex).startNode == tfData.nodes.get(nodeIndexes[i]).id) && (tfData.links.get(linkIndex).endNode == tfData.nodes.get(nodeIndexes[i+1]).id )){
						linkIndexes[i] = linkIndex;
						extracted[linkIndex] = true;
						break;
					}
				}
			}
		}

		return linkIndexes;
	}

	private double computeTravelTime(double t){

		//index of the timeStep (time click) prior to t
		int indexTimeBefore = ((int)(t/tStep));


		if(indexTimeBefore + 1 == travelCosts.length){
			//if we have passed the last time click the travelTime is the last travel time
			//as in matlab code.
			return travelCosts[travelCosts.length-1];
		}
		else{

			//proportion of time between t and previous time click
			double tBetween  = (t - (indexTimeBefore * tStep)) / tStep;

			//should always work if I have an exception here I made the wrong assumption regarding possible indexTimeBefore

			if(travelCosts[indexTimeBefore] == Double.MAX_VALUE){
				return Double.MAX_VALUE;
			}
			else{
				return travelCosts[indexTimeBefore] + tBetween * (travelCosts[indexTimeBefore + 1] - travelCosts[indexTimeBefore]);	
			}

		}

	}
}