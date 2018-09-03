package org.jlab.service.dc;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import org.jlab.clas.physics.Particle;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;

import org.jlab.utils.options.OptionParser;

public class TrackDictionaryValidation {

    private Map<ArrayList<Integer>, Integer> dictionary = null;
    private DataGroup                        dataGroup  = new DataGroup(4,2);
    private EmbeddedCanvas                   canvas     = new EmbeddedCanvas();
            
    public TrackDictionaryValidation(){

    }

    public void createDictionary(String inputFileName) {
        // create dictionary from event file
        System.out.println("Creating dictionary from file: " + inputFileName);
        Map<ArrayList<Integer>, Integer> newDictionary = new HashMap<>();
        HipoDataSource reader = new HipoDataSource();
        reader.open(inputFileName);
        int nevent = -1;
        while(reader.hasEvent() == true && nevent<100000) {
            DataEvent event = reader.getNextEvent();
            nevent++;
            if(nevent%10000 == 0) System.out.println("Analyzed " + nevent + " events");
            DataBank recTrack = null;
            DataBank recHits = null;
            if (event.hasBank("TimeBasedTrkg::TBTracks")) {
                recTrack = event.getBank("TimeBasedTrkg::TBTracks");
            }
            if (event.hasBank("TimeBasedTrkg::TBHits")) {
                recHits = event.getBank("TimeBasedTrkg::TBHits");
            }
            if (recTrack != null && recHits != null) {
                for (int i = 0; i < recTrack.rows(); i++) {
                    int charge = recTrack.getByte("q",i);
                     Particle part = new Particle(
                                        -charge*11,
                                        recTrack.getFloat("p0_x", i),
                                        recTrack.getFloat("p0_y", i),
                                        recTrack.getFloat("p0_z", i),
                                        recTrack.getFloat("Vtx0_x", i),
                                        recTrack.getFloat("Vtx0_y", i),
                                        recTrack.getFloat("Vtx0_z", i));
                    int[] wireArray = new int[36];
                    for (int j = 0; j < recHits.rows(); j++) {
                        if (recHits.getByte("trkID", j) == recTrack.getShort("id", i)) {
                            int sector = recHits.getByte("sector", j);
                            int superlayer = recHits.getByte("superlayer", j);
                            int layer = recHits.getByte("layer", j);
                            int wire = recHits.getShort("wire", j);
                            wireArray[(superlayer - 1) * 6 + layer - 1] = wire;
                        }
                    }
                    ArrayList<Integer> wires = new ArrayList<Integer>();
                    for (int k = 0; k < 6; k++) {
                        for (int l=0; l<6; l++) {
                            if(wireArray[k*6 +l] != 0) {
                               wires.add(wireArray[k*6+l]);
                               break;
                            }
                        }
                    }
                    if(wires.size()==6) {
                        if(newDictionary.containsKey(wires))  {
                            int nRoad = newDictionary.get(wires) + 1;
                            newDictionary.replace(wires, nRoad);
                        }
                        else {
                            newDictionary.put(wires, 1);
                        }   
                    }
                }
            }
        }
        this.setDictionary(newDictionary);
    }
    
