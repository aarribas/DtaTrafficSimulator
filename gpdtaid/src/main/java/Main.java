import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.aarribas.dtasim.TrafficSimulator;
import com.aarribas.dtasim.TrafficSwappingHeuristic;
import com.aarribas.dtasim.TrafficSwappingHeuristicDECF;
import com.aarribas.dtasim.TrafficSwappingHeuristicMSA;
import com.aarribas.dtasim.TrafficSwappingHeuristicDEC;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TrafficSimulator sim = new TrafficSimulator("/Users/andresaan/Documents/MAI/Thesis/matlab/Exercise Final/toy_parfix.mat", 3, 0.004);
		//sim.runDNLOnly();
		//create a swapping heuristic
		TrafficSwappingHeuristic  heuristic = new TrafficSwappingHeuristicDECF(1.0); 
//		TrafficSwappingHeuristic  heuristic = new TrafficSwappingHeuristicMSA(); 
		sim.runDTA(300000, heuristic);
		System.out.println(sim.getIteration());
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		System.out.println(dateFormat.format(cal.getTime()));
		
	}

}
