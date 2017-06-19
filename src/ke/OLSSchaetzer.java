package ke;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.misc.TransposeAlgs_CDRM;
import org.ejml.simple.SimpleMatrix;

public class OLSSchaetzer {
	int MAX = 5000;
	int size;
	DMatrixRMaj X;
	DMatrixRMaj y;
	DMatrixRMaj b;

	double SSR = 0;
	int n = 0;
	int k = 0;
	double sigmaSquare = 0;
	SimpleMatrix Var;
	SimpleMatrix t;

	public OLSSchaetzer(HashMap<String, RatingFile> files, ArrayList<Map.Entry<String, Integer>> allWords) {
		size = (MAX > allWords.size()) ? allWords.size() : MAX;
		List<Map.Entry<String, Integer>> reducedList = allWords.subList(0, size);
		files = removeUnrepresentedFiled(files, reducedList);

		k = size;
		n = files.size();

		X = new DMatrixRMaj(n, size + 1);
		//X = new SimpleMatrix(n, size + 1, Integer.class);
		y = new DMatrixRMaj(n, 1);

		int i = 0;
		for (String word : files.keySet()) {
			RatingFile rf = files.get(word);
			y.set(i, 0, Double.valueOf(rf.givenRating));

			for (int j = 0; j <= size; j++) {
				if (j == 0) {
					X.set(i, 0, 1);
				} else {
					Entry<String, Integer> e = reducedList.get(j - 1);
					if (rf.words.containsKey(e.getKey())) {
						X.set(i, j, rf.words.get(e.getKey()));
					} else {
						X.set(i, j, 0);
					}
				}
			}
			i++;
		}
		DMatrixRMaj beta = getBeta(X, y);
		System.out.println(beta);
		
		/*
		 * System.out.println("Speichere X..."); try {
		 * X.saveToFileCSV("debug_matrix.csv"); } catch (IOException e1) {
		 * e1.printStackTrace(); }
		 
		System.out.println("Errechne b...");
		SimpleMatrix XT = X.transpose();
		System.out.println("Errechne XTX...");
		SimpleMatrix XTX = XT.mult(X);
		System.out.println("det(XTX)=" + XTX.determinant());
		*/
		/*
		b = (XTX.invert().mult(X.transpose())).mult(y);
		SimpleMatrix u = y.minus(X.mult(b));
		SSR = u.transpose().mult(u).get(0);
		System.out.println("SSR=" + SSR);
		sigmaSquare = SSR / (n - k);
		System.out.println("Errechne Var...");
		Var = X.transpose().mult(X).invert();
		for (int y = 0; y < Var.numRows(); y++) {
			for (int x = 0; x < Var.numCols(); x++) {
				Var.set(y, x, Var.get(y, x) * sigmaSquare);
			}
		}
		System.out.println("Errechne t...");
		t = new SimpleMatrix(b.numRows(), 1);
		for (int x = 0; x < b.numRows(); x++) {
			t.set(x, 1, Math.abs(b.get(x, 0)) / Math.sqrt(Var.get(x, x)));
		}

		System.out.println(t);
		*/
	}
	
	public DMatrixRMaj getBeta(DMatrixRMaj X, DMatrixRMaj y) {
		DMatrixRMaj XT = new DMatrixRMaj(X);
		CommonOps_DDRM.transpose(XT);
		DMatrixRMaj XTX = null;
		CommonOps_DDRM.multTransA(1.0, X, X, XTX);
		return XTX;
		
		
		/*
		DMatrixRMaj XTX = null;
		CommonOps_DDRM.mult(X, XT, XTX);
		DMatrixRMaj beta = XTX;
		return beta;
		*/
	}

	public static HashMap<String, RatingFile> removeUnrepresentedFiled(HashMap<String, RatingFile> files,
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

	public SimpleMatrix removeUncountable(SimpleMatrix m) {
		for (int row = 0; row < m.numRows(); row++) {
			for (int col = 0; col < m.numCols(); col++) {
				Double val = m.get(row, col);
				if (Double.isInfinite(val) || Double.isNaN(val)) {
					m.set(row, col, 0.0);
				}
			}
		}
		return m;
	}
}
