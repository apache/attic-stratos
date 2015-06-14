package org.apache.stratos.cep.extension;


import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.log4j.Logger;

public class CurveFitter {
    private final Logger log = Logger.getLogger(CurveFitter.class);
    private double[] xValues;
    private double[] yValues;

    public CurveFitter(double[] xValues, double[] yValues){
        this.xValues = xValues;
        this.yValues = yValues;
    }


    /**
     *fit the XValues and yValues into a second order polynomial
     * @return the coefficient array of the polynomial
     */
    public double[] fit(){
        WeightedObservedPoints weightedObservedPoints = new WeightedObservedPoints();

        for(int i = 0 ; i < xValues.length && i < yValues.length ; i++){
            weightedObservedPoints.add(xValues[i], yValues[i]);
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
