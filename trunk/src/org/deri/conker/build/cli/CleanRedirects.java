package org.deri.conker.build.cli;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.deri.conker.build.util.cons.SameAsIndex;
import org.deri.conker.build.util.uri.RedirectsMap;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.namespace.OWL;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Removes dupes and cycles from redirects and performs transitive hops
 * such that a->b, b->c becomes a->c.
 * 
 * @author aidhog
 *
 */
public class CleanRedirects {
	static transient Logger _log = Logger.getLogger(CleanRedirects.class.getName());
	
	static int TICKS = org.semanticweb.yars.nx.cli.Main.TICKS;
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
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
		
		InputStream is = org.semanticweb.yars.nx.cli.Main.getMainInputStream(cmd);
		OutputStream os = org.semanticweb.yars.nx.cli.Main.getMainOutputStream(cmd);
		int ticks = org.semanticweb.yars.nx.cli.Main.getTicks(cmd);
		
		TICKS = ticks;
		
		Iterator<Node[]> nx = new NxParser(is);
		nx = new TicksIterator(nx, ticks);
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		Callback cb = new CallbackNxBufferedWriter(bw);
				
		_log.info("Loading redirects ... ");
		
		RedirectsMap rm = RedirectsMap.load(nx);
		
		_log.info("... done; forwarding and removing cycles ... ");
		
		rm.forwardAll();
		
		_log.info("... done; storing ... ");
		
		RedirectsMap.write(rm, cb);
		
		_log.info("... done");
		
		is.close();
		bw.close();
	}
	
	public static SameAsIndex extractOwlSameAs(Iterator<Node[]> in){
		SameAsIndex sai = new SameAsIndex();
		
		int c = 0, sa = 0;
		while(in.hasNext()){
			Node[] next = in.next();
			c++;
			
			if(next[1].equals(OWL.SAMEAS)){
				sai.addSameAs(next[0], next[2]);
				sa++;
			}
			
			if(c%TICKS==0 || !in.hasNext())
				_log.info("Read "+c+". Found "+sa+" owl:sameAs statements.");
		}
		return sai;
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
				if(!(next[2] instanceof Literal)){
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
