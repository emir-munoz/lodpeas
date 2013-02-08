package org.deri.conker.build.util.concur;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.deri.conker.build.util.index.LabelPicker;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.NodeComparator;
import org.semanticweb.yars.nx.NodeComparator.NodeComparatorArgs;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.mem.MemoryManager;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;
import org.semanticweb.yars.nx.reorder.ReorderIterator;
import org.semanticweb.yars.nx.sort.SortIterator;
import org.semanticweb.yars.nx.sort.SortIterator.SortArgs;
import org.semanticweb.yars.util.Array;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;

/**
 * Main engine for running concurrence (lite) over a set of RDF data.
 * 
 * @author	Aidan Hogan
 * @since	1.6
 */
public class ConcurrenceEngine  {
	static Logger _log = Logger.getLogger(ConcurrenceEngine.class.getSimpleName());

	/**
	 * Arguments for the concurrence task
	 */
	private ConcurrenceArgs _ca;

	/**
	 * Logger ticks
	 */
	public final static int TICKS = 10000000;
	
	/**
	 * Used to estimate batch size
	 */
	public final static int STMT_LENGTH = 6;
	
	/**
	 * A marker to distinguish s-p-o ordered data in the merged stream
	 */
	public static final Node SP_MARKER = new Literal("s");
	
	/**
	 * A marker to distinguish o-p-s ordered data in the merged stream
	 */
	public static final Node OP_MARKER = new Literal("o");
	
	/**
	 * Order for the o-p-s sorted data
	 */
	public static final int[] OPS_ORDER = new int[]{2,1,0};
	
	/**
	 * Order for the o-p-s sorted and labelled data
	 */
	public static final int[] OPS_LAB_ORDER = new int[]{2,1,0,3,10,11,12,7,8,9,4,5,6};
	
	
	static boolean _logthis = false;
	
	
	/**
	 * Constructor
	 * 
	 * @param ca the arguments for the task
	 */
	public ConcurrenceEngine(ConcurrenceArgs ca){
		_ca = ca;
		org.semanticweb.yars.nx.cli.Main.mkdirs(_ca.getOutDir());
	}
	
