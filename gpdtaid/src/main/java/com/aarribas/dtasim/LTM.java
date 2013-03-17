package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.linear.RealVector;

import com.aarribas.traffictools.CumulativeBasedCalculator;
import com.aarribas.utils.Pair;

public class LTM implements TrafficNetworkModel {

	private ArrayList<double[][]> expandedODMatrices;
	private TrafficData tfData = null;
	private double tEnd;
	private double tStep;
	private ArrayList<ArrayList<double[][]>> turningFractions;

	public LTM(ArrayList<double[][]> expandedODMatrices, TrafficData tfData, double tEnd, double tStep, ArrayList<ArrayList<double[][]>> turningFractions){
		this.expandedODMatrices = expandedODMatrices;
		this.tfData = tfData;
		this.tEnd = tEnd;
		this.tStep = tStep;
		this.turningFractions = turningFractions;
		init();

	}

	private void init(){
		
		//caculate the w's per link
		calculateWs();

		//create a map of id/index per node
		Map<Double, Integer> nodeIndexes = new HashMap<Double, Integer>();
		for(int nodeIndex = 0; nodeIndex < tfData.nodes.size(); nodeIndex++){

			nodeIndexes.put(tfData.nodes.get(nodeIndex).id, nodeIndex);

		}

		for(TrafficLink link : tfData.links){
			//initialise link to 0 for all links  and timesteps
			link.downStreamCumulative = new double[(int)(tEnd/tStep)];
			link.upStreamCumulative = new double[(int)(tEnd/tStep)];
			Arrays.fill(link.downStreamCumulative, 0);
			Arrays.fill(link.upStreamCumulative, 0);


			//save references to this link at node level 
			int startNodeIndex = nodeIndexes.get(link.startNode);
			int endNodeIndex = nodeIndexes.get(link.endNode);
			tfData.nodes.get(startNodeIndex).incomingLinks.add(link);
			tfData.nodes.get(endNodeIndex).outgoingLinks.add(link);

		}

	}

	private void initLinkCumulativeMaximas(){

		for(TrafficLink link: tfData.links){

			link.upStreamCumulativeMax = 0;
			link.downStreamCumulativeMax = 0;

		}

	}

	private void updateLinkCumulativeMaximas(int timeClick){

		for(TrafficLink link: tfData.links){

			if(link.upStreamCumulativeMax < link.upStreamCumulative[timeClick]){

				link.upStreamCumulativeMax = link.upStreamCumulative[timeClick];

			}

			if(link.downStreamCumulativeMax < link.downStreamCumulative[timeClick]){

				link.downStreamCumulativeMax = link.downStreamCumulative[timeClick];

			}

		}

	}

	public void run() {
		
		//initialise the cumulative maximas
		initLinkCumulativeMaximas();

		for(int timeClick = 0; timeClick< (int)(tEnd/tStep); timeClick++){

			for(int nodeIndex = 0; nodeIndex < tfData.nodes.size(); nodeIndex++){
				TrafficNode node  = tfData.nodes.get(nodeIndex);

				if(node.incomingLinks.isEmpty() && node.outgoingLinks.isEmpty()){

					//unconnected node = do nothing

				}
				else if (node.incomingLinks.isEmpty() ){

					//origin node, set upstream cumulative only
					for(TrafficLink link: node.outgoingLinks){
						link.upStreamCumulative[timeClick] = calculateUpstreamCumulativeGivenOriginNode(nodeIndex, link, timeClick);					
					}

				}
				else if (node.outgoingLinks.isEmpty()){

					//destination node, set downstream cumulative only
					for(TrafficLink link: node.incomingLinks){
						link.downStreamCumulative[timeClick] = calculateCumulativeValue(link.downStreamCumulative, timeClick*tStep - link.length/link.freeSpeed);
					}
				}
				else{
					if(!turningFractions.get(nodeIndex).isEmpty()){

						//compute the overall sum of the turningFraction

						if (calculateTurningFractionSum(turningFractions.get(nodeIndex).get(timeClick)) > 0){

							//call updateCumulative

							//set downstreamcumulative
							for(TrafficLink link: node.incomingLinks){

							}

							//set upstreamcumulative
							for(TrafficLink link: node.outgoingLinks){

							}

						}

					}

				}
			}

			//update cumulative maximas.
			updateLinkCumulativeMaximas(timeClick);
		}



	}

	private Pair<double[],double[]> updateCumulativesforNode(int nodeIndex, int timeClick){

		TrafficNode currNode = tfData.nodes.get(nodeIndex);
		
		//initialise the downStream/Upstream Cumulative at Node
		double[] downStreamCumulativeAtNode = new double[currNode.incomingLinks.size()];
		double[] upStreamCumulativeAtNode = new double[currNode.outgoingLinks.size()];
		
		//calculate flows
		calculateSendingFlowForNode(nodeIndex, timeClick);
		calculateReceivingFlow(nodeIndex, timeClick);
		
		//use a capacity proportional node model
		
		//compute receiving flow
		
		Pair<double[], double[]>flows = null;  //to change when node model is done
		
		//save the cumulatives
		for(int linkIndex = 0; linkIndex < currNode.incomingLinks.size(); linkIndex++){
			
			downStreamCumulativeAtNode[linkIndex] = currNode.incomingLinks.get(linkIndex).downStreamCumulativeMax
					+ flows.a[linkIndex];
			
		}
		
		for(int linkIndex = 0; linkIndex < currNode.outgoingLinks.size(); linkIndex++){
			
			upStreamCumulativeAtNode[linkIndex] = currNode.outgoingLinks.get(linkIndex).upStreamCumulativeMax
					+ flows.b[linkIndex];
			
		}
		
		return new Pair<double[],double[]>(upStreamCumulativeAtNode, downStreamCumulativeAtNode);

	}

