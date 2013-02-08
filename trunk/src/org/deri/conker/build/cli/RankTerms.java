package org.deri.conker.build.cli;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
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
import org.deri.conker.build.util.rank.PageRank;
import org.deri.conker.build.util.rank.TermRank;
import org.deri.conker.build.util.rank.TermRank.TermRankArgs;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Rank elements of the data based on document ranks.
 * 
 * @author aidhog
 *
 */
public class RankTerms {
	static transient Logger _log = Logger.getLogger(RankTerms.class.getName());
	
	static enum TermTypes {
		URI, URI_BNODE, URI_BNODE_LIT
	};
	
	static enum TermPos {
		ALL, ABOX, TBOX
	};
	
	static final TermTypes DEFAULT_TYPE = TermTypes.URI_BNODE;
	static final TermPos DEFAULT_POS = TermPos.ALL;
	
	

	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		
		Option rinO = new Option("r", "input document ranks file (don't have to be sorted)");
		rinO.setRequired(true);
		rinO.setArgs(1);
		options.addOption(rinO);

		Option ringzO = new Option("rgz", "flag to state that document ranks file is GZipped (default not)");
		ringzO.setRequired(false);
		ringzO.setArgs(0);
		options.addOption(ringzO);
		
		Option typeO = new Option("t", "type of terms to rank (0 = URIs, 1 = URIs + BNodes [default], 2 = URIs + BNodes + Lits");
		typeO.setRequired(false);
		typeO.setArgs(1);
		options.addOption(typeO);
		
		Option posO = new Option("p", "positions of terms to rank (0 = all [default], 1 = abox only [subjects and objects of non-rdf:type], 2 = tbox only [predicates and objects of rdf:type]");
		posO.setRequired(false);
		posO.setArgs(1);
		options.addOption(posO);
		
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
		
		TermTypes t = DEFAULT_TYPE;
		if(cmd.hasOption(typeO.getOpt())){
			try {
				int i = Integer.parseInt(cmd.getOptionValue(typeO.getOpt()));
				t = TermTypes.values()[i];
			} catch(Exception e){
				_log.warning("Invalid value for -"+typeO.getOpt());
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("parameters:", options );
				return;
			}
		}
		
		TermPos p = DEFAULT_POS;
		if(cmd.hasOption(posO.getOpt())){
			try {
				int i = Integer.parseInt(cmd.getOptionValue(posO.getOpt()));
				p = TermPos.values()[i];
			} catch(Exception e){
				_log.warning("Invalid value for -"+posO.getOpt());
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("parameters:", options );
				return;
			}
		}

		_log.info("Loading document ranks ...");
		
		InputStream isr = new FileInputStream(cmd.getOptionValue(rinO.getOpt()));
		if(cmd.hasOption(ringzO.getOpt())){
			isr = new GZIPInputStream(isr);
		}
		Iterator<Node[]> nxr = new NxParser(isr);
		nxr = new TicksIterator(nxr, ticks);
		
		HashMap<Node,Double> ranks = PageRank.loadRanks(nxr);
		isr.close();
		_log.info("... done. Loaded "+ranks.size()+" ranks with sum "+PageRank.sum(ranks)+".");

		//rank terms
		
		_log.info("Ranking terms now ...");
		TermRankArgs tra = new TermRankArgs(nx, ranks);
		tra.setConsiderTboxPositions(p == TermPos.ALL || p == TermPos.TBOX);
		tra.setConsiderAboxPositions(p == TermPos.ALL || p == TermPos.ABOX);
		tra.setRankBNodes(t == TermTypes.URI_BNODE || t == TermTypes.URI_BNODE_LIT);
		tra.setRankLiterals(t == TermTypes.URI_BNODE_LIT);
		
		TermRank.rankTerms(tra, cb);
		
		is.close();
		bw.close();
	}
}
