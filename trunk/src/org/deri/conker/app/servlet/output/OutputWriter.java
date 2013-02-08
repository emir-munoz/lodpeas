package org.deri.conker.app.servlet.output;

import java.io.PrintWriter;
import java.util.Collection;

import org.deri.conker.app.lookup.results.ConcurrenceResult;
import org.deri.conker.app.lookup.results.SnippetResult;
import org.semanticweb.yars.nx.Node;

public abstract class OutputWriter {
	protected PrintWriter out = null;
	
	public OutputWriter(PrintWriter out){
		this.out = out;
	}
	
	public abstract void startOutput();
	public abstract void printQuery(String query);
	public abstract void endOutput();
	
	public abstract void startConcurrenceSubject(SnippetResult sr);
	public abstract void printNoConcurrenceSubject();
	public abstract void endConcurrenceSubject();
	
	public abstract void printNoConcurrenceObjects();
	public abstract void printConcurrenceObject(ConcurrenceResult cr);

	public abstract void printDisamibuationResults(Collection<SnippetResult> keyword);
	
	public abstract void startFocusResults(Node subject, Node subLabel, Node subRank, Node subInternal);
	public abstract void printNoFocusResult();
	public abstract void printFocusTuple(Node[] tuple);
	public abstract void endFocusResults();
	
	public abstract String getOutputContentType();
}
