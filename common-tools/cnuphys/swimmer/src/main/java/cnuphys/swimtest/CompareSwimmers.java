package cnuphys.swimtest;

import java.util.Random;

import cnuphys.rk4.RungeKutta;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.swim.SwimTrajectory;
import cnuphys.swim.Swimmer;
import cnuphys.swim.Swimmer2;
import cnuphys.swimZ.SwimZ;
import cnuphys.swimZ.SwimZException;
import cnuphys.swimZ.SwimZResult;
import cnuphys.swimZ.SwimZStateVector;

public class CompareSwimmers {

	/**
	 * Compare swimmer  vs swimmer 2
	 */
	public static void swimmerVswimmer2Test(long seed, int num) {
		
		double zTarg = 5.0; //m
		double zTargCM = zTarg*100; //cm
		double accuracy = 1.0e-5; //m
		double stepSize = 0.01; //m
		double stepSizeCM = stepSize*100; //cm


		Random rand = new Random(seed);
		
		int charge[] = new int[num];
		double pTot[] = new double[num];
		double theta[] = new double[num];
		double phi[] = new double[num];
		double x0[] = new double[num];
		double y0[] = new double[num];
		double z0[] = new double[num];
		
		SwimZStateVector szV[] = new SwimZStateVector[num];
		SwimZResult szTraj[] = new SwimZResult[num];
		
		double hdata[] = new double[3];
		
		SwimTrajectory traj1[] = new SwimTrajectory[num];
		SwimTrajectory traj2[] = new SwimTrajectory[num];
		
		Swimmer swimmer1 = new Swimmer();
		Swimmer2 swimmer2 = new Swimmer2();
		SwimZ swimZ = new SwimZ();
		
		for (int i = 0; i < num; i++) {
			charge[i] = ((rand.nextFloat() < 0.5f) ? -1 : 1);
			pTot[i] = SwimTest.randVal(1., 2., rand);
			theta[i] = SwimTest.randVal(15, 25, rand);
			phi[i] = SwimTest.randVal(-10, 10, rand);
			x0[i] = SwimTest.randVal(-2, 2, rand)/100;
			y0[i] = SwimTest.randVal(-2, 2, rand)/100;
			z0[i] = SwimTest.randVal(-2, 2, rand)/100;

			
//			zTarg = 5.75;
//			zTargCM = zTarg*100; //cm
//			charge[i] = -1;
//			pTot[i] = 2.;
//			theta[i] = 15;
//			phi[i] = 0;
//			x0[i] = 0;
//			y0[i] = 0;
//			z0[i] = 0;
			
			//swimZ uses CM
			szV[i] = new SwimZStateVector(x0[i]*100, y0[i]*100, z0[i]*100, pTot[i], theta[i], phi[i]);
		}
		
		//prime the pump
		for (int i = 0; i < num/2; i++) {
			try {
				traj1[i] = swimmer1.swim(charge[i], x0[i], y0[i], z0[i], pTot[i], theta[i], phi[i], zTarg, accuracy, 10, 10, stepSize,
						Swimmer.CLAS_Tolerance, hdata);
				traj2[i] = swimmer2.swim(charge[i], x0[i], y0[i], z0[i], pTot[i], theta[i], phi[i], zTarg, accuracy, 10, stepSize,
						Swimmer.CLAS_Tolerance, hdata);
			} catch (RungeKuttaException e) {
				e.printStackTrace();
			}
		}
		
		long time;
		
		System.out.println("Swimmer 1");
		time = System.currentTimeMillis();
		for (int i = 0; i < num; i++) {
			try {
				traj1[i] = swimmer1.swim(charge[i], x0[i], y0[i], z0[i], pTot[i], theta[i], phi[i], zTarg, accuracy, 10, 10, stepSize,
						Swimmer.CLAS_Tolerance, hdata);
			} catch (RungeKuttaException e) {
				e.printStackTrace();
			}
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Swimmer 1 time: " + timeString(time, num) + "  max step size = " + hdata[2] + "  numStep: " + (traj1[num-1].size()));
 
		SwimTrajectory lastTraj = traj1[num-1];
		SwimTest.printSummary("Last for swimmer 1", lastTraj.size(), pTot[num-1], lastTraj.lastElement(), hdata);
		lastTraj.computeBDL(swimmer1.getProbe());
		System.out.println("**** BDL for swimmer 1 = " + 100*lastTraj.lastElement()[SwimTrajectory.BXDL_IDX] + "  kG cm");
		System.out.println("**** PATHLENGTH for swimmer 1 = " + 100*lastTraj.lastElement()[SwimTrajectory.PATHLEN_IDX] + "  cm");

		
		System.out.println("Swimmer 2");
		time = System.currentTimeMillis();
		for (int i = 0; i < num; i++) {
			try {
				traj2[i] = swimmer2.swim(charge[i], x0[i], y0[i], z0[i], pTot[i], theta[i], phi[i], 
						zTarg, accuracy, 10, stepSize,
						Swimmer.CLAS_Tolerance, hdata);
			} catch (RungeKuttaException e) {
				e.printStackTrace();
			}
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Swimmer 2 time: " + timeString(time, num) + "  max step size = " + hdata[2] + "  numStep: " + (traj2[num-1].size()));
		lastTraj = traj2[num-1];
        SwimTest.printSummary("Last for swimmer 2", lastTraj.size(), pTot[num-1], lastTraj.lastElement(), hdata);
		lastTraj.computeBDL(swimmer2.getProbe());
		System.out.println("**** BDL for swimmer 2 = " + 100*lastTraj.lastElement()[SwimTrajectory.BXDL_IDX] + "  kG cm");
		System.out.println("**** PATHLENGTH for swimmer 2 = " + 100*lastTraj.lastElement()[SwimTrajectory.PATHLEN_IDX] + "  cm");


		int maxDiffIndex = -1;
		double maxDiff = -1;
		
		System.out.println("Computing Swimmer1-Swimmer2 diffs");
		for (int i = 0; i < num; i++) {
			double diff = SwimTest.locDiff(traj1[i].lastElement(), traj2[i].lastElement());
			if (diff > maxDiff) {
				maxDiff = diff;
				maxDiffIndex = i;
			}
		}
		
		System.out.println("Max Swimmer1-Swimmer2 difference at index " + maxDiffIndex + " = " + maxDiff);
		
		//SWIMMER 2 NO TRAJ
		System.out.println("\nSwimmer 2 No Traj");
		time = System.currentTimeMillis();
		
		int numStep = 0;
		double finalV[] = new double[6];
		for (int i = 0; i < num; i++) {
			try {
				numStep = swimmer2.swim(charge[i], x0[i], y0[i], z0[i], pTot[i], 
						theta[i], phi[i], zTarg, accuracy, 10, stepSize,
						RungeKutta.DEFMAXSTEPSIZE,
						Swimmer.CLAS_Tolerance, hdata, finalV);
			} catch (RungeKuttaException e) {
				e.printStackTrace();
			}
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Swimmer 2 no traj time " + timeString(time, num) + "  max step size = " + hdata[2] + "  numStep: " + numStep);
        SwimTest.printSummary("Last for swimmer 2 no traj", numStep, pTot[num-1], finalV, hdata);

		
		System.out.println("\nSwimZ");
		time = System.currentTimeMillis();
		
		for (int i = 0; i < num; i++) {
			try {
				szTraj[i] = swimZ.adaptiveRK(charge[i],pTot[i], szV[i], zTargCM, stepSizeCM, hdata);
			} catch (SwimZException e) {
				e.printStackTrace();
			}
		}
		time = System.currentTimeMillis() - time;
		SwimZResult lastSZR =  szTraj[num-1];
		System.out.println("SwimZ time: " + timeString(time, num) + "  max step size = " + hdata[2]/100. + "  numStep: " + (lastSZR.size()));
        SwimTest.printSummary("Last for swimZ", lastSZR.size(), pTot[num-1], theta[num-1], lastSZR.last(), hdata);
        
       
		System.out.println("**** BDL for swimZ = " + lastSZR.getBDL(swimZ.getProbe()) + "  kG cm");
		System.out.println("**** PATHLENGTH for swimZ = " + lastSZR.getPathLength() + "  cm");


//		SwimTest.printSwimZ(szTraj[num-1].last(), "Last for swimZ");
		
		maxDiffIndex = -1;
		maxDiff = -1;
		
		System.out.println("Computing Swimmer1-SwimZ diffs");
		for (int i = 0; i < num; i++) {
			double diff = SwimTest.locDiff(traj1[i].lastElement(), szTraj[i].last());
			if (diff > maxDiff) {
				maxDiff = diff;
				maxDiffIndex = i;
			}
		}
		
		System.out.println("Max Swimmer1-SwimZ difference at index " + maxDiffIndex + " = " + maxDiff);		
		
		System.out.println("\nSwimZ no traj");
		time = System.currentTimeMillis();
		
		SwimZStateVector stopSV = new SwimZStateVector();
		for (int i = 0; i < num; i++) {
			try {
				numStep = swimZ.adaptiveRK(charge[i],pTot[i], szV[i], stopSV, zTargCM, stepSizeCM, hdata);
			} catch (SwimZException e) {
				e.printStackTrace();
			}
		}
		time = System.currentTimeMillis() - time;
		System.out.println("SwimZ no traj time: " + timeString(time, num) + "  max step size = " + hdata[2]/100. + "  numStep: " + (szTraj[num-1].size()));
        SwimTest.printSummary("Last for swimZ no traj", numStep, pTot[num-1], theta[num-1], stopSV, hdata);
//		SwimTest.printSwimZ(stopSV, "Last for swimZ no traj");
		
		
	}
	
	private static String timeString(long time, int num) {
		return String.format("%-7.4f ms ", (1.*time)/num);
	}

}
