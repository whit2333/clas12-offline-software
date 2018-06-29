package org.jlab.rec.fvt.track.fit;

import java.util.List;
import org.jlab.rec.fvt.track.fit.StateVecs.StateVec;

import org.apache.commons.math3.special.Erf;

public class MeasVecs {

    public List<MeasVec> measurements ;

    public class MeasVec implements Comparable<MeasVec> {

        public double z = Double.NaN; 
        public double centroid; 
        public double seed; 
        public double error;
        public int layer;
        public int k;
        public int size;

        MeasVec() {
        }

        @Override
        public int compareTo(MeasVec arg) {
            int CompLay = this.layer < arg.layer ? -1 : this.layer == arg.layer ? 0 : 1;
            return CompLay;
        }

    }
    
    public MeasVec setMeasVec(int l, double cent, int seed, int size) {
                
        MeasVec meas    = new MeasVec();
        double err      = (double) Constants.FVT_Pitch/Math.sqrt(12.); 
        meas.error      = err*err*size;
        meas.layer      = l+1;
        if(l>-1)
            meas.z      = Constants.FVT_Zlayer[l]+Constants.hDrift/2;

        meas.centroid   = cent;
        meas.seed   = seed;
        return meas;
        
    }
    
    
    public double h(StateVec stateVec) {
        if (stateVec == null) {
            return 0;
        }
        if (this.measurements.get(stateVec.k) == null) {
            return 0;
        }
        
        int layer = this.measurements.get(stateVec.k).layer;
        
        //return this.getCentroidEstimate(layer, stateVec.x, stateVec.y);
        //return (double) this.getClosestStrip(stateVec.x, stateVec.y, layer);
        return stateVec.y*Math.cos(Constants.FVT_Alpha[layer-1])-stateVec.x*Math.sin(Constants.FVT_Alpha[layer-1]);
       //return stateVec.transportTroughDriftGap(0.99, this);
    }

    public int getClosestStrip(double x, double y, int layer) {
        int closestStrip = 0;
        if(Math.sqrt(x*x+y*y)<Constants.FVT_Rmax && Math.sqrt(x*x+y*y)>Constants.FVT_Beamhole) {
	
            double x_loc =  x*Math.cos(Constants.FVT_Alpha[layer-1])+ y*Math.sin(Constants.FVT_Alpha[layer-1]);
            double y_loc =  y*Math.cos(Constants.FVT_Alpha[layer-1])- x*Math.sin(Constants.FVT_Alpha[layer-1]);

            if(y_loc>-(Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.) && y_loc < (Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.)){ 
              if (x_loc<=0) closestStrip = (int) (Math.floor(((Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.)-y_loc)/Constants.FVT_Pitch) + 1 );
              if (x_loc>0) closestStrip =  (int) ((Math.floor((y_loc+(Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.))/Constants.FVT_Pitch) + 1 ) + Constants.FVT_Halfstrips +0.5*( Constants.FVT_Nstrips-2.*Constants.FVT_Halfstrips));
            }
            else if(y_loc <= -(Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.) && y_loc > -Constants.FVT_Rmax){ 
              closestStrip =  (int) (Math.floor(((Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.)-y_loc)/Constants.FVT_Pitch) +1 ); 
            }
            else if(y_loc >= (Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.) && y_loc < Constants.FVT_Rmax){ 
              closestStrip = (int) (Math.floor((y_loc+(Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.))/Constants.FVT_Pitch) + 1 + Constants.FVT_Halfstrips+0.5*( Constants.FVT_Nstrips-2.*Constants.FVT_Halfstrips));  
            }
        } 
        return closestStrip;
    }
    
    public double getWeightEstimate(int strip, int layer, double x, double y) {
        double sigmaDrift = 0.01;
        double strip_y = Constants.FVT_stripsYlocref[strip-1];
        double strip_x = Constants.FVT_stripsXlocref[strip-1];
     
        
        double strip_length = Constants.FVT_stripslength[strip-1];
        double sigma = sigmaDrift*Constants.hDrift;
        double wght=(Erf.erf((strip_y+Constants.FVT_Pitch/2.-y)/sigma/Math.sqrt(2))-Erf.erf((strip_y-Constants.FVT_Pitch/2.-y)/sigma/Math.sqrt(2)))*(Erf.erf((strip_x+strip_length/2.-x)/sigma/Math.sqrt(2))-Erf.erf((strip_x-strip_length/2.-x)/sigma/Math.sqrt(2)))/2./2.;
        if (wght<0) wght=-wght;
        return wght;
    }
    
