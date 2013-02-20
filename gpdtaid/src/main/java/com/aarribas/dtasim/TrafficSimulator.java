package com.aarribas.dtasim;

import java.util.ArrayList;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.RealMatrix;

public class TrafficSimulator {

	private ArrayList<RealMatrix> expandedODMatrices = new ArrayList<RealMatrix>();
	private TrafficData tfData = null;
	private double tEnd;
	private double tStep;

	public TrafficSimulator(String fileName, double tEnd, double tStep){
		this.tEnd = tEnd;
		this.tStep = tStep;

		TrafficDataLoader loader = new TrafficDataLoader();
		tfData = loader.load(fileName);

		expandODMatrices();
		
		System.out.println(expandedODMatrices.get(450).toString());

	}

	public void expandODMatrices(){

		int timeSliceNumber = 0;
		
		//compute all odmatrices - note that we expect the tEnd to be a multiple of tStep
		//TO DO add a check and possibly throw an exception to verify the above
		for(int i = 0; i < tEnd/tStep; i++){
			
			//we compute the corresponding ctime casting to float in order to avoid rounding errors
			float ctime = (float) (tStep * i);

			RealMatrix expandedODMatrix;

			System.out.println(ctime);

			//if we have reached the last timeslice we know that it is safe to add the remaining odmatrix
			if(timeSliceNumber == tfData.timeSlices.size()-1){
				expandedODMatrix = new Array2DRowRealMatrix(tfData.ODMatrices.get(timeSliceNumber));
				expandedODMatrix = expandedODMatrix.scalarMultiply(tStep);
			}
			else{
				
				//check if we change timeslice at a time not multiple of the tStep - the first timeSlice is expected to be 0
				//this is a mistake in the .mat
				//TO DO change .mat or adapt code to more reasonable values
				if((float)(ctime + tStep) > tfData.timeSlices.get(timeSliceNumber+1) && ctime !=0 ){

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
			expandedODMatrices.add(expandedODMatrix);
		}

	}

}
