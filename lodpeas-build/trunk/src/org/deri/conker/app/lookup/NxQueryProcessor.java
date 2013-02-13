package org.deri.conker.app.lookup;

import java.io.IOException;

import org.deri.conker.app.lookup.results.NxResultIterator;
import org.semanticweb.nxindex.NodesIndex;
import org.semanticweb.yars.nx.Node;

public abstract class NxQueryProcessor {
	private NodesIndex _ni;
	
	public NxQueryProcessor(NodesIndex ni){
		_ni = ni;
	}

	protected NxResultIterator getResultIterator(Node n) throws IOException {
		return new NxResultIterator(n, _ni);
	}
}