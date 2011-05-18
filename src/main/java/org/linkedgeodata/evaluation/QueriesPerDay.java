package org.linkedgeodata.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class QueriesPerDay
{

	public static void main(String[] args) throws IOException, ParseException {
	
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(
						"/home/raven/Desktop/LGDSparql.txt"))));
		
		Multimap<Integer, String> queryMap = HashMultimap.create();
		Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
		
		
		String line;
		int lineCount = 0;
		int errorCount = 0;
		int selectCount = 0;
		DateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");

		DateFormat dateF = new SimpleDateFormat("yyyy-MM-dd");
		
		Map<String, Integer> dayToCount = new TreeMap<String, Integer>();
		
		while((line = reader.readLine()) != null) {

			String[] parts = line.split("\t");

			String str = parts[0];
			String ip = parts[1];;
			
			if(ip.contains("139.18.")) {
				continue;
			}
			++lineCount;
			
			Date date = dateFormat.parse(str);
			
			Calendar cal = new GregorianCalendar();
			cal.setTime(date);
			//cal.get(Calendar.D)
			
			String dateStr = dateF.format(date);
			
			
			QueryLog.increment(dayToCount, dateStr);
			
			//Calendar c = new GregorianCalendar(year, month, dayOfMonth, 0, 0, 0);
			
			
			//System.out.println(date);
			
		}
		
		for(Entry<String, Integer> entry : dayToCount.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
		
		System.out.println(lineCount);
	}
}
