package cnuphys.swimS;

import cnuphys.magfield.RotatedCompositeProbe;

public class SectorSwimSDerivative extends SwimSDerivative {
	
	//the sector 1..6. Used for swimming in the tilted sector system
	private int _sector;
		
	/**
	 * Set the sector [1..6]
	 * @param sector the sector [1..6]
	 */
	public void setSector(int sector) {
		_sector = sector;
	}

	/**
	 * Set the parameters of this derivative
	 * @param sector the sector [1..6]
	 * @param charge the integer charge
	 * @param momentum the momentum in GeV/c
	 * @param probe the magnetic field probe (must be a RotatedCompositeProbe)
	 */
	public void set(int sector, int charge, double momentum, RotatedCompositeProbe probe) {
		_sector = sector;
		set(charge, momentum, probe);
	}
	
	/**
	 * Compute the derivatives given the value of s (path length) and the values
	 * of the state vector.
	 * 
	 * @param s
	 *            the value of the independent variable path length (input).
	 * @param u
	 *            the values of the state vector ([x,y,z, px/p, py/p, pz/p]) at
	 *            s (input).
	 * @param du
	 *            will be filled with the values of the derivatives wrt s at s
	 *            (output).
	 */
	@Override
	public void derivative(double s, double[] u, double[] du) {
		double Bx = 0.0;
		double By = 0.0;
		double Bz = 0.0;

		if (_probe != null) {
	
			float b[] = new float[3];

			// get the field
			((RotatedCompositeProbe)_probe).field(_sector, (float) u[0], (float) u[1], (float) u[2], b);
			Bx = b[0];
			By = b[1];
			Bz = b[2];
		}

		du[0] = u[3];
		du[1] = u[4];
		du[2] = u[5];
		du[3] = _alpha * (u[4] * Bz - u[5] * By); // vyBz-vzBy
		du[4] = _alpha * (u[5] * Bx - u[3] * Bz); // vzBx-vxBz
		du[5] = _alpha * (u[3] * By - u[4] * Bx); // vxBy-vyBx
	}
	

}
