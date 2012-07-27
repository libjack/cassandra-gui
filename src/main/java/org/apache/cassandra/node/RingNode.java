package org.apache.cassandra.node;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.dht.Token;

public class RingNode implements Serializable {
    private static final long serialVersionUID = 8351368757758010586L;

    private Map<Token, String> rangeMap;
    private List<Token> ranges;
    private List<String> liveNodes;
    private List<String> deadNodes;
    private Map<String, String> loadMap;

    /**
     * @return the rangeMap
     */
    public Map<Token, String> getRangeMap() {
        return rangeMap;
    }

    /**
     * @param rangeMap the rangeMap to set
     */
    public void setRangeMap(Map<Token, String> rangeMap) {
        this.rangeMap = rangeMap;
    }

    /**
     * @return the ranges
     */
    public List<Token> getRanges() {
        return ranges;
    }

    /**
     * @param ranges the ranges to set
     */
    public void setRanges(List<Token> ranges) {
        this.ranges = ranges;
    }

    /**
     * @return the liveNodes
     */
    public List<String> getLiveNodes() {
        return liveNodes;
    }

    /**
     * @param liveNodes the liveNodes to set
     */
    public void setLiveNodes(List<String> liveNodes) {
        this.liveNodes = liveNodes;
    }

    /**
     * @return the deadNodes
     */
    public List<String> getDeadNodes() {
        return deadNodes;
    }

    /**
     * @param deadNodes the deadNodes to set
     */
    public void setDeadNodes(List<String> deadNodes) {
        this.deadNodes = deadNodes;
    }

    /**
     * @return the loadMap
     */
    public Map<String, String> getLoadMap() {
        return loadMap;
    }

    /**
     * @param loadMap the loadMap to set
     */
    public void setLoadMap(Map<String, String> loadMap) {
        this.loadMap = loadMap;
    }
}
