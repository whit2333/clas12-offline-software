package cnuphys.swim.halfstep;

public class HalfStepTest {
	
	private static double g = 10;  //m/s2

	
	public static void main(String arg[]) {
		System.out.println("Half-Step test");
		
		double t = 0;
		double h = 10;
		
		double xo = 0;
		double vo = 0;
		
		double xexact = xo + vo*h + 0.5*g*h*h;
		double vexact = vo + g*h;
		
		System.out.println("EXACT SOLUTION: x = " + xexact + "  v = " + vexact);
		
		HalfStepTest test = new HalfStepTest();
		GAdvancer gAdvancer = test.new GAdvancer(xo, vo);
		
		int ncompute = Advance.advance(gAdvancer, 0, h, 0.2, 1.0e-2, 1.0e-6);
		
		System.out.println("nCompute = " + ncompute);
		System.out.println("APPROX: x = " + gAdvancer.x[0] + "  v = " + gAdvancer.x[1]);
	}
	
	class GAdvancer extends AdvancingObject {
		
		
		double x[] = new double[2];
		
		public GAdvancer(double xo, double vo) {
			x[0] = xo;
			x[1] = vo;
		}

		@Override
		public void advance(double t, double h) {
			double xn = x[0];
			double vn = x[1];
			
			x[0] = xn + vn*h + 0.5*g*h*h; 
			x[1] = vn + g*h;
		}

		@Override
		public double difference(AdvancingObject aobj) {
			GAdvancer sg = (GAdvancer)aobj;
			double d0 = Math.abs(sg.x[0]-x[0]);
			System.out.println("d0 = " + d0);
			return d0;
		}

		@Override
		public AdvancingObject copy() {
			return new GAdvancer(x[0], x[1]);
		}

		@Override
		public void copyFrom(AdvancingObject source) {
			GAdvancer sg = (GAdvancer)source;
			x[0] = sg.x[0];
			x[1] = sg.x[1];
		}

		@Override
		public void acceptedSubstep(double z) {
			System.out.println("accepted substep z = " + z);
		}

		@Override
		public void copyEndToStart(AdvancingObject source) {
			GAdvancer sg = (GAdvancer)source;
			x[0] = sg.x[0];
			x[1] = sg.x[1];
		}

		@Override
		public void copyEndToEnd(AdvancingObject source) {
			GAdvancer sg = (GAdvancer)source;
			x[0] = sg.x[0];
			x[1] = sg.x[1];
		}

		@Override
		public void copyStartToStart(AdvancingObject source) {
			GAdvancer sg = (GAdvancer)source;
			x[0] = sg.x[0];
			x[1] = sg.x[1];
		}

		
	}

}
