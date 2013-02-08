package org.deri.conker.build.util.index;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.deri.conker.build.cli.JoinQuadsLabelsRank;
import org.semanticweb.nxindex.ScanIterator;
import org.semanticweb.nxindex.block.NodesBlockReaderIO;
import org.semanticweb.nxindex.block.util.CallbackIndexerIO;
import org.semanticweb.nxindex.block.util.CallbackIndexerIO.CallbackIndexerArgs;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.NodeComparator;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;
import org.semanticweb.yars.nx.sort.SortIterator;
import org.semanticweb.yars.nx.sort.SortIterator.SortArgs;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;
import org.semanticweb.yars.util.ResetableIterator;
import org.semanticweb.yars.util.TicksIterator;

public class QuadLabelJoiner {
	
	static transient Logger _log = Logger.getLogger(JoinQuadsLabelsRank.class.getName());
	static final Node[] BLANK_LRI = new Node[]{ LabelPicker.BLANK_LIT, LabelPicker.BLANK_LIT, new Literal(Double.toString(LabelPicker.MIN_RANK)), LabelPicker.NOINTERNAL };
	static final short SPARSE_LEN = 1;
	
	public static void indexQuadsLabels(Iterator<Node[]> quads, ResetableIterator<Node[]> labels, QuadLabelJoinOrder[] qlios, int ticks) throws IOException, ParseException{
		for(int i=0; i<qlios.length; i++){
			_log.info("Performing order "+i);
			QuadLabelJoinOrder qlio = qlios[i];
			
			if(i>0){
				_log.info("... performing pre-sort for order "+i);
				QuadLabelJoinOrder qlio_old = qlios[i-1]; 
				Iterator<Node[]> in = null;
	
				if(qlio_old.ot.equals(OutputType.PLAIN) || qlio_old.ot.equals(OutputType.GZIPPED)){
					InputStream is = new FileInputStream(qlio_old.outfile);
					if(qlio_old.ot.equals(OutputType.GZIPPED))
						is = new GZIPInputStream(is);
					in = new NxParser(is);
				} else{
					in = new ScanIterator(new NodesBlockReaderIO(qlio_old.outfile+CallbackIndexerArgs.INDEX_SUFFIX));
				}
				
				SortArgs sa = new SortArgs(in);
				sa.setComparator(new NodeComparator(qlio.order));
				quads = new TicksIterator(new SortIterator(sa), ticks);
			}
			
			_log.info("... joining quads and labels for order "+i);
			
			BufferedWriter bw = null;
			CallbackIndexerIO cbi = null;
			Callback cb = null;
			
			if(qlio.ot.equals(OutputType.PLAIN) || qlio.ot.equals(OutputType.GZIPPED)){
				OutputStream os = new FileOutputStream(qlio.outfile);
				if(qlio.ot.equals(OutputType.GZIPPED))
					os = new GZIPOutputStream(os);
				bw = new BufferedWriter(new OutputStreamWriter(os));
				cb = new CallbackNxBufferedWriter(bw);
			} else{
				CallbackIndexerArgs cia = new CallbackIndexerArgs(qlio.outfile);
				cia.setSparseLength(SPARSE_LEN);
				cia.setWriteSparse(true);
				cbi = new CallbackIndexerIO(cia);
				cb = cbi;
			}
			
			appendQuadsLabels(quads, labels, qlio.order[0], cb);
			
			if(bw!=null)
				bw.close();
			if(cbi!=null)
				cbi.close();
			
			labels.reset();
			
			_log.info("... finished order "+i);
		}
	}
	
	public static short appendQuadsLabels(Iterator<Node[]> in, Iterator<Node[]> labels, int index, Callback out){
		Node oldKey = null;
		Node[] cur_lri = null;
		Node[] lri = null;
		
		int len = 0;
		
		while (in.hasNext()) {	
			Node[] q = in.next();
			
			Node key = q[index];
			
			if(oldKey==null || !key.equals(oldKey)){
				if (lri == null && labels.hasNext()) {
					lri = labels.next();
				}
				
				int comp =  Integer.MIN_VALUE;
				if (lri != null){
					comp = lri[0].compareTo(key);
				}
				
//				while (rna!=null && ranks.hasNext() && comp < 0) {
				while (labels.hasNext() && comp < 0) {
					lri = labels.next();
					comp =  lri[0].compareTo(key);
				}
				
				if(!lri[0].equals(key)){
					cur_lri = BLANK_LRI;
				} else{
					cur_lri = lri;
				}
			}
			
			len = q.length + cur_lri.length-1;
			Node[] qlri = new Node[q.length + cur_lri.length-1];
			
			System.arraycopy(q, 0, qlri, 0, q.length);
			System.arraycopy(cur_lri, 1, qlri, q.length, cur_lri.length-1);
			
			out.processStatement(qlri);

			oldKey = key;
		}
		
		return (short)len;
	}
	
	public enum OutputType{
		PLAIN, GZIPPED, INDEX
	}
	
	public static class QuadLabelJoinOrder {
		public static final OutputType DEFAULT_OUTPUT_TYPE = OutputType.GZIPPED;
		String outfile;
		int[] order;
		OutputType ot;
		
		public QuadLabelJoinOrder(String outfile, int[] order){
			this(outfile, order, DEFAULT_OUTPUT_TYPE);
		}
		
		public QuadLabelJoinOrder(String outfile, int[] order, OutputType ot){
			this.outfile = outfile;
			this.order = order;
			this.ot = ot;
		}
	}
}
