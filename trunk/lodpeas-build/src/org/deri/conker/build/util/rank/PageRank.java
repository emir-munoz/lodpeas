package org.deri.conker.build.util.rank;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.ParseException;
import org.semanticweb.yars.util.ResetableIterator;

/**
 * Calculate PageRank from graph
 * 
 * @author aharth, sheila, aidhog
 *
 */
public class PageRank {
	private static Logger _log = Logger.getLogger(PageRank.class.getName());
	
	public static final int DEFAULT_ITERS = 10;
	public static final double DEFAULT_DAMP = 0.15;
	
	/**
	 * Run PageRank algorithm over a graph represented as a (resetable) iterator
	 * over a collection of Node pairs { n1, n2 } where n1 links to n2.
	 * 
	 * @param in Resetable iterator of input Node pairs.
	 * @return Map from input nodes to output ranks (doubles).
	 * @throws ParseException
	 * @throws IOException
	 */
	public static Map<Node, Double> process(ResetableIterator<Node[]> in) throws ParseException, IOException {
		return process(in, DEFAULT_ITERS, DEFAULT_DAMP);
	}

	/**
	 * Run PageRank algorithm over a graph represented as a (resetable) iterator
	 * over a collection of Node pairs { n1, n2 } where n1 links to n2.
	 * 
	 * @param in Resetable iterator of input Node pairs, grouped on n1.
	 * @param iterations Number of iterations to run.
	 * @param damping Value of damping factor (typically set to 0.15).
	 * @return Map from input nodes to output ranks (doubles).
	 * @throws ParseException
	 * @throws IOException
	 */
	public static Map<Node, Double> process(ResetableIterator<Node[]> in, int iterations, double damping) throws ParseException, IOException {
		Map<Node, Double> eig0 = new HashMap<Node, Double>(),
			eig1 = new HashMap<Node, Double>();

		Node[] nx = null;
		
		while (in.hasNext()) {		
			nx = in.next();
			
			Node v = nx[0];
			Node u = nx[1];
			
			eig0.put(v, 1.0);
			eig0.put(u, 1.0);	
		}
		
		double initial = 1.0/(double)eig0.size();
		
		for (Node s: eig0.keySet()) {
			eig0.put(s, initial);
		}
		
		_log.info("Sum " + sum(eig0));
		
		// eig0 is initialised vector with 1/no nodes
		
		while (iterations-- > 0) {
			_log.info("iterations to go: " + iterations);
			
			// initialise with damping factor
			double dampval = damping/(double)eig0.size();
			eig1 = new HashMap<Node, Double>();
			for (Node s : eig0.keySet()) {
				eig1.put(s, dampval);
			}

			in.reset();

			Set<Node> targets = new HashSet<Node>();

			Node oldSource = null;

			boolean done = !in.hasNext();
			while (!done) {
				if(in.hasNext()){
					nx = in.next();
				} else{
					// run distribution for last node
					done = true;
				}
				
				
				if (done || (oldSource != null && !nx[0].equals(oldSource))){
					// distribute pr from oldv to nodes in us
					if (!eig0.containsKey(oldSource)) {
						// hm... that's probably because the input is not sorted
						_log.info("not in eig0: " + oldSource + " make sure input is grouped on first element (export LC_TYPE=C)");
					} else {
						double pr = eig0.get(oldSource);
						eig0.remove(oldSource);
						
						for (Node r: targets) {
							double rank = (pr/(double)targets.size())*(1.0f-damping);
							if (eig1.containsKey(r)) {
								rank += eig1.get(r);														
							}
							
							eig1.put(r, rank);
						}
					}
					targets = new HashSet<Node>();
				}

				if(!done){
					Node target = nx[1];
					targets.add(target);
	
					oldSource = nx[0];
				}
			}
			
			// distribute pr of dangling links to every node
			double pernode = 0f;
			for (Node r : eig0.keySet()) {
				// distribute pr to all nodes (except itself)
				double split = (eig0.get(r)/(eig1.size()-1))*(1.0f-damping);
				pernode += split;
				// remove self-reference of page rank value in advance
				double pr = eig1.get(r);
				eig1.put(r, pr-split);
			}
			
			for (Node s : eig1.keySet()) {
				double sum = eig1.get(s)+pernode;
				eig1.put(s, sum);
			}
			
			_log.info("Sum " + sum(eig1));
			
			eig0 = eig1;
			/*
			eig0 = new HashMap<Resource, Double>();
			for (Resource s : eig1.keySet()) {
				eig0.put(s, eig1.get(s));
			}
			*/
		}
		
		return eig0;
	}

	/**
	 * Print sum of map elements.
	 * @param m Map
	 */
	public static double sum(Map<?, Double> m) {
		double sum = 0;
		
		for(double f:m.values()){
			sum += f;
		}
		
		return sum;
	}
	
	/**
	 * Print map pairs to log, one per line.
	 * @param m Map
	 */
	public static void logRanks(Map<?,?> m) {
		for (Map.Entry<?,?> e: m.entrySet()) {
			_log.info(e.getKey()+" "+e.getValue());
		}
	}
	
	/**
	 * Print map pairs to output file in Nx format.
	 * @param m Map
	 */
	public static void outputRanks(Map<Node,Double> m, Callback c) {
		for(Map.Entry<Node, Double> e: m.entrySet()){
			c.processStatement(new Node[]{e.getKey(), new Literal(e.getValue().toString())});
		}
	}
	
	/**
	 * Load ranks from file (as printed by {@link #outputRanks(Map, Callback) outputRanks} method).
	 * '
	 */
	public static HashMap<Node,Double> loadRanks(Iterator<Node[]> in) {
		HashMap<Node,Double> map = new HashMap<Node,Double>();
		while(in.hasNext()){
			Node[] next = in.next();
			double rank = Double.parseDouble(next[1].toString());
			Double old = map.put(next[0], rank);
			if(old!=null){
				if(old>rank) map.put(next[0], old);
				_log.warning("Multiple ranks for "+next[0]+" including "+old+" and "+rank+" ... accepting larger");
			}
		}
		return map;
	}
}