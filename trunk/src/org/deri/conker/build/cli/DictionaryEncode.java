package org.deri.conker.build.cli;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
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
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.NodeComparator.NodeComparatorArgs;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.TicksIterator;

/**
 * Class to convert strings to incremental Integer IDs.
 * Assumes dictionary can fit as a map in memory.
 * 
 * @author aidhog
 *
 */
public class DictionaryEncode {
	static transient Logger _log = Logger.getLogger(RankTerms.class.getName());
	
	public static void main(String[] args) throws IOException{
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		
		Option eO = new Option("p", "encode positions (e.g., 02 for subjects and objects, 1 for predicates, etc.; default all)");
		eO.setArgs(1);
		eO.setRequired(false);
		options.addOption(eO);
		
		Option doutO = new Option("d", "output dictionary (optional)");
		doutO.setArgs(1);
		doutO.setRequired(false);
		options.addOption(doutO);

		Option doutgzO = new Option("dgz", "output dictionary gzipped");
		doutgzO.setRequired(false);
		doutgzO.setArgs(0);
		options.addOption(doutgzO);
		
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
		int ticks = org.semanticweb.yars.nx.cli.Main.getTicks(cmd);
				
		InputStream is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
		Iterator<Node[]> nx = new NxParser(is);
		nx = new TicksIterator(nx,  ticks);

		OutputStream os = org.semanticweb.yars.nx.cli.Main.getMainOutputStream(cmd);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		Callback cb = new CallbackNxBufferedWriter(bw);
		_log.info("... files opened.");
		
		
		//open dictionary output
		Callback cbd = null;
		BufferedWriter bwd = null;
		if(cmd.hasOption(doutO.getOpt())){
			_log.info("Opening dictionary output ...");
			OutputStream dos = new FileOutputStream(cmd.getOptionValue(doutO.getOpt()));
			if(cmd.hasOption(doutgzO.getOpt())){
				dos = new GZIPOutputStream(dos);
			}
			bwd = new BufferedWriter(new OutputStreamWriter(dos));
			cbd = new CallbackNxBufferedWriter(bwd);
			_log.info("... opened.");
		}
		
		
		//load encode flag
		boolean[] b = {};
		if(cmd.hasOption(eO.getOpt())){
			b = NodeComparatorArgs.getBooleanMask(cmd.getOptionValue(eO.getOpt()));
		}
		
		HashMap<Node,Integer> dict = new HashMap<Node,Integer>();
		
		_log.info("Encoding ...");
		while(nx.hasNext()){
			Node[] next = nx.next();
			Node[] enc = new Node[next.length];
//			System.arraycopy(next, 0, enc, 0, next.length);
			for(int i=0; i<next.length; i++){
				if(b.length==0 || (b.length>i && b[i])){
					Integer id = dict.get(next[i]);
					if(id==null){
						id = dict.size();
						dict.put(next[i], id);
						if(cbd!=null){
							cbd.processStatement(new Node[]{next[i], new Literal(Integer.toString(id))});
						}
					}
					enc[i] = new Literal(Integer.toString(id));
				} else{
					enc[i] = next[i];
				}
			}
			cb.processStatement(enc);
		}
		_log.info("... done.");
		
		bw.close();
		if(bwd!=null)
			bwd.close();
		is.close();
	}
	
	
}
