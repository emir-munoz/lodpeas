package org.deri.conker.build.cli;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.deri.conker.build.util.concur.ConcurrenceArgs;
import org.deri.conker.build.util.concur.ConcurrenceEngine;


/**
 * Index keyword and snippet information in lucene from an index.
 * 
 * @author aidhog
 *
 */
public class RunConcurrence {
	static transient Logger _log = Logger.getLogger(RunConcurrence.class.getName());
	public static final short DEFAULT_KEY_LENGTH = 1;
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
		Options options = new Options();

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		
		org.semanticweb.yars.nx.cli.Main.addTicksOption(options);
		
		Option insO = new Option("is", "input SPOC ordered data with LRI");
		insO.setRequired(true);
		insO.setArgs(1);
		options.addOption(insO);

		Option insgzO = new Option("isgz", "flag to state that input SPOC file is GZipped (default not)");
		insgzO.setRequired(false);
		insgzO.setArgs(0);
		options.addOption(insgzO);
		
		Option inoO = new Option("io", "input OPSC ordered data with LRI");
		inoO.setRequired(true);
		inoO.setArgs(1);
		options.addOption(inoO);

		Option inogzO = new Option("iogz", "flag to state that input OPSC file is GZipped (default not)");
		inogzO.setRequired(false);
		inogzO.setArgs(0);
		options.addOption(inogzO);
		
		Option outO = new Option("o", "output directory for concurrence scores");
		outO.setRequired(true);
		outO.setArgs(1);
		options.addOption(outO);
		
		Option ogzO = new Option("ogz", "flag to state output should be GZipped");
		ogzO.setRequired(false);
		ogzO.setArgs(0);
		options.addOption(ogzO);
		
		Option fO = new Option("f", "flag to filter redundant relations to same value");
		fO.setRequired(false);
		fO.setArgs(0);
		options.addOption(fO);
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("***ERROR: " + e.getClass() + ": " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options );
			return;
		}

		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options );
			return;
		}
		
		// running concurrence
		_log.info("Running concurrence ...");
		ConcurrenceArgs ca = new ConcurrenceArgs(cmd.getOptionValue(insO.getOpt()), cmd.getOptionValue(inoO.getOpt()), cmd.getOptionValue(outO.getOpt()));
		ca.setGzData(cmd.hasOption(ogzO.getOpt()));
		ca.setGzRawOut(cmd.hasOption(ogzO.getOpt()));
		ca.setGzRawSortedOut(cmd.hasOption(ogzO.getOpt()));
		ca.setGzUnsortedOut(cmd.hasOption(ogzO.getOpt()));
		ca.setGzInSpo(cmd.hasOption(insgzO.getOpt()));
		ca.setGzInOps(cmd.hasOption(inogzO.getOpt()));
		ca.initDefaults(cmd.getOptionValue(outO.getOpt()));
		
		ca.setLabel(true);
		ca.setFilterNonSubjects(true);
		ca.setVerbose(false);
		ca.setSymIndex(true);
		ca.setFilterRelations(cmd.hasOption(fO.getOpt()));
		
		ConcurrenceEngine ce = new ConcurrenceEngine(ca);
		ce.run();
		_log.info("... done.");
		
	}
}
