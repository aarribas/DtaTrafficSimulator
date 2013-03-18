package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;

public class TrafficNodeModel {

	//data that stays the same for all nodes
	private TrafficData tfData = null;
	private double[][] turningFractions;
	private double tStep;

	//data initialised and used when calling the method run
	private TrafficNode currNode;
	private double[] currSendingFlow;
	private double[] currReceivingFlow;

	private int linksToDo;
	private double[] receivingFraction;
	private double[][] sendingCapFlows;
	private double[] capFlows;
	private double[][] capacities;
	private LINK_FLOW_STATUS[] linkFlowCalculationStatus;
	private double[][] transferFlows;

	private enum LINK_FLOW_STATUS {

		NOT_DONE,
		TO_RECALCULATE,
		DONE

	};

	public TrafficNodeModel(TrafficData tfData, double[][] turningFractions, double tStep){

		this.tfData = tfData;
		this.turningFractions = turningFractions;
		this.tStep = tStep;

	}

	public void run(TrafficNode node, double[] receivingFlow, double[] sendingFlow){

		//reset attributes
		initRunData(node, receivingFlow, sendingFlow);

		updateReceivingFractions();
		updateIndividualFlows();
		calculateIncomingFlows();
		calculateOutogoingFlows();

	}

	private void initRunData(TrafficNode node, double[] receivingFlow, double[] sendingFlow){

		//save the passed data
		this.currNode = node;
		this.currReceivingFlow = receivingFlow;
		this.currSendingFlow = sendingFlow;

		int numIncoming = node.incomingLinks.size(); 
		int numOutgoing = node.outgoingLinks.size();

		//initialise the structures required by run
		linksToDo = numIncoming;
		receivingFraction = new double[numIncoming];
		sendingCapFlows = new double[numIncoming][numOutgoing];
		capFlows = new double[numOutgoing];
		capacities = new double[numIncoming][numOutgoing] ;
		linkFlowCalculationStatus = new LINK_FLOW_STATUS[numIncoming];
		transferFlows = new double[numIncoming][numOutgoing];

		//TODO -- review how required the initialisation to 0 really is.
		Arrays.fill(receivingFraction, 0);
		Arrays.fill(capFlows,0);
		Arrays.fill(linkFlowCalculationStatus, LINK_FLOW_STATUS.NOT_DONE);

		for(int i= 0; i< numIncoming; i++){
			Arrays.fill(sendingCapFlows[i],0);
			Arrays.fill(capacities[i],0);
			Arrays.fill(transferFlows[i],0);

		}

	}

	private void updateReceivingFractions(){

		double cumFlow = 0;

		for(int j=0; j<currNode.outgoingLinks.size(); j++){
			cumFlow = 0;
			for(int i=0; i<currNode.incomingLinks.size(); i++){
				cumFlow = cumFlow + turningFractions[i][j]*currSendingFlow[i];
			}
			if(cumFlow>0){
				receivingFraction[j] = currReceivingFlow[j]/cumFlow;
			}
			else{
				//double max value is used to represent infinity 
				//must propagate it.
				receivingFraction[j] = Double.MAX_VALUE; 

			}
		}

		checkIncominglinkDemandOrSuppliedConstrained(cumFlow);
	}

	private void checkIncominglinkDemandOrSuppliedConstrained(double cumFlow){

		for(int i=0; i<currNode.incomingLinks.size(); i++){
			for(int j=0; j<currNode.outgoingLinks.size(); j++){
				if(receivingFraction[j]<1){
					if(turningFractions[i][j]*currSendingFlow[i]>0){
						linkFlowCalculationStatus[i] = LINK_FLOW_STATUS.TO_RECALCULATE;
					}
				}	
			}
			
			if(linkFlowCalculationStatus[i] == LINK_FLOW_STATUS.TO_RECALCULATE){
				cumFlow = currNode.incomingLinks.get(i).capacity*tStep/currSendingFlow[i];
				for(int j=0; j<currNode.outgoingLinks.size(); j++){
					sendingCapFlows[i][j] = turningFractions[i][j]*currSendingFlow[i]*cumFlow;
					capFlows[j] = capFlows[j] + sendingCapFlows[i][j];
				}
			}
			else{
				for(int j=0; j<currNode.outgoingLinks.size(); j++){
					capacities[i][j] = turningFractions[i][j]*currSendingFlow[i];
				}
				linkFlowCalculationStatus[i] = LINK_FLOW_STATUS.DONE;
				linksToDo--;
			}
		}
	}

