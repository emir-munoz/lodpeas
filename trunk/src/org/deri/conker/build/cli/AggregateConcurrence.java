package org.deri.conker.build.cli;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.deri.conker.build.util.concur.ConcurrenceEngine;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.sort.SortIterator;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Aggregate raw concurrence relations.
 * 
 * @author aidhog
 *
 */
public class AggregateConcurrence {
	static transient Logger _log = Logger.getLogger(AggregateConcurrence.class.getName());
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();
		
		Option sO = new Option("s", "sort input");
		sO.setRequired(false);
		options.addOption(sO);
		
		Option fO = new Option("f", "filter relations to same value");
		fO.setRequired(false);
		options.addOption(fO);
		
		Option routO = new Option("r", "output for simple results (optional)");
		routO.setArgs(1);
		options.addOption(routO);

		Option routgzO = new Option("rgz", "flag to state that results should be GZipped (optional)");
		routgzO.setArgs(0);
		options.addOption(routgzO);

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		
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
		
		InputStream is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
		OutputStream os = org.semanticweb.yars.nx.cli.Main.getMainOutputStream(cmd);
		int ticks = org.semanticweb.yars.nx.cli.Main.getTicks(cmd);
		
		boolean sort = cmd.hasOption(sO.getOpt());
		
		Callback cbr = null;
		BufferedWriter bwr = null;
		
		if(cmd.hasOption(routO.getOpt())){
			OutputStream ros = new FileOutputStream(cmd.getOptionValue(routO.getOpt()));
			if(cmd.hasOption(routgzO.getOpt())){
				ros = new GZIPOutputStream(ros);
			}
			bwr = new BufferedWriter(new OutputStreamWriter(ros));
			cbr= new CallbackNxBufferedWriter(bwr);
		} else{
			cbr = new CallbackDummy();
		}
		
		Iterator<Node[]> nx = new NxParser(is);
		if(sort)
			nx = new SortIterator(nx);
		
		nx = new TicksIterator(nx,ticks);
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		Callback cb = new CallbackNxBufferedWriter(bw);
		
		ConcurrenceEngine.aggregateSimilarity(nx, cbr, false, cmd.hasOption(fO.getOpt()), cb);
		
		is.close();
		bw.close();
		if(bwr!=null)
			bwr.close();
	}
	
	public static class CallbackDummy implements Callback{

		public void endDocument() {
			;
		}

		public void processStatement(Node[] arg0) {
			;
		}

		public void startDocument() {
			;
		}
	}

}
