package com.aarribas.traffictools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TrafficDataLogger {

	String outputFileName;

	public TrafficDataLogger() {

		//at the moment this is just a shortcut

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		this.outputFileName = new String(cal.getTime() + ".txt");
		saveLine("#" + dateFormat.format(cal.getTime()));

	}
	
	public void saveLine(String line){
		PrintWriter outputStream = null;
		try {
			outputStream  = new PrintWriter(new FileWriter(outputFileName, true));
		} catch(IOException e){
			System.err.println("Error opening the file " + outputFileName + " for append.");
			e.printStackTrace();
		}

		outputStream.println(line);
		outputStream.close();
	}

	public void saveGap(int iteration, double gap) {
		saveLine(iteration + " " + gap );

	}

}
