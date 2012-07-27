package org.apache.cassandra.node;

import java.io.Serializable;

public class Tpstats implements Serializable {
    private static final long serialVersionUID = -7848179032971193937L;

    private String poolName;
    private int activeCount;
    private long pendingTasks;
    private long completedTasks;

    /**
     * @return the poolName
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * @param poolName the poolName to set
     */
    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    /**
     * @return the activeCount
     */
    public int getActiveCount() {
        return activeCount;
    }

    /**
     * @param activeCount the activeCount to set
     */
    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    /**
     * @return the pendingTasks
     */
    public long getPendingTasks() {
        return pendingTasks;
    }

    /**
     * @param pendingTasks the pendingTasks to set
     */
    public void setPendingTasks(long pendingTasks) {
        this.pendingTasks = pendingTasks;
    }

    /**
     * @return the completedTasks
     */
    public long getCompletedTasks() {
        return completedTasks;
    }

    /**
     * @param completedTasks the completedTasks to set
     */
    public void setCompletedTasks(long completedTasks) {
        this.completedTasks = completedTasks;
    }
}
