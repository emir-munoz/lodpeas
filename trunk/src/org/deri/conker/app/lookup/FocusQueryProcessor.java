package org.deri.conker.app.lookup;

import java.io.IOException;
import java.util.Iterator;

import org.semanticweb.nxindex.NodesIndex;
import org.semanticweb.yars.nx.Node;

public class FocusQueryProcessor extends NxQueryProcessor {
	public FocusQueryProcessor(NodesIndex ni){
		super(ni);
	}
	
	public Iterator<Node[]> getFocusResult(Node n) throws IOException {
		return getResultIterator(n);
	}
}
