package ke;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;


public class SerializableList implements Serializable {
	private static final long serialVersionUID = 1L;
	public ArrayList<Map.Entry<String, Integer>> al;

	public SerializableList(ArrayList<Map.Entry<String, Integer>> al) {
		this.al = al;
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<Map.Entry<String, Integer>> load() {
		HashMap<String, Integer> words = null;
		ArrayList<Map.Entry<String, Integer>> l = null;
		try {
			FileInputStream fileIn = new FileInputStream("allWords.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			words = (HashMap<String, Integer>) in.readObject();
			in.close();
			fileIn.close();
			l = new ArrayList<Map.Entry<String, Integer>>(words.entrySet());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return l;
	}

	public void sort() {
		Collections.sort(al, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
				return Integer.compare(b.getValue(), a.getValue());
			}
		});
	}

	public static ArrayList<Map.Entry<String, Integer>> sortList(ArrayList<Entry<String, Integer>> entries) {
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
				return Integer.compare(b.getValue(), a.getValue());
			}
		});

		return entries;
	}
	
	public static HashMap<String, Integer> asHashMap(ArrayList<Map.Entry<String, Integer>> al) {
		HashMap<String, Integer> words = new HashMap<String, Integer>();

		int size = al.size();
		for (int i = 0; i < size; i++) {
			Map.Entry<String, Integer> e = al.get(i);
			words.put(e.getKey(), e.getValue());
		}
		
		return words;
	}

	public void save() {
		HashMap<String, Integer> words = SerializableList.asHashMap(al);

		try {
			FileOutputStream fileOut = new FileOutputStream("allWords.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(words);
			out.close();
			fileOut.close();
			System.out.println("Serialized data is saved in allWords.ser");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void removeUnWords() {
		System.out.println("Size before removing stupid words: " + al.size());
		
		HashSet<String> goodWords = new HashSet<String>();
		try (BufferedReader br = new BufferedReader(new FileReader("wordlist.txt"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       line = line.replace("'", "");
		       goodWords.add(line.toLowerCase());
		    }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<Integer> indicesToRemove = new ArrayList<Integer>();
		for(Entry<String, Integer> e : al) {
			String word = e.getKey();
			//System.out.println(word);
			if(!goodWords.contains(word)) {
				indicesToRemove.add(al.indexOf(e));
			}
		}
		
		Collections.reverse(indicesToRemove);
		for(int i : indicesToRemove) {
			al.remove(i);
		}
		
		System.out.println("Size after removing stupid words: " + al.size());
		
	}
	
	public void saveMatrixCsv(HashMap<String, RatingFile> files) {
		removeUnWords();
		
		StringBuilder sb = new StringBuilder();
		StringBuilder rCommands = new StringBuilder();
		String SEP = ";";
		int MAX = 1000;
		String file = "matrix.csv";
		
		rCommands.append("dat <- read.csv(file=\"" + file + "\", header=TRUE, sep=\";\")\n");
		rCommands.append("formel <- formula(Class ~ ");
		// Build header
		sb.append("Class" + SEP);
		int size = (al.size() > MAX) ? MAX : al.size();
		String[] array = new String[size];
		for(int i = 0; i < size; i++) {
			String col = al.get(i).getKey();
			//System.out.println("Appended " + col + " : " + al.get(i).getValue() + "x");
			sb.append(col + SEP);
			array[i] = col;
		}
		rCommands.append(String.join(" + ", array) + ")\n");

		rCommands.append("output <- lm(formel, data = dat)\n");
		rCommands.append("summary(output)");
		sb.append("\n");
		
		// Write data
		for(String filename : files.keySet()) {
			//System.out.println(files.get(filename).givenRating);
			sb.append(files.get(filename).toCsvLine(SEP, al, MAX));
		}
		
		// Write to file
		try{
		    PrintWriter writer = new PrintWriter(file, "UTF-8");
		    writer.print(sb.toString());
		    writer.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		   // do something
		}
		
		// Write R commands
		try{
		    PrintWriter writer = new PrintWriter("OLS.R", "UTF-8");
		    writer.print(rCommands.toString());
		    writer.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		   // do something
		}
	}

}