    public void createHistos() {
        // negative tracks
        H2F hi_ptheta_neg_found = new H2F("hi_ptheta_neg_found", "hi_ptheta_neg_found", 100, 0.0, 10.0, 100, 0.0, 50.0);     
        hi_ptheta_neg_found.setTitleX("p (GeV)");
        hi_ptheta_neg_found.setTitleY("#theta (deg)");
        H2F hi_ptheta_neg_missing = new H2F("hi_ptheta_neg_missing", "hi_ptheta_neg_missing", 100, 0.0, 10.0, 100, 0.0, 50.0);     
        hi_ptheta_neg_missing.setTitleX("p (GeV)");
        hi_ptheta_neg_missing.setTitleY("#theta (deg)");
        H2F hi_phitheta_neg_found = new H2F("hi_phitheta_neg_found", "hi_phitheta_neg_found", 100, -30, 30, 100, 0.0, 50.0);     
        hi_phitheta_neg_found.setTitleX("#phi (deg)");
        hi_phitheta_neg_found.setTitleY("#theta (deg)");
        H2F hi_phitheta_neg_missing = new H2F("hi_phitheta_neg_missing", "hi_phitheta_neg_missing", 100, -30, 30, 100, 0.0, 50.0);     
        hi_phitheta_neg_missing.setTitleX("#phi (deg)");
        hi_phitheta_neg_missing.setTitleY("#theta (deg)");
        // positive tracks
        H2F hi_ptheta_pos_found = new H2F("hi_ptheta_pos_found", "hi_ptheta_pos_found", 100, 0.0, 10.0, 100, 0.0, 50.0);     
        hi_ptheta_pos_found.setTitleX("p (GeV)");
        hi_ptheta_pos_found.setTitleY("#theta (deg)");
        H2F hi_ptheta_pos_missing = new H2F("hi_ptheta_pos_missing", "hi_ptheta_pos_missing", 100, 0.0, 10.0, 100, 0.0, 50.0);     
        hi_ptheta_pos_missing.setTitleX("p (GeV)");
        hi_ptheta_pos_missing.setTitleY("#theta (deg)");
        H2F hi_phitheta_pos_found = new H2F("hi_phitheta_pos_found", "hi_phitheta_pos_found", 100, -30, 30, 100, 0.0, 50.0);     
        hi_phitheta_pos_found.setTitleX("#phi (deg)");
        hi_phitheta_pos_found.setTitleY("#theta (deg)");
        H2F hi_phitheta_pos_missing = new H2F("hi_phitheta_pos_missing", "hi_phitheta_pos_missing", 100, -30, 30, 100, 0.0, 50.0);     
        hi_phitheta_pos_missing.setTitleX("#phi (deg)");
        hi_phitheta_pos_missing.setTitleY("#theta (deg)");
        this.dataGroup.addDataSet(hi_ptheta_neg_found,     0);
        this.dataGroup.addDataSet(hi_ptheta_neg_missing,   1);
        this.dataGroup.addDataSet(hi_phitheta_neg_found,   2);
        this.dataGroup.addDataSet(hi_phitheta_neg_missing, 3);
        this.dataGroup.addDataSet(hi_ptheta_pos_found,     4);
        this.dataGroup.addDataSet(hi_ptheta_pos_missing,   5);
        this.dataGroup.addDataSet(hi_phitheta_pos_found,   6);
        this.dataGroup.addDataSet(hi_phitheta_pos_missing, 7);
    }    

    private boolean findRoad(ArrayList<Integer> wires, int smear) {
        boolean found = false;
        if(smear>0) {
            for(int k1=-smear; k1<=smear; k1++) {
            for(int k2=-smear; k2<=smear; k2++) {
            for(int k3=-smear; k3<=smear; k3++) {
            for(int k4=-smear; k4<=smear; k4++) {
            for(int k5=-smear; k5<=smear; k5++) {
            for(int k6=-smear; k6<=smear; k6++) {
                ArrayList<Integer> wiresCopy = new ArrayList(wires);
                wiresCopy.set(0, wires.get(0) + k1);
                wiresCopy.set(1, wires.get(1) + k2);
                wiresCopy.set(2, wires.get(2) + k3);
                wiresCopy.set(3, wires.get(3) + k4);
                wiresCopy.set(4, wires.get(4) + k5);
                wiresCopy.set(5, wires.get(5) + k6);
                if(this.dictionary.containsKey(wiresCopy)) {
                    found=true;
                    break;
                }
            }}}}}}
        }
        else {
            if(this.dictionary.containsKey(wires)) found=true;
        } 
        return found;
    }
    
    public EmbeddedCanvas getCanvas() {
        return canvas;
    }

    public Map<ArrayList<Integer>, Integer> getDictionary() {
        return dictionary;
    }
    
    public boolean init() {
        this.createHistos();
        return true;
    }
    
    public void plotHistos() {
        this.canvas.divide(4, 2);
        this.canvas.setGridX(false);
        this.canvas.setGridY(false);
        this.canvas.draw(dataGroup);
    }
    
    public void printDictionary() {
        if(this.dictionary !=null) {
            for(Map.Entry<ArrayList<Integer>, Integer> entry : this.dictionary.entrySet()) {
                ArrayList<Integer> wires = entry.getKey();
                int nRoad = entry.getValue();
                for(int wire: wires) System.out.print(wire + " ");
                System.out.println(nRoad);
            }
        }
    }
    
