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
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.NodeComparator.NodeComparatorArgs;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.TicksIterator;

/**
 * Class to convert incremental Integer IDs back to strings.
 * Assumes dictionary can fit as a map in memory.
 * 
 * @author aidhog
 *
 */
public class DictionaryDecode {
	static transient Logger _log = Logger.getLogger(RankTerms.class.getName());
	
	public static void main(String[] args) throws IOException{
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		
		Option eO = new Option("p", "decode positions (e.g., 02 for subjects and objects, 1 for predicates, etc.; default all)");
		eO.setArgs(1);
		eO.setRequired(false);
		options.addOption(eO);
		
		Option dinO = new Option("d", "input dictionary");
		dinO.setArgs(1);
		dinO.setRequired(true);
		options.addOption(dinO);

		Option dingzO = new Option("dgz", "input dictionary gzipped");
		dingzO.setArgs(0);
		dingzO.setRequired(false);
		options.addOption(dingzO);
		
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
		
		
		//open dictionary input
		_log.info("Loading dictionary ...");
		InputStream dis = new FileInputStream(cmd.getOptionValue(dinO.getOpt()));
		if(cmd.hasOption(dingzO.getOpt()))
			dis = new GZIPInputStream(dis);
		Iterator<Node[]> dnx = new NxParser(dis);
		dnx = new TicksIterator(dnx,  ticks);
		
		HashMap<Integer,Node> inv_dict = new HashMap<Integer,Node>();
		while(dnx.hasNext()){
			Node[] next = dnx.next();
			
			try{
				Integer id = Integer.parseInt(next[1].toString());
				Node n = inv_dict.put(id,next[0]);
				if(n!=null){
					_log.warning("Multiple values for OID "+id+" in dictionary, including "+n+" "+next[0]);
				}
			} catch (Exception e){
				_log.warning("Error parsing dictionary entry :"+Nodes.toN3(next));
			}
		}
		_log.info("... dictionary loaded with "+inv_dict.size()+" keys.");
		
		//load decode flag
		boolean[] b = {};
		if(cmd.hasOption(eO.getOpt())){
			b = NodeComparatorArgs.getBooleanMask(cmd.getOptionValue(eO.getOpt()));
		}
		
		HashMap<Node,Integer> dict = new HashMap<Node,Integer>();
		
		_log.info("Decoding ...");
		while(nx.hasNext()){
			Node[] next = nx.next();
			Node[] dec = new Node[next.length];
//			System.arraycopy(next, 0, enc, 0, next.length);
			for(int i=0; i<next.length; i++){
				if(b.length==0 || (b.length>i && b[i])){
					try{
						Integer id = Integer.parseInt(next[i].toString());
						Node n = inv_dict.get(id);
						if(n==null){
							_log.warning("Cannot find id "+next[i]+" in dictionary! (Buffering as-is to output.)");
							dec[i] = next[i];
						} else{
							dec[i] = n;
						}
					} catch(Exception e){
						_log.warning("Cannot decode "+next[i]+"! (Buffering as-is to output.)");
						dec[i] = next[i];
					}
				} else{
					dec[i] = next[i];
				}
			}
			cb.processStatement(dec);
		}
		_log.info("... done.");
		
		bw.close();
		dis.close();
		is.close();
	}
}
