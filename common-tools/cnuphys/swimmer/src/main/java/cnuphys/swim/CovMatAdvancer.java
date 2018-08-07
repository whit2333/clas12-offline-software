package cnuphys.swim;

import java.util.Map;

import Jama.Matrix;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.swim.halfstep.AdvancingObject;

public class CovMatAdvancer extends AdvancingObject {

	private  RotatedCompositeProbe probe;
	private  int sector;
	private  int i;
	private  int f;
	private  StateVec start;
	private  CovMat covMat;
	private  double zf;
	private  Map<Integer, StateVec> trackTraj;
	private  Map<Integer, CovMat> trackCov;
	private  double[] A;
	private  double[] dA;
	
	private StateVec end = new StateVec();

	public CovMatAdvancer(final RotatedCompositeProbe probe, final int sector, final int i, int f, final StateVec start, 
			final CovMat covMat, final double zf, final Map<Integer, StateVec> trackTraj,
			final Map<Integer, CovMat> trackCov, final double[] A, final double[] dA) {
		this.probe = probe;
		this.sector = sector;
		this.i = i;
		this.f = f;
		this.start = new StateVec(start);
		this.covMat = covMat;
		this.zf = zf;
		this.A = A;
		this.trackTraj = trackTraj;
		this.trackCov = trackCov;
		this.dA = dA;
	}
	
	/**
	 * Copy constructor for a covariance matrix
	 * @param source the source to copy
	 */
	public CovMatAdvancer(CovMatAdvancer source) {
		this.copyFrom(source);
	}
	
	@Override
	public void acceptedSubstep(double z) {
//		System.out.println("Accepted z = " + z + " end\n" + end );
	
	}
	
	public StateVec getEnd() {
//		System.out.println("AT END \n" + end);
		return end;
	}


	@Override
	public void advance(double z, double h) {
		double zf = z + h;
		
		System.out.println("start.Z = " + start.z + "  z = " + z + "   zf = " +  zf + "   h = " + h);
		CovMatTransport.oneStep(probe, sector, i, f, start, end, covMat, zf, trackTraj, trackCov, A, dA);
		
		System.out.println(String.format("AFTER ADV START (%-7.3f, %-7.3f,%-7.3f)", start.x, start.y, start.z));
		System.out.println(String.format("AFTER ADV END   (%-7.3f, %-7.3f,%-7.3f)\n", start.x, start.y, start.z));
//		System.out.println("fv:\n" + fV);
	}

	@Override
	public double difference(AdvancingObject aobj) {
		CovMatAdvancer acm = (CovMatAdvancer) aobj;
		Matrix mat = covMat.covMat;
		Matrix amat = acm.covMat.covMat;
		
//		Swim.printMatrix("mat",  mat);
//		Swim.printMatrix("amat",  amat);
		

//		double dif00 = Math.abs(mat.get(0, 0) - amat.get(0, 0));
//		double dif11 = Math.abs(mat.get(1, 1) - amat.get(1, 1));
		
//		double m00 = mat.get(0, 0);
//		double m11 = mat.get(1, 1);
//		double a00 = amat.get(0, 0);
//		double a11 = amat.get(1, 1);
//		
//		System.out.println("m00 = " + m00);
//		
//		double dif00 = fractDiff(m00, a00);
//		double dif11 = fractDiff(m11, a11);

		
//		double diffMax = Math.max(dif00, dif11);
		
		double diffMax = Math.abs(acm.end.x - this.end.x);
		
//		System.out.println("DIFF: " + diffMax);
		
		return  diffMax;
	}

	@Override
	public AdvancingObject copy() {
		return new CovMatAdvancer(this);
	}

	@Override
	public void copyFrom(AdvancingObject asource) {
		CovMatAdvancer  source = (CovMatAdvancer)asource;
		this.probe = source.probe;
		this.sector = source.sector;
		this.i = source.i;
		this.f = source.f;
		this.start = new StateVec(source.start);
		this.end = new StateVec(source.end);
		this.covMat = new CovMat(source.covMat);
		this.zf = source.zf;
		this.A = source.A;
		this.trackTraj = source.trackTraj;
		this.trackCov = source.trackCov;
		this.dA = source.dA;
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

	@Override
	public void copyEndToStart(AdvancingObject asource) {
		CovMatAdvancer  source = (CovMatAdvancer)asource;
		start.copy(source.getEnd());
		covMat.copy(source.covMat);
	}

	@Override
	public void copyEndToEnd(AdvancingObject asource) {
		CovMatAdvancer  source = (CovMatAdvancer)asource;
		end.copy(source.getEnd());
		covMat.copy(source.covMat);
	}

	@Override
	public void copyStartToStart(AdvancingObject asource) {
		CovMatAdvancer  source = (CovMatAdvancer)asource;
		start.copy(source.start);
		covMat.copy(source.covMat);
	}



}