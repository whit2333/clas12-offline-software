/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.clas.mla.ca;

import java.util.ArrayList;
/**
 *
 * @author ziegler
 */
public abstract class ACell implements Comparable<ACell>, ICellProperties {

    /**
     * @return the _node1
     */
    public ANode getSourceNode() {
        return _node1;
    }

    /**
     * @param node1 the _node1 to set
     */
    public void setSourceNode(ANode node1) {
        this._node1 = node1;
    }

    /**
     * @return the _node2
     */
    public ANode getSinkNode() {
        return _node2;
    }

    /**
     * @param node2 the _node2 to set
     */
    public void setSinkNode(ANode node2) {
        this._node2 = node2;
    }

    /**
     * @return the _state
     */
    public int getState() {
        return _state;
    }

    /**
     * @param state the state to set
     */
    public void setState(int state) {
        this._state = state;
    }

    /**
     * @return the _neighbors
     */
    public ArrayList<ACell> getNeighbors() {
        return _neighbors;
    }

    /**
     * @param neighbors the neighbors to set
     */
    public void setNeighbors(ArrayList<ACell> neighbors) {
        this._neighbors = neighbors;
    }
    
    /**
     * 
     * @param neighbor added neighbor
     */
    public void addNeighbor(ACell neighbor) {
        if(neighbor!=null)
            this._neighbors.add(neighbor);
    }
    
    
    
    private int _state;
    private ArrayList<ACell> _neighbors;
    private ANode _node1;
    private ANode _node2;
    
    private boolean _isused;
    
    public ACell(ANode node1, ANode node2) {
        this._node1 = node1;
        this._node2 = node2;
        this._isused = false; //init
        this._state = 0;
        
    }
    
    
    @Override
    public int compareTo(ACell arg0) {
        return this.getState() < arg0.getState() ? 1 : this.getState() == arg0.getState() ? 0 : -1;
    }

}
