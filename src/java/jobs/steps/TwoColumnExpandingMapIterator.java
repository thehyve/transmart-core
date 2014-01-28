package jobs.steps;

import com.google.common.base.Splitter;
import jobs.steps.helpers.CompositeResultDataRow;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class that expects the context (map key) to be a concatenation to be in the
 * form &lt;concept path>|&lt;row label> and populates two columns after
 * the one with the original value -- the first with the concept path and the
 * second with the row label
 */
public class TwoColumnExpandingMapIterator extends ExpandingMapIterator {

    private Splitter splitter =
            Splitter.on(CompositeResultDataRow.SEPARATOR.charAt(0)).limit(2);

    public TwoColumnExpandingMapIterator(Iterator<List<Object>> preResults,
                                         List<Integer> mapIndexes) {
        super(preResults, mapIndexes);
        numberOfNewRowsPerMapColumn = 2;
    }

    protected void writeEntry(Map.Entry<String, Object> entry, int index) {
        Iterator<String> iterator = splitter.split(entry.getKey()).iterator();
        getReturnArray()[index] = entry.getValue().toString();
        getReturnArray()[index + 1] = iterator.next();
        getReturnArray()[index + 2] = iterator.next();
    }
}
