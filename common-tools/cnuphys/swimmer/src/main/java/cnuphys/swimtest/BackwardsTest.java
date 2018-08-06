package cnuphys.swimtest;

import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.MagneticFields.FieldType;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.swim.StateVec;
import cnuphys.swim.SwimException;
import cnuphys.swim.SwimTrajectory;
import cnuphys.swim.Swimmer;
import cnuphys.swim.Swimming;
import cnuphys.swim.Trajectory;
import cnuphys.swimS.SwimS;
import cnuphys.swimZ.SwimZ;

public class BackwardsTest {

	public static void backwardsTest() {
		
		System.out.println("BACKWARDS TEST");
		
		
		MagneticFields.getInstance().setActiveField(FieldType.TORUS);
		
		//cm
		double x0 = 0;
		double y0 = 20;
		double z0 = 275;
		double p = 0.8;
		double accuracy = .01;
		double theta = 30;
		double phi = 10;
		int q = -1;
		double zf = 450;
		double[] hdata = new double[3];
		double stepSize = 0.01;
		double zSmaller = 360; //cm
		
		System.out.println(String.format("Charge: %d   Momentum: %-6.3f   Zf: %-6.3f cm", q, p, zf));
		System.out.println(String.format("Vertex = (%-6.3f, %-6.3f, %-6.3f) cm   theta: %-6.3f   phi: %-6.3f"
				, x0, y0, z0, theta, phi));
		
		
		System.out.println(" TRADITIONAL SWIMMER FORWARD");
		Swimmer swimmer1 = new Swimmer();
		SwimZ swimZ = new SwimZ();
		SwimS swimS = new SwimS();
		
		try {
			SwimTrajectory traj = swimmer1.swim(q, x0/100, y0/100, z0/100, p, theta, phi, zf/100, accuracy/100, 10, 10, stepSize/100,
					Swimmer.CLAS_Tolerance, hdata);
			
	        SwimTest.printSummary("Last for traditional swimmer forward", traj.size(), p, traj.lastElement(), hdata);
			traj.computeBDL(swimmer1.getProbe());
			
			double last[] = traj.lastElement();
			System.out.println("**** BDL for trad swimmer forward = " + 100*last[SwimTrajectory.BXDL_IDX] + "  kG cm");
			System.out.println("**** PATHLENGTH trad swimmer forward = " + 100*last[SwimTrajectory.PATHLEN_IDX] + "  cm");
			
	//		Swimming.addMCTrajectory(traj);
			
			System.out.println("\n TRADITIONAL SWIMMER RETRACE STEPS BACKWARD");
			
			/*
			 * WITH TRADITIONAL SWIMMER, TO MAKE IT RETRACE ITS STEPS BACKWARDS YOU MUST
			 * USE THETA = PI-THETA_FINAL and PHI = PI + PHIF, AND YOU MUST CHANGE THE SIGN OF Q
			 * OR IT WILL BEND THE WRONG WAY
			 */
			
			double xB = last[0]*100; //cm
			double yB = last[1]*100; //cm
			double zB = last[2]*100; //cm
			double phiF = Math.toDegrees(Math.atan2(last[4], last[3]));
			double thetaF = Math.toDegrees(Math.acos(last[5]));
			double phiB = 180 + phiF;
			double thetaB = 180-thetaF;
			
			System.out.println(String.format("thetaF: %-9.6f  phiF: %-9.6f  thetaB: %-9.6f  phiB: %-9.6f", thetaF, phiF, thetaB, phiB));
			traj = swimmer1.swim(-q, xB/100, yB/100, zB/100, p, thetaB, phiB, z0/100, accuracy/100, 10, 10, stepSize/100,
					Swimmer.CLAS_Tolerance, hdata);
			
	        SwimTest.printSummary("Last for traditional swimmer backward (retrace)", traj.size(), p, traj.lastElement(), hdata);
			traj.computeBDL(swimmer1.getProbe());
			
			last = traj.lastElement();
			System.out.println("**** BDL for trad swimmer backward (retrace) = " + 100*last[SwimTrajectory.BXDL_IDX] + "  kG cm");
			System.out.println("**** PATHLENGTH trad swimmer backward (retrace) = " + 100*last[SwimTrajectory.PATHLEN_IDX] + "  cm");
			
//			Swimming.addMCTrajectory(traj);
			
			System.out.println("\n TRADITIONAL SWIMMER BIG Z TO SMALL Z");
			
			traj = swimmer1.swim(q, xB/100, yB/100, zB/100, p, thetaB, phiB, zSmaller/100, accuracy/100, 10, 10, stepSize/100,
					Swimmer.CLAS_Tolerance, hdata);
			
	        SwimTest.printSummary("Last for traditional swimmer big Z to small Z", traj.size(), p, traj.lastElement(), hdata);
			traj.computeBDL(swimmer1.getProbe());
			
			last = traj.lastElement();
			System.out.println("**** BDL for trad swimmer backward (big Z to small Z) = " + 100*last[SwimTrajectory.BXDL_IDX] + "  kG cm");
			System.out.println("**** PATHLENGTH trad swimmer backward (big Z to small Z) = " + 100*last[SwimTrajectory.PATHLEN_IDX] + "  cm");
			
	//		Swimming.addMCTrajectory(traj);


		} catch (RungeKuttaException e) {
			e.printStackTrace();
		};
		
		System.out.println("\n SWIMZ FORWARD");

		StateVec start = new StateVec(x0, y0, z0, q/p, theta, phi);
		try {
			Trajectory szr = swimZ.adaptiveRK(start, zf, stepSize, hdata);
			
	        SwimTest.printSummary("Swim Z Forwards", szr.size(), p, szr.last(), hdata);
	        
		       
			System.out.println("**** BDL for swimZ Forwards = " + szr.getBDL(swimZ.getProbe()) + "  kG cm");
			System.out.println("**** PATHLENGTH for swimZ Forwards = " + szr.getPathLength() + "  cm");
			
			Swimming.addMCTrajectory(szr.toSwimTrajectory());
			
			
			System.out.println("\n SWIMZ RETRACE STEPS BACKWARD");
			
			/*
			 * WITH SWIMZ, TO MAKE IT RETRACE ITS STEPS BACKWARDS CHANGE THE PZ SIGN
			 * AND THE Q SIGN
			 */
			
			StateVec revZ = szr.last();
			StateVec rev2Z = new StateVec(revZ);
			
			StateVec revS = new StateVec(revZ);
			StateVec rev2S = new StateVec(revS);

			
			revZ.pzSign = -revZ.pzSign;
			revZ.Q = -revZ.Q;
			
			szr = swimZ.adaptiveRK(revZ, z0, stepSize, hdata);
	        SwimTest.printSummary("Swim Z Retrace", szr.size(), p, szr.last(), hdata);
	        
		       
			System.out.println("**** BDL for swimZ  (retrace) = " + szr.getBDL(swimZ.getProbe()) + "  kG cm");
			System.out.println("**** PATHLENGTH for swimZ Backwards (retrace) = " + szr.getPathLength() + "  cm");
			Swimming.addMCTrajectory(szr.toSwimTrajectory());

			
			System.out.println("\n SWIMZ SWIMMER BIG Z TO SMALL Z");
			rev2Z.pzSign = (zSmaller > rev2Z.z) ? 1 : -1;
			
			szr = swimZ.adaptiveRK(rev2Z, zSmaller, stepSize, hdata);
	        SwimTest.printSummary("Swim Z (big Z to small Z)", szr.size(), p, szr.last(), hdata);
	        
		       
			System.out.println("**** BDL for swimZ  (big Z to small Z) = " + szr.getBDL(swimZ.getProbe()) + "  kG cm");
			System.out.println("**** PATHLENGTH for swimZ Backwards (big Z to small Z) = " + szr.getPathLength() + "  cm");
			Swimming.addMCTrajectory(szr.toSwimTrajectory());


			
			szr = swimS.adaptiveRK(start, zf, stepSize, hdata);
			
	        SwimTest.printSummary("\nSwim S Forwards", szr.size(), p, szr.last(), hdata);
	        
		       
			System.out.println("**** BDL for swimS Forwards = " + szr.getBDL(swimS.getProbe()) + "  kG cm");
			System.out.println("**** PATHLENGTH for swimS Forwards = " + szr.getPathLength() + "  cm");
			
			Swimming.addMCTrajectory(szr.toSwimTrajectory());

			
			revS.pzSign = -revS.pzSign;
			revS.Q = -revS.Q;
			
			szr = swimS.adaptiveRK(revS, z0, stepSize, hdata);
	        SwimTest.printSummary("\nSwim S Retrace", szr.size(), p, szr.last(), hdata);
	        
		       
			System.out.println("**** BDL for swimS  (retrace) = " + szr.getBDL(swimS.getProbe()) + "  kG cm");
			System.out.println("**** PATHLENGTH for swimS Backwards (retrace) = " + szr.getPathLength() + "  cm");
			Swimming.addMCTrajectory(szr.toSwimTrajectory());

			
			System.out.println("\n SWIMS SWIMMER BIG Z TO SMALL Z");
			rev2S.pzSign = (zSmaller > rev2Z.z) ? 1 : -1;
			
			szr = swimS.adaptiveRK(rev2S, zSmaller, stepSize, hdata);
	        SwimTest.printSummary("Swim S (big Z to small Z)", szr.size(), p, szr.last(), hdata);
	        
		       
			System.out.println("**** BDL for swimS  (big Z to small Z) = " + szr.getBDL(swimS.getProbe()) + "  kG cm");
			System.out.println("**** PATHLENGTH for swimS Backwards (big Z to small Z) = " + szr.getPathLength() + "  cm");
			Swimming.addMCTrajectory(szr.toSwimTrajectory());


		} catch (SwimException e) {
			e.printStackTrace();
		}
		


		
	}
}
