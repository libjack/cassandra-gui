package org.apache.cassandra.node;

import java.io.Serializable;

public class NodeInfo implements Serializable {
    private static final long serialVersionUID = -6585600091642457499L;

    private String endpoint;
    private String load;
    private int generationNumber;
    private long uptime;
    private double memUsed;
    private double memMax;

    /**
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return the load
     */
    public String getLoad() {
        return load;
    }

    /**
     * @param load the load to set
     */
    public void setLoad(String load) {
        this.load = load;
    }

    /**
     * @return the generationNumber
     */
    public int getGenerationNumber() {
        return generationNumber;
    }

    /**
     * @param generationNumber the generationNumber to set
     */
    public void setGenerationNumber(int generationNumber) {
        this.generationNumber = generationNumber;
    }

    /**
     * @return the uptime
     */
    public long getUptime() {
        return uptime;
    }

    /**
     * @param uptime the uptime to set
     */
    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    /**
     * @return the memUsed
     */
    public double getMemUsed() {
        return memUsed;
    }

    /**
     * @param memUsed the memUsed to set
     */
    public void setMemUsed(double memUsed) {
        this.memUsed = memUsed;
    }

    /**
     * @return the memMax
     */
    public double getMemMax() {
        return memMax;
    }

    /**
     * @param memMax the memMax to set
     */
    public void setMemMax(double memMax) {
        this.memMax = memMax;
    }
}
