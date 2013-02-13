package org.deri.conker.build.util.cons;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deri.conker.build.util.uri.URIUtils;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.namespace.OWL;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.stats.Count;

public class SameAsIndex implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7558342206327360421L;
	private static Logger _log = Logger.getLogger(SameAsIndex.class.getName());
	
	//same as list ... map from nodes to their aliases (including self)
	private HashMap<Node, SameAsList> _sal;
	
	//number of unique sameAs partitions (equivalence classes)
	private transient int _eqcs = 0; //eqcs needed for serialisation/deserialisation
	
	private final static byte[] DOT_NEWLINE = ("."+System.getProperty("line.separator")).getBytes();
	
	/**
	 * Default serialisation keeps copies of all objects seen thus far
	 * to ensure consistent references. This has lead to memory problems
	 * when serialising the large SameAsIndex across the wire. Thus, we
	 * override the writeObject() and readObject() methods with more
	 * memory efficient versions. We considered using writeUnshared()/
	 * readUnshared(), but these will continue to store the "deeper"
	 * objects such as the node strings. Instead, we flush the cache
	 * of objects after SERIALISATION_RESET_COUNTER SameAsLists have been
	 * written. The readObject() method is implemented to ensure objects are 
	 * properly interned.
	 */
//	private static final int SERIALISATION_RESET_COUNTER = 500;

	public static final String NO_PLD = "no_pld";
	
	// was the last getPivot() call different from argument?
	private boolean _change = false; 
	
	public SameAsIndex(){
		_sal = new HashMap<Node, SameAsList>();
	}

	public int size(){
		return _sal.size();
	}

	public boolean addSameAs(Node a, Node b){
		if(a.equals(b)){
			return false;
		}

		SameAsList sala = _sal.get(a);
		SameAsList salb = _sal.get(b);

		if(sala==null && salb==null){
			SameAsList sal = new SameAsList();
			sal.add(a);
			sal.add(b);
			_sal.put(a, sal);
			_sal.put(b, sal);
			_eqcs++;
		} else if(sala==null){
			salb.add(a);
			_sal.put(a, salb);
		} else if(salb==null){
			sala.add(b);
			_sal.put(b, sala);
		} else if(sala!=salb){
			sala.addAll(salb);
			_eqcs--;
			for(Node n:salb){
				_sal.put(n, sala);
			}
		} else{
			return false;
		}
		return true;
	}

	public SameAsList getSameAsList(Node n){
		return _sal.get(n);
	}
	
	/**
	 * See if last getPivot call returned a node different
	 * from the argument.
	 * 
	 * @return true if different, false if not or if getPivot() was not called.
	 */
	public boolean wasNew(){
		return _change;
	}

	public Node getPivot(Node n){
		SameAsList sal = getSameAsList(n);

		_change = false;
				
		if(sal==null){
			return n;
		} 
		Node pivot = sal.getPivot();

		if(pivot==null){
			return n;
		}
		
		_change = true;
		return pivot;
	}

	public void logStats(){
		logStats(0,0);
	}
	
	public void logStats(int topK, int randK){
		SameAsStatistics sas = new SameAsStatistics(topK, randK);
		
		for(Map.Entry<Node, SameAsList> e:_sal.entrySet()){
			if(e.getKey().equals(e.getValue().getPivot())){
				sas.addSameasList(e.getValue());
			}
		}

		sas.logStats(_log, Level.INFO);
	}
	
	public static TreeSet<String> getPldPairs(TreeSet<String> plds){
		TreeSet<String> pairs = new TreeSet<String>();
		String[] sa = new String[plds.size()];
		plds.toArray(sa);
		
		for(int i=0; i<sa.length; i++){
			for(int j=i+1; j<sa.length; j++){
				pairs.add(sa[i]+" "+sa[j]);
			}
		}
		return pairs;
	}

	
	/**
	 * Write the sameAs information from this index to a callback. The sameAs
	 * relations will be written in compressed form from pivot to non-pivot. 
	 * 
	 * @param c Callback for output
	 */
	public void writeSameAs(Callback c){
		writeSameAs(c, null, false);
	}

	/**
	 * Write the sameAs information from this index to a callback. The sameAs
	 * relations will be written in compressed form from pivot to non-pivot. 
	 * 
	 * @param c Callback for output
	 * @param context Context is quads should be printed (set null for triples)
	 * @param sym Set true if sameAs relations from non-pivot to pivot should 
	 * also be printed. 
	 */
	public void writeSameAs(Callback c, Node context, boolean sym){
		for(Map.Entry<Node, SameAsList> sa:_sal.entrySet()){
			Node p = sa.getValue().getPivot();
			if(p!=null && sa.getKey().equals(p)){
				for(Node n:sa.getValue()){
					if(!p.equals(n)){
						if(context!=null){
							c.processStatement(new Node[]{p, OWL.SAMEAS, n, context});
							if(sym)
								c.processStatement(new Node[]{n, OWL.SAMEAS, p, context});
						} else{
							c.processStatement(new Node[]{p, OWL.SAMEAS, n});
							if(sym)
								c.processStatement(new Node[]{n, OWL.SAMEAS, p});
						}
					}
				}
			}
		}
	}
	
	/**
	 * Takes a node/rank pair and sets node as pivot if higher
	 * than rank of existing node.
	 * 
	 * @param nx Node/rank pair.
	 */
	public void setRank(Node[] nx){
		try{
			double rank = Double.parseDouble(nx[1].toString());
			setRank(nx[0], rank);
		} catch(NumberFormatException e){
			_log.warning("Error parsing rank "+e.getMessage());
		}
	}

	/**
	 * Takes a node/rank pair and sets node as pivot if higher
	 * than rank of existing node.
	 * 
	 * @param n Node
	 * @param f Rank
	 */
	public void setRank(Node n, double r){
		SameAsList sal = getSameAsList(n);
		if(sal!=null){
			try{
				double mrank = sal.getMaxRank();

				if(r>mrank){
					sal.setMaxRank(r);
					sal.setPivot(n);
				}
			} catch(NumberFormatException e){
				_log.warning("Error parsing rank "+e.getMessage());
			}
		}
	}

	
	//need a streaming serialisation/deserialisation...
	//default too memory-greedy
	private void writeObject(ObjectOutputStream oos) throws IOException {
		_log.info("Serialising sameAs index with "+_eqcs+" equivalence classes and "+_sal.size()+" nodes...");
		oos.writeInt(_eqcs);
		oos.writeInt(_sal.size());
		
		int eqc = 0, nc = 0;
		for(Map.Entry<Node, SameAsList> e:_sal.entrySet()){
			Node p = e.getValue().getPivot();
			if(e.getKey().equals(p)){
				oos.write((p.toN3()+" ").getBytes());
				for(Node n:e.getValue()){
					if(!n.equals(p)){
						oos.write((n.toN3()+" ").getBytes());
					}
				}
				oos.write(DOT_NEWLINE);
				
				nc += e.getValue().size();
//				oos.writeObject(e.getValue());
				
				eqc++;
				
//				if(eqc%SERIALISATION_RESET_COUNTER == 0){
//					oos.reset();
//				}
			}
		}
		if(eqc!=_eqcs){
			_log.severe("SameAsIndex serialisation exception: expecting "+_eqcs+" equivalence classes but found "+eqc);
			throw new IOException("SameAsIndex serialisation exception: expecting "+_eqcs+" equivalence classes but found "+eqc);
		} else if(nc!=_sal.size()){
			_log.severe("SameAsIndex serialisation exception: expecting "+_sal.size()+" nodes but found "+nc);
			throw new IOException("SameAsIndex serialisation exception: expecting "+_sal.size()+" nodes but found "+nc);
		}
		_log.info("Serialised "+_eqcs+" equivalence classes...");
	}
	
	
	//need a streaming serialisation/deserialisation...
	//default too memory-greedy
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		_log.info("Deserialising sameAs index...");
		_sal = new HashMap<Node,SameAsList>();
		_eqcs = ois.readInt();
		_log.info("...expecting "+_eqcs+" equivalence classes...");
		int size = ois.readInt();
		_log.info("...and "+size+" nodes...");
		
		int sets = 0;

		NxParser nxp = new NxParser(ois);

		while(nxp.hasNext()){
			Node[] set = nxp.next();
			SameAsList sal = new SameAsList();
			for(Node n:set){
				sal.add(n);
				_sal.put(n, sal);
			}
			sets++;
			sal.setPivot(set[0]);
		}

		
		if(size!=_sal.size()){
			_log.severe("SameAsIndex deserialisation exception: expecting "+size+" nodes but found "+_sal.size());
			throw new IOException("SameAsIndex deserialisation exception: expecting "+size+" nodes but found "+_sal.size());
		} else if(sets!=_eqcs){
			_log.severe("SameAsIndex deserialisation exception: expecting "+_eqcs+" sets but found "+sets);
			throw new IOException("SameAsIndex deserialisation exception: expecting "+_eqcs+" sets but found "+sets);
		}
		_log.info("deserialised sameAs index with "+_eqcs+" equivalence classes and "+size+" nodes...");
	}

	public static class SameAsList extends PriorityQueue<Node> implements Comparable<SameAsList>{
		/**
		 * 
		 */
		private static final long serialVersionUID = -5975665868041226497L;
		Node _pivot = null;
		transient double _maxrank = 0;

		public SameAsList(){
			super(2);
		}

		public void setPivot(Node n){
			_pivot = n;
		}

		public void setMaxRank(double f){
			_maxrank = f;
		}

		public double getMaxRank(){
			return _maxrank;
		}

		public Node getPivot(){
			if(_pivot==null){
				return this.peek();
			}
			return _pivot;
		}

		public int compareTo(SameAsList arg0) {
			if(this==arg0){
				return 0;
			} else if(arg0.size()>size()){
				return 1;
			} else if(arg0.size()==size()){
				Node n1 = arg0.peek();
				Node n2 = peek();
				return n2.compareTo(n1);
			} else{
				return -1;
			}
		}

		public String toString(){
			StringBuffer buf = new StringBuffer();
			buf.append("Size: "+size()+" Pivot: "+_pivot+" Els: ");
			for(Node n:this){
				buf.append(n.toN3()+" ");
			}
			return buf.toString();
		}
	}
	
	public static class SameAsStatistics implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private static final Resource DEBUG_NODE = new Resource("http://1.status.net/user/45");
		
		private final int topK;
		private final int randK;
		
		Count<Integer> distrib = null;
		Count<Integer> pldDistrib = null;
		Count<String> topPldPairs = null;
		
		PriorityQueue<PriorityQueue<Node>> topkEQCS = null;
		PriorityQueue<PriorityQueue<Node>> randomkEQCS = null;
		
		PriorityQueue<Set<String>> topkPLDS = null;
		PriorityQueue<Set<String>> randomkPLDS = null;
		
		ArrayList<PriorityQueue<Node>> specialEQCS = null;
		
		int lits = 0, bnodes = 0, uris = 0;
		int plits = 0, pbnodes = 0, puris = 0;
		int terms = 0, eqcs = 0;
		
		public SameAsStatistics(){
			this(0, 0);
		}
		
		public SameAsStatistics(int topK, int randK){
			this.topK = topK;
			this.randK = randK;
			
			distrib = new Count<Integer>();
			pldDistrib = new Count<Integer>();
			
			specialEQCS = new ArrayList<PriorityQueue<Node>>();
			
			if(topK>0){
				topkEQCS = new PriorityQueue<PriorityQueue<Node>>(topK+1, new PQueueSizeComparator<Node>());
				topkPLDS = new PriorityQueue<Set<String>>(topK+1, new SetSizeComparator<String>());
				topPldPairs = new Count<String>();
			}
			
			if(randK>0){
				randomkEQCS = new PriorityQueue<PriorityQueue<Node>>(randK+1, new PQueueRandomComparator<Node>());
				randomkPLDS = new PriorityQueue<Set<String>>(randK+1, new SetRandomComparator<String>());
			}
		}
		
		public void addSameasList(SameAsList sal){
			distrib.add(sal.size());
			
			eqcs++;
			
			if(topK>0){	
				topkEQCS.add(sal);
				if(topkEQCS.size()==(topK+1)){
					topkEQCS.poll();
				}
			}
			
			if(randK>0){
				randomkEQCS.add(sal);
				if(randomkEQCS.size()==(randK+1)){
					randomkEQCS.poll();
				}
			}
			
			PLDSet plds = new PLDSet(sal);
			for(Node n:sal){
				terms++;
				plds.add(URIUtils.getPld(n));
				if(n instanceof BNode){
					bnodes++;
				} else if(n instanceof Resource){
					uris++;
				} else{
					lits++;
				}
			}
			
			pldDistrib.add(plds.size());
			
			if(DEBUG_NODE!=null && sal.contains(DEBUG_NODE))
				specialEQCS.add(sal);
			
			if(topK>0){
				topkPLDS.add(plds);
				if(topkPLDS.size()==(topK+1)){
					topkPLDS.poll();
				}
			}
			
			if(randK>0){
				randomkPLDS.add(plds);
				if(randomkPLDS.size()==(randK+1)){
					randomkPLDS.poll();
				}
			}
			
			Node p = sal.getPivot();
			if(p instanceof BNode){
				pbnodes++;
			} else if(p instanceof Resource){
				puris++;
			} else{
				plits++;
			}
			
			if(topK>0){
				for(String s:getPldPairs(plds)){
					topPldPairs.add(s);
				}
			}
		}
		
		public void logStats(Logger log, Level l){
			log.log(l, "Equivalence classes: "+eqcs);
			log.log(l, "Terms: "+terms);
			log.log(l, "Average equiv class: "+(double)terms/(double)eqcs);
			log.log(l, "Literals: "+lits);
			log.log(l, "Blank-nodes: "+bnodes);
			log.log(l, "URIs: "+uris);
			log.log(l, "Pivot literals: "+plits);
			log.log(l, "Pivot blank-nodes: "+pbnodes);
			log.log(l, "Pivot URIs: "+puris);
			log.log(l, "====Equivalence Class Size Distrib====");
			distrib.printOrderedStats(_log, Level.INFO);
			if(topK>0){
				log.log(l, "====Top "+topK+" biggest equiv. classes====");
				PriorityQueue<Node> t= null;
				while((t= topkEQCS.poll())!=null){
					log.log(l, "Size "+t.size());
					log.log(l, t.toString());
				}
			}
			if(randK>0){
				log.log(l, "====Random "+randK+" equiv. classes====");
				for(PriorityQueue<Node> r: randomkEQCS){
					log.log(l, r.toString());
				}
			}
			log.log(l, "====Equivalence Class # of PLDs Distrib====");
			pldDistrib.printOrderedStats(_log, Level.INFO);
			if(topK>0){
				log.log(l, "====Top "+topK+" biggest PLD-level classes====");
				Set<String> t= null;
				while((t= topkPLDS.poll())!=null){
					log.log(l, "Size "+t.size());
					log.log(l, t.toString());
				}
			}
			if(randK>0){
				log.log(l, "====Random "+randK+" PLD-level classes====");
				for(Set<String> r: randomkPLDS){
					log.log(l, r.toString());
				}
			}
			log.log(l, "====Top "+topK+" PLD Pairs====");
			if(topK>0){
				topPldPairs.printOrderedStats(topK, _log, Level.INFO);
			}
			if(!specialEQCS.isEmpty()){
				for(PriorityQueue<Node> r: specialEQCS){
					log.log(l, r.toString());
				}
			}
		}
	}
	
	/**
	 * Extract owl:sameAs data from raw file
	 * @param in Iterator of triples or greater
	 * @return SameAsIndex encoding owl:sameAs relations in the iterator
	 */
	public static SameAsIndex extract(Iterator<Node[]> in){
		SameAsIndex sai = new SameAsIndex();
		
		_log.info("Extracting owl:sameAs data ...");
		int sa = 0;
		while(in.hasNext()){
			Node[] next = in.next();
			
			if(next[1].equals(OWL.SAMEAS)){
				sai.addSameAs(next[0], next[2]);
				sa++;
			}
		}
		_log.info("... done. Found "+sa+" input owl:sameAs statements.");
		return sai;
	}
	
	public static class SetSizeComparator<E> implements Comparator<Set<E>>{
		public int compare(Set<E> arg0, Set<E> arg1) {
			int comp = Double.compare(arg0.size(), arg1.size());
			if(comp==0){
				return -1;
			}
			return comp;
		}
	}
	
	public static class SetRandomComparator<E> implements Comparator<Set<E>>{

		public int compare(Set<E> arg0, Set<E> arg1) {
			int comp = Double.compare(arg0.iterator().next().hashCode(), arg1.iterator().next().hashCode());
			if(comp==0){
				return -1;
			}
			return comp;
		}
		
	}
	
	
	public static class PQueueSizeComparator<E> implements Comparator<PriorityQueue<E>>{

		public int compare(PriorityQueue<E> arg0, PriorityQueue<E> arg1) {
			int comp = Double.compare(arg0.size(), arg1.size());
			if(comp==0){
				return -1;
			}
			return comp;
		}
		
	}
	
	public static class PQueueRandomComparator<E> implements Comparator<PriorityQueue<E>>{

		public int compare(PriorityQueue<E> arg0, PriorityQueue<E> arg1) {
			int comp = Double.compare(arg0.peek().hashCode(), arg1.peek().hashCode());
			if(comp==0){
				return -1;
			}
			return comp;
		}
	}
	
	public static class PLDSet extends TreeSet<String>{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3887738641223217246L;
		public SameAsList _sal = null;
		
		public PLDSet(SameAsList sal){
			super();
			_sal = sal;
		}
		
		public SameAsList getSameAsList(){
			return _sal;
		}
		
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append(_sal.toString());
			sb.append(" PLDs: ");
			for(String p:this){
				sb.append(p+" ");
			}
			return sb.toString();
		}
	}
}
