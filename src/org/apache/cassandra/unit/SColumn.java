package org.apache.cassandra.unit;

import java.io.Serializable;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

public class SColumn implements Unit, Serializable {
    private static final long serialVersionUID = -8041985483479505351L;

    private Unit parent;
    private String name;
    private DefaultMutableTreeNode treeNode;
    private Map<String, Cell> cells;

    public SColumn() {
    }

    public SColumn(Unit parent, String name, Map<String, Cell> cells) {
        this.parent = parent;
        this.name = name;
        this.cells = cells;
    }

    /**
     * @return the parent
     */
    public Unit getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(Unit parent) {
        this.parent = parent;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the treeNode
     */
    public DefaultMutableTreeNode getTreeNode() {
        return treeNode;
    }

    /**
     * @param treeNode the treeNode to set
     */
    public void setTreeNode(DefaultMutableTreeNode treeNode) {
        this.treeNode = treeNode;
    }

    /**
     * @return the cells
     */
    public Map<String, Cell> getCells() {
        return cells;
    }

    /**
     * @param keys the keys to set
     */
    public void setCells(Map<String, Cell> cells) {
        this.cells = cells;
    }
}