	/**
	 * Method to start the concurrence
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public void run() throws IOException, ParseException{
		generateConcurrenceScores();
		aggregateConcurrenceScores();
	}
	
	/**
	 * Static method to generate the raw concurrence scores over the 
	 * iterator of sorted triples
	 * 
	 * @param iter	input iterator of sorted triples
	 * @param cb	callback to write concurrence score tuples to
	 * @param limit	max limit of groups to write concurrence scores for
	 * @param suffix	write the given suffix to the concurrence tuples produced by this call
	 * @return number of output tuples produced
	 */
	public static int generateConcurrenceScores(Iterator<Node[]> iter, Callback cb, int limit, Node suffix, boolean label, boolean filter, boolean sym){
		//subject/predicate has changed
		boolean es = false, ep = false;
		
		//old triple
		Node[] old = null;
		
		//set of resources sharing same value for same predicate
		TreeSet<Node> simClass = new TreeSet<Node>();
		
		//if the simClass is not full
		boolean full = false;
		
		//number of triples read, skipped, output
		int count = 0, skip = 0, out = 0;
		
		//number of literals skipped (not matched)
		HashSet<Node> lits = new HashSet<Node>();
		
		//number of non-subjects skipped (not matched)
		HashSet<Node> nonsubs = new HashSet<Node>();
		
		//!done while iterator has next
		boolean done = !iter.hasNext();
		
		//size of shared group
		int size = 0;
		
		while(!done){
			Node[] next = null;
			
			if(iter.hasNext())
				next  = iter.next();
			else
				done = true;
			
			if(next!=null && next[0].equals(new Resource("http://dbpedia.org/resource/Kol%C3%ADn")) && (next[1].equals(new Resource("http://dbpedia.org/ontology/assembly")) || next[1].equals(new Resource("<http://dbpedia.org/ontology/assembly")))){
				_log.info("Kolin assembly quad : "+Nodes.toN3(next));
			}
			
			count++;
			
			//log message if read TICKS
			if(count%TICKS==0){
				_log.info("...read "+count+" stmts... skipped "+skip+" classes larger than "+limit+"... output "+out+"...");
			}
			
			//if not first
			if(old!=null){
				//if not last
				if(!done){
					es = next[0].equals(old[0]);
					ep = next[1].equals(old[1]);
				}
				
				//if changed or last
				if(!es || !ep || done){
					if(old[0].equals(new Resource("http://dbpedia.org/resource/Kol%C3%ADn")) && (old[1].equals(new Resource("http://dbpedia.org/ontology/assembly")) || old[1].equals(new Resource("<http://dbpedia.org/ontology/assembly")))){
						_log.info("Kolin assembly full : "+full);
						_log.info("Kolin assembly size : "+size);
						_log.info("Kolin assembly sim-class : "+simClass);
						_log.info("Kolin assembly sim-class size : "+simClass.size());
						_log.info("Kolin assembly lits : "+lits);
						_log.info("Kolin assembly lits size : "+lits.size());
						_log.info("Kolin assembly non-subs : "+nonsubs);
						_log.info("Kolin assembly non-subs size : "+nonsubs.size());
						if(next!=null)
							_log.info("Kolin next : "+Nodes.toN3(next));
						
						_logthis = true;
					}
					
					//if some resources in group
					if(!simClass.isEmpty()){
						//output confidence score for group
						if(label){
							out += output(simClass, new Node[]{old[0], old[1], suffix, new Literal(Integer.toString(size)), old[4], old[5], old[6], old[7], old[8], old[9]}, sym, cb);
						} else out += output(simClass, new Node[]{old[0], old[1], suffix, new Literal(Integer.toString(size))}, sym, cb);
					}
					_logthis = false;
					
					//clear confidence scores
					simClass.clear();
					//reset full flag
					full = false;
					//reset literals
					lits.clear();
					//reset non-subjects
					nonsubs.clear();
					//reset size
					size = 0;
				}

			}
			
			//if we still have fresh data (not last)
			if(!done){
				//if we haven't already reached the limit
				if(!full){
					if(size==limit){
						//reached limit
						full = true;
						simClass.clear();
						skip++;
					} else if(next[2] instanceof Literal){
						//a literal (include in count)
						if(lits.add(next[2]))
							size++;
					} else if(filter && next[12].equals(LabelPicker.NOINTERNAL)){ 
						//a non-subject (include in count)
						if(nonsubs.add(next[2]))
							size++;
					} else{
						//add to group
						if(simClass.add(next[2]))
							size++;
					}
				}
			}
			
			//set old to new
			old = next;
		}
		_log.info("...read "+count+" stmts... skipped "+skip+" classes larger than "+limit+"... output "+out+"...");
		
		return out;
	}
	
