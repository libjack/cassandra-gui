package org.apache.cassandra.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.cassandra.concurrent.IExecutorMBean;
import org.apache.cassandra.concurrent.JMXEnabledThreadPoolExecutorMBean;
import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.db.marshal.AbstractCompositeType;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.node.NodeInfo;
import org.apache.cassandra.node.RingNode;
import org.apache.cassandra.node.Tpstats;
import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnDef;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ColumnPath;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.thrift.KeySlice;
import org.apache.cassandra.thrift.KsDef;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.SchemaDisagreementException;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.SuperColumn;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.TokenRange;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.cassandra.unit.Cell;
import org.apache.cassandra.unit.ColumnFamily;
import org.apache.cassandra.unit.ColumnFamilyMetaData;
import org.apache.cassandra.unit.Key;
import org.apache.cassandra.unit.SColumn;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * Client class to interact with Cassandara cluster
 *
 */
public class Client {
    public static final String DEFAULT_THRIFT_HOST = "localhost";
    public static final int DEFAULT_THRIFT_PORT = 9160;
    public static final int DEFAULT_JMX_PORT = 7199;

    public enum ColumnType {
        SUPER("Super"),
        STANDARD("Standard");

        private String type;

        private ColumnType(String type) {
            this.type = type;
        }

        public String toString() {
            return type;
        }
    }

    private TTransport transport;
    private TProtocol protocol;
    private Cassandra.Client client;
    private NodeProbe probe;

    private boolean connected = false;
    private String host;
    private int thriftPort;
    private int jmxPort;

    private String keyspace;
    private String columnFamily;
    private boolean superColumn;

    public Client() {
        this(DEFAULT_THRIFT_HOST, DEFAULT_THRIFT_PORT, DEFAULT_JMX_PORT);
    }

    public Client(String host) {
        this(host, DEFAULT_THRIFT_PORT, DEFAULT_JMX_PORT);
    }

    public Client(String host, int thriftPort, int jmxPort) {
        this.host = host;
        this.thriftPort = thriftPort;
        this.jmxPort = jmxPort;
    }

    public void connect()
            throws TTransportException, IOException, InterruptedException {
        if (!connected) {
            // Updating the transport to Framed one as it has been depreciated with Cassandra 0.7.0
            transport = new TFramedTransport(new TSocket(host, thriftPort));
            protocol = new TBinaryProtocol(transport);
            client = new Cassandra.Client(protocol);
            probe = new NodeProbe(host, jmxPort);
            transport.open();
            connected = true;
        }
    }

