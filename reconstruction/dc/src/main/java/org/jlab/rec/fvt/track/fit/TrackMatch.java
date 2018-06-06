/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.rec.fvt.track.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.dc.trajectory.Trajectory;
import org.jlab.rec.fvt.track.fit.MeasVecs.MeasVec;
/**
 *
 * @author ziegler
 */
public class TrackMatch {
    
    public List<MeasVecs.MeasVec> matchDCTrack2FMTClusters(DataEvent event, List<Trajectory.TrajectoryStateVec> fMTTraj, int stripsOff) {
        List<MeasVecs.MeasVec> measurements = new ArrayList<MeasVecs.MeasVec>();
        if (event.hasBank("FMTRec::Clusters") == false || fMTTraj ==null) {
            return null;
        }
        Map<Integer, Double> clusMap = new HashMap<Integer, Double>();
        MeasVecs mv = new MeasVecs();
        DataBank bank = event.getBank("FMTRec::Clusters");

        for (int i = 0; i < bank.rows(); i++) {
            int layer = bank.getByte("layer", i); 
            int ns = this.findNearestStrip(fMTTraj.get(layer-1).getX(), fMTTraj.get(layer-1).getY(), layer);
            if(Math.abs(ns-bank.getInt("seedStrip", i))<stripsOff) { // pass if within # strips
                System.out.println(" strips off "+(Math.abs(ns-bank.getInt("seedStrip", i))));
                if(clusMap.containsKey(layer)) {
                    if(Math.abs(ns-bank.getInt("seedStrip", i))<Math.abs(ns-clusMap.get(layer)));
                        clusMap.put(layer, (double) bank.getFloat("centroid", i));
                } else {
                    clusMap.put(layer, (double) bank.getFloat("centroid", i));
                }
            }
        }
         measurements.add(mv.setMeasVec(-1, 0));
        for(int l = 0; l<6; l++) {
            if(clusMap.containsKey(l+1)) {
                System.out.println(" added measurement "+clusMap.get(l+1)+" at layer "+(l+1));
                measurements.add(mv.setMeasVec(l, clusMap.get(l+1)));
            }
        }
        return measurements;
    }
    
    public int findNearestStrip(double x, double y, int layer) {
        
        int ClosestStrip = -1;
        
        if(Math.sqrt(x*x+y*y)<Constants.FVT_Rmax && Math.sqrt(x*x+y*y)>Constants.FVT_Beamhole) {
	
            double x_loc = x*Math.cos(Constants.FVT_Alpha[layer-1])+y*Math.sin(Constants.FVT_Alpha[layer-1]);
            double y_loc = y*Math.cos(Constants.FVT_Alpha[layer-1])-x*Math.sin(Constants.FVT_Alpha[layer-1]);
            if(y_loc>-(Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.) && y_loc < (Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.)){ 
                if (x_loc<=0) 
                    ClosestStrip = (int) Math.floor(((Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.)-y_loc)/Constants.FVT_Pitch) + 1;
                if (x_loc>0) 
                    ClosestStrip = (int) (Math.floor((y_loc+(Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.))/Constants.FVT_Pitch) + 1 
                          + Constants.FVT_Halfstrips +0.5*( Constants.FVT_Nstrips-2.*Constants.FVT_Halfstrips)); 
            } else if(y_loc <= -(Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.) && y_loc > -Constants.FVT_Rmax){ 
                ClosestStrip = (int) Math.floor(((Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.)-y_loc)/Constants.FVT_Pitch) + 1; 
            }
            else if(y_loc >= (Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.) && y_loc < Constants.FVT_Rmax){ 
                ClosestStrip = (int) (Math.floor((y_loc+(Constants.FVT_Halfstrips*Constants.FVT_Pitch/2.))/Constants.FVT_Pitch) + 1 
                      + Constants.FVT_Halfstrips+0.5*( Constants.FVT_Nstrips-2.*Constants.FVT_Halfstrips));  
            }
        } 
        return ClosestStrip;
    }
}
