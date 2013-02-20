package com.aarribas.dtasim;

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

}
