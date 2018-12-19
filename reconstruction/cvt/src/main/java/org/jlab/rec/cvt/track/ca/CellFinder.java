/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.rec.cvt.track.ca;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jlab.clas.clas.mla.ca.cvt.Node;
import org.jlab.rec.cvt.cross.Cross;

/**
 *
 * @author ziegler
 */
public class CellFinder {
    int[] lZ = { 2, 3, 5};
    int[] lC = { 1, 4, 6}; 
    int[] sourceReg = {1,1,2,2,3,2,3,4,3,4,5,3,4,5,6,4,5,6,7,5,6,7,8};
    int[] sinkReg   = {2,3,3,4,4,5,5,5,6,6,6,7,7,7,7,8,8,8,8,9,9,9,9};
    
    private Map<Integer, Node> nodesFromCrosses = null;
    private void getNodes(List<Cross> crosses) {
        nodesFromCrosses = new HashMap<Integer, Node>();
        Collections.sort(crosses);
        for(Cross c : crosses) {
            if(c.get_Detector().equalsIgnoreCase("SVT")) {
                double x = c.get_Point().x();
                double y = c.get_Point().y();
                double radius = Math.sqrt(x*x + y*y);
                Node nodet = new Node(c.get_Point(), radius, Node.DetectorType.SVTT);
                Node nodel = new Node(c.get_Point(), radius, Node.DetectorType.SVTL);
                nodet.setRegion(c.get_Region());
                nodel.setRegion(c.get_Region());
                nodesFromCrosses.put(nodet.getRegion(), nodet);
                nodesFromCrosses.put(nodel.getRegion(), nodel);
            }
            if(c.get_Detector().equalsIgnoreCase("BMT")) {
                if(c.get_DetectorType().equalsIgnoreCase("Z")) {
                    double x = c.get_Point().x();
                    double y = c.get_Point().y();
                    double radius = Math.sqrt(x*x + y*y);
                    Node node = new Node(c.get_Point(), radius, Node.DetectorType.BMTZ);
                    node.setRegion(3 + lZ[c.get_Region()-1]);
                    nodesFromCrosses.put(node.getRegion(), node);
                } 
                if(c.get_DetectorType().equalsIgnoreCase("C")) {
                    double radius = org.jlab.rec.cvt.bmt.Constants.getCRCRADIUS()[c.get_Region()-1];
                    Node node = new Node(c.get_Point(), radius, Node.DetectorType.BMTC);
                    node.setRegion(3 + lC[c.get_Region()-1]);
                    nodesFromCrosses.put(node.getRegion(), node);
                } 
            }
        }
    }
    
    private void getCells() {
        for(int i = 0; i < sourceReg.length; i++) {
            if(nodesFromCrosses.containsKey(sourceReg[i]) && 
                    nodesFromCrosses.containsKey(sinkReg[i])) {
                
            }
        }
    }
    
}
