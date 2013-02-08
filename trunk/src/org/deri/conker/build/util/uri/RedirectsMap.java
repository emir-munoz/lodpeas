package org.deri.conker.build.util.uri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.parser.Callback;

/**
 * Class that stores redirects as a map of source -> target
 * Resource objects. Allows for "forwarding" transitive
 * redirects to their final destination. Also removes cycles.
 * 
 * A static method allows for loading a RedirectsMap from an
 * iterator of Node pairs.
 * 
 * @author aidhog
 *
 */
public class RedirectsMap extends HashMap<Resource,Resource> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 802795213472545733L;
	static Logger _log = Logger.getLogger(RedirectsMap.class.getName());
	
	public RedirectsMap(){
		super();
	}

	/**
	 * If a -> b and b -> c, then a -> b is replaced
	 * with a -> c. Cycles are detected and warned about
	 * in the log.
	 */
	public void forwardAll(){
		HashSet<Resource> remove = new HashSet<Resource>();
		HashSet<Nodes> forwarded = new HashSet<Nodes>();
		RedirectsMap rm = new RedirectsMap();
		
		_log.info("Forwarding redirects (size "+size()+")...");
		int i = 0;
		
		do{
			i++;
			_log.info("Loop "+i+" ...");
			remove.clear();
			rm.clear();
			
			for(Map.Entry<Resource, Resource> e: entrySet()){
				if(e.getKey().equals(e.getValue())){
					_log.warning("Cycle for "+e.getKey().toN3()+" ... removing.");
					remove.add(e.getKey());
				} else{
					Resource c = get(e.getValue());
					if(c!=null && !c.equals(e.getValue())){
						if(forwarded.add(new Nodes(e.getKey(),c)))
							rm.put(e.getKey(), c);
						else{
							_log.warning("Cycle through path "+e.getKey()+" "+c+". Removing both.");
							remove.add(e.getKey());
							remove.add(c);
						}
					}
				}
			}
			
			putAll(rm);
			
			for(Resource r:remove){
				remove(r);
			}
			_log.info("... loop done "+i+" : removed "+remove.size()+"; forwarded "+rm.size()+".");
			if(rm.size()<20){
				StringBuffer sb = new StringBuffer();
				for(Map.Entry<Resource,Resource> r:rm.entrySet()){
					sb.append(r.getKey()+"\t"+r.getValue()+"\n");
				}
				_log.info(sb.toString());
			}
		} while(!remove.isEmpty() || !rm.isEmpty());
		_log.info("Forwarded redirects (size "+size()+")...");
	}
	
	/**
	 * Loads a redirects map from an input iterator of Node pairs.
	 * Each pair is taken to represent { source, target }. Only
	 * Resource types are read.
	 * 
	 * @param in Input iterator of redirect pairs
	 * @return Redirects map with URIs forwarded.
	 */
	public static RedirectsMap load(Iterator<Node[]> in){
		RedirectsMap rm = new RedirectsMap();
		_log.info("Loading redirects ...");
		while(in.hasNext()){
			Node[] next = in.next();
			if(next.length==2 && next[0] instanceof Resource && next[1] instanceof Resource && next[0].toN3().length()>2 && next[1].toN3().length()>2){
				Resource source = (Resource)next[0];
				Resource target = new Resource(URIUtils.normalise(next[1].toString()));
				
				Resource old = rm.put(source,target);
				if(old!=null && !old.equals(target)){
					_log.warning("Source URI "+source.toN3()+" redirects to multiple URIs including "+old.toN3()+" and "+target.toN3()+" ... accepting "+target.toN3());
				}
			} else{
				_log.warning("Unrecognised redirect entry "+Nodes.toN3(next));
			}
		}
		_log.info("... loaded redirects");
		
		return rm;
	}
	
	/**
	 * Write redirects in Nx from in-memory RedirectsMap object.
	 * @param rm object to write
	 * @param cb callback for Nx output
	 */
	public static void write(RedirectsMap rm, Callback cb){
		for(Map.Entry<Resource,Resource> r:rm.entrySet()){
			cb.processStatement(new Node[]{r.getKey(), r.getValue()});
		}
	}
	
//  QUICK TEST CASE	
//	public static void main(String[] args){
//		RedirectsMap rm = new RedirectsMap();
//		rm.put(new Resource("a"), new Resource("b"));
//		rm.put(new Resource("b"), new Resource("c"));
//		rm.put(new Resource("c"), new Resource("a"));
//		
//		System.err.println(rm);
//		
//		rm.forwardAll();
//		System.err.println(rm);
//		
//		rm = new RedirectsMap();
//		rm.put(new Resource("a"), new Resource("b"));
//		rm.put(new Resource("b"), new Resource("c"));
//		rm.put(new Resource("c"), new Resource("d"));
//		rm.put(new Resource("d"), new Resource("a"));
//		rm.put(new Resource("e"), new Resource("f"));
//		
//		rm.forwardAll();
//		System.err.println(rm);
//	}
}
