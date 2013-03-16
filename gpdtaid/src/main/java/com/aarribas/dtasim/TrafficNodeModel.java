package com.aarribas.dtasim;

public class TrafficNodeModel {
	
	private enum linkFlowStatus {
		
		NOT_DONE,
		TO_RECALCULATE,
		DONE
		
	};
	
	public TrafficNodeModel(){
		
		//update and initialize class attributes
		init();
		
		//run the node model, which ultimately updates the incoming and outgoing flows
		run();
	}
	
	private void run(){
		
		updateReceivingFractions();
		checkIncominglinkDemandOrSuppliedConstrained();
		updateIndividualFlows();
		calculateIncomingFlows();
		calculateOutogoingFlows();
			
	}
	
	private void init(){
		
	}
	
	private void updateReceivingFractions(){
		
	}
	
	private void checkIncominglinkDemandOrSuppliedConstrained(){
		
	}
	
	private void updateIndividualFlows(){
		
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