	private void updateIndividualFlows(){
		int mostRestrictiveOutgoingLink = 0;
	
		while(linksToDo > 0){
			
			if(mostRestrictiveOutgoingLink != currNode.outgoingLinks.size()){
				mostRestrictiveOutgoingLink = mostRestrictiveOutgoingLink + 1;
			}
			else{
				mostRestrictiveOutgoingLink = 1;
			}
			
			//establish constraint
			double mostRestrictiveConstraint = 1;
			for(int j=0; j<currNode.outgoingLinks.size(); j++){
				if(capFlows[j]> 0.0000000001d){  //prevents rounding errors
					if(currReceivingFlow[j] >= 0){
						receivingFraction[j] = currReceivingFlow[j] / capFlows[j];
					}
					else{
						receivingFraction[j] = 1;
					}
				}
				else{
					receivingFraction[j] = 1;
				}
				if (receivingFraction[j] == mostRestrictiveConstraint){
					mostRestrictiveOutgoingLink = j;
					mostRestrictiveConstraint = receivingFraction[j];
				}
			}
			ArrayList<Integer> indexesOfLinksInFreeFlow = new ArrayList<Integer>();
			for(int i=0; i<currNode.incomingLinks.size(); i++){
				if(linkFlowCalculationStatus[i] != LINK_FLOW_STATUS.DONE){
					if(turningFractions[i][mostRestrictiveOutgoingLink]>0){
						if(currSendingFlow[i] <= currNode.incomingLinks.get(i).capacity*tStep*mostRestrictiveConstraint){
							indexesOfLinksInFreeFlow.add(i);
							for(int j=0; j<currNode.outgoingLinks.size(); j++){
								capacities[i][j] = turningFractions[i][j]*currSendingFlow[i];
							}
							linkFlowCalculationStatus[i] = LINK_FLOW_STATUS.DONE;
							linksToDo--;
						}
					}
				}
			}
			
			if(!indexesOfLinksInFreeFlow.isEmpty()){
				for(int j=0; j<currNode.outgoingLinks.size(); j++){
					for(int indexLinkInFF : indexesOfLinksInFreeFlow){
						capFlows[j] = capFlows[j] - sendingCapFlows[indexLinkInFF][j];
						currReceivingFlow[j] = currReceivingFlow[j] - turningFractions[indexLinkInFF][j]
								*currSendingFlow[indexLinkInFF];
						
					}
				}
			}
			else{
				for(int i=0; i<currNode.incomingLinks.size(); i++){
					if(linkFlowCalculationStatus[i] != LINK_FLOW_STATUS.DONE){
						if(turningFractions[i][mostRestrictiveOutgoingLink] > 0){
							for(int j=0; j<currNode.outgoingLinks.size(); j++){
								capFlows[j] = capFlows[j] - sendingCapFlows[i][j];
								currReceivingFlow[j] = currReceivingFlow[j] - (sendingCapFlows[i][j]*mostRestrictiveConstraint);
								capacities[i][j] = sendingCapFlows[i][j]*mostRestrictiveConstraint;
							}
							linkFlowCalculationStatus[i] = LINK_FLOW_STATUS.DONE;
							linksToDo--;
						}
					}
				}
			}
			
		}

	}

	private void calculateIncomingFlows(){

	}

	private void calculateOutogoingFlows(){

	}

	public void getIncomingFlows(){

	}

	public void getOutgoingFlows(){

	}

}
