package cnuphys.swimS;

import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.MagneticFields.FieldType;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.swim.StateVec;
import cnuphys.swim.SwimException;
import cnuphys.swim.SwimTrajectory;
import cnuphys.swim.Swimmer;
import cnuphys.swim.Trajectory;
import cnuphys.swimZ.SwimZ;
import cnuphys.swimtest.SwimTest;

public class SwimSTest {

	
	
	public static void main(String arg[]) {
		MagneticFields.getInstance().initializeMagneticFields();
		MagneticFields.getInstance().setActiveField(FieldType.TORUS);

		int q = -1;
		double p = 2.;
		double xo= 10;
		double yo= 10;
		double zo= 20;
		double zTarg = 575;
		double theta = 15;
		double phi = 10;
		double pz = p*Math.cos(Math.toRadians(theta));
		double pperp = p*Math.sin(Math.toRadians(theta));
		double px = pperp*Math.cos(Math.toRadians(phi));
		double py = pperp*Math.sin(Math.toRadians(phi));
		
		double stepSize = 0.01;
		double hdata[] = new double[3];
		
		StateVec start = new StateVec();

		double u0[] = {xo, yo, zo, px/p, py/p, pz/p};
		SwimS.uToStateVec(q, p, start, u0);
		
		System.out.println("start:" + start);
		
		SwimZ swimZ = new SwimZ();
		SwimS swimS = new SwimS();
		Swimmer swimmer = new Swimmer();
		
		
		try {
			Trajectory traj = swimZ.adaptiveRK(start, zTarg, stepSize, hdata);
	        SwimTest.printSummary("\nLast for swimZ", traj.size(), p, traj.last(), hdata);
			System.out.println("**** BDL for swimZ  = " + traj.getBDL(swimZ.getProbe()) + "  kG cm");
			System.out.println("**** PATHLENGTH for swimZ = " + traj.getPathLength() + "  cm");

			
			traj = swimS.adaptiveRK(start, zTarg, stepSize, hdata);
	        SwimTest.printSummary("\nLast for swimS", traj.size(), p, traj.last(), hdata);
			System.out.println("**** BDL for swimS  = " + traj.getBDL(swimZ.getProbe()) + "  kG cm");
			System.out.println("**** PATHLENGTH for swimS = " + traj.getPathLength() + "  cm");
			

		} catch (SwimException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
			SwimTrajectory swimTraj = swimmer.swim(q, xo/100, yo/100, zo/100, p, theta, phi, zTarg/100, 1.0e-06, 10, 10, stepSize/100,
					Swimmer.CLAS_Tolerance, hdata);
			
	        SwimTest.printSummary("\nEnd of Traj for old swimmer", swimTraj.size(), p, swimTraj.lastElement(), hdata);
	        swimTraj.computeBDL(swimmer.getProbe());
			System.out.println("**** BDL for old swimmer = " + 100*swimTraj.lastElement()[SwimTrajectory.BXDL_IDX] + "  kG cm");
			System.out.println("**** PATHLENGTH for old swimmer = " + 100*swimTraj.lastElement()[SwimTrajectory.PATHLEN_IDX] + "  cm");

		} catch (RungeKuttaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
