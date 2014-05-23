package it.geosolutions.jaiext.range;


/**
 * This class is a subclass of the {@link Range} class handling double data.
 */
public class RangeDouble extends Range {

    /** Minimum range bound */
    private final double minValue;

    /** Maximum range bound */
    private final double maxValue;
    
    /** If the Range is degenerated and it is a NaN value, then this value is taken as a Long */
    private final long intValue;

    /** Boolean indicating if the minimum bound is included */
    private final boolean minIncluded;

    /** Boolean indicating if the maximum bound is included */
    private final boolean maxIncluded;
    
    /** Boolean indicating if the maximum bound is included */
    private final boolean isPoint;

    /** Boolean indicating the presence of No Data, only used for degenerated Range(single-point) */
    private final boolean isNaN;
    
    /** Boolean indicating if No Data in should be considered always inside or outside the Range (only for non-degenerated Ranges) */
    private final boolean nanIncluded;

    RangeDouble(double minValue, boolean minIncluded, double maxValue, boolean maxIncluded,boolean nanIncluded) {        // If one of the 2 bound values is NaN an exception is thrown
        if (Double.isNaN(minValue) && !Double.isNaN(maxValue) || !Double.isNaN(minValue) && Double.isNaN(maxValue)) {
            throw new UnsupportedOperationException(
                    "NaN values can only be set inside a single-point Range");
        }else if (minValue < maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.isPoint = false;
            this.isNaN = false;
            this.minIncluded = minIncluded;
            this.maxIncluded = maxIncluded;
            this.intValue=0;
            this.nanIncluded=nanIncluded;
        } else if (minValue > maxValue) {
            this.minValue = maxValue;
            this.maxValue = minValue;
            this.isPoint = false;
            this.isNaN = false;
            this.minIncluded = minIncluded;
            this.maxIncluded = maxIncluded;
            this.intValue=0;
            this.nanIncluded=nanIncluded;
        } else {
            this.minValue = minValue;
            this.maxValue = minValue;
            this.isPoint = true;
            this.nanIncluded=false;
            if (Double.isNaN(minValue)) {
                this.isNaN = true;
                this.intValue=Double.doubleToLongBits(minValue); 
            } else {
                this.isNaN = false;
                this.intValue=0;
            }
            if (!minIncluded && !maxIncluded) {
                throw new IllegalArgumentException(
                        "Cannot create a single-point range without minimum and maximum "
                                + "bounds included");
            } else {
                this.minIncluded = true;
                this.maxIncluded = true;
            }
        }}

    @Override
    public boolean contains(double value) {
        if (isPoint) {
            if (isNaN) {
                long valueLong = Double.doubleToLongBits(value);
                return valueLong == intValue;
            } else {
                return this.minValue == value;
            }
        }  else if(nanIncluded){
            final boolean lower;
            final boolean upper;

            if (minIncluded) {
                lower = value < minValue;
            } else {
                lower = value <= minValue;
            }

            if (maxIncluded) {
                upper = value > maxValue;
            } else {
                upper = value >= maxValue;
            }

            return !lower && !upper;
        }else{
            final boolean notLower;
            final boolean notUpper;

            if (minIncluded) {
                notLower = value >= minValue;
            } else {
                notLower = value > minValue;
            }

            if (maxIncluded) {
                notUpper = value <= maxValue;
            } else {
                notUpper = value < maxValue;
            }

            return notLower && notUpper;
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.DOUBLE;
    }
    
    @Override
    public boolean isPoint() {
        return isPoint;
    }
    
    @Override
    public Number getMax() {
        return maxValue;
    }

    @Override
    public Number getMin() {
        return minValue;
    }
    
}