    public void readDictionary(String fileName) {
        
        this.dictionary = new HashMap<>();
        
        System.out.println("Reading dictionary from file " + fileName);
        int nLines = 0;
        int nDupli = 0;
        
        File fileDict = new File(fileName);
        BufferedReader txtreader = null;
        try {
            txtreader = new BufferedReader(new FileReader(fileDict));
            String line = null;
            while ((line = txtreader.readLine()) != null) {
                nLines++;
                String[] lineValues;
                lineValues = line.split("\t ");
                ArrayList<Integer> wires = new ArrayList<Integer>();
                if(lineValues.length < 40) {
                    System.out.println("WARNING: dictionary line " + nLines + " incomplete: skipping");
                }
                else {
//                    System.out.println(line);
                    for(int i=0; i<6; i++) {
//                        System.out.println(lineValues[i]);
                        int wire = Integer.parseInt(lineValues[3+i*6]);
                        wires.add(wire);
                    }
                    if(this.dictionary.containsKey(wires)) {
                        nDupli++;
                        if(nDupli<10) System.out.println("WARNING: found duplicate road");
                        else if(nDupli==10) System.out.println("WARNING: reached maximum number of warnings, switching to silent mode");
//                        for(int wire: wires) System.out.println(wire + " ");
//                        System.out.println(" ");
//                        System.out.println(line);
                        int nRoad = this.dictionary.get(wires) + 1;
                        this.dictionary.replace(wires, nRoad);
                    }
                    else {
                        this.dictionary.put(wires, 1);
                    }
                }
                if(nLines % 1000000 == 0) System.out.println("Read " + nLines + " roads");
            }
            System.out.println("Found " + nLines + " roads with " + nDupli + " duplicates");
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
   }
    
    public void processFile(String fileName, int wireSmear, int maxEvents) {
        // testing dictionary on event file
        HipoDataSource reader = new HipoDataSource();
        reader.open(fileName);
        int nevent = -1;
        while(reader.hasEvent() == true) {
            if(maxEvents>0) {
                if(nevent>= maxEvents) break;
            }
            DataEvent event = reader.getNextEvent();
            nevent++;
            if(nevent%10000 == 0) System.out.println("Analyzed " + nevent + " events");
            DataBank recTrack = null;
            DataBank recHits = null;
            if (event.hasBank("TimeBasedTrkg::TBTracks")) {
                recTrack = event.getBank("TimeBasedTrkg::TBTracks");
            }
            if (event.hasBank("TimeBasedTrkg::TBHits")) {
                recHits = event.getBank("TimeBasedTrkg::TBHits");
            }
            if (recTrack != null && recHits != null) {
                for (int i = 0; i < recTrack.rows(); i++) {
                    int charge = recTrack.getByte("q",i);
                    Particle part = new Particle(
                                        -charge*11,
                                        recTrack.getFloat("p0_x", i),
                                        recTrack.getFloat("p0_y", i),
                                        recTrack.getFloat("p0_z", i),
                                        recTrack.getFloat("Vtx0_x", i),
                                        recTrack.getFloat("Vtx0_y", i),
                                        recTrack.getFloat("Vtx0_z", i));
                    if(Math.abs(part.vz())>15) continue;
                    int[] wireArray = new int[36];
                    for (int j = 0; j < recHits.rows(); j++) {
                        if (recHits.getByte("trkID", j) == recTrack.getShort("id", i)) {
                            int sector = recHits.getByte("sector", j);
                            int superlayer = recHits.getByte("superlayer", j);
                            int layer = recHits.getByte("layer", j);
                            int wire = recHits.getShort("wire", j);
                            wireArray[(superlayer - 1) * 6 + layer - 1] = wire;
                        }
                    }
                    ArrayList<Integer> wires = new ArrayList<Integer>();
                    for (int k = 0; k < 6; k++) {
                        for (int l=0; l<6; l++) {
                            if(wireArray[k*6 +l] != 0) {
                               wires.add(wireArray[k*6+l]);
                               break;
                            }
                        }
                    }
    //                System.out.println("");
                    if(wires.size()==6) {
    //                    System.out.print(charge + " " + wires.size() + " ");
    //                    for(int wire: wires) System.out.print(wire + " ");
    //                    System.out.println(" ");
                        double phi = (Math.toDegrees(part.phi())+180+30)%60-30;
                        if(this.findRoad(wires,wireSmear))  {
                            if(charge==-1) {
                                this.dataGroup.getH2F("hi_ptheta_neg_found").fill(part.p(), Math.toDegrees(part.theta()));
                                this.dataGroup.getH2F("hi_phitheta_neg_found").fill(phi, Math.toDegrees(part.theta()));
                            }
                            else {
                                this.dataGroup.getH2F("hi_ptheta_pos_found").fill(part.p(), Math.toDegrees(part.theta()));
                                this.dataGroup.getH2F("hi_phitheta_pos_found").fill(phi, Math.toDegrees(part.theta()));
                            }
                        }
                        else {
                            if(charge==-1) {
                                this.dataGroup.getH2F("hi_ptheta_neg_missing").fill(part.p(), Math.toDegrees(part.theta()));
                                this.dataGroup.getH2F("hi_phitheta_neg_missing").fill(phi, Math.toDegrees(part.theta()));
                            }
                            else {
                                this.dataGroup.getH2F("hi_ptheta_pos_missing").fill(part.p(), Math.toDegrees(part.theta()));
                                this.dataGroup.getH2F("hi_phitheta_pos_missing").fill(phi, Math.toDegrees(part.theta()));
                            }                    
                        }
                    }
                }
            }
        }

    }
    
    private void setDictionary(Map<ArrayList<Integer>, Integer> newDictionary) {
        this.dictionary = newDictionary;
    }
    
    public ArrayList<Integer> xWires() {
        ArrayList<Integer> wires = new ArrayList<Integer>();
        wires.add(41);
        wires.add(41);
        wires.add(41);
        wires.add(41);
        wires.add(41);
        wires.add(41);
        
        wires.add(37);
        wires.add(38);
        wires.add(37);
        wires.add(38);
        wires.add(37);
        wires.add(37);
        
        wires.add(38);
        wires.add(39);
        wires.add(38);
        wires.add(39);
        wires.add(38);
        wires.add(38);
        
        wires.add(34);
        wires.add(34);
        wires.add(34);
        wires.add(34);
        wires.add(33);
        wires.add(34);
        
        wires.add(34);
        wires.add(34);
        wires.add(33);
        wires.add(34);
        wires.add(33);
        wires.add(33);
        
        wires.add(30);
        wires.add(30);
        wires.add(29);
        wires.add(30);
        wires.add(29);
        wires.add(29);
//        ArrayList<Integer> wires = null;
//        if(this.dictionary !=null) {
//            for(Map.Entry<ArrayList<Integer>, Integer> entry : this.dictionary.entrySet()) {
//                wires = entry.getKey();
//                break;
//            }
//        }
        return wires;
    }

    public static void main(String[] args) {
        
        OptionParser parser = new OptionParser("dict-validation");
        parser.addOption("-d","dictionary.txt", "read dictionary from file");
        parser.addOption("-c","input.hipo", "create dictionary from event file");
        parser.addOption("-i","test.hipo", "set event file for dictionary validation");
        parser.addOption("-w", "0", "wire smearing in road finding");
        parser.addOption("-n", "-1", "maximum number of events to process");
        parser.parse(args);
        
        String dictionaryFileName = null;
        if(parser.hasOption("-d")==true){
            dictionaryFileName = parser.getOption("-d").stringValue();
        }
        String inputFileName = null;
        if(parser.hasOption("-c")==true){
            inputFileName = parser.getOption("-c").stringValue();
        }
        String testFileName = null;
        if(parser.hasOption("-i")==true){
            testFileName = parser.getOption("-i").stringValue();
        }
        int wireSmear = parser.getOption("-w").intValue();
        int maxEvents = parser.getOption("-n").intValue();
            
//        dictionaryFileName="/Users/devita/TracksDicTorus-1.0Solenoid-1.0InvPBinSizeiGeV0.05PhiMinDeg-60.0PhiMaxDeg60.0.txt";
//        inputFileName = "/Users/devita/out_clas_004013.0.9.hipo";
//        testFileName  = "/Users/devita/out_clas_004013.0.9.hipo";
//        wireSmear=2;
//        maxEvents = 500000;  
        
        TrackDictionaryValidation tm = new TrackDictionaryValidation();
        tm.init();
        if(parser.hasOption("-d")==true) {
            tm.readDictionary(dictionaryFileName);
        }
        else {
            tm.createDictionary(inputFileName);
        }
//        tm.printDictionary();
        tm.processFile(testFileName,wireSmear,maxEvents);
        
        
                
        JFrame frame = new JFrame("Tracking");
        Dimension screensize = null;
        screensize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize((int) (screensize.getWidth() * 0.8), (int) (screensize.getHeight() * 0.8));
        frame.add(tm.getCanvas());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        tm.plotHistos();

    }
    
    
}
