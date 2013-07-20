package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import com.aarribas.traffictools.CumulativeBasedCalculator;
import com.aarribas.utils.Pair;

public class LTM implements TrafficNetworkModel {

	private ArrayList<double[][]> expandedODMatrices;
	private TrafficData tfData = null;
	private double tEnd;
	private double tStep;
	private ArrayList<ArrayList<double[][]>> turningFractions = null;
	private TrafficNodeModel nodeModel = null;

	public LTM(ArrayList<double[][]> expandedODMatrices, TrafficData tfData, double tEnd, double tStep){
		this.expandedODMatrices = expandedODMatrices;
		this.tfData = tfData;
		this.tEnd = tEnd;
		this.tStep = tStep;
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
			Arrays.fill(link.downStreamCumulative, 0.0);
			Arrays.fill(link.upStreamCumulative, 0.0);

			

		}
		
		//The following is a way of adding references at node level to the incoming and outgoing links
		//There are more efficient ways of doing this, but it is important to keep the addition of links
		//in this order, because it is relevant when using turningFractions.
		//This is based on the original MATLAB code.
		for(TrafficNode node: tfData.nodes){
			node.incomingLinks = new ArrayList<TrafficLink>();
			node.outgoingLinks = new ArrayList<TrafficLink>();
			for(TrafficLink link : tfData.links){ 
				if(node.id == link.startNode){
					node.outgoingLinks.add(link);
				}
				if(node.id == link.endNode){
					node.incomingLinks.add(link);
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
		
		//TODO: REMOVE THIS IN THE FUTURE

		for(TrafficLink link: tfData.links){

			if(link.upStreamCumulativeMax < link.upStreamCumulative[timeClick]){

				link.upStreamCumulativeMax = link.upStreamCumulative[timeClick];

			}

			if(link.downStreamCumulativeMax < link.downStreamCumulative[timeClick]){

				link.downStreamCumulativeMax = link.downStreamCumulative[timeClick];

			}

		}

	}

	public void run(ArrayList<ArrayList<double[][]>> turningFractions) {
		this.turningFractions  = turningFractions;
		
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
//						System.out.println("node" + tfData.nodes.get(nodeIndex).id);
//						System.out.println("link" + link.id);
//						System.out.println("cumu" + link.upStreamCumulative[timeClick]);
//						Scanner scan = new Scanner(System.in);
//						scan.nextLine();
						}

				}
				else if (node.outgoingLinks.isEmpty()){
					

					//destination node, set downstream cumulative only
					for(TrafficLink link: node.incomingLinks){
						link.downStreamCumulative[timeClick] = CumulativeBasedCalculator.calculateCumulativeValue(link.upStreamCumulative, timeClick*tStep - link.length/link.freeSpeed, tStep);
					}
				}
				else{
					
					if(!turningFractions.get(nodeIndex).isEmpty()){
						//compute the overall sum of the turningFraction and continue if sum >0
						if (!(calculateTurningFractionSum(turningFractions.get(nodeIndex).get(timeClick)) == 0)){
							
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

			
			//TODO REMOVE THIS
			//update cumulative maximas.
			//maximas can be used in the logic or for debugging purposes 
			//at the moment they are not used in the logic
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
		double[] receivingFlow = calculateReceivingFlowForNode(nodeIndex, timeClick);
		

		//run the capacity proportional node model
		nodeModel.run(currNode,turningFractionsForNodeAndTime, receivingFlow, sendingFlow);
		
		//compute valid previous click
		int prevClick = Math.max(0, timeClick-1);
	
		//save the cumulatives
		for(int linkIndex = 0; linkIndex < currNode.incomingLinks.size(); linkIndex++){
			
			downStreamCumulativeAtNode[linkIndex] = currNode.incomingLinks.get(linkIndex).downStreamCumulative[prevClick]
					+ nodeModel.getIncomingFlows()[linkIndex];
			
		}
		for(int linkIndex = 0; linkIndex < currNode.outgoingLinks.size(); linkIndex++){
			
			upStreamCumulativeAtNode[linkIndex] = currNode.outgoingLinks.get(linkIndex).upStreamCumulative[prevClick]
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
			
			sendingFlowEntry = sendingFlowEntry - link.downStreamCumulative[Math.max(0, timeClick-1)];
			
			//real sending flow is the min(cap,sf)
			sendingFlow[linkIndex] = Math.min(cap, sendingFlowEntry);
			

		}

		return sendingFlow;

	}

	private double[] calculateReceivingFlowForNode(int nodeIndex, int timeClick){

		TrafficNode node = tfData.nodes.get(nodeIndex);

		double[] receivingFlow = new double[node.outgoingLinks.size()];
		
		for(int linkIndex = 0 ;  linkIndex < node.outgoingLinks.size(); linkIndex++){

			TrafficLink link = node.outgoingLinks.get(linkIndex);

			double cap = link.capacity*tStep;
			double updateTime = timeClick*tStep - link.length/(-link.w);
			double receivingFlowEntry = CumulativeBasedCalculator.calculateCumulativeValue(link.downStreamCumulative, updateTime, tStep);
	
			receivingFlowEntry = receivingFlowEntry 
					+ link.kJam*link.length
					- link.upStreamCumulative[Math.max(0, timeClick-1)];
			
			
			//real receiving flow is the min(cap,sf)
			receivingFlow[linkIndex] = Math.min(cap, receivingFlowEntry);
			

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
		
//	
//		System.out.println("sum" + sum);
//		System.out.println(nodeIndex);
//		System.out.println(timeClick);

		//compute number of outgoinglinks
		int numOutgoingLinks = tfData.nodes.get(nodeIndex).outgoingLinks.size();
		
//		System.out.println("outgoinglinks" + numOutgoingLinks);
		
		return link.upStreamCumulative[Math.max(0,timeClick-1)]+sum/numOutgoingLinks;
	}



	private void calculateWs(){

		//compute w per link + update the w in the link structure 
		for(int linkIndex = 0; linkIndex<tfData.links.size(); linkIndex++){
			TrafficLink link = tfData.links.get(linkIndex);
			link.w = - link.capacity / (link.kJam - (link.capacity/link.freeSpeed));

			//if -w > freespeed issue a warning 
			if(-link.w>link.freeSpeed){
				//original warning as in the matlab code is commented out
				//System.out.println("Warning: -w>freespeed on link: " + link.id + " with index " + linkIndex);	
				//instead we fix a reasonable spillback speed
				link.w = -link.freeSpeed;
			}
		}



	}



}
