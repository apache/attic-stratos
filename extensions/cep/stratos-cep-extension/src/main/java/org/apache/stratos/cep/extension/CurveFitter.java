/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */
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
    public Double[] fit(){
        WeightedObservedPoints weightedObservedPoints = new WeightedObservedPoints();

        for(int i = 0 ; i < timeStampValues.length && i < dataValues.length ; i++){
            if(timeStampValues[i] != 0 && dataValues[i] != 0)
                weightedObservedPoints.add(timeStampValues[i], dataValues[i]);
        }

        /**
         * create second degree polynomials from the observed points
         */
        final PolynomialCurveFitter polynomialCurveFitter = PolynomialCurveFitter.create(2);
        final double[] coefficients = polynomialCurveFitter.fit(weightedObservedPoints.toList());

        log.info("Coefficient a : " + coefficients[0] + " Coefficient b : " + coefficients[1]+ " Coefficient c : " + coefficients[2]);

        return convertDouble(coefficients);
    }

    /**
     * To convert a double array to Double array
     * @param array double array that need to be converted
     * @return Converted Double array
     */
    public Double[] convertDouble(double[] array){
        Double[]  converted =  new Double[array.length];

        for(int index =  0; index < converted.length; index++){
            converted[index] = new Double(array[index]);
        }
        return converted;
    }
}