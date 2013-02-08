package org.deri.conker.app.lookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.queryParser.ParseException;
import org.deri.conker.app.lookup.results.ConcurrenceResult;
import org.deri.conker.app.lookup.results.SnippetResult;
import org.semanticweb.nxindex.NodesIndex;
import org.semanticweb.yars.nx.Node;

public class ConcurrenceQueryProcessor extends NxQueryProcessor {
	private KeywordQueryProcessor kqp;
	
	public static int SCORE_INDEX = 1;
	public static int TARGET_INDEX = 2;
	
	public ConcurrenceQueryProcessor(NodesIndex ni, KeywordQueryProcessor kqp){
		super(ni);
		this.kqp = kqp;
	}
	
	public ArrayList<ConcurrenceResult> getConcurrenceResults(Node n) throws IOException, ParseException, org.semanticweb.yars.nx.parser.ParseException{
		return getConcurrenceResults(n,-1);
	}
	
	public ArrayList<ConcurrenceResult> getConcurrenceResults(Node n, int topK) throws IOException, ParseException, org.semanticweb.yars.nx.parser.ParseException{
		ArrayList<ConcurrenceResult> results = new ArrayList<ConcurrenceResult>();
		Iterator<Node[]> conIter = getResultIterator(n);
		
		ConcurrenceResult cr = null;
		Node[] last = null;
		Node[] next = null;
		int rank = 0;
		while(conIter.hasNext()){
			next = conIter.next();
			
			if(last==null || !next[TARGET_INDEX].equals(last[TARGET_INDEX])){
				if(topK>1 && rank>=topK){
					break;
				}
				
				SnippetResult sr = kqp.getInternalNode(next[TARGET_INDEX]);
				double score = Double.parseDouble(next[SCORE_INDEX].toString());
				rank++;
				
				cr = new ConcurrenceResult(sr, score, rank);
				results.add(cr);
			}
			
			cr.addTuple(next);
			last = next;
		}
		
		return results;
	}
}
