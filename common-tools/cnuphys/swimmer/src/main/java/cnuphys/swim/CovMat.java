package cnuphys.swim;

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
        this.covMat = m;
    }
    
    public CovMat(CovMat c) {
    	this.k = c.k;
    	this.covMat = c.covMat.copy();
    }
    
    public void copy(CovMat source) {
    	k = source.k;
    	covMat = source.covMat.copy();
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
}
