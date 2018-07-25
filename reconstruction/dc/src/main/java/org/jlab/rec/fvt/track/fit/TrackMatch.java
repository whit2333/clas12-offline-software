/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.rec.fvt.track.fit;

import java.util.ArrayList;
import java.util.List;
import org.jlab.rec.dc.track.Track;
import org.jlab.rec.dc.trajectory.Trajectory;
/**
 *
 * @author ziegler
 */
public class TrackMatch {
    
    public List<ArrayList<MeasVecs.MeasVec>> matchDCTrack2FMTClusters(List<Trajectory.TrajectoryStateVec> fMTTraj, 
            List<ArrayList<MeasVecs.MeasVec>> FMTClusList, 
            org.jlab.rec.fmt.Geometry fmtDetector) {
        List<ArrayList<MeasVecs.MeasVec>> cands = new ArrayList<ArrayList<MeasVecs.MeasVec>>();
        if(FMTClusList!=null && FMTClusList.size()>0){
            List<ArrayList<MeasVecs.MeasVec>> measurements = new ArrayList<ArrayList<MeasVecs.MeasVec>>();
            for(int l =0; l<6; l++) {
                measurements.add(new ArrayList<MeasVecs.MeasVec>());
            }
            for(int analLy=0; analLy<6; analLy++) {
                if(FMTClusList.get(analLy).size()>0) {
                    for(int clusIdx = 0; clusIdx<FMTClusList.get(analLy).size(); clusIdx++)   {     
                        if(Math.abs(fmtDetector.getClosestStrip(fMTTraj.get(analLy).getX(), fMTTraj.get(analLy).getY(), analLy+1)-
                            FMTClusList.get(analLy).get(clusIdx).seed)<=20)
                            measurements.get(analLy).add(FMTClusList.get(analLy).get(clusIdx));
                    }
                }
            }
            int[]L = new int[6];
            for(int l =0; l<6; l++) {
                L[l]=1;
                if(measurements.get(l).size()>1)
                    L[l]=measurements.get(l).size();
            }
            for(int l1 =0; l1<L[0]; l1++) {
                for(int l2 =0; l2<L[1]; l2++) {
                    for(int l3 =0; l3<L[2]; l3++) {
                        for(int l4 =0; l4<L[3]; l4++) {
                            for(int l5 =0; l5<L[4]; l5++) {
                                for(int l6 =0; l6<L[5]; l6++) {
                                    ArrayList<MeasVecs.MeasVec> measList = new ArrayList<MeasVecs.MeasVec>();
                                    measList.add(mv.setMeasVec(-1, (double) 0, 0, 1));
                                    this.addMeasToTracklet(measurements.get(0), l1, measList);
                                    this.addMeasToTracklet(measurements.get(1), l2, measList);
                                    this.addMeasToTracklet(measurements.get(2), l3, measList);
                                    this.addMeasToTracklet(measurements.get(3), l4, measList);
                                    this.addMeasToTracklet(measurements.get(4), l5, measList);
                                    this.addMeasToTracklet(measurements.get(5), l6, measList);
                                    if(measList.size()>3)
                                        cands.add(measList);
                                }
                            }
                        }
                    }
                }
            }
        }
        return cands;
    }

    
    private MeasVecs mv = new MeasVecs();
    
    /**
    * Get the sector [1..6] from the phi value
    * 
    * @param phi the value of phi in degrees
    * @return the sector [1..6]
    */
    private int getSector(double phi) {
           // convert phi to [0..360]

           while (phi < 0) {
                   phi += 360.0;
           }
           while (phi > 360.0) {
                   phi -= 360.0;
           }

           if ((phi > 330) || (phi <= 30)) {
                   return 1;
           }
           if (phi <= 90.0) {
                   return 2;
           }
           if (phi <= 150.0) {
                   return 3;
           }
           if (phi <= 210.0) {
                   return 4;
           }
           if (phi <= 270.0) {
                   return 5;
           }
           return 6;
        }
   

    private void AddMeasToList(ArrayList<MeasVecs.MeasVec> measList, List<ArrayList<MeasVecs.MeasVec>> listOfMeasurements, List<Trajectory.TrajectoryStateVec> fMTTraj, int analLy, int l1, 
            int stripsOff, org.jlab.rec.fmt.Geometry fmtDetector) {
      
        if(listOfMeasurements.get(analLy).size()>0) {
            //System.out.println(" ly "+analLy+" trk "+fMTTraj.get(analLy).getX()+","+ fMTTraj.get(analLy).getY()
            //    +" closest strip "+fmtDetector.getClosestStrip(fMTTraj.get(analLy).getX(), fMTTraj.get(analLy).getY(), analLy+1)+
            //            " cluster seed "+listOfMeasurements.get(analLy).get(l1).seed);
            if(Math.abs(fmtDetector.getClosestStrip(fMTTraj.get(analLy).getX(), fMTTraj.get(analLy).getY(), analLy+1)-
                    listOfMeasurements.get(analLy).get(l1).seed)<=stripsOff) {
                measList.add(listOfMeasurements.get(analLy).get(l1));
                
            } else {
            //    System.out.println("Matching fails....");
            }
        }
    }

    private void addMeasToTracklet(ArrayList<MeasVecs.MeasVec> measurementsLayer, int l1, ArrayList<MeasVecs.MeasVec> measList) {
        if(measurementsLayer.size()>0 &&measurementsLayer.get(l1)!=null)
        measList.add(measurementsLayer.get(l1));
    }
    
}
