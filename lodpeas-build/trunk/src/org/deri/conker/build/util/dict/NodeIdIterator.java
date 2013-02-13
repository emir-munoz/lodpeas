package org.deri.conker.build.util.dict;

import java.util.Iterator;
import java.util.TreeSet;

import org.semanticweb.yars.nx.Node;

/**
 * Takes a raw stream of Node[] containing literals
 * with numeric IDs and returns them in compressed 
 * NodeID form.
 * 
 */
public class NodeIdIterator implements Iterator<Node[]> {
	Iterator<Node[]> raw = null;
	boolean[] mask = new boolean[]{};
	 
	public NodeIdIterator(Iterator<Node[]> raw){
		this(raw,new boolean[]{});
	}
	
	public NodeIdIterator(Iterator<Node[]> raw, boolean[] mask){
		this.raw = raw;
		this.mask = mask;
	}
	
	public boolean hasNext() {
		return raw.hasNext();
	}

	public Node[] next() {
		Node[] next = raw.next();
		Node[] ids = new Node[next.length];
		for(int i=0; i<next.length; i++){
			if(mask.length==0 || (mask.length>i && mask[i])){
				ids[i] = new NodeId(Integer.parseInt(next[i].toString()));
			} else{
				ids[i] = next[i];
			}
		}
		return ids;
	}

	public void remove() {
		raw.remove();
	}
	
	public static class NodeId implements Node{
		/**
		 * 
		 */
		private static final long serialVersionUID = 2118089503605756112L;
		
		int id;
		
		public NodeId(int id){
			this.id = id;
		}
		
		public int compareTo(Object arg0) {
			if(!(arg0 instanceof NodeId)){
				return Integer.MAX_VALUE;
			} else{
				return (id < ((NodeId)arg0).id) ? -1 : ((id == ((NodeId)arg0).id) ? 0 : 1);
			}
		}
		
		public int hashCode() {
			return id;
		}
		
		public boolean equals(Object o){
			if(o == this || (o instanceof NodeId && ((NodeId)o).id == id))
				return true;
			return false;
		}

		public String toN3() {
			return "\""+id+"\"";
		}
		
		public String toString(){
			return Integer.toString(id);
		}
	}
	
	public static void main(String[] args){
		TreeSet<Node> stress = new TreeSet<Node>();
		for(int i=0; i<100000000; i++){
			stress.add(new NodeId(i));
			if(i%1000==0)
				System.err.println(i);
		}
	}
}
