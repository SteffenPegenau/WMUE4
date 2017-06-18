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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Classifier {
	String STOPWORDS = "stopwords/english";
	
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
		
		for(String filename : getFileNames(trainingFolder)) {
			trainingFiles.put(filename, new RatingFile(trainingFolder, filename, stopwords));
		}
		
		ArrayList<Map.Entry<String, Integer>> sortedListOfWords = SerializableList.sortList(mergeWords(trainingFiles));
		SerializableList sl = new SerializableList(sortedListOfWords);
		sl.removeUnWords();
		//sl.saveMatrixCsv(trainingFiles);
		
		OLSSchaetzer ols = new OLSSchaetzer(trainingFiles, sl.al);
	}
	
	private ArrayList<Map.Entry<String, Integer>> mergeWords(HashMap<String, RatingFile> files) {
		ArrayList<Map.Entry<String, Integer>> entries = SerializableList.load();
		
		if(entries != null) return entries;
		
		entries = new ArrayList<Map.Entry<String, Integer>>();
		
		int i = 1;
		int size = files.size();
		for(String key : files.keySet()) {
			RatingFile rf = files.get(key);
			HashMap<String, Integer> words = rf.words;

			System.out.println("[" + i + "\t/" + size + "] "); //Adding words: " + rf.toString());
			
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
