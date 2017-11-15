package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.EndingPreProcessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

public class Model_VSM_full {
	public final static int num_FragTutorial 	= 57; 
	public static Map<String,List<String>> training_map = new HashMap<>();
	public static Map<String,Map<String,Double>> result_map = new HashMap<>();

	public final static String input_name = "col_concatinate";
	public final static String model_name = input_name + "_VSM";
	public static String path = "/"+ input_name + ".txt";

	public static void main(String args[]) throws IOException{
		
		/*		Initialize Log file
		Store info of training */

		File file_output	= new File ("resources/output/model/"+model_name);
		FileWriter fWriter 	= new FileWriter (file_output);
		PrintWriter writer 	= new PrintWriter (fWriter);
		
		// *****************************************************************************
		
		/*	
		 * 	Step 1: Read Training Corpus and build a Map
		 *  Key		= API name
		 *  Value	= List / Array of its DOC_ID
		 */
		File file_training_corpus	= new File ("resources/input"+path);
		Scanner sc_training_corpus = new Scanner(file_training_corpus);
		int i = 0;
		String first_token 	= "";
		String last_token	= "";
		List<String> DOC_ID = new ArrayList<String>();
		while (sc_training_corpus.hasNextLine()){
			String s = sc_training_corpus.nextLine();


			if (i > num_FragTutorial - 1){				
				String arr[] = s.split(" ", 2);
				first_token = arr[0].replace(":", "");	// Read the first token

				if (!first_token.equals(last_token)){

					DOC_ID = new ArrayList<String>();
				}
				DOC_ID.add("DOC_"+i);

				if (training_map.containsKey(first_token)){

					training_map.replace(first_token, DOC_ID);
				}else{
					training_map.put(first_token, DOC_ID);

				}

				last_token = first_token;	
			}
			i++;
		}
		sc_training_corpus.close();


		// *****************************************************************************

		/*
		 *	Step 2: Build TF or IF-IDF for TUT and DOC 
		 *	Key: ID
		 *	Value: TF / TF-IDF
		 */
		Map<String,Map<String,Double>> TF_IDF_map 	= new HashMap<>();
		Map<String,Map<String,Double>> term_map		= new HashMap<>();
		
		TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
		
		int j =0;
		Scanner sc = new Scanner(file_training_corpus);
		while (sc.hasNextLine()){
			String line = sc.nextLine();
			
			//remove stop words
			line = line.replaceAll("[.,?:;!//(//)//[//]]", " ");
			
			EndingPreProcessor end = new EndingPreProcessor();
			line = end.preProcess(line); // Gets rid of endings: ed,ing, ly, s, .
			
			Tokenizer tokenizer = tokenizerFactory.create(line);
			
			//get the whole list of tokens
			List<String> tokens = tokenizer.getTokens();

			//iterate over the tokens
			while(tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				tokens.add(token);
			}
//			double test = 0;
			Map<String,Double> m1 = new HashMap<>();
			Map<String,Double> m2 = new HashMap<>();
			TFIDFCalculator cal = new TFIDFCalculator();
			for (String s : tokens){
				if (!m1.containsKey(s)){
					m1.put(s, cal.tf(tokens, s));
					m2.put(s, cal.term_count(tokens, s));
				}
					
			}
			
			TF_IDF_map.put("DOC_"+j, m1);
			term_map.put("DOC_"+j, m2);
			j++;
		}
		
		for (Entry<String,Map<String,Double>> entry : TF_IDF_map.entrySet()){
//			System.out.println(entry.getKey() + ": " +entry.getValue());
			System.out.println(entry.getKey() + ": " +get_length(entry.getValue()));
			
//			System.out.println(entry.getKey());
		}		
		sc.close();
		// *****************************************************************************
		/*
		 * Test Cosine: Pass
		 *
		 */
		
//		String example = "this is a an a example of this ";
//		Tokenizer tokenizer = tokenizerFactory.create(example);
//		List<String> tokens = tokenizer.getTokens();
//		while(tokenizer.hasMoreTokens()) {
//			String token = tokenizer.nextToken();
//			tokens.add(token);
//		}
//		Map<String,Double> m = new HashMap<>();
//		TFIDFCalculator cal = new TFIDFCalculator();
//		for (String s : tokens){
//			if (!m.containsKey(s))
//				m.put(s, cal.tf(tokens, s));
//		}
//		System.out.println(cosine_sim(m, m));
//		
//		System.out.println(cosine_sim(TF_IDF_map.get("DOC_1"), TF_IDF_map.get("DOC_2")));
//		System.out.println(m.toString());
//		
		/*
		 * Test Precision
		 */
//		String example = "java.util.Date.parse	(	String	): Attempts to interpret the string s as a representation of a date and time. If the attempt is successful, the time indicated is returned represented as the distance, measured in milliseconds, of that time from the epoch (00:00:00 GMT on January 1, 1970). If the attempt fails, an IllegalArgumentException is thrown.  It accepts many syntaxes; in particular, it recognizes the IETF standard date syntax: Sat, 12 Aug 1995 13:30:00 GMT. It also understands the continental U.S. time-zone abbreviations, but for general use, a time-zone offset should be used: Sat, 12 Aug 1995 13:30:00 GMT+0430 (4 hours, 30 minutes west of the Greenwich meridian). If no time zone is specified, the local time zone is assumed. GMT and UTC are considered equivalent.  The string s is processed from left to right, looking for data of interest. Any material in s that is within the ASCII parenthesis characters ( and ) is ignored. Parentheses may be nested. Otherwise, the only characters permitted within s are these ASCII characters:  abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789,+-:/ and whitespace characters. A consecutive sequence of decimal digits is treated as a decimal number: If a number is preceded by + or - and a year has already been recognized, then the number is a time-zone offset. If the number is less than 24, it is an offset measured in hours. Otherwise, it is regarded as an offset in minutes, expressed in 24-hour time format without punctuation. A preceding - means a westward offset. Time zone offsets are always relative to UTC (Greenwich). Thus, for example, -5 occurring in the string would mean five hours west of Greenwich and +0430 would mean four hours and thirty minutes east of Greenwich. It is permitted for the string to specify GMT, UT, or UTC redundantly-for example, GMT-5 or utc+0430. The number is regarded as a year number if one of the following conditions is true:  The number is equal to or greater than 70 and followed by a space, comma, slash, or end of string The number is less than 70, and both a month and a day of the month have already been recognized  If the recognized year number is less than 100, it is interpreted as an abbreviated year relative to a century of which dates are within 80 years before and 19 years after the time when the Date class is initialized. After adjusting the year number, 1900 is subtracted from it. For example, if the current year is 1999 then years in the range 19 to 99 are assumed to mean 1919 to 1999, while years from 0 to 18 are assumed to mean 2000 to 2018. Note that this is slightly different from the interpretation of years less than 100 that is used in java.text.SimpleDateFormat. If the number is followed by a colon, it is regarded as an hour, unless an hour has already been recognized, in which case it is regarded as a minute. If the number is followed by a slash, it is regarded as a month (it is decreased by 1 to produce a number in the range 0 to 11), unless a month has already been recognized, in which case it is regarded as a day of the month. If the number is followed by whitespace, a comma, a hyphen, or end of string, then if an hour has been recognized but not a minute, it is regarded as a minute; otherwise, if a minute has been recognized but not a second, it is regarded as a second; otherwise, it is regarded as a day of the month.  A consecutive sequence of letters is regarded as a word and treated as follows: A word that matches AM, ignoring case, is ignored (but the parse fails if an hour has not been recognized or is less than 1 or greater than 12). A word that matches PM, ignoring case, adds 12 to the hour (but the parse fails if an hour has not been recognized or is less than 1 or greater than 12). Any word that matches any prefix of SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, or SATURDAY, ignoring case, is ignored. For example, sat, Friday, TUE, and Thurs are ignored. Otherwise, any word that matches any prefix of JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, or DECEMBER, ignoring case, and considering them in the order given here, is recognized as specifying a month and is converted to a number (0 to 11). For example, aug, Sept, april, and NOV are recognized as months. So is Ma, which is recognized as MARCH, not MAY. Any word that matches GMT, UT, or UTC, ignoring case, is treated as referring to UTC. Any word that matches EST, CST, MST, or PST, ignoring case, is recognized as referring to the time zone in North America that is five, six, seven, or eight hours west of Greenwich, respectively. Any word that matches EDT, CDT, MDT, or PDT, ignoring case, is recognized as referring to the same time zone, respectively, during daylight saving time. Once the entire string s has been scanned, it is converted to a time result in one of two ways. If a time zone or time-zone offset has been recognized, then the year, month, day of month, hour, minute, and second are interpreted in UTC and then the time-zone offset is applied. Otherwise, the year, month, day of month, hour, minute, and second are interpreted in the local time zone.";
////		String example = "Attempts to interpret the string s as a representation of a date and time. If the attempt is successful, the time indicated is returned represented as the distance, measured in milliseconds, of that time from the epoch (00:00:00 GMT on January 1, 1970). If the attempt fails, an IllegalArgumentException is thrown.  It accepts many syntaxes; in particular, it recognizes the IETF standard date syntax: Sat, 12 Aug 1995 13:30:00 GMT. It also understands the continental U.S. time-zone abbreviations, but for general use, a time-zone offset should be used: Sat, 12 Aug 1995 13:30:00 GMT+0430 (4 hours, 30 minutes west of the Greenwich meridian). If no time zone is specified, the local time zone is assumed. GMT and UTC are considered equivalent.  The string s is processed from left to right, looking for data of interest. Any material in s that is within the ASCII parenthesis characters ( and ) is ignored. Parentheses may be nested. Otherwise, the only characters permitted within s are these ASCII characters:  abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789,+-:/ and whitespace characters. A consecutive sequence of decimal digits is treated as a decimal number: If a number is preceded by + or - and a year has already been recognized, then the number is a time-zone offset. If the number is less than 24, it is an offset measured in hours. Otherwise, it is regarded as an offset in minutes, expressed in 24-hour time format without punctuation. A preceding - means a westward offset. Time zone offsets are always relative to UTC (Greenwich). Thus, for example, -5 occurring in the string would mean five hours west of Greenwich and +0430 would mean four hours and thirty minutes east of Greenwich. It is permitted for the string to specify GMT, UT, or UTC redundantly-for example, GMT-5 or utc+0430. The number is regarded as a year number if one of the following conditions is true:  The number is equal to or greater than 70 and followed by a space, comma, slash, or end of string The number is less than 70, and both a month and a day of the month have already been recognized  If the recognized year number is less than 100, it is interpreted as an abbreviated year relative to a century of which dates are within 80 years before and 19 years after the time when the Date class is initialized. After adjusting the year number, 1900 is subtracted from it. For example, if the current year is 1999 then years in the range 19 to 99 are assumed to mean 1919 to 1999, while years from 0 to 18 are assumed to mean 2000 to 2018. Note that this is slightly different from the interpretation of years less than 100 that is used in java.text.SimpleDateFormat. If the number is followed by a colon, it is regarded as an hour, unless an hour has already been recognized, in which case it is regarded as a minute. If the number is followed by a slash, it is regarded as a month it is decreased by 1 to produce a number in the range 0 to 11, unless a month has already been recognized, in which case it is regarded as a day of the month. If the number is followed by whitespace, a comma, a hyphen, or end of string, then if an hour has been recognized but not a minute, it is regarded as a minute; otherwise, if a minute has been recognized but not a second, it is regarded as a second; otherwise, it is regarded as a day of the month.  A consecutive sequence of letters is regarded as a word and treated as follows: A word that matches AM, ignoring case, is ignored but the parse fails if an hour has not been recognized or is less than 1 or greater than 12. A word that matches PM, ignoring case, adds 12 to the hour but the parse fails if an hour has not been recognized or is less than 1 or greater than 12. Any word that matches any prefix of SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, or SATURDAY, ignoring case, is ignored. For example, sat, Friday, TUE, and Thurs are ignored. Otherwise, any word that matches any prefix of JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, or DECEMBER, ignoring case, and considering them in the order given here, is recognized as specifying a month and is converted to a number. For example, aug, Sept, april, and NOV are recognized as months. So is Ma, which is recognized as MARCH, not MAY. Any word that matches GMT, UT, or UTC, ignoring case, is treated as referring to UTC. Any word that matches EST, CST, MST, or PST, ignoring case, is recognized as referring to the time zone in North America that is five, six, seven, or eight hours west of Greenwich, respectively. Any word that matches EDT, CDT, MDT, or PDT, ignoring case, is recognized as referring to the same time zone, respectively, during daylight saving time. Once the entire string s has been scanned, it is converted to a time result in one of two ways. If a time zone or time-zone offset has been recognized, then the year, month, day of month, hour, minute, and second are interpreted in UTC and then the time-zone offset is applied. Otherwise, the year, month, day of month, hour, minute, and second are interpreted in the local time zone.";
////		String example = "Attempts to interpret the string s as a representation of a date and time If the attempt is successful the time indicated is returned represented as the distance measured in milliseconds of that time from the epoch (00:00:00 GMT on January 1, 1970) If the attempt fails an IllegalArgumentException is thrown k";
////		String example = "java.util.Date.readObject	(	ObjectInputStream	): Reconstitute this object from a stream (i.e., deserialize it).";
//		Tokenizer tokenizer = tokenizerFactory.create(example);
//		
//		//get the whole list of tokens
//		List<String> tokens = tokenizer.getTokens();
//
//		//iterate over the tokens
//		while(tokenizer.hasMoreTokens()) {
//			String token = tokenizer.nextToken();
//			tokens.add(token);
//		}
//		Map<String,Double> m = new HashMap<>();
//		TFIDFCalculator cal = new TFIDFCalculator();
//		for (String s : tokens){
//			System.out.println(s +": "+cal.tf(tokens, s));
//			if (!m.containsKey(s))
//				m.put(s, cal.tf(tokens, s));
//		}
//		double sum = 0.0;
//		for (Entry<String,Double> e : m.entrySet()){
//			sum = sum + e.getValue();
//			System.out.println("sum "+e.getKey()+": "+sum);
//		}
//		System.out.println("sum: "+Math.round(sum));
		
//		System.out.println(cal.tf(tokens, "a"));
		
		
		/*
		 * Step 3: Build API vector using MAX / AVERAGE function
		 * Cosine similarity
		 */

		for (Entry<String, List<String>> entry : training_map.entrySet()){
			Map<String,Double> simi_map  = max(entry.getValue(),TF_IDF_map);
			//			Map<String,Double> simi_map  = average(entry.getValue(),vec);

			simi_map	= sortByValue(simi_map);
			simi_map	= convert_local(simi_map);
			result_map.put(entry.getKey(), simi_map);
			
		}
		
		for (Entry<String, Map<String, Double>> entry : result_map.entrySet()){
//			System.out.println(entry.getKey()+"\n"+entry.getValue().toString());
			
		}		
		

		// *****************************************************************************
		
		
		/*
		 * Step: Export model
		 */
		for (Entry<String,Map<String,Double>> entry : result_map.entrySet()){
			writer.println(entry.getKey());
			for (Entry<String,Double> entry_in : entry.getValue().entrySet()){
				writer.print(entry_in.getKey() + " " + entry_in.getValue() + " ");
			}
			writer.println();
		}
		writer.close();
		// *****************************************************************************
	}
	