	/**
	 * Static method to aggregate concurrence scores for a set of
	 * sorted concurrence tuples generated for different predicate-value
	 * pairs
	 * 
	 * @param iter	sorted iterator of concurrence tuples
	 * @param cb	callback to write aggregated scores to
	 * @param verbose	if raw explanations of scores should be printed
	 * @param index 
	 * @return	number of aggregated tuples produced
	 */
	public static int aggregateSimilarity(Iterator<Node[]> iter, Callback cb, boolean verbose, boolean filterRel, Callback index){
		long start = System.currentTimeMillis();
		
		//last tuple
		Node[] old = null;
		//current tuple
		Node[] nodes = null;
		
		//number of literals
		double lit = 0;
		
		//count of input and output
		int count = 0, output = 0;
		
		//explanations for verbose output
		HashSet<Node> vals = new HashSet<Node>();
		
		//confidence scores the active pair
		ArrayList<Double> confs = new ArrayList<Double>();
		
		//buffer raw tuples so as to add final score to index output
		TreeSet<Node[]> buffer = null;
		TreeSet<Node[]>  big_buffer = null;
		
		if(index!=null){
			buffer = new TreeSet<Node[]>(NodeComparator.NC);
			
			NodeComparatorArgs nca = new NodeComparatorArgs();
			nca.setNumeric(new boolean[]{false, true, false, false, false, false, true});
			nca.setOrder(new int[]{0,1,2,5,6,3,4});
			nca.setReverse(new boolean[]{false, true});
			big_buffer = new TreeSet<Node[]>(new NodeComparator(nca));
		}
		
		//!done while iter has next
		boolean done = !iter.hasNext();
		
		Node[] keep_tup = null;
		RelationInformation keep_ri = null;
		
		while(!done){
			nodes = null;
			if(iter.hasNext()){
				nodes = iter.next();
			} else{
				done = true;
			}
			
			//log message if read TICKS
			count++;
			if(count%TICKS==0) {
				_log.info("...read "+count+" tuples... "+(System.currentTimeMillis()-start)+" ms.");
			}
			
			//if match doesn't involve a literal (or two)
			if(!done && (nodes[0] instanceof Literal || nodes[1] instanceof Literal)){
				lit++;
				continue;
			}
			
			//see what changed (if not first or done)
			boolean e1 = true, e2 = true, v = true; 
			if(old!=null && !done){
				e1 = nodes[0].equals(old[0]);
				e2 = nodes[1].equals(old[1]);
				v = nodes[2].equals(old[2]);
			}

			//if last or new pair or new value
			if((done || (!e1 || !e2 || !v)) && keep_tup!=null && keep_ri!=null){
				confs.add(getConcurrenceScore(keep_ri.getN()));
				if(buffer!=null)
					buffer.add(keep_tup);
				
				keep_ri = null;
				keep_tup = null;
			}
			
			//if last or new pair
			if(done || (!e1 || !e2)){
				//aggregate the confidence values
				double pconf = getAggregateValue(confs);

				//update the aggregated scores to the output callback
				update(old[0], old[1], pconf, cb, vals);
				output++;
				
				//buffer with score to sorted output
				if(buffer!=null && index!=null && big_buffer!=null){
					buffer(buffer, pconf, big_buffer);
					buffer.clear();
					if(done || !e1){
						for(Node[] na:big_buffer){
							index.processStatement(na);
						}
						big_buffer.clear();
					}
				}
				
				//clear state for pair
				vals.clear();
				confs.clear();
			}
			
			//if fresh data
			if(!done){	
				int n = Integer.parseInt(nodes[5].toString());
				if(filterRel){
					//for each value ...
					//index list of ...
					///predicate, number of vals, rank of pred and if has label
					//afterwards choose one predicate (relation) if filter option set
					boolean hasLabel = false;
					double rank = 0;
					boolean out = nodes[4].equals(OP_MARKER);
					if(nodes.length>=11){
						hasLabel = nodes.length>=10 && nodes[9].toString() !=null; 
						rank = Double.parseDouble(nodes[10].toString());
					}
					RelationInformation ri = new RelationInformation(nodes[3],n,out,hasLabel,rank);
					
					if(keep_tup==null || keep_ri==null || ri.compareTo(keep_ri)<0){
						keep_tup = nodes;
						keep_ri = ri;
					}
				} else{
					confs.add(getConcurrenceScore(n));
					if(buffer!=null)
						buffer.add(nodes);
				}
				
				//set old to new
				old = nodes;
				
				//add value and confidence score to verbose explanations
				if(verbose){
					vals.add(nodes[2]);
					vals.add(nodes[5]);
				}
			}
		}

		long end = System.currentTimeMillis();
		_log.info("total time elapsed for aggregation: "+(end-start)+" ms! Skipped "+lit+" literal sims.");	
		
		_log.info("...read "+count+" stmts... skipped "+lit+" literals... output "+output+"...");
		
		return output;
	}
	
	private static void buffer(TreeSet<Node[]> buffer, double pconf, TreeSet<Node[]> big_buffer) {
		for(Node[] na:buffer){
			//skip pruned relations
			Node[] nap = new Node[na.length+1];
			nap[0] = na[0];
			nap[1] = new Literal(Double.toString(pconf));
			System.arraycopy(na, 1, nap, 2, na.length-1);
			
			big_buffer.add(nap);
		}
	}

