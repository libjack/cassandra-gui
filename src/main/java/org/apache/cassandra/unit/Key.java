package org.apache.cassandra.unit;

import java.io.Serializable;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

public class Key implements Unit, Serializable {
    private static final long serialVersionUID = 2675876416744532430L;

    private String name;
    private DefaultMutableTreeNode treeNode;
    private boolean superColumn;
    private Map<String, SColumn> sColumns;
    private Map<String, Cell> cells;

    public Key() {
    }

    public Key(String name, Map<String, SColumn> sColumns, Map<String, Cell> cells) {
        this.name = name;
        this.sColumns = sColumns;
        this.cells = cells;
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
     * @return the superColumn
     */
    public boolean isSuperColumn() {
        return superColumn;
    }

    /**
     * @param superColumn the superColumn to set
     */
    public void setSuperColumn(boolean superColumn) {
        this.superColumn = superColumn;
    }

    /**
     * @return the sColumns
     */
    public Map<String, SColumn> getSColumns() {
        return sColumns;
    }

    /**
     * @param sColumns the sColumns to set
     */
    public void setSColumns(Map<String, SColumn> sColumns) {
        this.sColumns = sColumns;
    }

    /**
     * @return the cells
     */
    public Map<String, Cell> getCells() {
        return cells;
    }

    /**
     * @param cells the cells to set
     */
    public void setCells(Map<String, Cell> cells) {
        this.cells = cells;
    }
}