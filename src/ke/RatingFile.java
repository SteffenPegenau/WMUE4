package ke;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingFile implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 611922413741797203L;
	boolean isTestFile;
	public int givenRating;
	String rawText;
	private ArrayList<String> stopwords;

	public HashMap<String, Integer> words;

	public RatingFile(String folder, String filename, ArrayList<String> stopwords) {
		this.stopwords = stopwords;
		isTestFile = isTestFile(filename);
		givenRating = (isTestFile) ? getRating(filename) : -1;
		rawText = readFileContent(folder, filename);
		words = getWordMap();
	}
	

	private HashMap<String, Integer> getWordMap() {
		HashMap<String, Integer> words = new HashMap<String, Integer>();

		Pattern p = Pattern.compile("[\\w']+");
		Matcher m = p.matcher(rawText.replace('\'', ' '));

		while (m.find()) {
			String word = rawText.substring(m.start(), m.end()).toLowerCase();
			if(!stopwords.contains(word) && !word.matches(".*\\d+.*")) {
				if (words.containsKey(word)) {
					Integer currentVal = words.get(word);
					words.put(word, currentVal + 1);
				} else {
					words.put(word, 1);
				}
			}
		}
		return words;
	}

	private static boolean isTestFile(String filename) {
		return filename.contains("-");
	}

	private static int getRating(String filename) {
		return Integer.parseInt("" + filename.charAt(0));
	}

	@SuppressWarnings("finally")
	private static String readFileContent(String folder, String filename) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(folder + "/" + filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();
		try {
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return sb.toString();
		}

	}
	
	public String toCsvLine(String SEP, ArrayList<Map.Entry<String, Integer>> al, int MAX) {
		StringBuilder sb = new StringBuilder();
		sb.append(givenRating + SEP);
		
		int size = (al.size() > MAX) ? MAX : al.size();
		for(int i = 0; i < size; i++) {
			String word = al.get(i).getKey();
			if(words.containsKey(word)) {
				sb.append(words.get(word).toString());
			} else {
				sb.append("0");
			}
			sb.append(SEP);
		}
		
		sb.append("\n");
		return sb.toString();
	}

	public String toString() {
		List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(words.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
				return Integer.compare(b.getValue(), a.getValue());
			}
		});
		/*
		for (Map.Entry<String, Integer> e : entries) {
			// This loop prints entries. You can use the same loop
			// to get the keys from entries, and add it to your target list.
			//System.out.println(e.getKey() + ":" + e.getValue());
		}
		*/
		return entries.toString();

	}
}