	/**
	 * Output raw scores for a given group of resources sharing a
	 * property-value pair
	 * 
	 * @param simclass	group of resources
	 * @param suffix	a suffix to append to the score tuple
	 * @param sym 
	 * @param cb	callback to write tuple to
	 * @return number of tuples output
	 */
	public static int output(TreeSet<Node> simclass, Node[] suffix, boolean sym, Callback cb){
		if(simclass.size()<2)
			return 0;
		
		Node[] array  = new Node[simclass.size()];
		simclass.toArray(array);
		
		int out = 0;
		//only write tuples such that a < b
		for(int i=0; i<array.length-1; i++){
			for(int j=i+1; j<array.length; j++){
				//new nodearray each time, just in case
				//callback is storing them...
				Node[] tmpl = new Node[2+suffix.length];
				System.arraycopy(suffix, 0, tmpl, 2, suffix.length);
				
				tmpl[0] = array[i];
				tmpl[1] = array[j];
				cb.processStatement(tmpl);
				
				if(_logthis){
					_log.info("Kolin output : "+Nodes.toN3(tmpl));
				}
				
				if(sym){
					//write symmetric output
					Node[] tmpl2 = new Node[tmpl.length];
					System.arraycopy(suffix, 0, tmpl2, 2, suffix.length);
					
					tmpl2[0] = array[j];
					tmpl2[1] = array[i];
					cb.processStatement(tmpl2);
					
					if(_logthis){
						_log.info("Kolin sym output : "+Nodes.toN3(tmpl2));
					}
				}
				
				out++;
			}
		}
		
		return out;
	}
	
	/**
	 * Generate raw concurrence scores for sorted data specified in the
	 * arguments 
	 * 
	 * @return number of concurrence tuples generated for s-p-o/o-p-s streams
	 * @throws IOException
	 * @throws ParseException
	 */
	private ArrayList<Integer> generateConcurrenceScores() throws IOException, ParseException {
		//open stream to output data
		org.semanticweb.yars.nx.cli.Main.mkdirsForFile(_ca.getRawOut());
		OutputStream os = new FileOutputStream(_ca.getRawOut());
		if(_ca.getGzRawOut()){
			os = new GZIPOutputStream(os);
		}
		
		_log.info("...output to "+_ca.getRawOut());
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		CallbackNxBufferedWriter cb = new CallbackNxBufferedWriter(bw);
		
		String spo = _ca.getInSpo();
		String ops = _ca.getInOps();
		
		InputStream spocIs = new FileInputStream(spo);
		if(_ca.getGzInSpo())
			spocIs = new GZIPInputStream(spocIs); 
		Iterator<Node[]> spocInput = new NxParser(spocIs);
		
		_log.info("...input from "+spo);
		
		_log.info("...generating similarity file...");
		int outs = generateConcurrenceScores(spocInput, cb, _ca.getLimit(), SP_MARKER, _ca.getLabel(), _ca.getFilterNonSubjects(), _ca.getSymIndex());
		
		_log.info("...generated "+outs+" similarity tuples from "+spo+".");
		
		InputStream opscIs = new FileInputStream(ops);
		if(_ca.getGzInOps())
			opscIs = new GZIPInputStream(opscIs);
		Iterator<Node[]> opscInput = new NxParser(opscIs);
		
		_log.info("...input from "+ops);
		
		//explicitly reorder data based on sorting order
		if(_ca.getLabel() || _ca.getFilterNonSubjects()) opscInput = new ReorderIterator(opscInput, OPS_LAB_ORDER);
		else opscInput = new ReorderIterator(opscInput, OPS_ORDER);
		_log.info("...generating similarity file...");
		int outo = generateConcurrenceScores(opscInput, cb, _ca.getLimit(), OP_MARKER, _ca.getLabel(), _ca.getFilterNonSubjects(), _ca.getSymIndex());
		
		_log.info("...generated "+outo+" similarity tuples from "+ops+".");

		bw.close();
		spocIs.close();
		opscIs.close();
		
		ArrayList<Integer> sizes = new ArrayList<Integer>();
		sizes.add(outs);
		sizes.add(outo);
		
		return sizes;
	}
	
