package ke;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Map.Entry;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class Classifier {
	String STOPWORDS = "stopwords/english";
	int CHUNKSIZE = 500;
	double T_MIN = 2.0;
	
	private HashMap<String, RatingFile> trainingFiles;
	private HashMap<String, RatingFile> testingFiles;
	private ArrayList<String> stopwords;
	private String trainingFolder;
	private String testFolder;
	
	public static void main(String[] args) {
		Classifier c = new Classifier("unfoldedTraining", "test");
	}
	
	public Classifier(String trainingFolder, String testFolder) {
		this.trainingFolder = trainingFolder;
		this.testFolder = testFolder;
		
		populateStopwords();
		//System.out.println(stopwords);
		
		trainingFiles = new HashMap<>();
		testingFiles = new HashMap<>();
		
		HashSet<String> allWords = new HashSet<String>();
		HashSet<String> uniqueGoodWords = new HashSet<String>();
		HashSet<String> goodWords = getGoodWords();
		
		for(String filename : getFileNames(trainingFolder)) {
			RatingFile rf = new RatingFile(trainingFolder, filename, stopwords);
			trainingFiles.put(filename, rf);
			for(String w : rf.words.keySet()) {
				if(goodWords.contains(w)) {
					uniqueGoodWords.add(w);
				}
				allWords.add(w);
			}
		}
		
		System.out.println("Found " + uniqueGoodWords.size() + " good, unique words.");
		System.out.println("(Bad words: " + (allWords.size() - uniqueGoodWords.size()) + ")");
		
		
		
		ArrayList<Map.Entry<String, Integer>> sortedListOfWords = SerializableList.sortList(mergeWords(trainingFiles, uniqueGoodWords));
		SerializableList sl = new SerializableList(sortedListOfWords);
		
		System.out.println("* Iteration 1 (" + sl.al.size() + " words)");
		ArrayList<Integer> relevantIndices = new ArrayList<Integer>();
		int words = sl.al.size();
		int i = 0;
		int MAX = CHUNKSIZE;
		while(i < MAX) {
			System.out.println("\t Words from " + i + " to " + MAX);
			OLSSchaetzer ols = new OLSSchaetzer(trainingFiles, sl.al, getRangeList(i,MAX));
			relevantIndices.addAll(ols.getRelevantIndices(T_MIN));
			i = MAX;
			MAX = (words > i + CHUNKSIZE) ? i + CHUNKSIZE :  words - 1;
		}
		System.out.println(" Reduced list of " + words + " to " + relevantIndices.size() + " words");
		
		for(int iteration = 2; iteration <= 3; iteration++) {
			int sizeBefore = relevantIndices.size();
			System.out.println("* Iteration 2 (" + sizeBefore + " words)");
			OLSSchaetzer ols = new OLSSchaetzer(trainingFiles, sl.al, relevantIndices);
			relevantIndices = ols.getRelevantIndices(T_MIN);
			System.out.println(" Reduced list of " + sizeBefore + " to " + relevantIndices.size() + " words");
		}
		
		
		
		/*
		HashSet<Integer> relevantIndices = readRelevantIndices();
		if(relevantIndices.size() > 0) {
			sl.removeAllOtherIndices(readRelevantIndices());
		}
		*/
		
		//sl.saveMatrixCsv(trainingFiles);
		
		//OLSSchaetzer ols = new OLSSchaetzer(trainingFiles, sl.al);
		
	}
	
	public ArrayList<Integer> getRangeList(int start, int end) {
		return (ArrayList<Integer>) IntStream.range(start, end).boxed().collect(Collectors.toList());
	}
	
	public static HashSet<Integer> readRelevantIndices() {
		HashSet<Integer> indices = new HashSet<Integer>();
		try (BufferedReader br = new BufferedReader(new FileReader("relevantIndices.txt"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       indices.add(Integer.valueOf(line.replaceAll("\\s+","")));
		    }
		} catch (FileNotFoundException e) {
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(indices);
		return indices;
	}
	
	public static HashSet<String> getGoodWords() {
		HashSet<String> goodWords = new HashSet<String>();
		try (BufferedReader br = new BufferedReader(new FileReader("wordlist.txt"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       line = line.replace("'", "");
		       goodWords.add(line.toLowerCase());
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return goodWords;
	}
	
	private ArrayList<Map.Entry<String, Integer>> allWordOccurences(HashMap<String, RatingFile> files, HashSet<String> uniqueGoodWords) {
		HashMap<String, Integer> words = new HashMap<String, Integer>();
		
		int i = 1;
		int size = files.size();
		for(String filename : files.keySet()) {
			if(i % 100 == 0) System.out.println("[" + i + "\t/" + size + "] "); //Adding words: " + rf.toString());
			RatingFile rf = files.get(filename);
			HashMap<String, Integer> filesWords = rf.words;
			for(String word : filesWords.keySet()) {
				if(uniqueGoodWords.contains(word)) {
					int n = filesWords.get(word);
					if(words.containsKey(word)) {
						int oldN = words.get(word);
						words.put(word, oldN + n);
					} else {
						words.put(word, n);
					}
				}
			}
			i++;
		}
		ArrayList<Map.Entry<String, Integer>> result = new ArrayList<Map.Entry<String, Integer>>();
		for(Map.Entry<String, Integer> e : words.entrySet()) {
			result.add(e);
		}
		return SerializableList.sortList(result);
	}
	
	private ArrayList<Map.Entry<String, Integer>> mergeWords(HashMap<String, RatingFile> files, HashSet<String> uniqueGoodWords) {
		ArrayList<Map.Entry<String, Integer>> entries = SerializableList.load();
		
		if(entries != null) return entries;
		
		entries = allWordOccurences(files, uniqueGoodWords);
		/*
		entries = new ArrayList<Map.Entry<String, Integer>>();
		
		int i = 1;
		int size = files.size();
		for(String key : files.keySet()) {
			RatingFile rf = files.get(key);
			HashMap<String, Integer> words = rf.words;

			if(i % 100 == 0) System.out.println("[" + i + "\t/" + size + "] "); //Adding words: " + rf.toString());
			
			List<Map.Entry<String, Integer>> newEntries = new ArrayList<Map.Entry<String, Integer>>(words.entrySet()); 
			for(Entry<String, Integer> e : newEntries) {
				int index = entries.indexOf(e.getKey());
				if(index > 0) {
					int sum = e.getValue() + entries.get(index).getValue();
					entries.get(index).setValue(sum);
				} else {
					entries.add(e);
				}
			}
			i++;
		}
		entries = SerializableList.sortList(entries);
		*/
		SerializableList sl = new SerializableList(entries);
		sl.save();
		
		return entries;
	}
	
	private void populateStopwords() {
		ArrayList<String> sw = new ArrayList<>();
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(STOPWORDS);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = null;
        try {
			while ((line = bufferedReader.readLine()) != null) {
			    sw.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        try {
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        this.stopwords = sw;
	}

	private ArrayList<String> getFileNames(String path) {
		ArrayList<String> l = new ArrayList<String>();
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		for(File f : listOfFiles) {
			l.add(f.getName());
		}
		return l;
	}

}
