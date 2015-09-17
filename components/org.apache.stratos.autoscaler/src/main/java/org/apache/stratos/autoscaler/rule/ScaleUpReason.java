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
package org.apache.stratos.autoscaler.rule;

/**
 * Scaling up reason enumeration
 */
public enum ScaleUpReason {
    RIF("Scaling up due to RIF"), MC("Scaling up due to MC"), LA("Scaling up due to LA"),
    MIN("Scaling up to fulfil minimum count"), DEPENDENCY("Dependency scaling");
    private String value;

    ScaleUpReason(String val) {
        value = val;
    }

    public String getValue() {
        return value;
    }
}