	/**
	 * Aggregate raw concurrence scores for resource pairs as generated
	 * for different predicate-value pairs and different input files. Finally,
	 * sort results by concurrence scores to final output
	 * 
	 * @return number of aggregated concurrence tuples generated
	 * @throws IOException
	 * @throws ParseException
	 */
	private int aggregateConcurrenceScores() throws IOException, ParseException {
		long b4 = System.currentTimeMillis();
		
		//open raw concurrence scores
		String inFN = _ca.getRawOut();
		
		InputStream is = new FileInputStream(inFN);
		if(_ca.getGzRawOut())
			is = new GZIPInputStream(is);

		String outFN = _ca.getUnsortedOut();
		org.semanticweb.yars.nx.cli.Main.mkdirsForFile(outFN);
		
		_log.info("Aggregating concurrence tuples to "+outFN+".");

		//open output for (unsorted) aggregated concurrence scores
		OutputStream os = new FileOutputStream(outFN);
		if(_ca.getGzUnsortedOut())
			os = new GZIPOutputStream(os);
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
		CallbackNxBufferedWriter cb = new CallbackNxBufferedWriter(bw);

		_log.info("...starting aggregation...");
		SortArgs sa = new SortArgs(new NxParser(is));
		sa.setTicks(TICKS);
		sa.setLinesPerBatch(MemoryManager.estimateMaxStatements(STMT_LENGTH));

		//sort raw concurrence scores to group by
		//matched pairs
		Iterator<Node[]> msi = new SortIterator(sa);
		BufferedWriter bw_tmp = null;
		CallbackNxBufferedWriter cb_tmp = null;
		
		if(_ca.getRawSortedOut()!=null){
			OutputStream os_tmp = new FileOutputStream(_ca.getRawSortedOut());
			if(_ca.getGzRawSortedOut())
				os_tmp = new GZIPOutputStream(os_tmp);
			
			bw_tmp = new BufferedWriter(new OutputStreamWriter(os_tmp));
			cb_tmp = new CallbackNxBufferedWriter(bw_tmp);
		}

		int count = aggregateSimilarity(msi, cb, _ca.getVerbose(), _ca.getFilterRelations(), cb_tmp);
		
		bw.close();
		is.close();
		if(bw_tmp!=null)
			bw_tmp.close();

		_log.info("...output "+count+" aggregated ranked triples.");
		_log.info("...triples aggregated in "+(System.currentTimeMillis()-b4)+" ms.");
		
		if(_ca.getOutData()!=null){
			_log.info("...sorting by confidence...");
			
			is = new FileInputStream(outFN);
			if(_ca.getGzUnsortedOut())
				is = new GZIPInputStream(is);
			
			org.semanticweb.yars.nx.cli.Main.mkdirsForFile(_ca.getOutData());
			
			os = new FileOutputStream(_ca.getOutData());
			if(_ca.getGzData())
				os = new GZIPOutputStream(os);
			
			bw = new BufferedWriter(new OutputStreamWriter(os));
			cb = new CallbackNxBufferedWriter(bw);
			
			//create a comparator to sort tuples in descending order
			//by aggregated concurrence score 
			NodeComparatorArgs nca = new NodeComparatorArgs();
			nca.setReverse(new boolean[]{false, false, true});
			nca.setNumeric(new boolean[]{false, false, true});
			nca.setOrder(new int[]{2,0,1});
			
			sa = new SortArgs(new NxParser(is));
			sa.setTicks(TICKS);
			sa.setComparator(new NodeComparator(nca));
		
			SortIterator si = new SortIterator(sa);
			
			while(si.hasNext()){
				cb.processStatement(si.next());
			}
			
			_log.info("...finished sorting by confidence.");
		}
		
		bw.close();
		is.close();
		
		return count;
	}
	
	/**
	 * Get the concurrence score for a set of elements of size
	 * els sharing the same property-value pair
	 * 
	 * @param els
	 * @return concurrence score for that group
	 */
	private static double getConcurrenceScore(double els){
		return 1d / els;
	}
	
