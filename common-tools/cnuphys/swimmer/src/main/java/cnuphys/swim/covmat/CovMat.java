package cnuphys.swim.covmat;

import Jama.Matrix;

/**
 * Covariance matrix
 * @author heddle
 *
 */
public class CovMat {
    
    public int k;
    public Matrix covMat;
    
    public CovMat(int k) {
        this.k = k;
    }
    
    public CovMat(int k, Matrix m) {
        this.k = k;
        this.covMat = m.copy();
    }
    
    public CovMat(CovMat c) {
    	this.k = c.k;
    	this.covMat = c.covMat.copy();
    }
    
    public void copy(CovMat source) {
    	k = source.k;
    	covMat = source.covMat.copy();
    }
    
    public double get(int i, int j) {
    	return covMat.get(i, j);
    }
    
    
    public void set(int i, int j, double val) {
    	covMat.set(i, j, val);
    }
    

    @Override
    public String toString() {
    	StringBuffer sb = new StringBuffer(1024);
 
    	sb.append("k = " + k);
        for (int i = 0; i < 5; i++) {
        	sb.append("\n");
        	for (int j = 0; j < 5; j++) {
        		sb.append(String.format("%-12.8f ", covMat.get(i, j)));
        	}
        }
        
    	sb.append("\n");
    	return sb.toString();
    }
    
    public double diff(CovMat c2) {
    	//use four upper left elements. A guess.
    	Matrix cmat2 = c2.covMat;
    	double diff = 0;
    	
    	diff = Math.max(diff, fractDiff(covMat.get(0, 0), cmat2.get(0, 0)));
       	diff = Math.max(diff, fractDiff(covMat.get(0, 1), cmat2.get(0, 1)));
       	diff = Math.max(diff, fractDiff(covMat.get(1, 0), cmat2.get(1, 0)));
       	diff = Math.max(diff, fractDiff(covMat.get(1, 1), cmat2.get(1, 1)));
           	
    	return diff;
    }
    
    private static final double TINY = 1.0e-20;
    private double fractDiff(double v1, double v2) {
    	double numer = Math.abs(v2-v1);
    	double denom = Math.max(Math.abs(v1), Math.abs(v2));
    	
    	if (denom < TINY) {
    		return 1;
    	}
    	return numer/denom;
    }
}
