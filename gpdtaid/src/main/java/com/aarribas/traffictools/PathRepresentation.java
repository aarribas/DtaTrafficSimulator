package com.aarribas.traffictools;

public class PathRepresentation {
	
	//represent the path as an array of node indexes
	public Integer[] nodeIndexes;
	
	//represent the path as an array of link indexes
	public int[] linkIndexes;
	
	public PathRepresentation(Integer[] nodeIndexes,int[] linkIndexes){
		this.nodeIndexes = nodeIndexes;
		this.linkIndexes = linkIndexes;
	}
	
}
