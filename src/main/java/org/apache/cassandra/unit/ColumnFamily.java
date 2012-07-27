package org.apache.cassandra.unit;

import java.util.ArrayList;
import java.util.List;

public class ColumnFamily {
    private int id;
    private String columnFamilyName;
    private String columnType;
    private String comparator;
    private String subcomparator;
    private String comment;
    private String rowsCached;
    private String rowCacheSavePeriod;
    private String keysCached;
    private String keyCacheSavePeriod;
    private String readRepairChance;
    private String gcGrace;
    private String memtableOperations;
    private String memtableThroughput;
    private String memtableFlushAfter;
    private String defaultValidationClass;
    private String minCompactionThreshold;
    private String maxCompactionThreshold;
    private List<ColumnFamilyMetaData> metaDatas = new ArrayList<ColumnFamilyMetaData>();

    public ColumnFamily() {
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the columnFamilyName
     */
    public String getColumnFamilyName() {
        return columnFamilyName;
    }

    /**
     * @param columnFamilyName the columnFamilyName to set
     */
    public void setColumnFamilyName(String columnFamilyName) {
        this.columnFamilyName = columnFamilyName;
    }

    /**
     * @return the columnType
     */
    public String getColumnType() {
        return columnType;
    }

    /**
     * @param columnType the columnType to set
     */
    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    /**
     * @return the comparator
     */
    public String getComparator() {
        return comparator;
    }

    /**
     * @param comparator the comparator to set
     */
    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    /**
     * @return the subcomparator
     */
    public String getSubcomparator() {
        return subcomparator;
    }

    /**
     * @param subcomparator the subcomparator to set
     */
    public void setSubcomparator(String subcomparator) {
        this.subcomparator = subcomparator;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return the rowsCached
     */
    public String getRowsCached() {
        return rowsCached;
    }

    /**
     * @param rowsCached the rowsCached to set
     */
    public void setRowsCached(String rowsCached) {
        this.rowsCached = rowsCached;
    }

    /**
     * @return the rowCacheSavePeriod
     */
    public String getRowCacheSavePeriod() {
        return rowCacheSavePeriod;
    }

    /**
     * @param rowCacheSavePeriod the rowCacheSavePeriod to set
     */
    public void setRowCacheSavePeriod(String rowCacheSavePeriod) {
        this.rowCacheSavePeriod = rowCacheSavePeriod;
    }

    /**
     * @return the keysCached
     */
    public String getKeysCached() {
        return keysCached;
    }

    /**
     * @param keysCached the keysCached to set
     */
    public void setKeysCached(String keysCached) {
        this.keysCached = keysCached;
    }

    /**
     * @return the keyCacheSavePeriod
     */
    public String getKeyCacheSavePeriod() {
        return keyCacheSavePeriod;
    }

    /**
     * @param keyCacheSavePeriod the keyCacheSavePeriod to set
     */
    public void setKeyCacheSavePeriod(String keyCacheSavePeriod) {
        this.keyCacheSavePeriod = keyCacheSavePeriod;
    }

    /**
     * @return the readRepairChance
     */
    public String getReadRepairChance() {
        return readRepairChance;
    }

    /**
     * @param readRepairChance the readRepairChance to set
     */
    public void setReadRepairChance(String readRepairChance) {
        this.readRepairChance = readRepairChance;
    }

    /**
     * @return the gcGrace
     */
    public String getGcGrace() {
        return gcGrace;
    }

    /**
     * @param gcGrace the gcGrace to set
     */
    public void setGcGrace(String gcGrace) {
        this.gcGrace = gcGrace;
    }

    /**
     * @return the memtableOperations
     */
    public String getMemtableOperations() {
        return memtableOperations;
    }

    /**
     * @param memtableOperations the memtableOperations to set
     */
    public void setMemtableOperations(String memtableOperations) {
        this.memtableOperations = memtableOperations;
    }

    /**
     * @return the memtableThroughput
     */
    public String getMemtableThroughput() {
        return memtableThroughput;
    }

    /**
     * @param memtableThroughput the memtableThroughput to set
     */
    public void setMemtableThroughput(String memtableThroughput) {
        this.memtableThroughput = memtableThroughput;
    }

    /**
     * @return the memtableFlushAfter
     */
    public String getMemtableFlushAfter() {
        return memtableFlushAfter;
    }

    /**
     * @param memtableFlushAfter the memtableFlushAfter to set
     */
    public void setMemtableFlushAfter(String memtableFlushAfter) {
        this.memtableFlushAfter = memtableFlushAfter;
    }

    /**
     * @return the defaultValidationClass
     */
    public String getDefaultValidationClass() {
        return defaultValidationClass;
    }

    /**
     * @param defaultValidationClass the defaultValidationClass to set
     */
    public void setDefaultValidationClass(String defaultValidationClass) {
        this.defaultValidationClass = defaultValidationClass;
    }

    /**
     * @return the minCompactionThreshold
     */
    public String getMinCompactionThreshold() {
        return minCompactionThreshold;
    }

    /**
     * @param minCompactionThreshold the minCompactionThreshold to set
     */
    public void setMinCompactionThreshold(String minCompactionThreshold) {
        this.minCompactionThreshold = minCompactionThreshold;
    }

    /**
     * @return the maxCompactionThreshold
     */
    public String getMaxCompactionThreshold() {
        return maxCompactionThreshold;
    }

    /**
     * @param maxCompactionThreshold the maxCompactionThreshold to set
     */
    public void setMaxCompactionThreshold(String maxCompactionThreshold) {
        this.maxCompactionThreshold = maxCompactionThreshold;
    }

    /**
     * @return the metaDatas
     */
    public List<ColumnFamilyMetaData> getMetaDatas() {
        return metaDatas;
    }

    /**
     * @param metaDatas the metaDatas to set
     */
    public void setMetaDatas(List<ColumnFamilyMetaData> metaDatas) {
        this.metaDatas = metaDatas;
    }
}
