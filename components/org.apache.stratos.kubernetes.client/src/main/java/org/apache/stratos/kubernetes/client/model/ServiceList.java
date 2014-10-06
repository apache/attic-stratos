/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.stratos.kubernetes.client.model;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.ArrayUtils;

@XmlRootElement
public class ServiceList {

	private String kind;
	private String apiVersion;
	private Service[] items;
	
	public String getKind() {
		return kind;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	public String getApiVersion() {
		return apiVersion;
	}
	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}
	public Service[] getItems() {
		return items;
	}
	public void setItems(Service[] items) {
		this.items = ArrayUtils.clone(items);
	}
	@Override
	public String toString() {
		return "ServiceList [kind=" + kind + ", apiVersion=" + apiVersion
				+ ", items=" + Arrays.toString(items) + "]";
	}
	
}
