import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



import com.aarribas.dtasim.TrafficData;
import com.aarribas.dtasim.TrafficDataLoader;
import com.jmatio.io.*;
import com.jmatio.types.*;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TrafficDataLoader loader = new TrafficDataLoader();
		TrafficData mydata = loader.load("/Users/andresaan/Documents/MAI/Thesis/matlab/Exercise Final/toy_y.mat");
		}

}
