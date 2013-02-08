package org.deri.conker.build.cli;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.deri.conker.build.util.dict.NodeIdIterator;
import org.deri.conker.build.util.rank.PageRank;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.ResetableIterator;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Rank an extracted graph.
 * 
 * @author aidhog
 *
 */
public class RankGraph {
	static transient Logger _log = Logger.getLogger(RankGraph.class.getName());

	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		
		Option eO = new Option("e", "flag to state that graph is integer encoded (saves space)");
		eO.setRequired(false);
		eO.setArgs(0);
		options.addOption(eO);

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
		
		ResetableFileIterator rfi = new ResetableFileIterator(cmd.getOptionValue("i"),cmd.hasOption("igz"),cmd.hasOption(eO.getOpt()),org.semanticweb.yars.nx.cli.Main.getTicks(cmd));

		OutputStream os = org.semanticweb.yars.nx.cli.Main.getMainOutputStream(cmd);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		Callback cb = new CallbackNxBufferedWriter(bw);
		_log.info("... files opened.");

		//recycle string references
//		HashMap<Node,Node> nodes = new HashMap<Node,Node>();
//		nx = new FlyweightNodeIterator(nodes, nx);
//		
//		_log.info("Loading graph from file ...");
//		TreeSet<Node[]> graph = new TreeSet<Node[]>(NodeComparator.NC);
//		while(nx.hasNext()){
//			graph.add(nx.next());
//		}
//		_log.info("... loaded "+graph.size()+" unique edges and "+nodes.size()+" unique nodes.");
//		
//		nodes = null;
//		nx = null;
//		
//		ResetableCollectionIterator<Node[]> iter = new ResetableCollectionIterator<Node[]>(graph);
		
		// # rank graph
		_log.info("Ranking document-level graph ...");
		Map<Node,Double> ranks = PageRank.process(rfi);
		_log.info("... done; writing to output ...");
		PageRank.outputRanks(ranks, cb);
		_log.info("... done.");
		
		bw.close();
		rfi.close();
	}
	
	
	public static class ResetableFileIterator implements ResetableIterator<Node[]>{
		Iterator<Node[]> iter = null;
		InputStream is = null;
		String in = null;
		boolean gz = false;
		boolean ids = false;
		int ticks = 0;
		Exception e = null;
		
		public ResetableFileIterator(String in, boolean gz){
			this(in,gz,false);
		}
		
		public ResetableFileIterator(String in, boolean gz, boolean ids){
			this.in = in;
			this.gz = gz;
			this.ids = ids;
			reset();
		}
		
		public ResetableFileIterator(String in, boolean gz, boolean ids, int ticks){
			this.in = in;
			this.gz = gz;
			this.ids = ids;
			this.ticks = ticks;
			reset();
		}

		private void resetFile() throws IOException {
			InputStream is = new FileInputStream(in);
			if(gz)
				is = new GZIPInputStream(is);
			iter = new NxParser(is);
			if(ids)
				iter = new NodeIdIterator(iter);
			if(ticks>0)
				iter = new TicksIterator(iter,ticks);
		}

		public boolean hasNext() {
			return iter!=null && iter.hasNext();
		}

		public Node[] next() {
			if(!hasNext())
				throw new NoSuchElementException();
			return iter.next();
		}

		public void remove() {
			iter.remove();
		}
		
		public void close() throws IOException{
			if(is!=null)
				is.close();
		}

		public void reset() {
			iter = null;
			try{
				resetFile();
			} catch(Exception e){
				this.e = e;
			}
		}
		
		public Exception getException(){
			return e;
		}
		
	}
}
