package org.deri.conker.build.util.rank;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.deri.conker.build.cli.Consolidate;
import org.deri.conker.build.cli.RankGraph;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.mem.MemoryManager;
import org.semanticweb.yars.nx.namespace.RDF;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;
import org.semanticweb.yars.nx.sort.SortIterator;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.LRUMapCache;

/**
 * Rank terms in a dataset based on summation of ranks of 
 * documents containing them.
 * 
 * @author aidhog
 *
 */
public class TermRank {
	static transient Logger _log = Logger.getLogger(RankGraph.class.getName());
	static final String TMP_FILE_NAME = "termranks.buffer.nx";
	static final String GZ_SUFFIX = ".gz";
	static final boolean GZ = true;
	
	static final Set<Node> SKIP = new HashSet<Node>();
	static {
		SKIP.add(Consolidate.CONTEXT);
	}
	
	
	/**
	 * Rank terms in a dataset based on summation of ranks of 
	 * documents containing them.
	 * @param in Input dataset (quadruples sorted by context)
	 * @param ranks Ranks of documents
	 * @param cb Output for ranks
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static void rankTerms(Iterator<Node[]> in, Map<Node,Double> ranks, Callback cb) throws IOException, ParseException{
		rankTerms(new TermRankArgs(in, ranks), cb);
	}
	
	/**
	 * Rank terms in a dataset based on summation of ranks of 
	 * documents containing them.
	 * @param tra Arguments for process
	 * @param cb Output for ranks
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void rankTerms(TermRankArgs tra, Callback cb) throws IOException, ParseException{
		Iterator<Node[]> in = tra._in;
		Map<Node,Double> ranks = tra._ranks;
		
		String tmpdir = org.semanticweb.yars.nx.cli.Main.getTempSubDir();
		String tmpfile = tmpdir+TMP_FILE_NAME;
		OutputStream os = null;
		if(GZ){
			tmpfile+=GZ_SUFFIX;
			os = new FileOutputStream(tmpfile);
			os = new GZIPOutputStream(os);
		} else{
			os = new FileOutputStream(tmpfile);
		}
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		CallbackNxBufferedWriter buffer = new CallbackNxBufferedWriter(bw);
		
		LRUTermCache termRankCache = new LRUTermCache(buffer,MemoryManager.estimateMaxNodes());
		
		_log.info("Ranking terms ...");
		
		boolean done = !in.hasNext();
		Node oldc = null;
		HashSet<Node> terms = new HashSet<Node>();
		
		while(!done){
			Node[] next = null;
			if(in.hasNext()){
				next = in.next();
				if(SKIP.contains(next[3])){
					continue;
				}
			} else{
				done = true;
			}
			
			//if there's a new context
			if(done || (oldc!=null && !next[3].equals(oldc))){
				Double rank = ranks.get(oldc);
				if(rank==null){
					_log.warning("No rank available for "+oldc);
				} else{
					for(Node t:terms){
						Double trank = termRankCache.get(t);
						if(trank==null){
							trank = 0d;
						}
						termRankCache.put(t, trank+rank);
					}
				}
				terms.clear();
			}
			
			if(!done){
				for(int i=0; i<3; i++){
					if(tra.consider(next[i],i,next[1])){
						terms.add(next[i]);
					}
				}
				oldc = next[3];
			}
		}
		
		_log.info("... finished pass ...");
		
		// nothing needed to be buffered
		// everything is in memory
		if(termRankCache.buffered() == 0){
			bw.close();
			_log.info("Done in memory ... outputing "+termRankCache.size()+" ranks to output.");
			PageRank.outputRanks(termRankCache, cb);
		} else{ //need to scan and summate buffered ranks 
			//first dump remaining ranks to buffer
			_log.info("Buffering "+termRankCache.size()+" remaining ranks to buffer.");
			PageRank.outputRanks(termRankCache, buffer);
			bw.close();
			
			//free space
			_log.info("...opening "+ termRankCache.size()+termRankCache.buffered()+" ranks from buffer "+tmpfile);
			termRankCache = null;
			
			InputStream is = new FileInputStream(tmpfile);
			if(GZ)
				is = new GZIPInputStream(is);
			NxParser nxp = new NxParser(is);
			
			_log.info("...sorting buffer...");
			SortIterator si = new SortIterator(nxp);
			
			Node old = null;
			done = !si.hasNext();
			double rank = 0;
			long count = 0;
			while(!done){
				Node[] next = null;
				if(si.hasNext()){
					next = si.next();
				} else{
					done = true;
				}
				
				if(done || (old!=null && !old.equals(next[0]))){
					count++;
					cb.processStatement(new Node[]{old, new Literal(Double.toString(rank))});
					rank = 0;
				}
				if(!done){
					rank += Double.parseDouble(next[1].toString());
					old = next[0];
				}
			}
			_log.info("... done. Wrote "+count+" unique ranks.");
		}
		
		_log.info("...finished.");
	}
	
	/**
	 * An in-memory cache for term ranks. Adds up ranks in
	 * memory and buffers them to callback if full.
	 * 
	 * @author aidhog
	 *
	 */
	public static class LRUTermCache extends LRUMapCache<Node,Double>{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3631283160220232545L;
		transient Callback _cb = null;
		