	public static Map<String,Double> max(List<String> list,Map<String,Map<String,Double>> map){
		/*
		 * Key 	 = DOC ID
		 * Value = Cosine Similarity
		 */
		Map<String,Double> simi_map  = new TreeMap<>();
		for (String DOC_ID : list){
			for (int i = 0; i < num_FragTutorial; i++){
				String TUT_ID = "DOC_"+i;
				double new_simi = cosine_sim(map.get(DOC_ID), map.get(TUT_ID));
				double current_simi	= simi_map.getOrDefault("DOC_"+i, 2.0);
				if (current_simi != 2.0 && current_simi < new_simi){
					simi_map.replace("DOC_"+i, new_simi);

				}else if(current_simi == 2.0){
					simi_map.put("DOC_"+i, new_simi); // Put DOC_ID and Similarity 
				} 
				
				//				double new_simi	= vec.similarity(ID, "DOC_"+i);
				//				double current_simi	= simi_map.getOrDefault("DOC_"+i, 2.0);
				//        		if (current_simi != 2.0 && current_simi < new_simi){
				//        			simi_map.replace("DOC_"+i, new_simi);
				//
				//        		}else if(current_simi == 2.0){
				//        			simi_map.put("DOC_"+i, new_simi); // Put DOC_ID and Similarity 
				//        		} 			
			}
		}

		return simi_map;
	}

