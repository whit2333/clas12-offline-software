package cnuphys.ced.geometry;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.geant4.v2.DCGeant4Factory;
import org.jlab.detector.geant4.v2.ECGeant4Factory;
import org.jlab.detector.geant4.v2.FTOFGeant4Factory;
import org.jlab.detector.geant4.v2.PCALGeant4Factory;
import org.jlab.geom.base.ConstantProvider;

import eu.mihosoft.vrl.v3d.Vector3d;

import java.util.Optional;


public class DCGeometry2 {
	
	// the angle in degrees for rotating between tilted and sector CS
	private static final double _angle = 25.0;
	private static final double _sin25 = Math.sin(Math.toRadians(_angle));
	private static final double _cos25 = Math.cos(Math.toRadians(_angle));

	
	//wire positions. Indices are suplerlayer, layer, wire
	private static double _wireLeftX[][][] = new double[6][6][112]; 
	private static double _wireLeftY[][][] = new double[6][6][112]; 
	private static double _wireLeftZ[][][] = new double[6][6][112]; 
	private static double _wireRightX[][][] = new double[6][6][112]; 
	private static double _wireRightY[][][] = new double[6][6][112]; 
	private static double _wireRightZ[][][] = new double[6][6][112]; 
	
	private static double _wireMidX[][][] = new double[6][6][112]; 
	private static double _wireMidY[][][] = new double[6][6][112]; 
	private static double _wireMidZ[][][] = new double[6][6][112]; 


	public static DCGeant4Factory dcDetector;

	/**
	 * Initialize the DC Geometry by loading all the wires
	 */
	public static void initialize(String geomDBVar) {
		ConstantProvider provider = GeometryFactory.getConstants(DetectorType.DC, 11, Optional.ofNullable(geomDBVar).orElse("default"));
		dcDetector = new DCGeant4Factory(provider, DCGeant4Factory.MINISTAGGERON);
		
		//get the wire endpoints
		loadWirePositions();
	}
	
	//load the wire positions
	private static void loadWirePositions() {
		for  (int supl0 = 0; supl0 < 6; supl0++) {
			for  (int lay0 = 0; lay0 < 6; lay0++) {
				for  (int wire0 = 0; wire0 < 112; wire0++) {
					Vector3d v = dcDetector.getWireLeftend(supl0, lay0, wire0);
					tiltedToSector(v);
					
					_wireLeftX[supl0][lay0][wire0] = v.x;
					_wireLeftY[supl0][lay0][wire0] = v.y;
					_wireLeftZ[supl0][lay0][wire0] = v.z;
					
					v = dcDetector.getWireRightend(supl0, lay0, wire0);
					tiltedToSector(v);

					_wireRightX[supl0][lay0][wire0] = v.x;
					_wireRightY[supl0][lay0][wire0] = v.y;
					_wireRightZ[supl0][lay0][wire0] = v.z;
					
					v = dcDetector.getWireMidpoint(supl0, lay0, wire0);
					tiltedToSector(v);

					_wireMidX[supl0][lay0][wire0] = v.x;
					_wireMidY[supl0][lay0][wire0] = v.y;
					_wireMidZ[supl0][lay0][wire0] = v.z;
					

				}
			}
		}
	}
	
	private static void tiltedToSector(Vector3d v) {
		double tx = v.x;
		double tz = v.z;
		
		v.x = tx * _cos25 + tz * _sin25;
		v.z = tz * _cos25 - tx * _sin25;

	}
	
	public static void main(String arg[]) {
		System.out.println("Testing the new DC geometry");
		
		initialize("default");
		
		//wire 000
		System.out.println(String.format(
				" end: (%-7.4f, %-7.4f, %-7.4f) end: (%-7.4f, %-7.4f, %-7.4f) mid: (%-7.4f, %-7.4f, %-7.4f)",
				_wireLeftX[0][0][0], _wireLeftY[0][0][0], _wireLeftZ[0][0][0],
				_wireRightX[0][0][0], _wireRightY[0][0][0], _wireRightZ[0][0][0],
				_wireMidX[0][0][0], _wireMidY[0][0][0], _wireMidZ[0][0][0]));
		
		System.out.println(String.format(
				" end: (%-7.4f, %-7.4f, %-7.4f) end: (%-7.4f, %-7.4f, %-7.4f) mid: (%-7.4f, %-7.4f, %-7.4f)",
				_wireLeftX[0][0][65], _wireLeftY[0][0][65], _wireLeftZ[0][0][65],
				_wireRightX[0][0][65], _wireRightY[0][0][65], _wireRightZ[0][0][65],
				_wireMidX[0][0][65], _wireMidY[0][0][65], _wireMidZ[0][0][65]));

		
		
		System.out.println("Done.");
	}
}
