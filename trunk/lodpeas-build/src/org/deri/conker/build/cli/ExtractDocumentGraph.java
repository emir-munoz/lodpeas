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
import org.deri.conker.build.util.rank.DocumentGraph;
import org.deri.conker.build.util.uri.RedirectsMap;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Extract document graph
 * 
 * @author aidhog
 *
 */
public class ExtractDocumentGraph {
	static transient Logger _log = Logger.getLogger(ExtractDocumentGraph.class.getName());

	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();

		Option rinO = new Option("r", "input redirects file (optional)");
		rinO.setRequired(false);
		rinO.setArgs(1);
		options.addOption(rinO);

		Option ringzO = new Option("rgz", "flag to state that redirects file is GZipped (default not)");
		ringzO.setRequired(false);
		ringzO.setArgs(0);
		options.addOption(ringzO);

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

		// # load redirects if given
		RedirectsMap rm = new RedirectsMap();
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

		// # extract document graph
		// - load contexts
		_log.info("Loading contexts from input ...");
		HashMap<Node,Node> contexts = new HashMap<Node,Node>();
		while(nx.hasNext()){
			Node[] next = nx.next();
			if(next.length<4){
				_log.warning("The statement "+Nodes.toN3(next)+" does not have context. Need quads to build doc-level graph!");
			} else{
				if(!contexts.containsKey(next[3]))
					contexts.put(next[3], next[3]);
			}
		}
		_log.info("... done. Found "+contexts.size()+" unique contexts.");

		// - reset input
		is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
		nx = new NxParser(is);
		nx = new TicksIterator(nx, ticks);

		// - extract graph
		_log.info("Extracting and loading document-level graph from input ...");
		int written = DocumentGraph.extractGraph(nx, rm, contexts, cb);
		_log.info("... done. Found "+written+" edges.");

		bw.close();
		is.close();
	}
}
