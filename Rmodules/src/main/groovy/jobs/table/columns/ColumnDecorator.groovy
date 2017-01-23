package jobs.table.columns

import jobs.table.Column

public interface ColumnDecorator extends Column {
    Column getInner()
    void setInner(Column inner)
}
