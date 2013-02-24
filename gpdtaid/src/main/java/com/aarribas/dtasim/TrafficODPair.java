package com.aarribas.dtasim;

import java.util.ArrayList;


public class TrafficODPair {
	
	public double x;
	public double y;
	
	public TrafficODPair(double x, double y){
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object p){
		TrafficODPair temp = (TrafficODPair) p;
		return (temp.x == x && temp.y == y);
	}
	
	public String toString(){
		return new String(x + "/" + y);
	}
	
	@Override
	public int hashCode(){
		return new String(x + "," + y).hashCode();
	}
	
	public int getIndexStartNode(ArrayList<TrafficNode> nodes){
		
		return getIndexOfNode(nodes, x);
		
	}
	
	public int getIndexEndNode(ArrayList<TrafficNode> nodes){
		
		return getIndexOfNode(nodes, y);
		
	}
	
	private int getIndexOfNode(ArrayList<TrafficNode> nodes,double id){
		
		for(int nodeIndex = 0; nodeIndex< nodes.size(); nodeIndex++){
			if(nodes.get(nodeIndex).id == id){
				return nodeIndex;
			}
		}
		
		return -1;
	}

}
