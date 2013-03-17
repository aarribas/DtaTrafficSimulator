package com.aarribas.dtasim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLStructure;

public class TrafficDataLoader {

	private MatFileReader mfr  = null;
	private TrafficData trafficData = null;
	private MLStructure data = null;

	public TrafficData load(String fileName)
	{
		//Read the .mat file
		try {
			mfr = new MatFileReader(fileName);
			data = (MLStructure) mfr.getMLArray("data");
			if(data == null)
			{
				//if there is no data structure we consider the .mat as empty.
				//we return no traffic data
				return null;
			}
		} catch (FileNotFoundException e) {
			System.err.println("The .mat file could not be found. Program will terminate.");
			System.exit(0);
		} catch (IOException e) {
			System.err.println("The .mat file could not be read. Program will terminate.");
			System.exit(0);
		}

		//load relevant content from the .mat file
		trafficData = new TrafficData();
		loadTimeSlices();
		loadNodes();
		loadLinks();
		loadODMatrices();

		return trafficData;

	}

	public void loadTimeSlices(){
		//obtain the timeslices as MLCell, and save the cells in an array
		ArrayList<MLArray> timeSlicesMLArray = ((MLCell)data.getField("timeslices")).cells();

		//read each cell as an MLDouble, and read the value (get(0)) of the MLDouble
		for(int i = 0; i < timeSlicesMLArray.size(); i++){
			trafficData.timeSlices.add(((MLDouble) timeSlicesMLArray.get(i)).get(0)); //TODO *1000 to justify
		}

	}

	public void loadNodes(){

		MLStructure nodeinfo  = (MLStructure) data.getField("node");


		//for each nodeinfo fill in the node values then add the node to the trafficData
		for(int i = 0; i < nodeinfo.getSize(); i++){
			TrafficNode tempNode = new TrafficNode();

			//save id and position
			tempNode.id = ((MLDouble) nodeinfo.getField("id", i)).get(0);
			tempNode.x = ((MLDouble) nodeinfo.getField("x", i)).get(0);
			tempNode.y = ((MLDouble) nodeinfo.getField("y", i)).get(0);

			trafficData.nodes.add(tempNode);
		}

	}

	public void loadLinks(){
		MLStructure linkinfo  = (MLStructure) data.getField("link");


		//for each nodeinfo fill in the node values then add the node to the trafficData
		for(int i = 0; i < linkinfo.getSize(); i++){
			TrafficLink tempLink = new TrafficLink();

			//save id and position
			tempLink.id = ((MLDouble) linkinfo.getField("id", i)).get(0);
			tempLink.startNode = ((MLDouble) linkinfo.getField("startNode", i)).get(0);
			tempLink.endNode = ((MLDouble) linkinfo.getField("endNode", i)).get(0);
			tempLink.type = ((MLDouble) linkinfo.getField("type", i)).get(0);
			tempLink.kJam = ((MLDouble) linkinfo.getField("kJam", i)).get(0);
			tempLink.capacity = ((MLDouble) linkinfo.getField("capacity", i)).get(0);
			tempLink.freeSpeed = ((MLDouble) linkinfo.getField("freeSpeed", i)).get(0);
			tempLink.length = ((MLDouble) linkinfo.getField("length", i)).get(0);

			trafficData.links.add(tempLink);
		}


	}

	public void loadODMatrices(){
		//obtain the matrices as MLCell, and save the cells in an array
		ArrayList<MLArray> ODMatricesMLArray = ((MLCell)data.getField("ODmatrices")).cells();

		//read each cell as an MLDouble, and read the corresponding double array
		for(int i = 0; i < ODMatricesMLArray.size(); i++){
			trafficData.ODMatrices.add(((MLDouble) ODMatricesMLArray.get(i)).getArray());
		}


	}

}
