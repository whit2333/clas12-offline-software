/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.rec.cvt.track.ca;

import java.util.List;
import org.jlab.clas.clas.mla.ca.cvt.Cell;
import org.jlab.clas.clas.mla.ca.cvt.CellOperations;
import org.jlab.rec.cvt.bmt.Geometry;
import org.jlab.rec.cvt.cross.Cross;

/**
 *
 * @author ziegler
 */
public class RunCA {
    
    private int nEpoch = 10;
    
    public void run(CellFinder cf, CellOperations co, List<Cross> crosses, Geometry bgeom) {
        
        cf.findCells(crosses, bgeom);
        if(cf.getCells()==null) {
            return;
        } 
        
        co.findNeighbors(cf.getCells());
        co.evolve(cf.getCells(), nEpoch);
        for(Cell c : cf.getCells())
            System.out.println(" cell "+c.getSourceNode().getNodeObjectType()+c.getSinkNode().getNodeObjectType());
    }
}
