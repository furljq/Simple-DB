package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int groupByFieldId, aggregateFieldId;
    private Boolean grouping;
    private Map<Field, Integer> counter;
    private TupleDesc tupleDesc;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        if (what != Op.COUNT) throw new IllegalArgumentException();
        groupByFieldId = gbfield;
        aggregateFieldId = afield;
        grouping = gbfield != NO_GROUPING;
        counter = new HashMap<>();
        if (grouping) tupleDesc = new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE});
        else tupleDesc = new TupleDesc(new Type[]{Type.INT_TYPE});
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        Field groupByField;
        if (grouping) groupByField = tup.getField(groupByFieldId);
        else groupByField = null;
        if (!counter.containsKey(groupByField)) {
            counter.put(groupByField, 1);
            return;
        }
        counter.put(groupByField, counter.get(groupByField) + 1);
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        // throw new UnsupportedOperationException("please implement me for lab3");
        List<Tuple> tuples = new ArrayList<>();
        for (Field field : counter.keySet()) {
            int value = counter.get(field);
            Tuple tuple = new Tuple(tupleDesc);
            if (grouping) {
                tuple.setField(0, field);
                tuple.setField(1, new IntField(value));
            } else tuple.setField(0, new IntField(value));
            tuples.add(tuple);
        }
        return new TupleIterator(tupleDesc, tuples);
    }

}
