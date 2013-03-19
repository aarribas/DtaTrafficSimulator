package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.Arrays;

public class TrafficNodeModel {

	//data that stays the same for all nodes
	private TrafficData tfData;
	private double tStep;

	//data initialised and used when calling the method run
	private TrafficNode currNode;
	private double[][] currTurningFractions;
	private double[] currSendingFlow;
	private double[] currReceivingFlow;

	private int linksToDo;
	private double[] receivingFraction;
	private double[][] sendingCapFlows;
	private double[] capFlows;
	private double[][] capacities;
	private LINK_FLOW_STATUS[] linkFlowCalculationStatus;
	private double[][] transferFlows;
	
	private double[] flowIncomingLinks;
	private double[] flowOutgoingLinks;
	
	private enum LINK_FLOW_STATUS {

		NOT_DONE,
		TO_RECALCULATE,
		DONE

	};

	public TrafficNodeModel(TrafficData tfData, double tStep){

		this.tfData = tfData;
		this.tStep = tStep;

	}

	public void run(TrafficNode node, double[][] turningFractions, double[] receivingFlow, double[] sendingFlow){

		//reset attributes
		initRunData(node, turningFractions, receivingFlow, sendingFlow);

		updateReceivingFractions();
		updateIndividualFlows();
		calculateIncomingFlows();
		calculateOutogoingFlows();

	}

	private void initRunData(TrafficNode node, double[][] turningFractions, double[] receivingFlow, double[] sendingFlow){

		//save the passed data
		this.currNode = node;
		this.currReceivingFlow = receivingFlow;
		this.currSendingFlow = sendingFlow;
		this.currTurningFractions = turningFractions;

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
				cumFlow = cumFlow + currTurningFractions[i][j]*currSendingFlow[i];
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
					if(currTurningFractions[i][j]*currSendingFlow[i]>0){
						linkFlowCalculationStatus[i] = LINK_FLOW_STATUS.TO_RECALCULATE;
					}
				}	
			}
			
			if(linkFlowCalculationStatus[i] == LINK_FLOW_STATUS.TO_RECALCULATE){
				cumFlow = currNode.incomingLinks.get(i).capacity*tStep/currSendingFlow[i];
				for(int j=0; j<currNode.outgoingLinks.size(); j++){
					sendingCapFlows[i][j] = currTurningFractions[i][j]*currSendingFlow[i]*cumFlow;
					capFlows[j] = capFlows[j] + sendingCapFlows[i][j];
				}
			}
			else{
				for(int j=0; j<currNode.outgoingLinks.size(); j++){
					capacities[i][j] = currTurningFractions[i][j]*currSendingFlow[i];
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
					if(currTurningFractions[i][mostRestrictiveOutgoingLink]>0){
						if(currSendingFlow[i] <= currNode.incomingLinks.get(i).capacity*tStep*mostRestrictiveConstraint){
							indexesOfLinksInFreeFlow.add(i);
							for(int j=0; j<currNode.outgoingLinks.size(); j++){
								capacities[i][j] = currTurningFractions[i][j]*currSendingFlow[i];
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
						currReceivingFlow[j] = currReceivingFlow[j] - currTurningFractions[indexLinkInFF][j]
								*currSendingFlow[indexLinkInFF];
						
					}
				}
			}
			else{
				for(int i=0; i<currNode.incomingLinks.size(); i++){
					if(linkFlowCalculationStatus[i] != LINK_FLOW_STATUS.DONE){
						if(currTurningFractions[i][mostRestrictiveOutgoingLink] > 0){
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
		
		flowIncomingLinks = new double[currNode.incomingLinks.size()];
		for(int i=0; i<currNode.incomingLinks.size(); i++){
			double constraint = 1;
			
			//iterate through all  possible constraints
			for(int j=0; j<currNode.outgoingLinks.size(); j++){
				if(currTurningFractions[i][j] * currSendingFlow[i] > 0){
					constraint = capacities[i][j] / (currTurningFractions[i][j]*currSendingFlow[i]);
					
					//this is implemented in Matlab in a weird way 
					//using an unnecesary maxconstraint variable 
					//in principle this is equivalent and simpler;
					if(constraint > 1){
						constraint = 1; //bigger than 1 does not make sense
					}
				}
			}
			//set final flow
			flowIncomingLinks[i] = constraint * currSendingFlow[i];
		}
	}

	private void calculateOutogoingFlows(){
		
		//compute transferflows
		for(int i=0; i<currNode.incomingLinks.size(); i++){
			for(int j=0; j<currNode.outgoingLinks.size(); j++){
				transferFlows[i][j] = currTurningFractions[i][j] * flowIncomingLinks[i];
			}
		}
		
		//compute final outgoingflows
		for(int j=0; j<currNode.outgoingLinks.size(); j++){
			flowOutgoingLinks[j] = 0;
			for(int i=0; i<currNode.incomingLinks.size(); i++){
				flowOutgoingLinks[j] = flowOutgoingLinks[j] + transferFlows[i][j];
			}
		}

	}

	public double[] getIncomingFlows(){
		return flowIncomingLinks;
	}

	public double[] getOutgoingFlows(){
		return flowOutgoingLinks;

	}

}
