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
import org.jlab.clas.clas.mla.ca.cvt.Cell;
import org.jlab.clas.clas.mla.ca.cvt.Node;
import org.jlab.rec.cvt.bmt.Geometry;
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
    
    private Map<Integer, ArrayList<Node>> nodesFromCrosses = null;
    private List<Cell> cells = null;
    
    private void getNodes(List<Cross> crosses) {
        nodesFromCrosses = new HashMap<Integer, ArrayList<Node>>();
        Collections.sort(crosses);
        for(Cross c : crosses) {
            if(c.get_Detector().equalsIgnoreCase("SVT")) {
                double x = c.get_Point().x();
                double y = c.get_Point().y();
                double radius = Math.sqrt(x*x + y*y);
                Node nodet = new Node(c.get_Point(), radius, Node.DetectorType.SVTT);
                Node nodel = new Node(c.get_Point(), radius, Node.DetectorType.SVTL);
                nodet.setRegion(c.get_Region());
                nodet.setSector(c.get_Sector());
                nodel.setRegion(c.get_Region());
                nodel.setSector(c.get_Sector());       
               
                if(!nodesFromCrosses.containsKey(nodet.getRegion())) {
                    nodesFromCrosses.put(nodet.getRegion(), new ArrayList<Node>());
                }
                nodesFromCrosses.get(nodet.getRegion()).add(nodet);
                nodesFromCrosses.get(nodet.getRegion()).add(nodel);
            }
            if(c.get_Detector().equalsIgnoreCase("BMT")) {
                if(c.get_DetectorType().equalsIgnoreCase("Z")) {
                    double x = c.get_Point().x();
                    double y = c.get_Point().y();
                    double radius = Math.sqrt(x*x + y*y);
                    Node node = new Node(c.get_Point(), radius, Node.DetectorType.BMTZ);
                    node.setRegion(3 + lZ[c.get_Region()-1]);
                    node.setSector(c.get_Sector());     
                    if(!nodesFromCrosses.containsKey(node.getRegion())) {
                    nodesFromCrosses.put(node.getRegion(), new ArrayList<Node>());
                }
                nodesFromCrosses.get(node.getRegion()).add(node);
                } 
                if(c.get_DetectorType().equalsIgnoreCase("C")) {
                    double radius = org.jlab.rec.cvt.bmt.Constants.getCRCRADIUS()[c.get_Region()-1];
                    Node node = new Node(c.get_Point(), radius, Node.DetectorType.BMTC);
                    node.setRegion(3 + lC[c.get_Region()-1]);
                    node.setSector(c.get_Sector());     
                    if(!nodesFromCrosses.containsKey(node.getRegion())) {
                    nodesFromCrosses.put(node.getRegion(), new ArrayList<Node>());
                }
                nodesFromCrosses.get(node.getRegion()).add(node);
                } 
            }
        }
    }
    
    private void getCells(Geometry bgeom) {
        cells = new ArrayList<Cell>();  
        for(int i = 0; i < sourceReg.length; i++) {
            if(nodesFromCrosses.containsKey(sourceReg[i]) && 
                    nodesFromCrosses.containsKey(sinkReg[i])) {
                for(int j = 0; j< nodesFromCrosses.get(sourceReg[i]).size(); j++) {
                    for(int k = 0; k< nodesFromCrosses.get(sinkReg[i]).size(); k++) {
                        if(nodesFromCrosses.get(sourceReg[i]).get(j).getCoordIdx()!=
                            nodesFromCrosses.get(sinkReg[i]).get(k).getCoordIdx())
                            continue;
                        if(nodesFromCrosses.get(sinkReg[i]).get(k).getRegion()>3) { // stay in the same BMT sector... clean-up if/then's (todo)
                            if(nodesFromCrosses.get(sourceReg[i]).get(j).getRegion()>3) {
                                if(nodesFromCrosses.get(sinkReg[i]).get(k).getSector()!=nodesFromCrosses.get(sourceReg[i]).get(j).getSector())
                                    continue;
                            } else { 
                                if( ! bgeom.checkIsInSector( nodesFromCrosses.get(sourceReg[i]).get(j).getPoint().toVector3D().phi(), 
                                        nodesFromCrosses.get(sinkReg[i]).get(k).getSector(), 1, Math.toRadians(15) )  )
                                    continue;
                            }
                        } 
                        
                        Cell cell = new Cell(nodesFromCrosses.get(sourceReg[i]).get(j),
                            nodesFromCrosses.get(sinkReg[i]).get(k));
                        if(cell.getPath()!=null) {
                            this.cells.add(cell);
                        //System.out.println("pass "+nodesFromCrosses.get(sourceReg[i]).get(j).getDtype()+") "+nodesFromCrosses.get(sourceReg[i]).get(j).getRegion()+") "+nodesFromCrosses.get(sourceReg[i]).get(j).getPoint().toString()
                          //      +" --> "+nodesFromCrosses.get(sinkReg[i]).get(k).getDtype()+") "+nodesFromCrosses.get(sinkReg[i]).get(k).getRegion()+") "+nodesFromCrosses.get(sinkReg[i]).get(k).getPoint().toString());
                        //System.out.println("add cell "+cell.getSourceNode().getNodeObjectType()+cell.getSinkNode().getNodeObjectType());
                        }
                    }
                }
            }
        }
    }
    
    public void findCells(List<Cross> crosses, Geometry bgeom) {
        this.getNodes(crosses);
        if(this.nodesFromCrosses==null) {
            return;
        } else {
            this.getCells(bgeom);
        }
        if(this.cells!=null) {
            List<Cell> uniqCells = new ArrayList<Cell>();
            for(Cell c : this.cells) {
                if(!uniqCells.contains(c)) {
                    uniqCells.add(c);
                } 
            }
            this.cells = uniqCells;
        }
    }

    /**
     * @return the cells
     */
    public List<Cell> getCells() {
        return this.cells;
    }
    
}
