package cnuphys.swim;

import java.util.Map;

import Jama.Matrix;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.swim.halfstep.AAdvancingObject;

public class CovMatAdvancer extends AAdvancingObject {

	private  RotatedCompositeProbe probe;
	private  int sector;
	private  int i;
	private  int f;
	private  StateVec iVec;
	private  CovMat covMat;
	private  double zf;
	private  Map<Integer, StateVec> trackTraj;
	private  Map<Integer, CovMat> trackCov;
	private  double[] A;
	private  double[] dA;

	public StateVec fV;

	public CovMatAdvancer(final RotatedCompositeProbe probe, final int sector, final int i, int f, final StateVec iVec,
			final CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj,
			final Map<Integer, CovMat> trackCov, final double[] A, final double[] dA) {
		this.probe = probe;
		this.sector = sector;
		this.i = i;
		this.f = f;
		this.iVec = iVec;
		this.covMat = covMat;
		this.zf = zf;
		this.A = A;
		this.trackTraj = trackTraj;
		this.trackCov = trackCov;
		this.dA = dA;
	}
	
	public CovMatAdvancer(CovMatAdvancer source) {
		this.copyFrom(source);
	}

	@Override
	public void advance(double z, double h) {
		double zf = z + h;
		
		System.err.println("z = " + z + "   zf = " +  zf + "   h = " + h);
		fV = CovMatTransport.oneStep(probe, sector, i, f, iVec, covMat, zf, trackTraj, trackCov, A, dA);
		System.out.println("fv:\n" + fV);
	}

	@Override
	public double difference(AAdvancingObject aobj) {
		CovMatAdvancer acm = (CovMatAdvancer) aobj;
		Matrix mat = covMat.covMat;
		Matrix amat = acm.covMat.covMat;
		
		Swim.printMatrix("mat",  mat);
		Swim.printMatrix("amat",  amat);

		double dif00 = Math.abs(mat.get(0, 0) - amat.get(0, 0));
		double dif11 = Math.abs(mat.get(1, 1) - amat.get(1, 1));
		
//		double m00 = mat.get(0, 0);
//		double m11 = mat.get(1, 1);
//		double a00 = amat.get(0, 0);
//		double a11 = amat.get(1, 1);
//		
//		System.out.println("m00 = " + m00);
//		
//		double dif00 = fractDiff(m00, a00);
//		double dif11 = fractDiff(m11, a11);

		
		double diffMax = Math.max(dif00, dif11);
		
		System.out.println("DIFF: " + diffMax);
		
		return  diffMax;
	}

	@Override
	public AAdvancingObject copy() {
		return new CovMatAdvancer(this);
	}

	@Override
	public void copyFrom(AAdvancingObject asource) {
		CovMatAdvancer  source = (CovMatAdvancer)asource;
		this.probe = source.probe;
		this.sector = source.sector;
		this.i = source.i;
		this.f = source.f;
		this.iVec = source.iVec;
		this.covMat = new CovMat(source.covMat);
		this.zf = source.zf;
		this.A = source.A;
		this.trackTraj = source.trackTraj;
		this.trackCov = source.trackCov;
		this.dA = source.dA;
		this.fV = source.fV;
	}
	
	private static final double TINY = 1.0e-20;
	public double fractDiff(double v1, double v2) {
		double v1abs = Math.abs(v1);
		double v2abs = Math.abs(v2);
		if (v1abs < TINY) {
			return v2abs;
		}
		else if (v2abs < TINY) {
			return v1abs;
		}

		return Math.abs(v2-v1)/(0.5*(v1abs + v2abs));
		
	}

}