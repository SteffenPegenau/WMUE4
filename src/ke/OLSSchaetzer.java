package ke;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;

public class OLSSchaetzer {
	int n;
	int k;
	private HashMap<String, RatingFile> files;
	private ArrayList<Map.Entry<String, Integer>> allWords;
	private ArrayList<Integer> indices;
	/*
	SimpleMatrix X;
	SimpleMatrix XTX;
	SimpleMatrix y;
	SimpleMatrix b;
	*/
	SimpleMatrix X;
	SimpleMatrix XTXInv;
	SimpleMatrix y;
	SimpleMatrix b;

	double SSR = 0;
	double sigmaSquare = 0;
	SimpleMatrix Var;
	SimpleMatrix t;

	public OLSSchaetzer(HashMap<String, RatingFile> f, ArrayList<Map.Entry<String, Integer>> allWords, ArrayList<Integer> indices) {
		this.indices = indices;
		this.allWords = allWords;
		this.files = removeFilesWithoutWords(f);
		//List<Map.Entry<String, Integer>> reducedList = allWords.subList(startIndex, endIndex);
		//files = removeUnrepresentedFiled(files, allWords);

		k = indices.size();
		n = files.size();

		X = new SimpleMatrix(n, k + 1);
		y = new SimpleMatrix(n, 1);
		//X = new SimpleMatrix(n, size + 1, Integer.class);
		//y = new SimpleMatrix(n, 1);

		int i = 0;
		for (String word : files.keySet()) {
			RatingFile rf = files.get(word);
			y.set(i, 0, Double.valueOf(rf.givenRating));

			for (int j = 0; j <= k; j++) {
				if (j == 0) {
					X.set(i, 0, 1);
				} else {
					Entry<String, Integer> e = allWords.get(indices.get(j-1));
					if (rf.words.containsKey(e.getKey())) {
						X.set(i, j, rf.words.get(e.getKey()));
					} else {
						X.set(i, j, 0);
					}
				}
			}
			i++;
		}
		
		System.out.println("Matrix: " + X);
		
		calcBeta();
		calcSSR();
		calcSigmaSquare();
		calcVarianz();
		calcTValue();
		//System.out.println(t);
	}
	
	
	public HashMap<String, RatingFile> removeFilesWithoutWords(HashMap<String, RatingFile> f) {
		HashMap<String, RatingFile> files = new HashMap<String, RatingFile>();
		HashSet<String> words = new HashSet<String>();
		for(int i = 0; i < indices.size(); i++) {
			words.add(allWords.get(indices.get(i)).getKey());
		}
		
//		System.out.println(files.keySet());
//		System.out.println(f.keySet());
		
		for(String filename : f.keySet()) {
			HashMap<String, Integer> fileWords = f.get(filename).words;
			int occurences = 0;
			for(String word : fileWords.keySet()) {
				occurences += (words.contains(word)) ? 1 : 0;
				if(occurences > 0) {
					files.put(filename, f.get(filename));
					break;
				}
			}
		}
		return files;
	}
	
	public ArrayList<Integer> getRelevantIndices(double tMin) {
		ArrayList<Integer> relevantIndices = new ArrayList<Integer>();
		
		for(int i = 1; i < indices.size(); i++) {
			if(t.get(i) > tMin) {
				int wordIndex = indices.get(i-1);
				relevantIndices.add(wordIndex);
				//System.out.println("Found relevant word i=" + i +" => wordIndex=" + wordIndex + " => word: " + allWords.get(wordIndex));
			}
		}
		
		return relevantIndices;
	}
	
	private void calcTValue() {
		t = new SimpleMatrix(b.numRows(), 1);
		for (int x = 0; x < b.numRows(); x++) {
			double val = Math.abs(b.get(x, 0)) / Math.sqrt(Var.get(x, x));
			t.set(x, 0, val);
		}
	}
	
	private void calcVarianz() {
		Var = X.transpose().mult(X).invert();
		for (int y = 0; y < Var.numRows(); y++) {
			for (int x = 0; x < Var.numCols(); x++) {
				Var.set(y, x, Var.get(y, x) * sigmaSquare);
			}
		}
	}
	
	private void calcSigmaSquare() {
		sigmaSquare = SSR / (n - k);
	}
	
	private void calcBeta() {
		SimpleMatrix XTX = X.transpose().mult(X);
		if(XTX.hasUncountable()) {
			System.out.println("ERROR: XTX HAS UNCOUNTABLE");
		}
		XTXInv = XTX.invert();
		b = XTXInv.mult(X.transpose()).mult(y);
	}
	
	private void calcSSR() {
		SimpleMatrix u = y.minus(X.mult(b));
		SSR = u.transpose().mult(u).get(0);
	}
	
	private static HashMap<String, RatingFile> removeUnrepresentedFiled(HashMap<String, RatingFile> files,
			List<Map.Entry<String, Integer>> allWords) {
		double sizeBefore = files.size();

		HashMap<String, Integer> words;
		ArrayList<String> filesToRemove = new ArrayList<>();
		for (String filename : files.keySet()) {
			words = files.get(filename).words;
			int foundWords = 0;
			for (Entry<String, Integer> e : allWords) {
				foundWords += (words.containsKey(e.getKey())) ? 1 : 0;
			}
			if (foundWords == 0)
				filesToRemove.add(filename);
		}

		for (String f : filesToRemove) {
			files.remove(f);
		}

		double sizeAfter = files.size();
		double percentDiff = 1.0 - (sizeAfter / sizeBefore);
		System.out.println("Vorher: " + sizeBefore + "\tNachher: " + sizeAfter + "\t-" + percentDiff + "%");
		return files;
	}

}
