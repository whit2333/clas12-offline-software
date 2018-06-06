package org.jlab.service.fvt;

import java.util.ArrayList;
import java.util.List;

import org.jlab.clas.reco.ReconstructionEngine;

import org.jlab.io.base.*;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.io.hipo.HipoDataSync;
import org.jlab.rec.fvt.fmt.Constants;
//import org.jlab.rec.fmt.Constants;
import org.jlab.rec.fvt.fmt.banks.HitReader;
import org.jlab.rec.fvt.fmt.banks.RecoBankWriter;
//import org.jlab.rec.fmt.CCDBConstantsLoader;
import org.jlab.rec.fvt.fmt.cluster.Cluster;
import org.jlab.rec.fvt.fmt.cluster.ClusterFinder;
import org.jlab.rec.fvt.fmt.cross.Cross;
import org.jlab.rec.fvt.fmt.cross.CrossMaker;
import org.jlab.rec.fvt.fmt.hit.FittedHit;
import org.jlab.rec.fvt.fmt.hit.Hit;



import org.jlab.rec.fvt.fmt.CCDBConstantsLoader;
//import org.jlab.rec.fvt.track.fit.KFitter;

/**
 * Service to return reconstructed  track candidates- the output is in hipo
 * format
 *
 * @author ziegler
 *
 */
public class FMTReconstruction extends ReconstructionEngine {

    org.jlab.rec.fvt.fmt.Geometry FVTGeom;

    public FMTReconstruction() {
        super("FMTTracks", "ziegler", "4.0");
        
        FVTGeom = new org.jlab.rec.fvt.fmt.Geometry();
        
        //GeometryLoader.Load(10, "default");
        //GeometryLoader gl = new GeometryLoader();
        //gl.LoadSurfaces();
        CCDBConstantsLoader.Load(10);
    }

    String FieldsConfig = "";
    private int Run = -1;
  
 

    public int getRun() {
        return Run;
    }

    public void setRun(int run) {
        Run = run;
    }

    public String getFieldsConfig() {
        return FieldsConfig;
    }

    public void setFieldsConfig(String fieldsConfig) {
        FieldsConfig = fieldsConfig;
    }
    
    CrossMaker crossMake;
    ClusterFinder clusFinder;
    
    @Override
    public boolean processDataEvent(DataEvent event) {
//        if(event.hasBank("RUN::config")==false ) {
//		System.err.println("RUN CONDITIONS NOT READ!");
//		return true;
//	}
//		
//        DataBank bank = event.getBank("RUN::config");
//		
//        // Load the constants
//        //-------------------
//        int newRun = bank.getInt("run", 0);
//
//        if(Run!=newRun) {
//            
//            double TORSCALE = (double)bank.getFloat("torus", 0);
//            double SOLSCALE = (double)bank.getFloat("solenoid", 0);
//            double shift =0;
//            if(Run>1890)
//                shift = -1.9;
//            DCSwimmer.setMagneticFieldsScales(SOLSCALE, TORSCALE, shift);
//            Run = newRun;
//        }
        
        List<Cluster> clusters = new ArrayList<Cluster>();    
        List<Cross> crosses = new ArrayList<Cross>();    
        
        this.FieldsConfig = this.getFieldsConfig();
        this.Run = this.getRun();
        
        RecoBankWriter rbc = new RecoBankWriter();
        //I) get the hits
        HitReader hitRead = new HitReader();
        hitRead.fetch_FMTHits(event);
        List<Hit> hits = hitRead.get_FMTHits();
        
        //II) process the hits	
        //1) exit if hit list is empty
        if (hits.size() != 0) {
            //2) find the clusters from these hits
            clusters = clusFinder.findClusters(hits);
            List<FittedHit> FMThits =  new ArrayList<FittedHit>();
            if (clusters.size() != 0) {
                if (clusters.size() != 0) {
                    for (int i = 0; i < clusters.size(); i++) {
                        FMThits.addAll(clusters.get(i));
                    }
                    crosses = crossMake.findCrosses(clusters);
                }
            }
            rbc.appendFMTBanks(event, FMThits, clusters, crosses);
        }
        
        return true;
   }

    @Override
    public boolean init() {
       
       Constants.Load();
       clusFinder = new ClusterFinder();
       crossMake = new CrossMaker();
      
       return true;
    }

     
    public static void main(String[] args) {
//String inputFile = args[0];
        //String outputFile = args[1];
        String inputFile="//Users/ziegler/Desktop/Work/Files/FMTDevel/gemc/electron.hipo";
        
        
        
        FMTReconstruction en = new FMTReconstruction();
        en.init();
        
        
        int counter = 0;
        
        HipoDataSource reader = new HipoDataSource();
        reader.open(inputFile);
        
        HipoDataSync writer = new HipoDataSync();
        //Writer
        
        String outputFile="/Users/ziegler/Desktop/Work/Files/FMTDevel/gemc/electron_rec.hipo";
       
        writer.open(outputFile);
        long t1 = 0;
        while (reader.hasEvent()) {
            
            counter++;
            System.out.println("************************************************************* ");
            DataEvent event = reader.getNextEvent();
            if (counter > 0) {
                t1 = System.currentTimeMillis();
            }
            //if(event.getBank("RUN::config").getInt("event", 0) <50)
             //   continue;
            en.processDataEvent(event);
            //event.show();
            
            writer.writeEvent(event);
            System.out.println("PROCESSED  EVENT "+event.getBank("RUN::config").getInt("event", 0));
           // event.show();
            //if (event.getBank("RUN::config").getInt("event", 0) > 11) {
            //    break;
            //}
            
            
            // event.show();
            if(counter%100==0)
                break;
            //if(event.hasBank("HitBasedTrkg::HBTracks")) {
            //    event.show();
            
            //}
        }
        writer.close();
        double t = System.currentTimeMillis() - t1;
        System.out.println(t1 + " TOTAL  PROCESSING TIME = " + (t / (float) counter));
    }

    

}
