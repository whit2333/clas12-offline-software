/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.clas.mla.ca.cvt;

import org.jlab.clas.clas.mla.ca.ACell;
import org.jlab.clas.clas.mla.ca.ANode;

/**
 *
 * @author ziegler
 */
public class Cell extends ACell {

    public Cell(ANode node1, ANode node2) {
        super(node1, node2);
    }

    @Override
    public boolean passNeighboringCondition(ACell acell) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