    public void disconnect() {
        if (connected) {
            transport.close();
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String describeClusterName() throws TException {
        return client.describe_cluster_name();
    }

    public String descriveVersion() throws TException {
        return client.describe_version();
    }

    public String describeSnitch() throws TException {
        return client.describe_snitch();
    }

    public Map<String,List<String>> describeSchemaVersions() throws InvalidRequestException, TException {
        return client.describe_schema_versions();
    }

    public String describePartitioner() throws TException {
        return client.describe_partitioner();
    }

    public List<TokenRange> describeRing(String keyspace)
            throws TException, InvalidRequestException {
        this.keyspace = keyspace;
        return client.describe_ring(keyspace);
    }

    public RingNode listRing() {
        RingNode r = new RingNode();
        r.setRangeMap(probe.getTokenToEndpointMap());
        List<Token> ranges = new ArrayList<Token>(r.getRangeMap().keySet());
        Collections.sort(ranges);
        r.setRanges(ranges);

        r.setLiveNodes(probe.getLiveNodes());
        r.setDeadNodes(probe.getUnreachableNodes());
        r.setLoadMap(probe.getLoadMap());

        return r;
    }

    public NodeInfo getNodeInfo(String endpoint) throws IOException, InterruptedException {
        NodeProbe p = new NodeProbe(endpoint, jmxPort);

        NodeInfo ni = new NodeInfo();
        ni.setEndpoint(endpoint);
        ni.setLoad(p.getLoadString());
        ni.setGenerationNumber(p.getCurrentGenerationNumber());
        ni.setUptime(p.getUptime() / 1000);

        MemoryUsage heapUsage = p.getHeapMemoryUsage();
        ni.setMemUsed((double) heapUsage.getUsed() / (1024 * 1024));
        ni.setMemMax((double) heapUsage.getMax() / (1024 * 1024));

        return ni;
    }

    public List<Tpstats> getTpstats(String endpoint) throws IOException, InterruptedException {
        List<Tpstats> l = new ArrayList<Tpstats>();

        NodeProbe p = new NodeProbe(endpoint, jmxPort);
        Iterator<Entry<String, JMXEnabledThreadPoolExecutorMBean>> threads = p.getThreadPoolMBeanProxies();
        for (;threads.hasNext();) {
            Entry<String, JMXEnabledThreadPoolExecutorMBean> thread = threads.next();

            Tpstats tp = new Tpstats();
            tp.setPoolName(thread.getKey());

            IExecutorMBean threadPoolProxy = thread.getValue();
            tp.setActiveCount(threadPoolProxy.getActiveCount());
            tp.setPendingTasks(threadPoolProxy.getPendingTasks());
            tp.setCompletedTasks(threadPoolProxy.getCompletedTasks());
            l.add(tp);
        }

        return l;
    }

    public List<KsDef> getKeyspaces() throws TException, InvalidRequestException {
        return client.describe_keyspaces();
    }

    public KsDef describeKeyspace(String keyspaceName) throws NotFoundException, InvalidRequestException, TException {
        return client.describe_keyspace(keyspaceName);
    }

    public void addKeyspace(String keyspaceName,
                            String strategy,
                            Map<String, String> strategyOptions,
                            int replicationFactgor) throws InvalidRequestException, TException, SchemaDisagreementException {
        KsDef ksDef = new KsDef(keyspaceName, strategy, new LinkedList<CfDef>());
        ksDef.setReplication_factor(replicationFactgor); // TODO should be provided in strategy options
        if (strategyOptions != null) {
            ksDef.setStrategy_options(strategyOptions);
        }

        client.system_add_keyspace(ksDef);
    }

    public void updateKeyspace(String keyspaceName,
                               String strategy,
                               Map<String, String> strategyOptions,
                               int replicationFactgor) throws InvalidRequestException, TException, SchemaDisagreementException {
        KsDef ksDef = new KsDef(keyspaceName, strategy, new LinkedList<CfDef>());
        ksDef.setReplication_factor(replicationFactgor); // TODO should be provided in strategy options
        if (strategyOptions != null) {
            ksDef.setStrategy_options(strategyOptions);
        }

        client.system_update_keyspace(ksDef);
    }

    public void dropKeyspace(String keyspaceName) throws InvalidRequestException, TException, SchemaDisagreementException {
        client.system_drop_keyspace(keyspaceName);
    }

    public void addColumnFamily(String keyspaceName,
                                ColumnFamily cf) throws InvalidRequestException, TException, SchemaDisagreementException {
        this.keyspace = keyspaceName;
        CfDef cfDef = new CfDef(keyspaceName, cf.getColumnFamilyName());
        cfDef.setColumn_type(cf.getColumnType());

        if (!isEmpty(cf.getComparator())) {
            cfDef.setComparator_type(cf.getComparator());
        }

        if (cf.getComparator().equals(ColumnType.SUPER)) {
            if (!isEmpty(cf.getSubcomparator())) {
                cfDef.setSubcomparator_type(cf.getSubcomparator());
            }
        }

        if (!isEmpty(cf.getComment())) {
            cfDef.setComment(cf.getComment());
        }

        if (!isEmpty(cf.getRowsCached())) {
            cfDef.setRow_cache_size(Double.valueOf(cf.getRowsCached()));
        }

        if (!isEmpty(cf.getRowCacheSavePeriod())) {
            cfDef.setRow_cache_save_period_in_seconds(Integer.valueOf(cf.getRowCacheSavePeriod()));
        }

        if (!isEmpty(cf.getKeysCached())) {
            cfDef.setKey_cache_size(Double.valueOf(cf.getKeysCached()));
        }

        if (!isEmpty(cf.getKeyCacheSavePeriod())) {
            cfDef.setKey_cache_save_period_in_seconds(Integer.valueOf(cf.getKeyCacheSavePeriod()));
        }

        if (!isEmpty(cf.getReadRepairChance())) {
            cfDef.setRead_repair_chance(Double.valueOf(cf.getReadRepairChance()));
        }

        if (!isEmpty(cf.getGcGrace())) {
            cfDef.setGc_grace_seconds(Integer.valueOf(cf.getGcGrace()));
        }

        if (!cf.getMetaDatas().isEmpty()) {
            List<ColumnDef> l = new ArrayList<ColumnDef>();
            for (ColumnFamilyMetaData metaData : cf.getMetaDatas()) {
                ColumnDef cd = new ColumnDef();
                cd.setName(metaData.getColumnName().getBytes());

                if (metaData.getValiDationClass() != null) {
                    cd.setValidation_class(metaData.getValiDationClass());
                }
                if (metaData.getIndexType() != null) {
                    cd.setIndex_type(metaData.getIndexType());
                }
                if (metaData.getIndexName() != null) {
                    cd.setIndex_name(metaData.getIndexName());
                }

                l.add(cd);
            }
            cfDef.setColumn_metadata(l);
        }

        if (!isEmpty(cf.getMemtableOperations())) {
            // FIXME @Deprecated cfDef.setMemtable_operations_in_millions(Double.valueOf(cf.getMemtableOperations()));
        }

        if (!isEmpty(cf.getMemtableThroughput())) {
            // FIXME @Deprecated cfDef.setMemtable_throughput_in_mb(Integer.valueOf(cf.getMemtableThroughput()));
        }

        if (!isEmpty(cf.getMemtableFlushAfter())) {
            // FIXME @Deprecated cfDef.setMemtable_flush_after_mins(Integer.valueOf(cf.getMemtableFlushAfter()));
        }

        if (!isEmpty(cf.getDefaultValidationClass())) {
            cfDef.setDefault_validation_class(cf.getDefaultValidationClass());
        }

        if (!isEmpty(cf.getMinCompactionThreshold())) {
            cfDef.setMin_compaction_threshold(Integer.valueOf(cf.getMinCompactionThreshold()));
        }

        if (!isEmpty(cf.getMaxCompactionThreshold())) {
            cfDef.setMax_compaction_threshold(Integer.valueOf(cf.getMaxCompactionThreshold()));
        }

        client.set_keyspace(keyspaceName);
        client.system_add_column_family(cfDef);
    }

    public void updateColumnFamily(String keyspaceName,
                                   ColumnFamily cf) throws InvalidRequestException, TException, SchemaDisagreementException {
        this.keyspace = keyspaceName;
        CfDef cfDef = new CfDef(keyspaceName, cf.getColumnFamilyName());
        cfDef.setId(cf.getId());
        cfDef.setColumn_type(cf.getColumnType());

        if (!isEmpty(cf.getComparator())) {
            cfDef.setComparator_type(cf.getComparator());
        }

        if (cf.getColumnType().equalsIgnoreCase(ColumnType.SUPER.name())) {
            if (!isEmpty(cf.getSubcomparator())) {
                cfDef.setSubcomparator_type(cf.getSubcomparator());
            }
        }

        if (!isEmpty(cf.getComment())) {
            cfDef.setComment(cf.getComment());
        }

        if (!isEmpty(cf.getRowsCached())) {
            cfDef.setRow_cache_size(Double.valueOf(cf.getRowsCached()));
        }

        if (!isEmpty(cf.getRowCacheSavePeriod())) {
            cfDef.setRow_cache_save_period_in_seconds(Integer.valueOf(cf.getRowCacheSavePeriod()));
        }

        if (!isEmpty(cf.getKeysCached())) {
            cfDef.setKey_cache_size(Double.valueOf(cf.getKeysCached()));
        }

        if (!isEmpty(cf.getKeyCacheSavePeriod())) {
            cfDef.setKey_cache_save_period_in_seconds(Integer.valueOf(cf.getKeyCacheSavePeriod()));
        }

        if (!isEmpty(cf.getReadRepairChance())) {
            cfDef.setRead_repair_chance(Double.valueOf(cf.getReadRepairChance()));
        }

        if (!isEmpty(cf.getGcGrace())) {
            cfDef.setGc_grace_seconds(Integer.valueOf(cf.getGcGrace()));
        }

        if (!cf.getMetaDatas().isEmpty()) {
            List<ColumnDef> l = new ArrayList<ColumnDef>();
            for (ColumnFamilyMetaData metaData : cf.getMetaDatas()) {
                ColumnDef cd = new ColumnDef();
                cd.setName(metaData.getColumnName().getBytes());

                if (metaData.getValiDationClass() != null) {
                    cd.setValidation_class(metaData.getValiDationClass());
                }
                if (metaData.getIndexType() != null) {
                    cd.setIndex_type(metaData.getIndexType());
                }
                if (metaData.getIndexName() != null) {
                    cd.setIndex_name(metaData.getIndexName());
                }

                l.add(cd);
            }
            cfDef.setColumn_metadata(l);
        }

        if (!isEmpty(cf.getMemtableOperations())) {
            // FIXME @Deprecated cfDef.setMemtable_operations_in_millions(Double.valueOf(cf.getMemtableOperations()));
        }

        if (!isEmpty(cf.getMemtableThroughput())) {
            // FIXME @Deprecated cfDef.setMemtable_throughput_in_mb(Integer.valueOf(cf.getMemtableThroughput()));
        }

        if (!isEmpty(cf.getMemtableFlushAfter())) {
            // FIXME @Deprecated cfDef.setMemtable_flush_after_mins(Integer.valueOf(cf.getMemtableFlushAfter()));
        }

        if (!isEmpty(cf.getDefaultValidationClass())) {
            cfDef.setDefault_validation_class(cf.getDefaultValidationClass());
        }

        if (!isEmpty(cf.getMinCompactionThreshold())) {
            cfDef.setMin_compaction_threshold(Integer.valueOf(cf.getMinCompactionThreshold()));
        }

        if (!isEmpty(cf.getMaxCompactionThreshold())) {
            cfDef.setMax_compaction_threshold(Integer.valueOf(cf.getMaxCompactionThreshold()));
        }

        client.set_keyspace(keyspaceName);
        client.system_update_column_family(cfDef);
    }

    public void dropColumnFamily(String keyspaceName, String columnFamilyName) throws InvalidRequestException, TException, SchemaDisagreementException {
        this.keyspace = keyspaceName;
        client.set_keyspace(keyspaceName);
        client.system_drop_column_family(columnFamilyName);
    }

    public void truncateColumnFamily(String keyspaceName, String columnFamilyName)
            throws InvalidRequestException, TException, UnavailableException {
        this.keyspace = keyspaceName;
        this.columnFamily = columnFamilyName;
        client.set_keyspace(keyspaceName);
        client.truncate(columnFamilyName);
    }

    /**
     *
     * Retrieve Column metadata from a given keyspace
     *
     * @param keyspace
     * @param columnFamily
     * @return
     * @throws NotFoundException
     * @throws TException
     * @throws InvalidRequestException
     */
    public Map<String, Object> getColumnFamily(String keyspace, String columnFamily)
            throws NotFoundException, TException, InvalidRequestException {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;

        for (Iterator<CfDef> cfIterator = client.describe_keyspace(keyspace).getCf_defsIterator(); cfIterator.hasNext();) {
            CfDef next = cfIterator.next();
            if (columnFamily.equalsIgnoreCase(next.getName())) {
                Map<String, Object> columnMetadata = new HashMap<String, Object>();

                CfDef._Fields[] fields = CfDef._Fields.values();

                for (int i = 0; i < fields.length; i++) {
                    CfDef._Fields field = fields[i];
                    // FIXME jdl changed back to objects, will just have to track down other NPE, etc..
                    // using string concat to avoin NPE, if the value is not null
                    // need to find an elegant solution
                    columnMetadata.put(field.name(), next.getFieldValue(field));
                }

                return columnMetadata;
            }
        }
        System.out.println("returning null");
        return null;
    }

    public ColumnFamily getColumnFamilyBean(String keyspace, String columnFamily)
            throws NotFoundException, TException, InvalidRequestException, UnsupportedEncodingException {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;

        for (Iterator<CfDef> cfIterator = client.describe_keyspace(keyspace).getCf_defsIterator(); cfIterator.hasNext();) {
            CfDef cd = cfIterator.next();
            if (columnFamily.equalsIgnoreCase(cd.getName())) {
                ColumnFamily cf = new ColumnFamily();
                cf.setId(cd.getId());
                cf.setColumnFamilyName(cd.getName());
                cf.setColumnType(cd.getColumn_type());
                cf.setComparator(cd.getComparator_type());
                cf.setSubcomparator(cd.getSubcomparator_type());
                cf.setComment(cd.getComment());
                cf.setRowsCached(String.valueOf(cd.getRow_cache_size()));
                cf.setRowCacheSavePeriod(String.valueOf(cd.getRow_cache_save_period_in_seconds()));
                cf.setKeysCached(String.valueOf(cd.getKey_cache_size()));
                cf.setKeyCacheSavePeriod(String.valueOf(cd.getKey_cache_save_period_in_seconds()));
                cf.setReadRepairChance(String.valueOf(cd.getRead_repair_chance()));
                cf.setGcGrace(String.valueOf(cd.getGc_grace_seconds()));
                // FIXME @Deprecated cf.setMemtableOperations(String.valueOf(cd.getMemtable_operations_in_millions()));
                // FIXME @Deprecated cf.setMemtableThroughput(String.valueOf(cd.getMemtable_throughput_in_mb()));
                // FIXME @Deprecated cf.setMemtableFlushAfter(String.valueOf(cd.getMemtable_flush_after_mins()));
                cf.setDefaultValidationClass(cd.getDefault_validation_class());
                cf.setMinCompactionThreshold(String.valueOf(cd.getMin_compaction_threshold()));
                cf.setMaxCompactionThreshold(String.valueOf(cd.getMax_compaction_threshold()));
                for (ColumnDef cdef : cd.getColumn_metadata()) {
                    ColumnFamilyMetaData cfmd = new ColumnFamilyMetaData();
                    cfmd.setColumnName(new String(cdef.getName(), "UTF8"));
                    cfmd.setValiDationClass(cdef.getValidation_class());
                    cfmd.setIndexType(cdef.getIndex_type());
                    cfmd.setIndexName(cdef.getIndex_name());
                    cf.getMetaDatas().add(cfmd);
                }

                return cf;
            }
        }

        System.out.println("returning null");
        return null;
    }

    public Set<String> getColumnFamilys(String keyspace)
            throws NotFoundException, TException, InvalidRequestException {
        this.keyspace = keyspace;

        Set<String> s = new TreeSet<String>();

        for (Iterator<CfDef> cfIterator = client.describe_keyspace(keyspace).getCf_defsIterator(); cfIterator.hasNext();) {
           CfDef next =  cfIterator.next();
           s.add(next.getName());
        }
        return s;
    }

    public int countColumnsRecord(String keyspace, String columnFamily, String key)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException, NotFoundException {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        Map<String, Object> cfdata = getColumnFamily(keyspace, columnFamily);

        ColumnParent colParent = new ColumnParent(columnFamily);
        //TODO - Verify if its working fine
        return client.get_count(getAsBytes(key, cfdata.get("KEY_VALIDATION_CLASS").toString()), colParent, null, ConsistencyLevel.ONE);
    }

    public int countSuperColumnsRecord(String keyspace, String columnFamily, String superColumn, String key)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException, NotFoundException {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        Map<String, Object> cfdata = getColumnFamily(keyspace, columnFamily);

        ColumnParent colParent = new ColumnParent(columnFamily);
        colParent.setSuper_column(superColumn.getBytes());
        // TODO - verify if its working fine
        return client.get_count(getAsBytes(key, cfdata.get("KEY_VALIDATION_CLASS").toString()), colParent, null, ConsistencyLevel.ONE);
    }

    @SuppressWarnings("unchecked")
	public Date insertColumn(String keyspace,
                             String columnFamily,
                             String key,
                             String superColumn,
                             String column,
                             String value)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException, NotFoundException {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        Map<String, Object> cfdata = getColumnFamily(keyspace, columnFamily);

        ColumnParent parent;

        parent = new ColumnParent(columnFamily);
        long timestamp = System.currentTimeMillis() * 1000;
        Column col = null;
        if (superColumn != null) {
        	parent.setSuper_column(getAsBytes(superColumn, cfdata.get("COMPARATOR_TYPE").toString()));
        	col = new Column(getAsBytes(column, cfdata.get("SUBCOMPARATOR_TYPE").toString()));
        } else {
        	col = new Column(getAsBytes(column, cfdata.get("COMPARATOR_TYPE").toString()));
        }

        // FIXME.... need to somehow allow a default of UTF8, helps to enter values, perhaps when default is BYTES? cause want to Long if that is the default
        // TODO add functionality similar to cli 'assume' command, doesn't change column family definition, only affects the gui, would want for reads as well
        // can easily override with metadata...
        col.setValue(getAsBytes(value, getValidationType(col.bufferForName(), (List<ColumnDef>)cfdata.get("COLUMN_METADATA"), cfdata.get("DEFAULT_VALIDATION_CLASS").toString())));
        col.setTimestamp(timestamp);

        client.set_keyspace(keyspace);
        client.insert(getAsBytes(key, cfdata.get("KEY_VALIDATION_CLASS").toString()), parent, col, ConsistencyLevel.ONE);

        return new Date(timestamp / 1000);
    }

    public void removeKey(String keyspace, String columnFamily, String key)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException, NotFoundException {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        Map<String, Object> cfdata = getColumnFamily(keyspace, columnFamily);

        ColumnPath colPath = new ColumnPath(columnFamily);
        long timestamp = System.currentTimeMillis() * 1000;

        client.set_keyspace(keyspace);
        client.remove(getAsBytes(key, cfdata.get("KEY_VALIDATION_CLASS").toString()), colPath, timestamp, ConsistencyLevel.ONE);
    }

    public void removeSuperColumn(String keyspace, String columnFamily, String key, String superColumn)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException, NotFoundException {
        ColumnPath colPath = new ColumnPath(columnFamily);
        colPath.setSuper_column(superColumn.getBytes());
        long timestamp = System.currentTimeMillis() * 1000;
        Map<String, Object> cfdata = getColumnFamily(keyspace, columnFamily);

        client.set_keyspace(keyspace);
        client.remove(getAsBytes(key, cfdata.get("KEY_VALIDATION_CLASS").toString()), colPath, timestamp, ConsistencyLevel.ONE);
    }

    public void removeColumn(String keyspace, String columnFamily, String key, String column)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException, NotFoundException {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        Map<String, Object> cfdata = getColumnFamily(keyspace, columnFamily);

        ColumnPath colPath = new ColumnPath(columnFamily);
        //colPath.setColumn(column.getBytes()); 
        colPath.setColumn(getAsBytes(column, cfdata.get("COMPARATOR_TYPE").toString()));
        long timestamp = System.currentTimeMillis() * 1000;

        client.set_keyspace(keyspace);
        client.remove(getAsBytes(key, cfdata.get("KEY_VALIDATION_CLASS").toString()), colPath, timestamp, ConsistencyLevel.ONE);
    }

    public void removeColumn(String keyspace, String columnFamily, String key, String superColumn, String column)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException, NotFoundException {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        Map<String, Object> cfdata = getColumnFamily(keyspace, columnFamily);

        ColumnPath colPath = new ColumnPath(columnFamily);
        colPath.setSuper_column(getAsBytes(superColumn, cfdata.get("COMPARATOR_TYPE").toString()));
        colPath.setColumn(getAsBytes(column, cfdata.get("SUBCOMPARATOR_TYPE").toString()));
        
        long timestamp = System.currentTimeMillis() * 1000;

        client.set_keyspace(keyspace);
        client.remove(getAsBytes(key, cfdata.get("KEY_VALIDATION_CLASS").toString()), colPath, timestamp, ConsistencyLevel.ONE);
    }

    public Map<String, Key> getKey(String keyspace, String columnFamily, String superColumn, String key)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException, UnsupportedEncodingException, NotFoundException {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        Map<String, Object> cfdata = getColumnFamily(keyspace, columnFamily);

        Map<String, Key> m = new TreeMap<String, Key>();

        ColumnParent columnParent = new ColumnParent(columnFamily);
        if (superColumn != null) {
            columnParent.setSuper_column(superColumn.getBytes());
        }

        SliceRange sliceRange = new SliceRange();
        sliceRange.setStart(new byte[0]);
        sliceRange.setFinish(new byte[0]);

        SlicePredicate slicePredicate = new SlicePredicate();
        slicePredicate.setSlice_range(sliceRange);

        List<ColumnOrSuperColumn> l = null;
        try {
            l = client.get_slice(getAsBytes(key, cfdata.get("KEY_VALIDATION_CLASS").toString()), columnParent, slicePredicate, ConsistencyLevel.ONE);
        } catch (Exception e) {
            return m;
        }

        Key k = new Key(key, new TreeMap<String, SColumn>(), new TreeMap<String, Cell>());
        for (ColumnOrSuperColumn column : l) {
            k.setSuperColumn(column.isSetSuper_column());
            if (column.isSetSuper_column()) {
                SuperColumn scol = column.getSuper_column();
                String scolName = getAsString(scol.bufferForName(), cfdata.get("COMPARATOR_TYPE").toString());
                SColumn s = new SColumn(k, scolName, new TreeMap<String, Cell>());
                for (Column col : scol.getColumns()) {
                    String name = getAsString(col.bufferForName(), cfdata.get("SUBCOMPARATOR_TYPE").toString());
					@SuppressWarnings("unchecked")
					String val = getAsString(col.bufferForValue(), getValidationType(col.bufferForName(), (List<ColumnDef>)cfdata.get("COLUMN_METADATA"), cfdata.get("DEFAULT_VALIDATION_CLASS").toString()));
                    Cell c = new Cell(s,
                                      name, val,
                                      new Date(col.getTimestamp() / 1000));
                    s.getCells().put(c.getName(), c);
                }

                k.getSColumns().put(s.getName(), s);
            } else {
                Column col = column.getColumn();
                String name = getAsString(col.bufferForName(), cfdata.get("COMPARATOR_TYPE").toString());
                @SuppressWarnings("unchecked")
				String val = getAsString(col.bufferForValue(), getValidationType(col.bufferForName(), (List<ColumnDef>)cfdata.get("COLUMN_METADATA"), cfdata.get("DEFAULT_VALIDATION_CLASS").toString()));
                Cell c = new Cell(k,
                                  name, val,
                                  new Date(col.getTimestamp() / 1000));
                k.getCells().put(c.getName(), c);
            }

            m.put(k.getName(), k);
        }

        return m;
    }
    
    @SuppressWarnings("rawtypes")
	private ByteBuffer getAsBytes(String str, String marshalType) {
    	ByteBuffer bytes = null;

    	try {
			AbstractType abstractType = TypeParser.parse(marshalType);
			if (str.isEmpty() && abstractType instanceof AbstractCompositeType) {
				// count the types and add empty : for each ... silly way but perhaps only way for now
				// form will be something like "CompositeType(UTF8Type, LongType, ....) 
				int i = marshalType.indexOf('('); // don't start till open paren (
				while ((i = marshalType.indexOf(',',i)) > 0) {
					str += ":";
					i++;
				}
			}
			bytes = abstractType.fromString(str);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
    	return bytes;
    }

    @SuppressWarnings("rawtypes")
	private String getAsString(java.nio.ByteBuffer bytes, String marshalType) {
    	String val = null;
    	//val = java.nio.charset.Charset.defaultCharset().decode(bytes.asReadOnlyBuffer()).toString();
    	try {
			AbstractType abstractType = TypeParser.parse(marshalType);
			val = abstractType.getString(bytes);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
    	return val;
    }
    
    private String getValidationType(java.nio.ByteBuffer nameBytes, List<ColumnDef> columns, String defaultType) {
    	String validationType = defaultType;
    	if (columns != null) {
    		for (ColumnDef cdef: columns) {
    			if (nameBytes.compareTo(cdef.bufferForName()) == 0) {
    				validationType = cdef.getValidation_class();
    			}
    		}
    	} 
    	
    	return validationType;
    }
    
    public Map<String, Key> listKeyAndValues(String keyspace, String columnFamily, String startKey, String endKey, int rows)
            throws Exception {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;
        Map<String, Object> cfdata = getColumnFamily(keyspace, columnFamily);
        //System.out.println(cfdata.toString());

        Map<String, Key> m = new TreeMap<String, Key>();

        ColumnParent columnParent = new ColumnParent(columnFamily);

        KeyRange keyRange = new KeyRange(rows);
        
        keyRange.setStart_key(getAsBytes(startKey, cfdata.get("KEY_VALIDATION_CLASS").toString()));
        keyRange.setEnd_key(getAsBytes(endKey, cfdata.get("KEY_VALIDATION_CLASS").toString()));

        SliceRange sliceRange = new SliceRange();
        sliceRange.setStart(new byte[0]);
        sliceRange.setFinish(new byte[0]);

        SlicePredicate slicePredicate = new SlicePredicate();
        slicePredicate.setSlice_range(sliceRange);
        client.set_keyspace(keyspace);

        List<KeySlice> keySlices = null;
        try {
            keySlices = client.get_range_slices(columnParent, slicePredicate, keyRange, ConsistencyLevel.ONE);
        } catch (UnavailableException e) {
            return m;
        }

        for (KeySlice keySlice : keySlices) {
        	String keyName = getAsString(keySlice.bufferForKey(), cfdata.get("KEY_VALIDATION_CLASS").toString());
			Key key = new Key(keyName, new TreeMap<String, SColumn>(), new TreeMap<String, Cell>());

            for (ColumnOrSuperColumn column : keySlice.getColumns()) {
                key.setSuperColumn(column.isSetSuper_column());
                if (column.isSetSuper_column()) {
                    SuperColumn scol = column.getSuper_column();
                    String scolName = getAsString(scol.bufferForName(), cfdata.get("COMPARATOR_TYPE").toString());
                    SColumn s = new SColumn(key, scolName, new TreeMap<String, Cell>());
                    for (Column col : scol.getColumns()) {
                        String name = getAsString(col.bufferForName(), cfdata.get("SUBCOMPARATOR_TYPE").toString());
    					@SuppressWarnings("unchecked")
						String val = getAsString(col.bufferForValue(), getValidationType(col.bufferForName(), (List<ColumnDef>)cfdata.get("COLUMN_METADATA"), cfdata.get("DEFAULT_VALIDATION_CLASS").toString()));
                        
                        Cell c = new Cell(s,
                                          name,
                                          val,
                                          new Date(col.getTimestamp() / 1000));
                        s.getCells().put(c.getName(), c);
                    }

                    key.getSColumns().put(s.getName(), s);
                } else if (column.isSetColumn()){
                    Column col = column.getColumn();
                    String name = getAsString(col.bufferForName(), cfdata.get("COMPARATOR_TYPE").toString());
                    @SuppressWarnings("unchecked")
					String val = getAsString(col.bufferForValue(), getValidationType(col.bufferForName(), (List<ColumnDef>)cfdata.get("COLUMN_METADATA"), cfdata.get("DEFAULT_VALIDATION_CLASS").toString()));
                    
                    Cell c = new Cell(key,
                    					name,
                    					val,
                                      new Date(col.getTimestamp() / 1000));
                    key.getCells().put(c.getName(), c);
                } else {
                	throw new Exception("Unsupported Column type");
                }
            }

            m.put(key.getName(), key);
        }

        return m;
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * @return the keyspace
     */
    public String getKeyspace() {
        return keyspace;
    }

    /**
     * @param keyspace the keyspace to set
     */
    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    /**
     * @return the columnFamily
     */
    public String getColumnFamily() {
        return columnFamily;
    }

    /**
     * @param columnFamily the columnFamily to set
     */
    public void setColumnFamily(String columnFamily) {
        this.columnFamily = columnFamily;
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
     * @return the strategyMap
     */
    public static Map<String, String> getStrategyMap() {
        Map<String, String> strategyMap = new TreeMap<String, String>();
        strategyMap.put("SimpleStrategy", "org.apache.cassandra.locator.SimpleStrategy");
        strategyMap.put("LocalStrategy", "org.apache.cassandra.locator.LocalStrategy");
        strategyMap.put("NetworkTopologyStrategy", "org.apache.cassandra.locator.NetworkTopologyStrategy");
        strategyMap.put("OldNetworkTopologyStrategy", "org.apache.cassandra.locator.OldNetworkTopologyStrategy");
        return strategyMap;
    }

    public static Map<String, String> getComparatorTypeMap() {
        Map<String, String> comparatorMap = new TreeMap<String, String>();
        comparatorMap.put("org.apache.cassandra.db.marshal.AsciiType", "AsciiType");
        comparatorMap.put("org.apache.cassandra.db.marshal.BytesType", "BytesType");
        comparatorMap.put("org.apache.cassandra.db.marshal.LexicalUUIDType", "LexicalUUIDType");
        comparatorMap.put("org.apache.cassandra.db.marshal.LongType", "LongType");
        comparatorMap.put("org.apache.cassandra.db.marshal.TimeUUIDType", "TimeUUIDType");
        comparatorMap.put("org.apache.cassandra.db.marshal.UTF8Type", "UTF8Type");

        return comparatorMap;
    }

    public static Map<String, String> getValidationClassMap() {
        Map<String, String> validationClassMap = new TreeMap<String, String>();
        validationClassMap.put("org.apache.cassandra.db.marshal.AsciiType", "AsciiType");
        validationClassMap.put("org.apache.cassandra.db.marshal.BytesType", "BytesType");
        validationClassMap.put("org.apache.cassandra.db.marshal.IntegerType", "IntegerType");
        validationClassMap.put("org.apache.cassandra.db.marshal.LongType", "LongType");
        validationClassMap.put("org.apache.cassandra.db.marshal.TimeUUIDType", "TimeUUIDType");
        validationClassMap.put("org.apache.cassandra.db.marshal.UTF8Type", "UTF8Type");

        return validationClassMap;
    }
}
