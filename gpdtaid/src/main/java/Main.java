import com.aarribas.dtasim.TrafficSimulator;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TrafficSimulator sim = new TrafficSimulator("/Users/andresaan/Documents/MAI/Thesis/matlab/Exercise Final/toy_par.mat", 3, 0.004);
		//sim.runDNLOnly();
		sim.runDTA(30);
	}

}
