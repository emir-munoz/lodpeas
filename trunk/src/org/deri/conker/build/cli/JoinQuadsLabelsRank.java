package org.deri.conker.build.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.deri.conker.build.cli.RankGraph.ResetableFileIterator;
import org.deri.conker.build.util.index.QuadLabelJoiner;
import org.deri.conker.build.util.index.QuadLabelJoiner.OutputType;
import org.deri.conker.build.util.index.QuadLabelJoiner.QuadLabelJoinOrder;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Write a file of preferred labels and ranks for RDF terms.
 * 
 * @author aidhog
 *
 */
public class JoinQuadsLabelsRank {
	static transient Logger _log = Logger.getLogger(JoinQuadsLabelsRank.class.getName());
	
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
		
		Option inO = new Option("i", "input quads sorted by SPOC");
		inO.setRequired(true);
		inO.setArgs(1);
		options.addOption(inO);

		Option ingzO = new Option("igz", "flag to state that input file is GZipped (default not)");
		ingzO.setRequired(false);
		ingzO.setArgs(0);
		options.addOption(ingzO);
		
		Option rinO = new Option("l", "input labels and ranks file");
		rinO.setRequired(true);
		rinO.setArgs(1);
		options.addOption(rinO);

		Option ringzO = new Option("lgz", "flag to state that labels and ranks file is GZipped (default not)");
		ringzO.setRequired(false);
		ringzO.setArgs(0);
		options.addOption(ringzO);
		
		Option outO = new Option("o", "output file");
		outO.setRequired(true);
		outO.setArgs(1);
		options.addOption(outO);
		
		Option ogzO = new Option("ogz", "gzip output file");
		ogzO.setRequired(false);
		ogzO.setArgs(0);
		options.addOption(ogzO);
		
		
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
		
		_log.info("... files opened.");
		
		int ticks = org.semanticweb.yars.nx.cli.Main.getTicks(cmd);
		nx = new TicksIterator(nx, ticks);

		_log.info("Opening labels and ranks ...");
		
		ResetableFileIterator rfi = new ResetableFileIterator(cmd.getOptionValue(rinO.getOpt()), cmd.hasOption(ringzO.getOpt()), false, ticks);
		
		_log.info("... done.");

		//rank terms
		
		_log.info("Setting up orderings ...");
		
		String tmpdir = org.semanticweb.yars.nx.cli.Main.getTempSubDir();
		QuadLabelJoinOrder[] qlios = new QuadLabelJoinOrder[]{
			new QuadLabelJoinOrder(tmpdir+"spoc.lri.nx.gz", new int[]{0,1,2,3}),
			new QuadLabelJoinOrder(tmpdir+"posc.lri.nx.gz", new int[]{1,2,0,3}),
			new QuadLabelJoinOrder(cmd.getOptionValue(outO.getOpt()), new int[]{2,1,0,3}, cmd.hasOption(ogzO.getOpt()) ? OutputType.GZIPPED : OutputType.PLAIN)
		};
		
		_log.info("... done.");
		
		_log.info("Joining quads and labels/ranks ...");
		
		QuadLabelJoiner.indexQuadsLabels(nx, rfi, qlios, ticks);
		
		_log.info("... done.");
	}
}
