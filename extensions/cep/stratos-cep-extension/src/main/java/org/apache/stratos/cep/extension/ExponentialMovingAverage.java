package org.apache.stratos.cep.extension;

/**
 * class to demonstrate how EMA works
 */
public class ExponentialMovingAverage {
    public Double value;

    /**
     * smooth the value with respect to the previous value
     * @param value
     * @return
     */
    public double getSmoothedValue(double value){
        if(this.value == null){
            this.value = value;
            return value;
        }

        /**
         * calculating smoothed value
         */
        double newValue = CurveFinderWindowProcessor.ALPHA * value + (1.0 - CurveFinderWindowProcessor.ALPHA)*value;
        this.value = newValue;

        return newValue;
    }

    public Double getValue() {
        return value;
    }

}
