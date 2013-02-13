package org.deri.conker.app.lookup.results;

import java.util.ArrayList;

import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;

public class ConcurrenceResult {
	private SnippetResult snippet;
	private double overallScore;
	private int rank;
	
	private ArrayList<ConcurrenceTuple> tuples = new ArrayList<ConcurrenceTuple>();
	
	public ConcurrenceResult(SnippetResult snippet, double overallScore, int rank){
		this.snippet = snippet;
		this.overallScore = overallScore;
		this.rank = rank;
	}
	
	public SnippetResult getSnippet() {
		return snippet;
	}

	public double getOverallScore() {
		return overallScore;
	}

	public int getRank() {
		return rank;
	}

	public ArrayList<ConcurrenceTuple> getTuples() {
		return tuples;
	}
	
	public void addTuple(Node[] tuple){
		tuples.add(new ConcurrenceTuple(tuple));
	}
	
	public void addTuple(ConcurrenceTuple tuple){
		tuples.add(tuple);
	}
	
	public static class ConcurrenceTuple {
		public static final int VALUE_INDEX = 3;
		public static final int PRED_INDEX = VALUE_INDEX+1;
		public static final int DIR_INDEX = PRED_INDEX+1;
		public static final int N_INDEX = DIR_INDEX+1;
		public static final int VALUE_LAB_INDEX = N_INDEX+1;
		public static final int PRED_LAB_INDEX = VALUE_LAB_INDEX+3;
		
		private Node value = null;
		private Node predicate = null;
		
		private Node direction = null;
		private int n = -1;
		
		private Literal valueLabel = null;
		private Literal predicateLabel = null;
		
		
		public ConcurrenceTuple(Node[] tuple) throws NumberFormatException {
			value = tuple[VALUE_INDEX];
			predicate = tuple[PRED_INDEX];
			direction = tuple[DIR_INDEX];
			n = Integer.parseInt(tuple[N_INDEX].toString());
			if(tuple[VALUE_LAB_INDEX] instanceof Literal){
				valueLabel = (Literal)tuple[VALUE_LAB_INDEX];
			}
			if(tuple[PRED_LAB_INDEX] instanceof Literal){
				predicateLabel = (Literal)tuple[PRED_LAB_INDEX];
			}
			
		}
		
		public Node getPredicate() {
			return predicate;
		}

		public Node getValue() {
			return value;
		}

		public Node getDirection() {
			return direction;
		}

		public int getN() {
			return n;
		}

		public Literal getPredicateLabel() {
			return predicateLabel;
		}

		public Literal getValueLabel() {
			return valueLabel;
		}
	}
}
