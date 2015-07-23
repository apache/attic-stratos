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
