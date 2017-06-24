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
	private HashSet<String> relevantWords;
	private ArrayList<Integer> indices;
	/*
	 * SimpleMatrix X; SimpleMatrix XTX; SimpleMatrix y; SimpleMatrix b;
	 */
	SimpleMatrix X;
	SimpleMatrix XTXInv;
	SimpleMatrix y;
	SimpleMatrix b;

	double SSR = 0;
	double sigmaSquare = 0;
	SimpleMatrix Var;
	SimpleMatrix t;

	public OLSSchaetzer(HashMap<String, RatingFile> f, ArrayList<Map.Entry<String, Integer>> allWords,
			ArrayList<Integer> indices) {
		this.indices = indices;
		this.allWords = allWords;

		relevantWords = new HashSet<String>();
		for (int i : indices) {
			relevantWords.add(allWords.get(i).getKey());
		}

		//System.out.println("Before: " + f.size());
		this.files = removeFilesWithoutWords(f, relevantWords);
		//System.out.println("After: " + this.files.size());
		
		
		
		// List<Map.Entry<String, Integer>> reducedList =
		// allWords.subList(startIndex, endIndex);
		// files = removeUnrepresentedFiled(files, allWords);

		/*
		 * k = indices.size(); n = files.size();
		 * 
		 * X = new SimpleMatrix(n, k + 1); y = new SimpleMatrix(n, 1); // X =
		 * new SimpleMatrix(n, size + 1, Integer.class); // y = new
		 * SimpleMatrix(n, 1);
		 * 
		 * int i = 0;
		 * 
		 * System.out.println("Indices: " + indices); for (String filename :
		 * files.keySet()) { RatingFile rf = files.get(filename); y.set(i, 0,
		 * Double.valueOf(rf.givenRating));
		 * 
		 * for (int j = 0; j <= k; j++) { if (j == 0) { X.set(i, 0, 1); } else {
		 * for (String word : relevantWords) { if (rf.words.containsKey(word)) {
		 * double val = rf.words.get(word); System.out.println("**** " + i + "|"
		 * + j + "\tMEHR ALS 0: " + val); X.set(i, j, val); } else { X.set(i, j,
		 * 0.0); } } /* Entry<String, Integer> e =
		 * allWords.get(indices.get(j-1)); //System.out.println("key: " +
		 * e.getKey()); if(rf.words.get(e.getKey()) != null) { //if
		 * (rf.words.containsKey(e.getKey())) { X.set(i, j,
		 * rf.words.get(e.getKey())); } else { X.set(i, j, 0); }
		 */
		/*
		 * 
		 * } } i++; }
		 * 
		 */
		populateXMatrix(this.files, relevantWords);
		//System.out.println("Matrix: " + X);
		
		while(hasZeroCol(X) != -1) {
			ArrayList<String> relWordList = new ArrayList<String>(relevantWords);
			int col = hasZeroCol(X);
			//System.out.println("Found zero col at " + col);
			//System.out.println("Matrix vorher: " + X);
			String wordToRemove = relWordList.get(col-1);
			//System.out.println("Entferne Wort " + wordToRemove + " aus " + relevantWords);
			relevantWords.remove(wordToRemove);
			//System.err.println("=> " + relevantWords);
			this.files = removeFilesWithoutWords(f, relevantWords);
			populateXMatrix(this.files, relevantWords);
			//System.out.println("Matrix nachher: " + X);
		}
		
		y = new SimpleMatrix(files.size(), 1);
		int row = 0;
		for(String filename : files.keySet()) {
			y.set(row, 0, files.get(filename).givenRating);
		}
		

		calcBeta();
		calcSSR();
		calcSigmaSquare();
		calcVarianz();
		calcTValue();
		// System.out.println(t);

	}
	
	private int hasZeroCol(SimpleMatrix M) {
		for(int col = 1; col < M.numCols(); col++) {
			double colSum = 0.0;
			for(int row = 0; row < M.numRows(); row++) {
				colSum += M.get(row, col);
			}
			if(colSum == 0.0) {
				return col;
			}
		}
		
		return -1;
	}

	private void populateXMatrix(HashMap<String, RatingFile> files, HashSet<String> relevantWords) {

		int n = files.size();
		int k = relevantWords.size() + 1;

		ArrayList<String> words = new ArrayList<>(relevantWords);

		ArrayList<String> filenames = new ArrayList<>();
		for (String key : files.keySet()) {
			filenames.add(key);
		}

		SimpleMatrix newX = new SimpleMatrix(n, k);
		
		int row = 0;
		for(String filename : files.keySet()) {
			HashMap<String, Integer> fileWords = files.get(filename).words;
			for (int col = 0; col < k; col++) {
				if (col == 0) {
					newX.set(row, col, 1.0);
				} else {
					String word = words.get(col - 1);
					//System.out.println("WORD: " + word);
					if (fileWords.get(word) != null) {
						//System.out.println("Word found:" + word);
						newX.set(row, col, fileWords.get(word));
					}
				}
				
			}
			if (newX.extractVector(true, row).elementSum() == 1.0) {
				System.out.println("ERROR: Empty line!");
			}
			
			row++;
		}
		

		X = newX;
	}

	public HashMap<String, RatingFile> removeFilesWithoutWords(HashMap<String, RatingFile> f, HashSet<String> relevantWords) {
		HashMap<String, RatingFile> files = new HashMap<String, RatingFile>();
		
		for(String filename : f.keySet()) {
			int occurences = 0;
			
			for(String relevantWord : relevantWords) {
				if(f.get(filename).words.get(relevantWord) != null) {
					occurences++;
				}
			}
			if(occurences > 0) {
				files.put(filename, f.get(filename));
			}
		}
		
		/*
		String all = relevantWords.toString();
		for (String filename : f.keySet()) {
			HashMap<String, Integer> fileWords = f.get(filename).words;
			// System.out.println("Words in file: " + fileWords.size());
			int occurences = 0;
			// System.out.println(fileWords.keySet());
			for (String word : fileWords.keySet()) {
				if (all.indexOf(word) > 0) {
					occurences++;
				}
			}
			// System.out.println("Occurences: " + occurences);
			if (occurences > 0) {
				files.put(filename, f.get(filename));
			} else {
				// System.out.println("Removed " + filename);
			}
		}
		*/
		// System.out.println("RemoveFilesWithoutWords- vorher: " + f.size() +
		// "\tnachher: " + files.size());
		return files;
	}

	public ArrayList<Integer> getRelevantIndices(double tMin) {
		ArrayList<Integer> relevantIndices = new ArrayList<Integer>();

		for (int i = 1; i < indices.size(); i++) {
			if (t.get(i) > tMin) {
				int wordIndex = indices.get(i - 1);
				relevantIndices.add(wordIndex);
				// System.out.println("Found relevant word i=" + i +" =>
				// wordIndex=" + wordIndex + " => word: " +
				// allWords.get(wordIndex));
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
		repairMatrix(XTX);
		if (XTX.hasUncountable()) {
			System.out.println("ERROR: XTX HAS UNCOUNTABLE");
		}
		double det = XTX.determinant();
		System.out.println("Det(XTX)=" + det);
		if(det == 0.0) {
			System.out.println(X);
			System.out.println(XTX);
		}
		//System.out.println(XTX);
		XTXInv = XTX.invert();
		b = XTXInv.mult(X.transpose()).mult(y);
	}

	private void calcSSR() {
		SimpleMatrix u = y.minus(X.mult(b));
		SSR = u.transpose().mult(u).get(0);
	}

	/**
	 * Replace NaN or infinite values in Matrix with 0.0
	 * 
	 * @param X
	 *            the Matrix to check
	 */
	private void repairMatrix(SimpleMatrix X) {
		for (int row = 0; row < X.numRows(); row++) {
			for (int col = 0; col < X.numCols(); col++) {
				double val = X.get(row, col);
				if (Double.isInfinite(val) || Double.isNaN(val)) {
					X.set(row, col, 0.0);
				}
			}
		}
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
