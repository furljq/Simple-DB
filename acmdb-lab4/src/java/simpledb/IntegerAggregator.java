package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int groupByFieldId, aggregateFieldId;
    private Op op;
    private Boolean grouping;
    private Map<Field, Integer> aggregate;
    private Map<Field, Integer> counter;
    private TupleDesc tupleDesc;

    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        groupByFieldId = gbfield;
        aggregateFieldId = afield;
        op = what;
        grouping = gbfield != NO_GROUPING;
        aggregate = new HashMap<>();
        counter = new HashMap<>();
        if (grouping) tupleDesc = new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE});
        else tupleDesc = new TupleDesc(new Type[]{Type.INT_TYPE});
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        Field groupByField;
        if (grouping) groupByField = tup.getField(groupByFieldId);
        else groupByField = null;
        int value = ((IntField) tup.getField(aggregateFieldId)).getValue();
        if (!aggregate.containsKey(groupByField)) {
            aggregate.put(groupByField, value);
            counter.put(groupByField, 1);
            return;
        }
        int oldValue = aggregate.get(groupByField);
        counter.put(groupByField, counter.get(groupByField) + 1);
        int newValue = 0;
        if (op == Op.MIN) newValue = min(value, oldValue);
        if (op == Op.MAX) newValue = max(value, oldValue);
        if (op == Op.COUNT) newValue = counter.get(groupByField);
        if (op == Op.SUM || op == Op.AVG) newValue = oldValue + value;
        aggregate.put(groupByField, newValue);
    }

    /**
     * Create a DbIterator over group aggregate results.
     * 
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        // throw new UnsupportedOperationException("please implement me for lab3");
        List<Tuple> tuples = new ArrayList<>();
        for (Field field : aggregate.keySet()) {
            int value = aggregate.get(field);
            if (op == Op.AVG) value /= counter.get(field);
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
