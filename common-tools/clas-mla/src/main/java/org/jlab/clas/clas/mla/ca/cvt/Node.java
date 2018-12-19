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
     * @return the _radius
     */
    public double getRadius() {
        return _radius;
    }

    /**
     * @param _radius the _radius to set
     */
    public void setRadius(double _radius) {
        this._radius = _radius;
    }

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
     * 
     * @param _point 3d point
     */
    public void setPoint(Point3D _point) {
        this._point = _point;
    }
    
    /**
     * 
     * @param x
     * @param y
     * @param z 
     */
    public void setPoint(double x, double y, double z) {
        this._point = new Point3D(x, y, z);
    }
    /**
     * 
     * @param p 3d point
     * @param radius radius to point
     * @param type detector type:SVT, Z(C)-BMT
     */
    public Node(Point3D p, double radius, DetectorType type) {
        super(p);
        this.setRadius(radius);
        this.setDtype(type);
        // SVT point = (x,y,z); BMTC point = (z, R, 0); BMTZ point = (x,y,0);
        if(type == DetectorType.SVTT)
            this.setPoint(p.x(), p.y(), 0);
        if(type == DetectorType.SVTL)
            this.setPoint(p.z(), radius, 0);
        if(type == DetectorType.BMTZ)
            this.setPoint(p.x(), p.y(), 0);
        if(type == DetectorType.BMTC)
            this.setPoint(p.z(), radius, 0);
    }
    /**
     * SVTL: z, r info ; SVTT: x,y info; BMTZ; BMTC
     */
    public enum DetectorType {
        SVTT, SVTL, BMTZ, BMTC
    }
    private int _id;
    private Point3D _point;
    private double _radius;
    private int _region;
    private DetectorType _dtype;
    
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
    
    

    @Override
    public Object connector(ANode anode1, ANode anode2) {
        Point3D node1 = (Point3D) anode1.getNodeObjectType();
        Point3D node2 = (Point3D) anode2.getNodeObjectType();
        
        Vector3D path = node2.toVector3D().sub(node1.toVector3D());
        
        return path;
    }

    /**
     * @return the _dtype
     */
    public DetectorType getDtype() {
        return _dtype;
    }

    /**
     * @param _dtype the _dtype to set
     */
    public void setDtype(DetectorType _dtype) {
        this._dtype = _dtype;
    }

    /**
     * @return the _region
     */
    public int getRegion() {
        return _region;
    }

    /**
     * @param _region the _region to set
     */
    public void setRegion(int _region) {
        this._region = _region;
    }
 
    
}
