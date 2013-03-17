package com.aarribas.traffictools;

import java.util.ArrayList;

import com.aarribas.dtasim.TrafficLink;

public class CumulativeBasedCalculator {

	static public double computeCumulativeTime(double t, double tEnd, double tStep){
		return 0.0;
	}


	static public double[] computeCumulativeToSpeeds(ArrayList<TrafficLink>links, double tEnd, double tStep){

		//uses findcumulative internally
		
//simSpeeds= [links.freeSpeed]'*ones(1,size(timeSteps,2));
//for t=1:size(timeSteps,2)
//    for i=1:length(links)
//        time = findCumulativeTime(links(i).downStreamCumulative(1,:),links(i).upStreamCumulative(1,t),timeSteps);
//        if (time > timeSteps(end))
//            simSpeeds(i,t)=max(0,simSpeeds(i,max(1,t-1))-(simSpeeds(i,max(1,t-2))-simSpeeds(i,max(1,t-1))));
//        else
//            simSpeeds(i,t) = links(i).length/(time-timeSteps(t));
//        end
//        if simSpeeds(i,t)<=0 || links(i).downStreamCumulative(1,t) == 0
//            simSpeeds(i,t)=links(i).freeSpeed;
//        elseif simSpeeds(i,t)>links(i).freeSpeed;
//            simSpeeds(i,t)=links(i).freeSpeed;
//        end
//    end
//end

		return null;
	}

	static public double computeCumulativeToFlows(){

		//TODO
		return 0.0;
	}

	static public double computeCumulativeToDensity(){
		//TODO
		return 0.0;
	}

}
