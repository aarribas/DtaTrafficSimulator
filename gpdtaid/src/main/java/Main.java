import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.aarribas.dtasim.TrafficSimulator;
import com.aarribas.dtasim.TrafficSwappingHeuristic;
import com.aarribas.dtasim.TrafficSwappingHeuristicDEC;
import com.aarribas.dtasim.TrafficSwappingHeuristicMSA;
import com.aarribas.dtasim.TrafficSimulator.VERBOSITY;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//start simulator params = file, overall simulation duration, time click duration, duration of route interval in timeClicks
	//	TrafficSimulator sim = new TrafficSimulator("/Users/andresaan/Documents/MAI/Thesis/matlab/Exercise Final/toy_parfix.mat", 3, 0.004, 50, TrafficSimulator.VERBOSITY.VERY_VERBOSE);
		//TrafficSimulator sim = new TrafficSimulator("/Users/andresaan/Documents/MAI/Thesis/matlab/Exercise Final/net1.mat", 0.75, 0.0025, 50, TrafficSimulator.VERBOSITY.VERY_VERBOSE);
		TrafficSimulator sim = new TrafficSimulator("/Users/andresaan/Documents/MAI/Thesis/matlab/Exercise Final/rotterdam2.mat", 3.5, 0.0025, 150, TrafficSimulator.VERBOSITY.VERY_VERBOSE);
		
		//display initial time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		System.out.println(dateFormat.format(cal.getTime()));
		//save to file 
		sim.setSaveToFile(true);
		
		//create a swapping heuristic
 		//TrafficSwappingHeuristic  heuristic = new TrafficSwappingHeuristicDEC(0.1); 
 		TrafficSwappingHeuristic  heuristic = new TrafficSwappingHeuristicMSA(); 
 		sim.runDTA(10000, 0.0015, heuristic);
		
		//display some final info
		sim.displayRouteFractionPerRouteInterval();
		sim.displayRouteTravelTimesPerRoute();
		System.out.println("Iterations: " + sim.getIteration());
		
		//display final time
		cal = Calendar.getInstance();
		System.out.println(dateFormat.format(cal.getTime()));
		
	}

}
