package org.apache.cassandra.node;

import java.io.Serializable;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.unit.Unit;

public class TreeNode implements Serializable {
    private static final long serialVersionUID = -227448839733721587L;

    private Client client;
    private DefaultMutableTreeNode node;
    private DefaultTreeModel treeModel;
    private Unit unit;
    private Map<DefaultMutableTreeNode, Unit> unitMap;
    private Map<String, Unit> keyMap;

    public TreeNode() {
    }

    public TreeNode(Client client,
                    DefaultMutableTreeNode node,
                    DefaultTreeModel treeModel,
                    Unit unit,
                    Map<DefaultMutableTreeNode, Unit> unitMap,
                    Map<String, Unit> keyMap) {
        this.client = client;
        this.node = node;
        this.treeModel = treeModel;
        this.unit = unit;
        this.unitMap = unitMap;
        this.keyMap = keyMap;
    }

    /**
     * @return the client
     */
    public Client getClient() {
        return client;
    }

    /**
     * @param client the client to set
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * @return the node
     */
    public DefaultMutableTreeNode getNode() {
        return node;
    }

    /**
     * @param node the node to set
     */
    public void setNode(DefaultMutableTreeNode node) {
        this.node = node;
    }

    /**
     * @return the treeModel
     */
    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * @param treeModel the treeModel to set
     */
    public void setTreeModel(DefaultTreeModel treeModel) {
        this.treeModel = treeModel;
    }

    /**
     * @return the unit
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * @param unit the unit to set
     */
    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    /**
     * @return the unitMap
     */
    public Map<DefaultMutableTreeNode, Unit> getUnitMap() {
        return unitMap;
    }

    /**
     * @param unitMap the unitMap to set
     */
    public void setUnitMap(Map<DefaultMutableTreeNode, Unit> unitMap) {
        this.unitMap = unitMap;
    }

    /**
     * @return the keyMap
     */
    public Map<String, Unit> getKeyMap() {
        return keyMap;
    }

    /**
     * @param keyMap the keyMap to set
     */
    public void setKeyMap(Map<String, Unit> keyMap) {
        this.keyMap = keyMap;
    }
}
