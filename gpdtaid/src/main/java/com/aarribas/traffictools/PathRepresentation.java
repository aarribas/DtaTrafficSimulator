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
	
	public boolean equals(PathRepresentation pathRepresentation){
		
		if(nodeIndexes.length != pathRepresentation.nodeIndexes.length){
			return false;
		}
		else{
			
			for(int i = 0; i<nodeIndexes.length; i++){
				if(nodeIndexes[i] != pathRepresentation.nodeIndexes[i]){
					return false;
				}
			}
			
		}
		return true;
		
	}
	
	public int findIndexInPathOfNodeIndex(int nodeIndex){
		
		//linear dumb search  -- TODO implement a more performant search / use one from a library.
		
		for(int index=0; index<nodeIndexes.length; index++){
			if (nodeIndexes[index] == nodeIndex) return index;
		}
		
		//not found
		return -1;
	}
	
	public int findIndexInPathOfLinkIndex(int linkIndex){
		
		//linear dumb search -- TODO implement a more performant search / use one from a library.
		for(int index=0; index<linkIndexes.length; index++){
			if (linkIndexes[index] == linkIndex) return index;
		}
		
		//not found
		return -1;
	}
	
	public boolean isBorderNode(int nodeIndex){
		
		//it is a border node if the index matches with the first node index or the last one
		if(nodeIndexes[0] == nodeIndex || nodeIndexes[nodeIndexes.length] == nodeIndex){
			return true;
		}
		else{
			return false;
		}
		
		
	}
	
}