    public double getCentroidEstimate(int layer, double x, double y) {
        if(this.getClosestStrip(x, y, layer)>1) {
            return Constants.FVT_stripsYlocref[this.getClosestStrip(x, y, layer)-1];
        } else {
            return y*Math.cos(Constants.FVT_Alpha[layer-1])- x*Math.sin(Constants.FVT_Alpha[layer-1]);

        }
    
    }
    public double[] H(StateVec stateVec, StateVecs sv) {
        /*
       // double Zk = this.measurements.get(stateVec.k).z;
        StateVec SVplus = null;// = new StateVec(stateVec.k);
        StateVec SVminus = null;// = new StateVec(stateVec.k);

        double delta_d_x = 7.449126e-03;
        SVplus = this.reset(SVplus, stateVec, sv);
        SVminus = this.reset(SVminus, stateVec, sv);

        SVplus.x = stateVec.x + delta_d_x / 2.; 
        SVminus.x = stateVec.x - delta_d_x / 2.;

        double delta_m_dx = (h(SVplus) - h(SVminus)) / delta_d_x;
        
        double delta_d_y = 9.019044e-02;
        SVplus = this.reset(SVplus, stateVec, sv);
        SVminus = this.reset(SVminus, stateVec, sv);

        SVplus.y = stateVec.y + delta_d_y / 2.; 
        SVminus.y = stateVec.y - delta_d_y / 2.;

        double delta_m_dy = (h(SVplus) - h(SVminus)) / delta_d_y;
        //int layer = this.measurements.get(stateVec.k).layer;
        //delta_m_dy = Math.cos(Constants.FVT_Alpha[layer-1]);
        //delta_m_dx = - Math.sin(Constants.FVT_Alpha[layer-1]);
        //System.out.println("* delta_m_dx "+delta_m_dx+" delta_m_dy "+delta_m_dy);
        
        double delta_d_tx = 7.159996e-04;
        SVplus = this.reset(SVplus, stateVec, sv);
        SVminus = this.reset(SVminus, stateVec, sv);

        SVplus.tx = stateVec.tx + delta_d_tx / 2.;
        SVminus.tx = stateVec.tx - delta_d_tx / 2.;

        double delta_m_dtx = (h(SVplus) - h(SVminus)) / delta_d_tx;

        double delta_d_ty = 7.169160e-04;
        SVplus = this.reset(SVplus, stateVec, sv);
        SVminus = this.reset(SVminus, stateVec, sv);

        SVplus.ty = stateVec.ty + delta_d_ty / 2.;
        SVminus.ty = stateVec.ty - delta_d_ty / 2.;

        double delta_m_dty = (h(SVplus) - h(SVminus)) / delta_d_ty;
        
        double delta_d_Q = 4.809686e-04;
        SVplus = this.reset(SVplus, stateVec, sv);
        SVminus = this.reset(SVminus, stateVec, sv);

        SVplus.Q = stateVec.Q + delta_d_Q / 2.;
        SVminus.Q = stateVec.Q - delta_d_Q / 2.;

        double delta_m_dQ = (h(SVplus) - h(SVminus)) / delta_d_Q;
       
        
        double[] H = new double[]{delta_m_dx, delta_m_dy, delta_m_dtx, delta_m_dty, delta_m_dQ};
        */
        int layer = this.measurements.get(stateVec.k).layer;
        
        //return this.getCentroidEstimate(layer, stateVec.x, stateVec.y);
        //return (double) this.getClosestStrip(stateVec.x, stateVec.y, layer);
        double[] H = new double[]{-Math.sin(Constants.FVT_Alpha[layer-1]), Math.cos(Constants.FVT_Alpha[layer-1]), 0, 0, 0};
        //for(int i = 0; i<H.length; i++)
        //    System.out.println("H["+i+"] = "+H[i]);
        
        return H;
        
    }

    private StateVec reset(StateVec SVplus, StateVec stateVec, StateVecs sv) {
        SVplus = sv.new StateVec(stateVec.k);
        SVplus.x = stateVec.x;
        SVplus.y = stateVec.y;
        SVplus.z = stateVec.z;
        SVplus.tx = stateVec.tx;
        SVplus.ty = stateVec.ty;
        SVplus.Q = stateVec.Q;

        return SVplus;
    }

}
