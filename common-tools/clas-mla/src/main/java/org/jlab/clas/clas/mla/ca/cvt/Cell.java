/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.clas.mla.ca.cvt;

import org.jlab.clas.clas.mla.ca.ACell;
import org.jlab.clas.clas.mla.ca.ANode;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;

/**
 *
 * @authors bossu, ziegler
 */
public class Cell extends ACell {

    double angleCut = 0.995;
    // in XY follow the linear relation obtained from simulations
    // Angle( DR ) = 0.175 * DR + 0.551 
    // where DR is the difference in radius of the crosses
    // The angles are in degrees
    double a = 1.75;
    double b = 0.551;
    
    public Cell(Node node1, Node node2) {
        super(node1, node2);
        if(this.isValid(node1, node2)) {
            this.setPath();
            _npath = _path.clone().asUnit();
        }
    }

    @Override
    public boolean passNeighboringCondition(ACell acell) {
        boolean pass = false;
        ANode anode1 = acell.getSourceNode();
        ANode anode2 = acell.getSinkNode();
        Point3D node1 = (Point3D) anode1.getNodeObjectType();
        Point3D node2 = (Point3D) anode2.getNodeObjectType();
        
        Vector3D apath = node2.toVector3D().sub(node1.toVector3D()).asUnit();
        if(_npath.dot(apath)>angleCut) {
            pass = true;
        }
        return pass;
    }
    
                
    private Vector3D _npath;
    private Vector3D _path;
    /**
     * @return the _path
     */
    public Vector3D getPath() {
        return _path;
    }

   /**
    * sets the path between nodes
    */
    
    public void setPath() {
        
        this._path = (Vector3D) this.getSourceNode().connector( this.getSourceNode(), this.getSinkNode() );
    }

    private boolean isValid(Node node1, Node node2) {
        boolean isValid = false;
        double cutAngle = this.a + (node2.getRadius() - node1.getRadius()) + this.b;
        double nodesAngle = Math.toDegrees(node1.getPoint().toVector3D().angle(node2.getPoint().toVector3D()));
        if (nodesAngle>cutAngle)
            return isValid;
        Vector3D dir = node2.getPoint().toVector3D()
                .sub(node1.getPoint().toVector3D());
        double relAngle = Math.toDegrees(node1.getPoint().toVector3D().angle(dir));
        
        
//check angle stuff goes here...
        return isValid;
        
    }
}
