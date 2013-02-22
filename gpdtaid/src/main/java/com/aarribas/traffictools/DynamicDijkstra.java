package com.aarribas.traffictools;

import com.aarribas.dtasim.TrafficData;
import com.aarribas.dtasim.TrafficLink;

import java.util.ArrayList;
import java.util.Arrays;

public class DynamicDijkstra extends PathFinder {

	private TrafficData tfData;

	public DynamicDijkstra(TrafficData tfdata) {
		this.tfData = tfData;
	}

	@Override
	public void findPath(){

		//compute parents
		//compute rest
	}

	private void computeParents(int source, ArrayList<double[]> travelTimes, double departureTime, double tEnd, double tStep){

		int numNodes = tfData.nodes.size();
		int numLinks = numNodes + 1;

		//initialise arrays.
		boolean[] visited = new boolean[numNodes];
		double[] distances = new double[numNodes];
		double[] parents = new double[numNodes];

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
						double completeTravelTime = computeTravelTime(departureTime + distances[indexMinNode], tEnd, tStep,  travelTimes.get(linkIndex));

						if((completeTravelTime + distances[indexMinNode]) < distances[neighborgNodeIndex]){
							distances[neighborgNodeIndex] = distances[indexMinNode];
							parents[neighborgNodeIndex] = indexMinNode;
						}

					}
				}

			}

		}     
	}

	private void computePath(){

	}

	private double computeTravelTime(double t, double tEnd, double tStep, double[] travelTimes){

		//		tBefore = find(t>=timeSteps,1,'last');
		//		tAfter = find(t<timeSteps,1,'first');
		//
		//		if isempty(tAfter)
		//		    value = travelTimes(end);
		//		elseif isempty(tBefore)
		//		    value = 0;
		//		else
		//		    tInt = (t-timeSteps(tBefore))/(timeSteps(tAfter)-timeSteps(tBefore));
		//		    value = travelTimes(tBefore)+tInt*(travelTimes(tAfter)-travelTimes(tBefore));
		//		end
		//		if isnan(value)
		//		    value = inf;
		//		end
		//		end
		//		
		//index of the timeStep (time click) prior to t
		int indexTimeBefore = ((int)(t/tStep));


		if(indexTimeBefore + 1 == travelTimes.length){
			//if we have passed the last time click the travelTime is the last travel time
			//as in matlab code.
			return travelTimes[travelTimes.length-1];
		}
		else{

			//proportion of time between t and previous time click
			double tBetween  = (t - (indexTimeBefore * tStep)) / tStep;

			//should always work if I have an exception here I made the wrong assumption regarding possible indexTimeBefore

			if(travelTimes[indexTimeBefore] == Double.MAX_VALUE){
				return Double.MAX_VALUE;
			}
			else{
				return travelTimes[indexTimeBefore] + tBetween * (travelTimes[indexTimeBefore + 1] - travelTimes[indexTimeBefore]);	
			}

		}

	}
}
