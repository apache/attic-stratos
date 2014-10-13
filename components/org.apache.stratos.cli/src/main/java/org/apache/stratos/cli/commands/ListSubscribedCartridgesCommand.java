/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cli.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.stratos.cli.RestCommandLineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;

public class ListSubscribedCartridgesCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(ListSubscribedCartridgesCommand.class);
	
	private final Options options;

	public ListSubscribedCartridgesCommand() {
		options = constructOptions();
	}
	
	/**
	 * Construct Options.
	 * 
	 * @return Options expected from command-line.
	 */
	private Options constructOptions() {
		final Options options = new Options();
		Option fullOption = new Option(CliConstants.FULL_OPTION, CliConstants.FULL_LONG_OPTION, false,
				"Display extra details");
		options.addOption(fullOption);
		return options;
	}

	public String getName() {
		return CliConstants.LIST_ACTION;
	}

	public String getDescription() {
		return "List subscribed cartridges with summarized details";
	}

	public String getArgumentSyntax() {
		return null;
	}

	public int execute(StratosCommandContext context, String[] args) throws CommandException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing {} command...", getName());
		}
		if (args == null || args.length == 0) {
            RestCommandLineService.getInstance().listSubscribedCartridges(false);
			//CommandLineService.getInstance().listSubscribedCartridges(false);
			return CliConstants.COMMAND_SUCCESSFULL;
		} else if (args != null && args.length > 0) {
			String[] remainingArgs = null;
			boolean full = false;
			final CommandLineParser parser = new GnuParser();
			CommandLine commandLine;
			try {
				commandLine = parser.parse(options, args);
				remainingArgs = commandLine.getArgs();
				if (!(remainingArgs == null || remainingArgs.length == 0)) {
					context.getStratosApplication().printUsage(getName());
					return CliConstants.COMMAND_FAILED;
				}

				if (commandLine.hasOption(CliConstants.FULL_OPTION)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Full option is passed");
					}
					full = true;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Listing subscribed cartridges, Full Option: {}", full);
				}
                RestCommandLineService.getInstance().listSubscribedCartridges(full);
				//CommandLineService.getInstance().listSubscribedCartridges(full);
				return CliConstants.COMMAND_SUCCESSFULL;
			} catch (ParseException e) {
				if (logger.isErrorEnabled()) {
					logger.error("Error parsing arguments", e);
				}
				System.out.println(e.getMessage());
				return CliConstants.COMMAND_FAILED;
			}
		} else {
			context.getStratosApplication().printUsage(getName());
			return CliConstants.COMMAND_FAILED;
		}
	}

	public Options getOptions() {
		return options;
	}

}
