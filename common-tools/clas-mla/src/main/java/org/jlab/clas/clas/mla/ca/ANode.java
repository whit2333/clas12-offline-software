/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.clas.mla.ca;

/**
 *
 * @author ziegler
 */
public abstract class ANode implements INodeProperties {

    /**
     * @return the _nodeObjectType
     */
    public Object getNodeObjectType() {
        return _nodeObjectType;
    }

    /**
     * @param _nodeObjectType the _nodeObjectType to set
     */
    public void setNodeObjectType(Object _nodeObjectType) {
        this._nodeObjectType = _nodeObjectType;
    }

    private Object _nodeObjectType;
    
    public ANode(Object nodeObject) {
        this.setNodeObjectType( nodeObject);
        
    }
    
}
