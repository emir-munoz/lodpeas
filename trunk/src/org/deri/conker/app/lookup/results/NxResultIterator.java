package org.deri.conker.app.lookup.results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.semanticweb.nxindex.NodesIndex;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;

public class NxResultIterator implements Iterator<Node[]>{
		public final Resource RANK_PRED = new Resource("http://swse.deri.org/#luceneRank");
		public final Resource CONTEXT = new Resource("http://swse.deri.org/");
		public final Resource FOCUS_TYPE = new Resource("http://swse.deri.org/#Focus");

		public final Resource ID_RANK_PRED = new Resource("http://swse.deri.org/#idRank");

		private Iterator<Node[]> _subIter = null;
		private Iterator<Node[]> _first = null;

		public NxResultIterator(Node n, NodesIndex ni) throws IOException {
			Node[] key = new Node[]{n};
			_subIter = ni.getIterator(key);

			ArrayList<Node[]> first = new ArrayList<Node[]>();
//			first.add(new Node[]{n, RDF.TYPE, FOCUS_TYPE, CONTEXT});

			_first = first.iterator();
		}

		public boolean hasNext() {
			if(_first!=null && _first.hasNext()){
				return true;
			} else if(_subIter!=null && _subIter.hasNext()){
				return true;
			}
			return false;
		}

		public Node[] next() {
			if(_first!=null && _first.hasNext()){
				return _first.next();
			}
			if(_subIter!=null && _subIter.hasNext()){
				return _subIter.next();
			}
			throw new NoSuchElementException();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}