package org.deri.conker.build.cli;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.deri.conker.build.util.uri.URIUtils;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.NodeComparator;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.mem.MemoryManager;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.LRUMapCache;
import org.semanticweb.yars.util.TicksIterator;

/**
 * Class to convert strings to incremental Integer IDs.
 * Assumes dictionary can fit as a map in memory.
 * 
 * @author aidhog
 *
 */
public class ExtractPldGraph {
	static transient Logger _log = Logger.getLogger(RankTerms.class.getName());
	static Resource NO_PLD = new Resource("http://"+URIUtils.NO_PLD+"/");
	
	public static void main(String[] args) throws IOException{
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();

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
		int ticks = org.semanticweb.yars.nx.cli.Main.getTicks(cmd);
				
		InputStream is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
		Iterator<Node[]> nx = new NxParser(is);
		nx = new TicksIterator(nx,  ticks);

		OutputStream os = org.semanticweb.yars.nx.cli.Main.getMainOutputStream(cmd);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		Callback cb = new CallbackNxBufferedWriter(bw);
		_log.info("... files opened.");
		
		
		//start parsing PLDs
		TreeSet<Node[]> pldGraph = new TreeSet<Node[]>(NodeComparator.NC);
		TreeSet<Node> plds = new TreeSet<Node>();
		
		LRUMapCache<Node,Node> pldCache = new LRUMapCache<Node,Node>(MemoryManager.estimateMaxNodes());
		
		_log.info("Mapping to plds ...");
		boolean skip = false;
		long skipped = 0;
		long refl = 0;
		while(nx.hasNext()){
			Node[] next = nx.next();
			Node[] pldn = new Node[next.length];
			skip = false;
			
			for(int i=0; i<next.length; i++){
				if(next[i] instanceof Resource){
					Node pld = getPld(pldCache, next[i]);
					if(pld.equals(NO_PLD)){
						skip = true;
					} else {
						pldn[i] = pld;
					}
				} else{
					skip = true;
				}
			}
			
			if(skip){
				skipped++;
			} else if(pldn[0].equals(pldn[1])){
				refl++;
			} else{
				pldGraph.add(pldn);
				for(Node pld:pldn)
					plds.add(pld);
			} 
		}
		_log.info("... read to memory. Found "+pldGraph.size()+" edges. Found "+plds.size()+" plds. Skipped "+skipped+" wo/ plds. Skipped "+refl+" reflexive links.");
		
		_log.info("Dumping ...");
		for(Node[] na: pldGraph){
			cb.processStatement(na);
		}
		_log.info("... done.");
		
		bw.close();
		is.close();
	}
	
	
	private static final Node getPld(LRUMapCache<Node,Node> pldCache, Node n){
		Node pld = pldCache.get(n);
		if(pld==null){
			pld = new Resource("http://"+URIUtils.getPld(n)+"/");
			pldCache.put(n,pld);
		}
		return pld;
	}
	
}
