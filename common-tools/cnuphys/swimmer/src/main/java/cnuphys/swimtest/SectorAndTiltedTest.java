package cnuphys.swimtest;

import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.magfield.MagneticFields.FieldType;
import cnuphys.swim.SwimTrajectory;
import cnuphys.swim.Swimmer;
import cnuphys.swimZ.SwimZ;

public class SectorAndTiltedTest {

	public static void sectorAndTiltedTest() {
		
		MagneticFields.getInstance().setActiveField(FieldType.COMPOSITE);
		MagneticFields.getInstance().getSolenoid().setScaleFactor(0.0);
		MagneticFields.getInstance().getTorus().setScaleFactor(-0.5);
		
		double xo =33.8117310; //cm
		double yo = 3.6370810; //cm
		double zo = 255.6238270; //cm
		double p = 1.2038595; //GeV
		double theta = 4.486124024665245;
		double phi =  3.907454370839187;
		double zTarg = 367.1830617; //cm
		int q = 1;

		Swimmer swimmer1 = new Swimmer();

		SwimZ swimZ = new SwimZ();
		double accuracy = 1.0e-5; //m
		double stepSize = 0.01; //m
		double stepSizeCM = stepSize*100; //cm
		
		double[] hdata = new double[3];

		SwimTrajectory traj;
		try {
			traj = swimmer1.swim(q, xo/100, yo/100, zo/100, p, theta, phi, zTarg/100, accuracy, 10, stepSize,
					Swimmer.CLAS_Tolerance, hdata);
			SwimTest.printSummary("Last for swimmer 1", traj.size(), p, traj.lastElement(), hdata);
			traj.computeBDL(swimmer1.getProbe());
			System.out.println("**** BDL for swimmer 1 = " + 100*traj.lastElement()[SwimTrajectory.BXDL_IDX] + "  kG cm");
			System.out.println("**** PATHLENGTH for swimmer 1 = " + 100*traj.lastElement()[SwimTrajectory.PATHLEN_IDX] + "  cm");
		} catch (RungeKuttaException e) {
			e.printStackTrace();
		}
		

	}
}
