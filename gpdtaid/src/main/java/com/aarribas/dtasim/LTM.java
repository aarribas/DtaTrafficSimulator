package com.aarribas.dtasim;

import java.util.ArrayList;

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
		
	}

	public void run() {
		// TODO Auto-generated method stub

	}
	
	public void calculateW(){
		
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
