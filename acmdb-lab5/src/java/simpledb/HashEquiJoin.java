package simpledb;

import java.util.*;

/**
 * The Join operator implements the relational join operation.
 */
public class HashEquiJoin extends Operator {

    private static final long serialVersionUID = 1L;

    private JoinPredicate joinPredicate;
    private DbIterator iterator1, iterator2;
    private Tuple tuple1, tuple2;

    /**
     * Constructor. Accepts to children to join and the predicate to join them
     * on
     * 
     * @param p
     *            The predicate to use to join the children
     * @param child1
     *            Iterator for the left(outer) relation to join
     * @param child2
     *            Iterator for the right(inner) relation to join
     */
    public HashEquiJoin(JoinPredicate p, DbIterator child1, DbIterator child2) {
        // some code goes here
        joinPredicate = p;
        iterator1 = child1;
        iterator2 = child2;
        tuple1 = null;
        tuple2 = null;
    }

    public JoinPredicate getJoinPredicate() {
        // some code goes here
        return joinPredicate;
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return TupleDesc.merge(iterator1.getTupleDesc(), iterator2.getTupleDesc());
    }
    
    public String getJoinField1Name() {
        // some code goes here
	    return iterator1.getTupleDesc().getFieldName(joinPredicate.getField1());
    }

    public String getJoinField2Name() {
        // some code goes here
        return iterator2.getTupleDesc().getFieldName(joinPredicate.getField2());
    }
    
    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        super.open();
        iterator1.open();
        iterator2.open();
    }

    public void close() {
        // some code goes here
        super.close();
        iterator1.close();
        iterator2.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        iterator1.rewind();
        iterator2.rewind();
        tuple1 = null;
        tuple2 = null;
    }

    transient Iterator<Tuple> listIt = null;

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, there will be two copies of the join attribute in
     * the results. (Removing such duplicate columns can be done with an
     * additional projection operator if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     * 
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        while (true) {
            if (tuple1 != null) {
                while (iterator2.hasNext()) {
                    tuple2 = iterator2.next();
                    if (joinPredicate.filter(tuple1, tuple2)) {
                        TupleDesc tupleDesc = TupleDesc.merge(tuple1.getTupleDesc(), tuple2.getTupleDesc());
                        Tuple tuple = new Tuple(tupleDesc);
                        for (int i = 0; i < tuple1.getTupleDesc().numFields(); i++)
                            tuple.setField(i, tuple1.getField(i));
                        for (int i = 0; i < tuple2.getTupleDesc().numFields(); i++)
                            tuple.setField(i + tuple1.getTupleDesc().numFields(), tuple2.getField(i));
                        return tuple;
                    }
                }
            }
            if (!iterator1.hasNext()) break;
            tuple1 = iterator1.next();
            iterator2.rewind();
            tuple2 = null;
        }
        return null;
    }

    @Override
    public DbIterator[] getChildren() {
        // some code goes here
        return new DbIterator[]{iterator1, iterator2};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        // some code goes here
        iterator1 = children[0];
        iterator2 = children[1];
    }
    
}