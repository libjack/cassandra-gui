package org.apache.cassandra.gui.component.model;

import javax.swing.table.DefaultTableModel;

import org.apache.cassandra.unit.ColumnFamilyMetaData;

public class ColumnFamilyMetaDataModel extends DefaultTableModel {
    private static final long serialVersionUID = -1392184063849854796L;

    public static final int COLUMN_COLUMN_NAME = 0;
    public static final int COLUMN_VALIDATION_CLASS = 1;
    public static final int COLUMN_INDEX_TYPE = 2;
    public static final int COLUMN_INDEX_NAME = 3;

    private static final ColumnContext[] columnArray = {
        new ColumnContext("Column Name", String.class, true),
        new ColumnContext("Validation Class", String.class, true),
        new ColumnContext("Index Type", Integer.class, true),
        new ColumnContext("Index Name", String.class, true),
        new ColumnContext("", String.class, true)
    };

    public void add(ColumnFamilyMetaData metaData) {
        Object[] obj = { metaData.getColumnName(),
                         metaData.getValiDationClass(),
                         metaData.getIndexType(),
                         metaData.getIndexName(),
                         ""};
        super.addRow(obj);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return columnArray[column].isEditable;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnArray[columnIndex].columnClass;
    }

    @Override
    public int getColumnCount() {
        return columnArray.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnArray[column].columnName;
    }

    @SuppressWarnings("rawtypes")
    private static class ColumnContext {
        public final String columnName;
        public final Class columnClass;
        public final boolean isEditable;

        public ColumnContext(String columnName, Class columnClass, boolean isEditable) {
            this.columnName = columnName;
            this.columnClass = columnClass;
            this.isEditable = isEditable;
        }
    }
}
