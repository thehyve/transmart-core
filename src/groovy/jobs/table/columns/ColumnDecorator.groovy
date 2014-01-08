package jobs.table.columns

import jobs.table.Column

public interface ColumnDecorator extends Column {
    void setInner(Column inner)
}
