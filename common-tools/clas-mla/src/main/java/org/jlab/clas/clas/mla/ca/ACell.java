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
    
    /**
     * 
     * @param allCells
     */
    public void findNeighbors(ArrayList<ACell> allCells) {
        for(int ic=0;ic<allCells.size();ic++){
      	  
            for(int jc=ic+1;jc<allCells.size();jc++){
      		  
                if( allCells.get(ic).getNeighbors().contains(allCells.get(jc))) {
                    continue;
                }
                if( allCells.get(ic).getSourceNode().equals(allCells.get(jc).getSinkNode()) ){ 
                    if(allCells.get(jc).passNeighboringCondition(allCells.get(ic))) {
                        allCells.get(ic).addNeighbor(allCells.get(jc));
                    }
                }
            }
        }
    }
    
    public void evolve(ArrayList<ACell> allCells, int N) {
        int[] states = new int[allCells.size()];
        // evolve
        for( int i=0; i<N; i++){ // epochs   
            // find the state for each cell
            int j=0;
            for( ACell c : allCells ){
                int max = 0;
                for( ACell n : c.getNeighbors() ){
                    if( n.getState() > max ) {
                        max = n.getState();
                    } 
                }
                states[j] =  1 + max ;
                j++;
            }
            // update all cell states at once
            for( j=0;j<allCells.size();j++) {
                allCells.get(j).setState(states[j]);
            }
        }
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