	/**
	 * Write concurrence tuple
	 * @param prevS1S2	pair being matching
	 * @param aggV	aggregated concurrence score
	 * @param cb	callback to write tuple to
	 * @param vals	verbose info
	 */
	private static void update(Node e1, Node e2, double aggV, Callback cb, Set<Node> vals){
		//write concurrence tuple along with verbose explanation (if required)
		Literal aggVl = new Literal(Double.toString(aggV));
		Node[] e1e2c = new Node[]{e1, e2, aggVl};
		if(!vals.isEmpty()){
			Node[] temp = new Node[e1e2c.length + vals.size()];
			System.arraycopy(e1e2c, 0, temp, 0, e1e2c.length);
			Iterator<Node> iter = vals.iterator();
			int i = e1e2c.length;
			while(iter.hasNext()){
				temp[i] = iter.next();
				i++;
			}
			e1e2c = temp;
		}
		cb.processStatement(e1e2c);
	}

	/**
	 * Aggregate a set of concurrence scores
	 * 
	 * @param confs set of concurrence scores
	 * @return aggregated score
	 */
	private static double getAggregateValue(ArrayList<Double> confs){
		return getAggregateValue(confs, 1d);
	}
	
	/**
	 * Aggregate a set of concurrence scores
	 * 
	 * @param confs	set of concurrence scores
	 * @param max	a maximum value for the aggregated score 
	 * @return aggregated score
	 */
	private static double getAggregateValue(ArrayList<Double> confs, double max){
		//use fuzzy-OR aggregation
		double agg = 0;

		for(double d:confs){
			double r = 1 - agg;
			double c = r * d;
			agg+=c;
		}
		return agg * max;
	}
	
//	public static class RelationInformationSet extends TreeSet<RelationInformation>{
//		
//		
//		public RelationInformationSet(){
//			super();
//		}
//		
//		
//	}
	
	public static class RelationInformation implements Comparable<RelationInformation>{
		private Node pred = null;
		private int n = 0;
		private boolean label = false;
		private double rank = 0;
		private boolean out = false;
		
		public RelationInformation(Node pred, int n, boolean out, boolean label, double rank){
			this.pred = pred;
			this.n = n;
			this.label = label;
			this.rank = rank;
			this.out = out;
		}
		
		public Node getPred(){
			return this.pred;
		}
		
		public int getN(){
			return this.n;
		}
		
		public boolean getLabel(){
			return this.label;
		}
		
		public double getRank(){
			return this.rank;
		}

		public boolean getOut(){
			return this.out;
		}
		
		public int compareTo(RelationInformation arg0) {
			//prefers relations with labels
			
			int comp = compareBoolean(arg0.label, label);
			
			if(comp==0){
				//... then a lower n
				comp = n - arg0.n;
				if(comp == 0){
					//... then out before in
					comp = compareBoolean(arg0.out, out);
					if(comp==0){
						//... then a higher rank
						comp = Double.compare(arg0.rank, rank);
						if(comp==0){
							//... then alphabetical
							return pred.compareTo(arg0.pred);
						}
					}
				}
			}
			return comp;
		}
		
		//get strange compiler errors for Booelan.compare
		public static int compareBoolean(boolean a, boolean b){
			return (a == b) ? 0 : (a ? 1 : -1);
		}
		
		public boolean equals(Object o){
			if(o == this){
				return true;
			}
			if(o instanceof RelationInformation){
				RelationInformation ri = (RelationInformation)o;
				if(ri.pred.equals(pred) && ri.n == n && ri.label == label && ri.rank == rank && ri.out==out){
					return true;
				}
			}
			return false;
		}
		
		public int hashCode(){
			return Array.hashCode(pred, n, label, rank, out);
		}
		
