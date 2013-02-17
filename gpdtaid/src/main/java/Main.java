import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.jmatio.common.MatDataTypes;
import com.jmatio.io.*;
import com.jmatio.types.*;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		MatFileReader mfr  = null;
		
		try {
			mfr = new MatFileReader("/Users/andresaan/Documents/MAI/Thesis/matlab/Exercise Final/toy_y.mat");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		MLStructure data = (MLStructure) mfr.getMLArray("data");
		
		Collection<String> names = data.getFieldNames();
		
		System.out.println(names.toString());
		
		MLStructure nodeinfo  = (MLStructure) data.getField("node");
		
		double t = ((MLDouble) nodeinfo.getField("outgoingLinks", 0)).get(0);
		
		System.out.println(t);
		
		
		
		
		
	}

}
