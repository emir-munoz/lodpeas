package org.deri.conker.build.util.rank;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.deri.conker.build.util.uri.URIUtils;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.mem.MemoryManager;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.util.NxUtil;
import org.semanticweb.yars.util.LRUMapCache;
import org.semanticweb.yars.util.LRUSetCache;


/**
 * Extracts the document level graph from a file of quads. Uses
 * normalisation and redirects to map a data URI to the document
 * it dereferences to. Builds graph of pairs pointing from context
 * URIs to the documents dereferenced by the data URIs it mentions.
 * Only creates links between URIs that are already contexts in the
 * data. Output will not be sorted and may contain duplicates.
 * 
 * @author aidhog
 *
 */
public class DocumentGraph {
	static Logger _log = Logger.getLogger(DocumentGraph.class.getName());
	
	static int CACHE_SIZE = MemoryManager.estimateMaxStatements(3);
	
	/**
	 * Extract and write a document-level graph from a set of Quads.
	 * Document-level graph may not be sorted and may contain dupes.
	 * 
	 *  
	 * @param in Input quad iterator
	 * @param redirects Map from URIs to the URIs they redirect to
	 * @param contexts Map from contexts to themselves (used to check that a URI is a context and to recycle references).
	 * @return number of edges written
	 */
	public static int extractGraph(Iterator<Node[]> in, Map<? extends Node,? extends Node> redirects, Map<Node,Node> contexts, Callback cb){
		//cache recently seen normalised terms
		LRUMapCache<Node,Node> cacheNorm = new LRUMapCache<Node,Node>(CACHE_SIZE);
		LRUSetCache<Nodes> cacheLinks = new LRUSetCache<Nodes>(CACHE_SIZE);
		
		Node oldCon = null;
		int written = 0;
		
		while(in.hasNext()){
			Node[] next = in.next();
			
			Node con = oldCon;
			if(oldCon==null || !oldCon.equals(next[3])){
				//recycle references ...
				con = contexts.get(next[3]);
				if(con == null){
					_log.warning("Did not find context "+next[3]+" skipping.");
				}
			}
			
			if(con!=null) for(int i=0; i<3; i++){
				if(next[i] instanceof Resource){
					Node target = cacheNorm.get(next[i]);
					
					if(target==null){
						Resource r = (Resource) next[i];
						Resource rn = new Resource(NxUtil.escapeForNx(URIUtils.normalise(r.toString())));
						target = contexts.get(rn);
						
						if(target==null){
							target = redirects.get(rn);
							if(target!=null){
								target = contexts.get(target);
							}
						}
						
						if(target!=null){
							cacheNorm.put(next[i],target);
						}
					}
					
					if(target!=null && !con.equals(target)){
						Node[] link = new Node[]{con, target};
						if(cacheLinks.add(new Nodes(link))){
							cb.processStatement(new Node[]{con, target});
							written ++;
						}
					}
				}
			}
			
			oldCon = con;
		}
		
		return written;
	}
}
