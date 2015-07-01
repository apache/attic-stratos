package org.apache.stratos.gce.extension.config.parser;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.exception.MalformedConfigurationFileException;
import org.apache.stratos.common.util.AxiomXpathParserUtil;

public class GCEConfigParser {
    private static final Log log = LogFactory.getLog(GCEConfigParser.class);

    /**
     * Parse the gce-configuration file.
     *
     * @param documentElement axiom document element.
     * @throws MalformedConfigurationFileException
     */
    public static void parse(OMElement documentElement) throws MalformedConfigurationFileException {

        //get cep info
        OMElement cepInfoElement = AxiomXpathParserUtil.getFirstChildElement(documentElement,"cepStatsPublisher");
        log.info(cepInfoElement.getText());

        //todo rest

    }

}
