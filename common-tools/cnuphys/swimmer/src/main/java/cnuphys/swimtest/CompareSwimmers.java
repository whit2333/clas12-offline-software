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
		double stepSizeCM = stepSize*100; //m


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
			pTot[i] = randVal(1., 2., rand);
			theta[i] = randVal(12, 18, rand);
			phi[i] = 0;
			x0[i] = randVal(-2, 2, rand)/100;
			y0[i] = randVal(-2, 2, rand)/100;
			z0[i] = randVal(-2, 2, rand)/100;
//			x0[i] = 0;
//			y0[i] = 0;
//			z0[i] = 0;
			
			//swimZ uses CM
			szV[i] = new SwimZStateVector(x0[i]*100, y0[i]*100, z0[i]*100, pTot[i], theta[i], phi[i]);
		}
		
		//prime the pump
		for (int i = 0; i < 1; i++) {
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
		
		System.err.println("Swimmer 1");
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
		System.err.println("Swimmer 1 time: " + timeString(time, num) + "  max step size = " + hdata[2] + "  numStep: " + (traj1[num-1].size()));
		SwimTest.printVect(traj1[num-1].lastElement(), "Last for swimmer 1");

		
		System.err.println("Swimmer 2");
		time = System.currentTimeMillis();
		for (int i = 0; i < num; i++) {
			try {
				traj2[i] = swimmer2.swim(charge[i], x0[i], y0[i], z0[i], pTot[i], theta[i], phi[i], zTarg, accuracy, 10, stepSize,
						Swimmer.CLAS_Tolerance, hdata);
			} catch (RungeKuttaException e) {
				e.printStackTrace();
			}
		}
		time = System.currentTimeMillis() - time;
		System.err.println("Swimmer 2 time: " + timeString(time, num) + "  max step size = " + hdata[2] + "  numStep: " + (traj2[num-1].size()));
		SwimTest.printVect(traj2[num-1].lastElement(), "Last for swimmer 2");

		int maxDiffIndex = -1;
		double maxDiff = -1;
		
		System.err.println("Computing diffs");
		for (int i = 0; i < num; i++) {
			double diff = locDiff(traj1[i].lastElement(), traj1[i].lastElement());
			if (diff > maxDiff) {
				maxDiff = diff;
				maxDiffIndex = i;
			}
		}
		
		System.err.println("Max difference at index " + maxDiffIndex + " = " + maxDiff);
		
		//SWIMMER 2 NO TRAJ
		System.err.println("\nSwimmer 2 No Traj");
		time = System.currentTimeMillis();
		
		int numStep = 0;
		double finalV[] = new double[6];
		for (int i = 0; i < num; i++) {
			try {
				numStep = swimmer2.swim(charge[i], x0[i], y0[i], z0[i], pTot[i], theta[i], phi[i], zTarg, accuracy, 10, stepSize,
						0.4,
						Swimmer.CLAS_Tolerance, hdata, finalV);
			} catch (RungeKuttaException e) {
				e.printStackTrace();
			}
		}
		time = System.currentTimeMillis() - time;
		System.err.println("Swimmer 2 no traj " + timeString(time, num) + "  max step size = " + hdata[2] + "  numStep: " + numStep);
		SwimTest.printVect(finalV, "Last for swimmer 2 no traj");

		//NOTE SWIMZ USES CM
		RungeKutta.setMaxStepSize(RungeKutta.getMaxStepSize()*100.);
		RungeKutta.setMinStepSize(RungeKutta.getMinStepSize()*100.);
		
		System.err.println("\nSwimZ");
		time = System.currentTimeMillis();
		
		for (int i = 0; i < num; i++) {
			try {
				szTraj[i] = swimZ.adaptiveRK(charge[i],pTot[i], szV[i], zTargCM, stepSizeCM, Swimmer.CLAS_Tolerance, hdata);
			} catch (SwimZException e) {
				e.printStackTrace();
			}
		}
		time = System.currentTimeMillis() - time;
		System.err.println("SwimZ time: " + timeString(time, num) + "  max step size = " + hdata[2]/100. + "  numStep: " + (szTraj[num-1].size()));
		SwimTest.printSwimZ(szTraj[num-1].last(), "Last for swimZ");
		
		System.err.println("\nSwimZ no traj");
		time = System.currentTimeMillis();
		
		SwimZStateVector stopSV = new SwimZStateVector();
		for (int i = 0; i < num; i++) {
			try {
				numStep = swimZ.adaptiveRK(charge[i],pTot[i], szV[i], stopSV, zTargCM, stepSizeCM, Swimmer.CLAS_Tolerance, hdata);
			} catch (SwimZException e) {
				e.printStackTrace();
			}
		}
		time = System.currentTimeMillis() - time;
		System.err.println("SwimZ no traj time: " + timeString(time, num) + "  max step size = " + hdata[2]/100. + "  numStep: " + (szTraj[num-1].size()));
		SwimTest.printSwimZ(stopSV, "Last for swimZ no traj");
		
		
	}
	
	private static String timeString(long time, int num) {
		return String.format("%-7.4f ms ", (1.*time)/num);
	}
	
	private static double randVal(double min, double max, Random rand) {
		return min + (max - min)*rand.nextDouble();
	}

	private static double locDiff(double v1[], double v2[]) {
		double dx = v2[0] - v1[0];
		double dy = v2[1] - v1[1];
		double dz = v2[2] - v1[2];
		return Math.sqrt(dx*dx + dy*dy + dz*dz);
	}
}
