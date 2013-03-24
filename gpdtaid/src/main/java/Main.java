import com.aarribas.dtasim.TrafficSimulator;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TrafficSimulator sim = new TrafficSimulator("/Users/andresaan/Documents/MAI/Thesis/matlab/Exercise Final/toy_y.mat", 1.2, 0.002);
		sim.runDNLOnly();
	}

}
