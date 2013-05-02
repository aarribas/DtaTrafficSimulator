import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.aarribas.dtasim.TrafficSimulator;
import com.aarribas.dtasim.TrafficSwappingHeuristic;
import com.aarribas.dtasim.TrafficSwappingHeuristicMSA;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TrafficSimulator sim = new TrafficSimulator("/Users/andresaan/Documents/MAI/Thesis/matlab/Exercise Final/toy_par.mat", 3, 0.004);
		//sim.runDNLOnly();
		//create a swapping heuristic
		TrafficSwappingHeuristic  msa = new TrafficSwappingHeuristicMSA();
		sim.runDTA(30000000, msa);
		System.out.println(sim.getIteration());
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		System.out.println(dateFormat.format(cal.getTime()));
		
	}

}