	private double[] calculateSendingFlowForNode(int nodeIndex, int timeClick){

		TrafficNode node = tfData.nodes.get(nodeIndex);

		double[] sendingFlow = new double[node.incomingLinks.size()];

		for(int linkIndex = 0 ;  linkIndex < node.incomingLinks.size(); linkIndex++){

			TrafficLink link = node.incomingLinks.get(linkIndex);

			double cap = link.capacity*tStep;
			double updateTime = timeClick*tStep - link.length/link.freeSpeed;
			double sendingFlowEntry = calculateCumulativeValue(link.upStreamCumulative, updateTime);
			sendingFlowEntry = sendingFlowEntry - link.downStreamCumulativeMax;

			//real sending flow is the min(cap,sf)
			if(cap < sendingFlowEntry){
				sendingFlow[linkIndex] = cap;
			}
			else{
				sendingFlow[linkIndex] = sendingFlowEntry;
			}

		}

		return sendingFlow;

	}

	private double[] calculateReceivingFlow(int nodeIndex, int timeClick){

		TrafficNode node = tfData.nodes.get(nodeIndex);

		double[] receivingFlow = new double[node.outgoingLinks.size()];
		
		for(int linkIndex = 0 ;  linkIndex < node.outgoingLinks.size(); linkIndex++){

			TrafficLink link = node.outgoingLinks.get(linkIndex);

			double cap = link.capacity*tStep;
			double updateTime = timeClick*tStep + link.length/link.w;
			double receivingFlowEntry = calculateCumulativeValue(link.downStreamCumulative, updateTime);
			
			receivingFlowEntry = receivingFlowEntry + link.kJam*link.length - link.upStreamCumulativeMax;
			
			//real receiving flow is the min(cap,sf)
			if(cap < receivingFlowEntry){
				receivingFlow[linkIndex] = cap;
			}
			else{
				receivingFlow[linkIndex] = receivingFlowEntry;
			}

		}

		return receivingFlow;
	}

	private double calculateTurningFractionSum(double[][] turningFraction){

		double sumTF = 0;
		for(int row = 0; row< turningFraction.length; row++){							
			//compute the sum of turningFraction for a row
			for(int column = 0; column<turningFraction[row].length; column++){

				sumTF = sumTF + turningFraction[row][column];
			}

		}

		return sumTF;
	}


	private double calculateUpstreamCumulativeGivenOriginNode(int nodeIndex, TrafficLink link, int timeClick){

		//compute sum per OD for current node and for all times
		double sum = 0;

		for(int possiblePairNodeIndex = 0; possiblePairNodeIndex < expandedODMatrices.get(timeClick)[nodeIndex].length; possiblePairNodeIndex++){

			sum = sum + expandedODMatrices.get(timeClick)[nodeIndex][possiblePairNodeIndex];

		}

		//compute number of outgoinglinks
		int numOutgoingLinks = tfData.nodes.get(nodeIndex).outgoingLinks.size();

		return link.upStreamCumulativeMax+sum/numOutgoingLinks;
	}


	private double calculateCumulativeValue(double cumulatives[], double t){

		//Attention: in the matlab code (findcumulative) the calculation o tbefore and tafter is inconsistent with the calculation in findTravelTimes
		//I decided to make it consistent here

		int tClickBefore = (int)(t/tStep);

		if(tClickBefore == cumulatives.length-1){
			//if we have reached the last time click return the last cumulative
			return cumulatives[cumulatives.length-1];
		}

		//if the we are at the very beginning the value is 0
		if(tClickBefore == 0){
			return 0;
		}
		else{
			int tClickAfter = tClickBefore + 1;
			double tempFasterTime = tClickBefore*tStep;
			double tempFasterCumulative = cumulatives[tClickBefore];
			double tInt = (t-tempFasterTime)/((tClickAfter*tStep)-tempFasterTime);
			return tempFasterCumulative + tInt * (cumulatives[tClickAfter]-tempFasterCumulative);

		}

	}


	private void calculateWs(){

		//compute w per link + update the w in the link structure 
		for(int linkIndex = 0; linkIndex<tfData.links.size(); linkIndex++){
			TrafficLink link = tfData.links.get(linkIndex);
			link.w = - link.capacity / (link.kJam - (link.capacity/link.freeSpeed));

			//if -w > freespeed issue a warning 
			if(-link.w>link.freeSpeed){
				System.out.println("Warning: -w>freespeed on link: " + link.id + " with index " + linkIndex);	
			}
		}



	}



}
