package com.aarribas.dtasim;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.RealMatrix;

public class TrafficSimulator {

	private ArrayList<double[][]> expandedODMatrices = new ArrayList<double[][]>();
	private TrafficData tfData = null;
	private HashSet<TrafficODPair> ODPairs = new HashSet<TrafficODPair>();
	private double tEnd;
	private double tStep;
	private ArrayList<double[]> linkTravelTimes = new ArrayList<double[]>();;

	public TrafficSimulator(String fileName, double tEnd, double tStep){
		this.tEnd = tEnd;
		this.tStep = tStep;

		TrafficDataLoader loader = new TrafficDataLoader();
		tfData = loader.load(fileName);

		expandODMatrices();
		computeODPairs();
		computeInitialTravelTimes();

//		System.out.println(new Array2DRowRealMatrix(expandedODMatrices.get(450)).toString());
//		System.out.println(ODPairs.toString());
//		for(int i = 0; i<linkTravelTimes.get(39).length; i++){
//			System.out.println(i + "=" +linkTravelTimes.get(39)[i]);
//		}

	}

	public void expandODMatrices(){

		//start at the first timeSlice
		int timeSliceNumber = 0;

		//a little error Tolerance is required in order to compare too doubles (foreseen representation errors of floating point)
		double doubleErrorTolerance = 0.000001d;

		//compute all odmatrices - note that we expect the tEnd to be a multiple of tStep
		//TO DO: add a check and possibly throw an exception
		for(int i = 0; i < tEnd/tStep; i++){

			double ctime = tStep * i;

			RealMatrix expandedODMatrix;

			System.out.println(ctime);

			//if we have reached the last timeslice we know that it is safe to add the remaining odmatrix
			if(timeSliceNumber == tfData.timeSlices.size()-1){
				expandedODMatrix = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber));
				expandedODMatrix = expandedODMatrix.scalarMultiply(tStep);
			}
			else{

				//check if we change timeslice at a time not multiple of the tStep
				//the first timeSlice is expected to be 0 this is a mistake in the .mat
				//TO DO change .mat or adapt code to more reasonable values
				if((ctime + tStep) > tfData.timeSlices.get(timeSliceNumber+1) + doubleErrorTolerance && ctime !=0 ){

					//part of the flow in the matrix is due to OD prior to timeSlice change
					expandedODMatrix = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber));
					expandedODMatrix = expandedODMatrix.scalarMultiply(tfData.timeSlices.get(timeSliceNumber) - ctime);
					//the remaining flow is due to the OD in the next timeSlice
					RealMatrix temp = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber + 1));
					temp = temp.scalarMultiply(tStep - tfData.timeSlices.get(timeSliceNumber));
					expandedODMatrix = expandedODMatrix.add(temp);

				}
				else{
					//if there is no timeslice change it is save to add the contribution of the ODMatrix for that timeSlice
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

	public void computeODPairs(){

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
						//save to the set of tuples
						ODPairs.add(new TrafficODPair(tfData.nodes.get(row).id, tfData.nodes.get(column).id));

					}

				}
			}

		}

	}

	public void computeInitialTravelTimes(){

		for(TrafficLink link : tfData.links){
			double[] travelTimes = new double[(int)(tEnd/tStep)];
			
			//we initialize traveltimes to length/freespeed
			java.util.Arrays.fill(travelTimes, link.length/link.freeSpeed);	
			
			//add the corresponding array of traveltimes
			linkTravelTimes.add(travelTimes);
		}


	}

}
