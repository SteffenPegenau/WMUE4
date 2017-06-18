package ke;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ejml.simple.SimpleMatrix;

public class OLSSchaetzer {
	int MAX = 20;
	int size;
	SimpleMatrix X;
	SimpleMatrix XT;
	SimpleMatrix XTX;
	SimpleMatrix XTXInv;
	SimpleMatrix y;
	SimpleMatrix b;
	double SSR = 0;
	int n = 0;
	int k = 0;
	double sigmaSquare = 0;
	SimpleMatrix Var;
	SimpleMatrix t;
	
	public OLSSchaetzer(HashMap<String, RatingFile> files, ArrayList<Map.Entry<String, Integer>> allWords) {
		size = (MAX > allWords.size()) ? allWords.size() : MAX;
		k = size;
		n = files.size();
		X = new SimpleMatrix(n, size);
		y = new SimpleMatrix(n, 1);
		
		int i = 0;
		for(String word : files.keySet()) {
			RatingFile rf = files.get(word);
			y.set(i, 0, Double.valueOf(rf.givenRating));
			
			List<Map.Entry<String, Integer>> reducedList = allWords.subList(0, size);
			for(int j = 0; j < size; j++) {
				Entry<String, Integer> e = reducedList.get(j);
				double v = 0.0;
				if(rf.words.containsKey(e.getKey())) {
					v = Double.valueOf(rf.words.get(e.getKey()));
					
					if(Double.isNaN(v) || Double.isInfinite(v)) {
						v = 0.0;
					}
				}
				X.set(i, j, v);
			}
			i++;
		}
		XT = X.transpose();
		if(XT.hasUncountable()) {
			System.out.println("Hier liegt der Fehler!");
		}
		XTX = XT.mult(X);
		XTX = removeUncountable(XTX);
		XTXInv = XTX.invert();
		
		b = (XTXInv.mult(X.transpose())).mult(y);
		try {
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(X);
		}
		
		SimpleMatrix u = y.minus(X.mult(b));
		SSR = u.transpose().mult(u).get(0);
		sigmaSquare = SSR / (n - k);
		Var = X.transpose().mult(X).invert();
		
		for(int y = 0; y < Var.numRows(); y++) {
			for(int x = 0; x < Var.numCols(); x++) {
				Var.set(y, x, Var.get(y, x) * sigmaSquare);
			}
		}
		
		t = new SimpleMatrix(b.numRows(), 1);
		for(int x = 0; x < b.numRows(); x++) {
			double t = Math.abs(b.get(x, 0)) / Math.sqrt(Var.get(x,x));
		}
		
		System.out.println(t);
	}
	
	public SimpleMatrix removeUncountable(SimpleMatrix m) {
		for(int row = 0; row < m.numRows(); row++) {
			for(int col = 0; col < m.numCols(); col++) {
				Double val = m.get(row, col);
				if(Double.isInfinite(val) || Double.isNaN(val)) {
					m.set(row, col, 0.0);
				}
			}
		}
		return m;
	}
}
