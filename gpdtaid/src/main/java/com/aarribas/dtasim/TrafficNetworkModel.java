package com.aarribas.dtasim;

import java.util.ArrayList;

public interface TrafficNetworkModel {
	
	public void run(ArrayList<ArrayList<double[][]>>turningFractions);
	
}
