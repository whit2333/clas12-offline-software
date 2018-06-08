package cnuphys.magfield;

public class SolenoidProbe extends FieldProbe {
	
	private static final double MISALIGNTOL = 1.0e-6; //cm

	
	private Cell2D _cell;
	
	private Solenoid _solenoid;

	//cache the z shift
	private double _shiftZ;
	
	private double _fakeZMax;

	public SolenoidProbe(Solenoid field) {
		super(field);
		_solenoid = (Solenoid)_field;
		_cell = new Cell2D(this);
		_scaleFactor = _solenoid.getScaleFactor();
		_shiftZ = _solenoid.getShiftZ();
		_fakeZMax = _solenoid.getFakeZMax();
		
		q1Coordinate = _solenoid.q1Coordinate.clone();
		q2Coordinate = _solenoid.q2Coordinate.clone();
		q3Coordinate = _solenoid.q3Coordinate.clone();

	}
	
	@Override
	public void field(float x, float y, float z, float result[]) {
		
		if (!contains(x, y, z)) {
			result[0] = 0f;
			result[1] = 0f;
			result[2] = 0f;
			return;
		}

		double rho = FastMath.sqrt(x * x + y * y);
		double phi = FastMath.atan2Deg(y, x);
		fieldCylindrical(phi, rho, z, result);
	}

	
	@Override
	public void fieldCylindrical(double phi, double rho, double z, float[] result) {
		fieldCylindrical(_cell, phi, rho, z, result);
	}
	
	
	/**
	 * Get the field by trilinear interpolation.
	 * 
	 * @param probe
	 *            for faster results
	 * @param phi
	 *            azimuthal angle in degrees.
	 * @param rho
	 *            the cylindrical rho coordinate in cm.
	 * @param z
	 *            coordinate in cm
	 * @param result
	 *            the result
	 * @result a Cartesian vector holding the calculated field in kiloGauss.
	 */
	private void fieldCylindrical(Cell2D cell, double phi, double rho, double z, float result[]) {
		
		if (!containsCylindrical(phi, rho, z)) {
			result[X] = 0f;
			result[Y] = 0f;
			result[Z] = 0f;
			return;
		}
		
		if (isZeroField()) {
			result[X] = 0f;
			result[Y] = 0f;
			result[Z] = 0f;
			return;
		}
		
		//misalignment??
		if (isMisaligned()) {
			z = z - _shiftZ;
		}
		

		if (phi < 0.0) {
			phi += 360.0;
		}

		//this will return
		//result[0] = bphi = 0;
		//result[1] = brho
		//result[2] = bphi

		cell.calculate(rho, z, result);
		// rotate onto to proper phi
		
//		if (phi > 0.001) {
			double rphi = Math.toRadians(phi);
			double cos = Math.cos(rphi);
			double sin = Math.sin(rphi);
			double bphi = result[0];
			double brho = result[1];
			result[X] = (float) (brho * cos - bphi * sin);
			result[Y] = (float) (brho * sin + bphi * cos);
//		}

		result[X] *= _scaleFactor;
		result[Y] *= _scaleFactor;
		result[Z] *= _scaleFactor;
	}
	
    /**
     * Is the physical solenoid represented by the map misaligned?
     * @return <code>true</code> if solenoid is misaligned
     */
	public boolean isMisaligned() {
    	return (Math.abs(_shiftZ) > MISALIGNTOL);
    }

	
	/**
	 * Check whether the field boundaries include the point
	 * 
	 * @param phi
	 *            azimuthal angle in degrees.
	 * @param rho
	 *            the cylindrical rho coordinate in cm.
	 * @param z
	 *            coordinate in cm
	 * @return <code>true</code> if the point is included in the boundary of the
	 *         field
	 * 
	 */
	@Override
	public boolean containsCylindrical(double phi, double rho, double z) {
		
		if (z >= _fakeZMax) {
			return false;
		}
		
		if ((z < getZMin()) || (z > getZMax())) {
			return false;
		}
		if ((rho < getRhoMin()) || (rho > getRhoMax())) {
			return false;
		}
		return true;
	}


}