		int _buffered = 0;
		
		public LRUTermCache(Callback cb){
			this(cb, MemoryManager.estimateMaxNodes());
		}
		
		public LRUTermCache(Callback cb, int size){
			super(size);
			_cb = cb;
		}
		
		public int buffered(){
			return _buffered;
		}
		
		// logs the rank before removing it
		protected boolean removeEldestEntry(Map.Entry<Node,Double> entry){
			if(super.removeEldestEntry(entry)){
				if(_cb != null){
					_cb.processStatement(new Node[]{entry.getKey(), new Literal(entry.getValue().toString())});
				} else{
					_log.warning("Callback for TermRank is null. May lose rank for "+entry);
				}
				_buffered ++;
				return true;
			}
			return false;
		}
	}

	/**
	 * Arguments for ranking terms.
	 * 
	 * @author aidhog
	 *
	 */
	public static class TermRankArgs{
		public static final boolean DEFAULT_ABOX = true;
		public static final boolean DEFAULT_TBOX = true;
		
		public static final boolean DEFAULT_LTIS = false;
		public static final boolean DEFAULT_BNODES = true;
		
		Iterator<Node[]> _in;
		Map<Node,Double> _ranks;

		boolean _tbox;
		boolean _abox;

		boolean _lits;
		boolean _bnodes;
		
		public TermRankArgs(Iterator<Node[]> in, Map<Node,Double> ranks){
			_in = in;
			_ranks = ranks;
		}

		/**
		 * Consider terms in the predicate and value position
		 * of rdf:type triples as linked.
		 * @param tbox
		 */
		public void setConsiderTboxPositions(boolean tbox){
			_tbox = tbox;
		}

		/**
		 * Consider terms in the subject position or the object
		 * object position of non-rdf:type triples as linked.
		 * @param tbox
		 */
		public void setConsiderAboxPositions(boolean abox){
			_abox = abox;
		}

		/**
		 * Rank literal terms.
		 * @param lits
		 */
		public void setRankLiterals(boolean lits){
			_lits = lits;
		}

		/**
		 * Rank BNode terms.
		 * @param bnodes
		 */
		public void setRankBNodes(boolean bnodes){
			_bnodes = bnodes;
		}
		
		/**
		 * Should the node in the position with the predicate
		 * in the triple be considered as "linked" from the context?
		 * 
		 * @param n
		 * @param pos
		 * @param pred
		 * @return
		 */
		protected boolean consider(Node n, int pos, Node pred){
			if(n instanceof Resource || 
					(n instanceof Literal && _lits) ||
					(n instanceof BNode && _bnodes)){
				boolean rdftype = pred.equals(RDF.TYPE);
				
				if((_abox && (pos==0 || (pos==2 && !rdftype)))
				  || (_tbox && (pos==1 || (pos==2 && rdftype)))){
					return true;
				}
			}
			return false;
		}
	}
}
