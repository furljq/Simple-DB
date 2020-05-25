package simpledb;

/** A class to represent a fixed-width height over a single integer-based field.
 */
public class IntHistogram {

    private int numBuckets, min, max, width, sum;
    private int[] height;

    /**
     * Create a new IntHistogram.
     * 
     * This IntHistogram should maintain a height of integer values that it receives.
     * It should split the height into "buckets" buckets.
     * 
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * 
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't 
     * simply store every value that you see in a sorted list.
     * 
     * @param buckets The number of buckets to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
    	// some code goes here
        numBuckets = buckets;
        this.min = min;
        this.max = max + 1;
        width = (int) Math.ceil((double) (this.max - this.min) / numBuckets);
        height = new int[numBuckets];
        sum = 0;
    }

    /**
     * Add a value to the set of values that you are keeping a height of.
     * @param v Value to add to the height
     */
    public void addValue(int v) {
    	// some code goes here
        int location = (v - min) / width;
        height[location]++;
        sum++;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * 
     * For example, if "op" is "GREATER_THAN" and "v" is 5, 
     * return your estimate of the fraction of elements that are greater than 5.
     * 
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
    	// some code goes here
        int location = (v - min) / width;
        if (op == Predicate.Op.EQUALS || op == Predicate.Op.NOT_EQUALS) {
            double percentage;
            if (v < min || v >= max) percentage = 0;
            else percentage = (double) height[location] / width / sum;
            if (op == Predicate.Op.EQUALS) return percentage;
            else return 1 - percentage;
        }
        if (op == Predicate.Op.GREATER_THAN || op == Predicate.Op.LESS_THAN_OR_EQ) {
            double percentage = 0;
            if (v < min) percentage = 1;
            if (v >= max) percentage = 0;
            if (min <= v && v < max) {
                percentage = height[location] * ((min + width * (location + 1)) - (v + 1)) / width;
                for (int i = location + 1; i < numBuckets; i++) percentage += height[i];
                percentage /= sum;
            }
            if (op == Predicate.Op.GREATER_THAN) return percentage;
            else return 1 - percentage;
        }
        if (op == Predicate.Op.LESS_THAN || op == Predicate.Op.GREATER_THAN_OR_EQ) {
            double percentage = 0;
            if (v < min) percentage = 0;
            if (v >= max) percentage = 1;
            if (min <= v && v < max) {
                percentage = height[location] * (v - (min + width * location)) / width;
                for (int i = 0; i < location; i++) percentage += height[i];
                percentage /= sum;
            }
            if (op == Predicate.Op.LESS_THAN) return percentage;
            else return 1 - percentage;
        }
        return -1.0;
    }
    
    /**
     * @return
     *     the average selectivity of this height.
     *     
     *     This is not an indispensable method to implement the basic
     *     join optimization. It may be needed if you want to
     *     implement a more efficient optimization
     * */
    public double avgSelectivity()
    {
        // some code goes here
        return 1.0;
    }
    
    /**
     * @return A string describing this height, for debugging purposes
     */
    public String toString() {
        // some code goes here
        return null;
    }
}
