package org.deri.conker.build.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.deri.conker.build.util.kw.KeywordIndexer;
import org.deri.conker.build.util.uri.RedirectsMap;
import org.semanticweb.nxindex.ScanIterator;
import org.semanticweb.nxindex.block.NodesBlockReaderIO;
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
 * Index keyword and snippet information in lucene from an index.
 * 
 * @author aidhog
 *
 */
public class BuildKeyword {
	static transient Logger _log = Logger.getLogger(BuildKeyword.class.getName());
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
		
		Option iigzO = new Option("ii", "flag to state that input file is an index (default not)");
		iigzO.setRequired(false);
		iigzO.setArgs(0);
		options.addOption(iigzO);
		
		Option soO = new Option("s", "sort order (omit if input already sorted)");
		soO.setRequired(false);
		soO.setArgs(1);
		options.addOption(soO);
		
		Option rinO = new Option("r", "input redirects file (optional)");
		rinO.setRequired(false);
		rinO.setArgs(1);
		options.addOption(rinO);

		Option ringzO = new Option("rgz", "flag to state that redirects file is GZipped (default not)");
		ringzO.setRequired(false);
		ringzO.setArgs(0);
		options.addOption(ringzO);
		
		Option outO = new Option("o", "output directory");
		outO.setRequired(true);
		outO.setArgs(1);
		options.addOption(outO);
		
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

		
		InputStream is = null;
		NodesBlockReaderIO nbr = null;
		Iterator<Node[]> nx = null;
		
		// # load primary input and output
		_log.info("Opening primary input ...");
		
		if(!cmd.hasOption(iigzO.getOpt())){
			is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
			nx = new NxParser(is);
		} else{
			nbr = new NodesBlockReaderIO(cmd.getOptionValue(inO.getOpt()));
			nx = new ScanIterator(nbr);
		}
		int ticks = org.semanticweb.yars.nx.cli.Main.getTicks(cmd);
		nx = new TicksIterator(nx, ticks);
		
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

		_log.info("... files opened.");
		
		
		
		// indexing
		_log.info("Indexing keywords ...");
		KeywordIndexer.buildLucene(nx, rm, cmd.getOptionValue(outO.getOpt()));
		_log.info("... done.");
		
		if(nbr!=null)
			nbr.close();
		else if(is!=null)
			is.close();
	}
}