		public String toString(){
			return pred.toN3()+" N:"+n+" Out:"+out+" Rank:"+rank+" Label:"+label;
		}
	}
	
//	public static class ConfidenceValues {
//		// map from values to confidence values
//		private Map<Node,TreeSet<ConfidenceValue>> valsToCvs;
//		
//		// map from predicate/direction pairs to confidence values
//		private Map<Nodes,TreeSet<ConfidenceValue>> pdsToCvs;
//		
//		public ConfidenceValues(){
//			valsToCvs = new HashMap<Node,TreeSet<ConfidenceValue>>();
//			pdsToCvs = new HashMap<Nodes,TreeSet<ConfidenceValue>>();
//		}
//		
//		public void addConfidenceValue(ConfidenceValue cf){
//			TreeSet<ConfidenceValue> vcvs = valsToCvs.get(cf.value);
//			if(vcvs==null){
//				vcvs = new TreeSet<ConfidenceValue>();
//				valsToCvs.put(cf.value, vcvs);
//			}
//			vcvs.add(cf);
//			
//			Nodes pd = new Nodes(cf.pred, cf.dir);
//			TreeSet<ConfidenceValue> pdcvs = pdsToCvs.get(pd);
//			if(pdcvs==null){
//				pdcvs = new TreeSet<ConfidenceValue>();
//				pdsToCvs.put(pd, pdcvs);
//			}
//			pdcvs.add(cf);
//		}
//		
//		public void pruneCommonValues(){
//			for(Map.Entry<Node,TreeSet<ConfidenceValue>> e: valsToCvs.entrySet()){
//				if(e.getValue().size()>1){
//					
//				}
//			}
//		}
//		
//		private HashSet<ConfidenceValue> pruneAllButMax(HashSet<ConfidenceValue> all){
//			HashSet<ConfidenceValue> pruned = new HashSet<ConfidenceValue>();
//			pruned.addAll(all);
//			
//			ConfidenceValue max = null;
//			for(ConfidenceValue cv: all){
//				if(max==null){
//					max = cv;
//				} else if(cv.conf>max.conf){
//					max = cv;
//				} else if(cv.pred.compareTo(max.c))
//			}
//		}
//	}
//	
//	public static class ConfidenceValue implements Comparable<ConfidenceValue>{
//		// confidence for value
//		private double conf;
//		
//		// predicate for shared pair
//		private Node pred;
//		
//		// value for shared pair
//		private Node value;
//		
//		// directoin of shared pair
//		private Node dir;
//		
//		public double getConfidence() {
//			return conf;
//		}
//
//		public Node getPredicate() {
//			return pred;
//		}
//
//		public Node getValue() {
//			return value;
//		}
//		
//		public Node getDirection() {
//			return dir;
//		}
//
//		public ConfidenceValue(Node pred, Node value, Node dir, double conf){
//			this.conf = conf;
//			this.pred = pred;
//			this.value = value;
//			this.dir = dir;
//		}
//		
//		public int hashCode(){
//			return Array.hashCode(pred, value, dir, conf);
//		}
//		
//		public boolean equals(Object o){
//			if(o == this){
//				return true;
//			} else if(o instanceof ConfidenceValue){
//				ConfidenceValue cv = ((ConfidenceValue) o);
//				return cv.conf == conf &&
//						cv.value.equals(value) &&
//						cv.pred.equals(pred) &&
//						cv.dir.equals(dir);
//			}
//			return false;
//		}
//		
//		public int compareTo(ConfidenceValue cv){
//			if(this==cv)
//				return 0;
//			
//			int comp = Double.compare(this.conf, cv.conf);
//			
//			if(comp==0){
//				//favour outgoing edge since more intuitive
//				if(dir.equals(SP_MARKER) && cv.dir.equals(OP_MARKER)){
//					return -1;
//				} else if(dir.equals(OP_MARKER) && cv.dir.equals(SP_MARKER)){
//					return 1;
//				}
//				comp = dir.compareTo(cv.dir);
//			}
//			if(comp==0){
//				comp = pred.compareTo(cv.pred);
//			}
//			if(comp==0){
//				comp = value.compareTo(cv.value);
//			}
//			
//			return comp;
//		}
//	}
//	
	public static void main(String[] args){
		TreeSet<RelationInformation> ts = new TreeSet<RelationInformation>();
		ts.add(new RelationInformation(new Resource("http://a.com/"),5,true,true,0.7d));
		ts.add(new RelationInformation(new Resource("http://a.com/"),5,false,true,0.7d));
		ts.add(new RelationInformation(new Resource("http://c.com/"),1,true,false,0.9d));
		ts.add(new RelationInformation(new Resource("http://d.com/"),2,true,true,0.7d));
		ts.add(new RelationInformation(new Resource("http://e.com/"),5,true,true,0.8d));
		
		for(RelationInformation ri:ts){
			System.err.println(ri);
		}
	}
}

