/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.clas.mla.ca;

import java.util.List;

/**
 *
 * @author ziegler
 */
public interface INodeProperties {
    
    public boolean equals(ANode node1, ANode node2);
    public boolean contains(List<ANode> listOfNodes, ANode node);
    public Object connector(ANode node1, ANode node2);
}
