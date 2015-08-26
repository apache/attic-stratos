/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.pojo.policy.autoscale;

import java.io.Serializable;

/**
 * The model class for LoadAverage definition.
 */
public class LoadAverage implements Serializable {

    private static final long serialVersionUID = -2109860338694123343L;
    private float average = 0.0f;
    private float secondDerivative = 0.0f;
    private float gradient = 0.0f;
    /**
     * curve finder coefficients
     */
    private double a;
    private double b;
    private double c;

    /**
     * Gets the value of the average property.
     */
    public float getAverage() {
        return average;
    }

    /**
     * Sets the value of the average property.
     */
    public void setAverage(float value) {
        this.average = value;
    }

    /**
     * Gets the value of the second-derivative property.
     */
    public float getSecondDerivative() {
        return secondDerivative;
    }

    /**
     * Sets the value of the second-derivative property.
     */
    public void setSecondDerivative(float value) {
        this.secondDerivative = value;
    }

    /**
     * Gets the value of the gradient property.
     */
    public float getGradient() {
        return gradient;
    }

    /**
     * Sets the value of the gradient property.
     */
    public void setGradient(float value) {
        this.gradient = value;
    }

    public double getA() {
        return a;
    }

    public double getB() {
        return b;
    }

    public double getC() {
        return c;
    }

    public void setA(double a) {
        this.a = a;
    }

    public void setB(double b) {
        this.b = b;
    }

    public void setC(double c) {
        this.c = c;
    }

    @Override
    public String toString() {
        return String.format("[average] %f [second-derivative] %f [gradient] %f",
                getAverage(), getSecondDerivative(), getGradient());
    }
}