package cnuphys.fastMCed.eventgen.sweep;

public class Odometer {
	
	//limits
	private int _numX;
	private int _numY;
	private int _numZ;
	private int _numP;
	private int _numTheta;
	private int _numPhi;
	
	private long total;
	
	
	public int x;
	public int y;
	public int z;
	public int p;
	public int theta;
	public int phi;
	
	public Odometer(int numX, int numY, int numZ, int numP, int numTheta, int numPhi) {
		_numX = numX;
		_numY = numY;
		_numZ = numZ;
		_numP = numP;
		_numTheta = numTheta;
		_numPhi = numPhi;
	}
	
	public void increment() {
		phi++;
		if (phi == _numPhi) {
			phi = 0;
			theta++;
			if (theta == _numTheta) {
				theta = 0;
				p++;
				if (p == _numP) {
					p = 0;
					z++;
					if (z == _numZ) {
						z = 0;
						y++;
						if (y == _numY) {
							y = 0;
							x++;
							if (x == _numX) {
								x = 0;
								System.err.println("Warning: Odometer rolled over");
							}
						}
					}
				}
			}
		}
	}

}
