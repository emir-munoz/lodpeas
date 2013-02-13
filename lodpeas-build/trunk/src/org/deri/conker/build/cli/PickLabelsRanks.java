package org.deri.conker.build.cli;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.deri.conker.build.util.index.LabelPicker;
import org.deri.conker.build.util.uri.RedirectsMap;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Write a file of preferred labels and ranks for RDF terms.
 * 
 * @author aidhog
 *
 */
public class PickLabelsRanks {
	static transient Logger _log = Logger.getLogger(PickLabelsRanks.class.getName());
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		
		Option dinO = new Option("d", "input id ranks file (sorted)");
		dinO.setRequired(true);
		dinO.setArgs(1);
		options.addOption(dinO);

		Option dingzO = new Option("dgz", "flag to state that id ranks file is GZipped (default not)");
		dingzO.setRequired(false);
		dingzO.setArgs(0);
		options.addOption(dingzO);
		
		Option rinO = new Option("r", "input redirects file (optional)");
		rinO.setRequired(false);
		rinO.setArgs(1);
		options.addOption(rinO);

		Option ringzO = new Option("rgz", "flag to state that redirects file is GZipped (default not)");
		ringzO.setRequired(false);
		ringzO.setArgs(0);
		options.addOption(ringzO);
		
		System.err.println("[Input must also be sorted]");
		
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

		// # load primary input and output
		_log.info("Opening primary input and output...");
		InputStream is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
		Iterator<Node[]> nx = new NxParser(is);
		
		OutputStream os = org.semanticweb.yars.nx.cli.Main.getMainOutputStream(cmd);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		Callback cb = new CallbackNxBufferedWriter(bw);
		_log.info("... files opened.");
		
		
		int ticks = org.semanticweb.yars.nx.cli.Main.getTicks(cmd);
		nx = new TicksIterator(nx, ticks);
		

		_log.info("Opening term ranks ...");
		
		InputStream isd = new FileInputStream(cmd.getOptionValue(dinO.getOpt()));
		if(cmd.hasOption(dingzO.getOpt())){
			isd = new GZIPInputStream(isd);
		}
		Iterator<Node[]> nxd = new NxParser(isd);
		nxd = new TicksIterator(nxd, ticks);
		
		
		_log.info("... done.");

		
		// # load redirects if given
		RedirectsMap rm = null;
		if(cmd.hasOption(rinO.getOpt())){
			_log.info("Loading redirects ...");
			InputStream isr = new FileInputStream(cmd.getOptionValue(rinO.getOpt()));
			if(cmd.hasOption(ringzO.getOpt())){
				isr = new GZIPInputStream(isr);
			}
			NxParser nxr = new NxParser(isr);
			rm = RedirectsMap.load(nxr);
			isr.close();
			_log.info("... done. Loaded "+rm.size()+" redirects.");
		}
		
		_log.info("Extracting labels and ranks now ...");
		
		LabelPicker.pickLabelsAndRanks(nx, nxd, rm, cb);
		
		_log.info("... done.");
		
		isd.close();
		is.close();
		bw.close();
	}
	
	
}