	public static double cosine_sim(Map<String,Double> m1, Map<String,Double> m2){
//		m1.
		double n = 0;
		double cosine_sim = 0;
		for (Entry<String,Double> entry : m1.entrySet()){
			String s	= entry.getKey();
			double x1 	= entry.getValue();
			double x2	= m2.getOrDefault(s, 0.0);
			double x	= x1 * x2;
//			System.out.println("x1: "+x1+" x2: "+x2+" x: "+x);
			
			n = n + x;		
			
		}
//		System.out.println("n: "+n);
//		System.out.println("m1: "+get_length(m1));
//		System.out.println("m2: "+get_length(m2));
		cosine_sim = n / (get_length(m1) * get_length(m2));
		return cosine_sim;
	}
	
	public static double get_length(Map<String,Double> m){
		double length = 0.0;
		for (Entry<String,Double> entry : m.entrySet()){
			length = length + entry.getValue() * entry.getValue();
//			length += entry.getValue();
		}	
		return Math.sqrt(length);	
//		return length;
	}
	
	public static double convert_range(double x ,double in_min, double in_max, double out_min, double out_max){
		double new_x = (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min; 
		return new_x;
		
	}
	public static Map<String,Double> convert_range_map(Map<String,Double> map, double range_min, double range_max){
		for (Entry<String,Double> entry : map.entrySet()){
			double new_simi = convert_range(entry.getValue(),range_min,range_max,0,1);
			entry.setValue(new_simi);
		}
		return map;
	}
	
	public static Map<String,Double> convert_local(Map<String,Double> map){
		double min = 1;
		double max = -1;
		for (Entry<String,Double> entry : map.entrySet()){
			double value = entry.getValue();
			if (max < value)
				max = value;
			if (min > value)
				min = value;

		}	
		return convert_range_map(map,min,max);
	}
	
	// Sort Map by Value
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey, 
						Map.Entry::getValue, 
						(e1, e2) -> e1, 
						LinkedHashMap::new
						));
	}
}
