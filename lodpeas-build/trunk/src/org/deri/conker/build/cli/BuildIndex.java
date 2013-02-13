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
import org.semanticweb.nxindex.block.util.CallbackIndexerIO;
import org.semanticweb.nxindex.block.util.CallbackIndexerIO.CallbackIndexerArgs;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.NodeComparator;
import org.semanticweb.yars.nx.NodeComparator.NodeComparatorArgs;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.sort.SortIterator;
import org.semanticweb.yars.nx.sort.SortIterator.SortArgs;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Index quads (and labels) in a final lookup index.
 * 
 * @author aidhog
 *
 */
public class BuildIndex {
	static transient Logger _log = Logger.getLogger(BuildIndex.class.getName());
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
		
		Option inO = new Option("i", "input data");
		inO.setRequired(true);
		inO.setArgs(1);
		options.addOption(inO);

		Option ingzO = new Option("igz", "flag to state that input file is GZipped (default not)");
		ingzO.setRequired(false);
		ingzO.setArgs(0);
		options.addOption(ingzO);
		
		Option soO = new Option("s", "sort order (omit if input already sorted)");
		soO.setRequired(false);
		soO.setArgs(1);
		options.addOption(soO);
		
		Option outO = new Option("oi", "output index file");
		outO.setRequired(true);
		outO.setArgs(1);
		options.addOption(outO);
		
		Option spO = new Option("os", "output sparse file");
		spO.setRequired(true);
		spO.setArgs(1);
		options.addOption(spO);
		
		Option kO = new Option("k", "index key length (default 1 | <1 interpreted as full key)");
		kO.setRequired(false);
		kO.setArgs(1);
		options.addOption(kO);
		
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
		_log.info("Opening primary input ...");
		InputStream is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
		Iterator<Node[]> nx = new NxParser(is);
		int ticks = org.semanticweb.yars.nx.cli.Main.getTicks(cmd);
		nx = new TicksIterator(nx, ticks);
		
		_log.info("... files opened.");
		
		// # opening output index files
		CallbackIndexerArgs cia = new CallbackIndexerArgs(cmd.getOptionValue(outO.getOpt()), cmd.getOptionValue(spO.getOpt()));
		short keylen = DEFAULT_KEY_LENGTH;
		if(cmd.hasOption(kO.getOpt())){
			keylen = Short.parseShort(cmd.getOptionValue(kO.getOpt()));
		}
		cia.setSparseLength(keylen);
		CallbackIndexerIO cii = new CallbackIndexerIO(cia);
		
		// # sort if needed
		if(cmd.hasOption(soO.getOpt())){
			_log.info("... sorting input data prior to indexing ...");
			int[] so = NodeComparatorArgs.getIntegerMask(cmd.getOptionValue(soO.getOpt()));
			SortArgs sa = new SortArgs(nx);
			sa.setComparator(new NodeComparator(so));
			SortIterator si = new SortIterator(sa);
			nx = new TicksIterator(si, ticks);
			
			_log.info("... sorted ...");
		}
		
		
		_log.info("Indexing ...");
		while(nx.hasNext()){
			cii.processStatement(nx.next());
		}
		cii.close();
		is.close();
		_log.info("... done.");
	}
}
