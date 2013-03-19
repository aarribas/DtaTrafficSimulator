package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;
import com.aarribas.traffictools.CumulativeBasedCalculator;
import com.aarribas.utils.Pair;

public class LTM implements TrafficNetworkModel {

	private ArrayList<double[][]> expandedODMatrices;
	private TrafficData tfData = null;
	private double tEnd;
	private double tStep;
	private ArrayList<ArrayList<double[][]>> turningFractions;
	private TrafficNodeModel nodeModel = null;

	public LTM(ArrayList<double[][]> expandedODMatrices, TrafficData tfData, double tEnd, double tStep, ArrayList<ArrayList<double[][]>> turningFractions){
		this.expandedODMatrices = expandedODMatrices;
		this.tfData = tfData;
		this.tEnd = tEnd;
		this.tStep = tStep;
		this.turningFractions = turningFractions;
		this.nodeModel = new TrafficNodeModel(tfData, tStep);
		init();

	}

	private void init(){
		
		//caculate the w's per link
		calculateWs();

		//initialise the cumulatives per link
		for(TrafficLink link : tfData.links){
			//initialise link to 0 for all links  and timesteps
			link.downStreamCumulative = new double[(int)(tEnd/tStep)];
			link.upStreamCumulative = new double[(int)(tEnd/tStep)];
			Arrays.fill(link.downStreamCumulative, 0);
			Arrays.fill(link.upStreamCumulative, 0);

			

		}
		
		//The following is a way of adding references at node level to the incoming and outgoing links
		//There are more efficient ways of doing this, but it is important to keep the addition of links
		//in this order, because it is relevant when using turningFractions.
		//This is based on the original MATLAB code.
		for(TrafficNode node: tfData.nodes){
			for(TrafficLink link : tfData.links){ 
				if(node.id == link.startNode){
					node.incomingLinks.add(link);
				}
				if(node.id == link.endNode){
					node.outgoingLinks.add(link);
				}
			}
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
						link.downStreamCumulative[timeClick] = CumulativeBasedCalculator.calculateCumulativeValue(link.downStreamCumulative, timeClick*tStep - link.length/link.freeSpeed, tStep);
					}
				}
				else{
					if(!turningFractions.get(nodeIndex).isEmpty()){

						//compute the overall sum of the turningFraction and continue if sum >0
						if (calculateTurningFractionSum(turningFractions.get(nodeIndex).get(timeClick)) > 0){

							//get updated pair of cumulatives a = upStreamCumulative, b = downStreamCumulative
							Pair<double[],double[]> PairOfCumulatives = updateCumulativesforNode(nodeIndex, timeClick, turningFractions.get(nodeIndex).get(timeClick));
							//set downstreamcumulative
							for(int i = 0; i<node.incomingLinks.size(); i++){
								node.incomingLinks.get(i).downStreamCumulative[timeClick] = PairOfCumulatives.b[i];
							}

							//set upstreamcumulative
							for(int j = 0; j<node.outgoingLinks.size(); j++){
								node.outgoingLinks.get(j).upStreamCumulative[timeClick] = PairOfCumulatives.a[j];
							}


						}

					}

				}
			}

			//update cumulative maximas.
			updateLinkCumulativeMaximas(timeClick);
		}



	}

	private Pair<double[],double[]> updateCumulativesforNode(int nodeIndex, int timeClick, double[][] turningFractionsForNodeAndTime){

		TrafficNode currNode = tfData.nodes.get(nodeIndex);
		
		//initialise the downStream/Upstream Cumulative at Node
		double[] downStreamCumulativeAtNode = new double[currNode.incomingLinks.size()];
		double[] upStreamCumulativeAtNode = new double[currNode.outgoingLinks.size()];
		
		//calculate flows
		double[] sendingFlow = calculateSendingFlowForNode(nodeIndex, timeClick);
		double[] receivingFlow = calculateReceivingFlow(nodeIndex, timeClick);
		
		//run the capacity proportional node model
		nodeModel.run(currNode,turningFractionsForNodeAndTime, receivingFlow, sendingFlow);
		
		//save the cumulatives
		for(int linkIndex = 0; linkIndex < currNode.incomingLinks.size(); linkIndex++){
			
			downStreamCumulativeAtNode[linkIndex] = currNode.incomingLinks.get(linkIndex).downStreamCumulativeMax
					+ nodeModel.getIncomingFlows()[linkIndex];
			
		}
		for(int linkIndex = 0; linkIndex < currNode.outgoingLinks.size(); linkIndex++){
			
			upStreamCumulativeAtNode[linkIndex] = currNode.outgoingLinks.get(linkIndex).upStreamCumulativeMax
					+ nodeModel.getOutgoingFlows()[linkIndex];
			
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
			double sendingFlowEntry = CumulativeBasedCalculator.calculateCumulativeValue(link.upStreamCumulative, updateTime, tStep);
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
			double receivingFlowEntry = CumulativeBasedCalculator.calculateCumulativeValue(link.downStreamCumulative, updateTime, tStep);
			
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
