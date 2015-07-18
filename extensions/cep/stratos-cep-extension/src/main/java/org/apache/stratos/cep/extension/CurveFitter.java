package org.apache.stratos.cep.extension;


import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.log4j.Logger;

public class CurveFitter {
    private final Logger log = Logger.getLogger(CurveFitter.class);
    private long[] timeStampValues;
    private double[] dataValues;

    public CurveFitter(long[] timeStampValues, double[] dataValues){
        this.timeStampValues = timeStampValues;
        this.dataValues = dataValues;
    }


    /**
     *fit the XValues and dataValues into a second order polynomial
     * @return the coefficient array of the polynomial
     */
    public double[] fit(){
        WeightedObservedPoints weightedObservedPoints = new WeightedObservedPoints();

        for(int i = 0 ; i < timeStampValues.length && i < dataValues.length ; i++){
            weightedObservedPoints.add(timeStampValues[i], dataValues[i]);
        }

        /**
         * create second degree polynomials from the observed points
         */
        final PolynomialCurveFitter polynomialCurveFitter = PolynomialCurveFitter.create(2);
        final double[] coefficients = polynomialCurveFitter.fit(weightedObservedPoints.toList());

        log.info("Coefficients a : " + coefficients[0] + " b : " + coefficients[1] + " c : " + coefficients[2]);

        return coefficients;
    }

}
