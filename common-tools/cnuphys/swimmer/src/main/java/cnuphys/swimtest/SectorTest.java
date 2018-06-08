package cnuphys.swimtest;

import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.magfield.MagneticFields.FieldType;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.swim.SwimTrajectory;
import cnuphys.swim.Swimmer;

public class SectorTest {

	
	//test the sector swimmer for rotated composite
	public static void testSectorSwim() {

		MagneticFields.getInstance().setActiveField(FieldType.COMPOSITEROTATED);

		double hdata[] = new double[3];
		
		int charge = -1;
		
		double x0 = (-40. + 20*Math.random())/100.;
		double y0 = (10. + 40.*Math.random())/100.;
		double z0 = (180 + 40*Math.random())/100.;
		double pTot = 1.0;
		double theta = 0;
		double phi = 0;
		double z = 511.0/100.;
		double accuracy = 10/1.0e6;
		double stepSize = 0.01;
		
		System.out.println("=======");
		for (int sector = 1; sector <= 6; sector ++) {
			Swimmer swimmer = new Swimmer();
			
			SwimTrajectory traj;
			try {
				traj = swimmer.sectorSwim(sector, charge, x0, y0, z0, pTot,
				            theta, phi, z, accuracy, 10,
				            10, stepSize, Swimmer.CLAS_Tolerance, hdata);
				
				
				FieldProbe probe = swimmer.getProbe();
				if (probe instanceof RotatedCompositeProbe) {
					traj.sectorComputeBDL(sector, (RotatedCompositeProbe)probe);
				} else {
					traj.computeBDL(probe);
				}
	            
	            double lastY[] = traj.lastElement();
				System.out.print("Sector: " + sector + "  ");
				SwimTest.printVect(lastY, " last ");
			} catch (RungeKuttaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			
		}
	}
}
