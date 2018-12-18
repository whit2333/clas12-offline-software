/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.clas.mla.ca.cvt;

import java.util.List;
import org.jlab.clas.clas.mla.ca.ANode;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;

/**
 *
 * @author ziegler
 */
public final class Node extends ANode {

    /**
     * @return the _id
     */
    public int getId() {
        return _id;
    }

    /**
     * @param _id the _id to set
     */
    public void setId(int _id) {
        this._id = _id;
    }

    /**
     * @return the _point
     */
    public Point3D getPoint() {
        return _point;
    }

    /**
     * @param _point the _point to set
     */
    public void setPoint(Point3D _point) {
        this._point = _point;
    }
    public Node(Point3D p) {
        super(p);
        this.setPoint(p);
    }
    private int _id;
    private Point3D _point;
    
    @Override
    public boolean equals(ANode anode1, ANode anode2) {
        Point3D node1 = (Point3D) anode1.getNodeObjectType();
        Point3D node2 = (Point3D) anode2.getNodeObjectType();
        
        boolean areEqual = false;
        
        if(node1.x()==node2.x() && node1.y()==node2.y() && node1.z()==node2.z()) {
            areEqual = true;
        }
        
        return areEqual;
    }

    @Override
    public boolean contains(List<ANode> listOfaNodes, ANode anode) {
        
        return listOfaNodes.stream().anyMatch((anode1) -> (this.equals(anode1, anode)));
    }
    
    private Vector3D _path;
    
    /**
     * @return the _path
     */
    public Vector3D getPath() {
        return _path;
    }

   /**
    * sets the path between nodes
    * @param anode1
    * @param anode2 
    */
    
    public void setPath(ANode anode1, ANode anode2) {
        
        this._path = (Vector3D) connector( anode1, anode2 );
    }

    @Override
    public Object connector(ANode anode1, ANode anode2) {
        Point3D node1 = (Point3D) anode1.getNodeObjectType();
        Point3D node2 = (Point3D) anode2.getNodeObjectType();
        
        Vector3D path = node2.toVector3D().sub(node1.toVector3D());
        
        return path;
    }
 
    
}
