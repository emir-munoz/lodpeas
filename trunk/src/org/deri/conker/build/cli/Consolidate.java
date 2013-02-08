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
import org.deri.conker.build.util.cons.SameAsIndex;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.namespace.OWL;
import org.semanticweb.yars.nx.namespace.RDF;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.SniffIterator;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Use owl:sameAs relations in the data to consolidate/canonicalise
 * aliases.
 * 
 * @author aidhog
 *
 */
public class Consolidate {
	static transient Logger _log = Logger.getLogger(Consolidate.class.getName());
	
	public static final Resource CONTEXT = new Resource("http://swse.deri.org/#owlSameAsClosure"); 
	
	static int TICKS = org.semanticweb.yars.nx.cli.Main.TICKS;
	
	//default to write pivot-alias owl:sameAs relations to output
	static int DEFAULT_WSA = 1;
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();
		
		Option wsaO = new Option("sa", "embed closed sameAs relations into output [0 - don't; 1 - pivot to alias; 2 - pivot to alias and alias to pivot] default 1");
		wsaO.setRequired(false);
		options.addOption(wsaO);
		
		Option rinO = new Option("r", "input ID ranks file (optional; for selecting best-ranked canonical identifiers; unsorted is fine)");
		rinO.setArgs(1);
		options.addOption(rinO);

		Option ringzO = new Option("rgz", "flag to state that ID ranks file is GZipped (default not)");
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
		
		InputStream is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
		OutputStream os = org.semanticweb.yars.nx.cli.Main.getMainOutputStream(cmd);
		int ticks = org.semanticweb.yars.nx.cli.Main.getTicks(cmd);
		
		TICKS = ticks;
		
		Integer wsa = null;
		
		if(options.getOption(wsaO.getArgName())!=null){
			wsa = Integer.parseInt(wsaO.getValue());
		}
		
		if(wsa==null) wsa = DEFAULT_WSA;
		
		Iterator<Node[]> nx = new NxParser(is);
		SniffIterator in = new SniffIterator(nx);
		
		int nxlen = in.nxLength();
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		Callback cb = new CallbackNxBufferedWriter(bw);
		
		InputStream ris = null;
		Iterator<Node[]> rin = null;
		
		if(cmd.hasOption(rinO.getOpt())){
			ris = new FileInputStream(cmd.getOptionValue(rinO.getOpt()));
			if(cmd.hasOption(ringzO.getOpt())){
				ris = new GZIPInputStream(ris);
			}
			rin = new NxParser(ris);
			rin = new TicksIterator(rin, ticks);
		}
				
		_log.info("Extracting owl:sameAs data ... ");
		SameAsIndex sai = SameAsIndex.extract(in);
		_log.info(" ... done ... ");
		
		if(rin!=null && rin.hasNext()){
			_log.info("Ranking canonical identifiers ...");
			while(rin.hasNext()){
				sai.setRank(rin.next());
			}
			ris.close();
			_log.info(" ... done ...");
		}
		
		is.close();
		is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
		nx = new NxParser(is);
		in = new SniffIterator(nx);
		
		_log.info("Rewriting data ... ");
		consolidate(in, sai, cb);
		_log.info(" ... done ... ");
		
		if(wsa>0){
			_log.info("Writing closed owl:sameAs relations ... ");
			if(nxlen>3)
				sai.writeSameAs(cb, CONTEXT, wsa==2);
			else sai.writeSameAs(cb, null, wsa==2);
			_log.info(" ... done ... ");
		}
		
		_log.info("Writing owl:sameAs closure stats ... ");
		sai.logStats(10, 0);
		_log.info("... done ...");
		
		is.close();
		bw.close();
	}
	
	public static void consolidate(Iterator<Node[]> in, SameAsIndex sai, Callback cb){
		int sa = 0, c = 0, rws = 0, rwo = 0;
		
		while(in.hasNext()){
			c++;
			
			Node[] next = in.next();
			if(next[1].equals(OWL.SAMEAS)){
				sa++;
			} else{
				next[0] = sai.getPivot(next[0]);
				if(sai.wasNew()){
					rws++;
				}
				if(!(next[2] instanceof Literal) && !next[1].equals(RDF.TYPE)){
					next[2] = sai.getPivot(next[2]);
					if(sai.wasNew()){
						rwo++;
					}
				}
				
				cb.processStatement(next);
			}
			
			if(c%TICKS==0 || !in.hasNext())
				_log.info("Read "+c+". Skipped "+sa+" owl:sameAs statements. Rewrote "+rws+" subjects and "+rwo+" objects.");
		}
	}

}
