package org.deri.conker.app.servlet;

import java.io.File;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.deri.conker.app.lookup.ConcurrenceQueryProcessor;
import org.deri.conker.app.lookup.FocusQueryProcessor;
import org.deri.conker.app.lookup.KeywordQueryProcessor;
import org.semanticweb.nxindex.NodesIndex;
import org.semanticweb.nxindex.block.NodesBlockReaderIO;
import org.semanticweb.nxindex.block.util.CallbackIndexerIO;
import org.semanticweb.nxindex.sparse.SparseIndex;



/**
 * This is the main routine, the entry and exit point of the web
 * application.
 *
 * The web application's contextInitialized() and contextDestroyed() are
 * called when Tomcat/the servlet container starts or stops the web
 * application.
 * 
 * @author aidhog
 */
public class Listener implements ServletContextListener {
	public final static String KEYWORDQUERYPROCESSOR = "k";
	public final static String FOCUSQUERYPROCESSOR = "f";
	public final static String CONCURRENCEQUERYPROCESSOR = "c";
	public final static String ERROR = "error";
	
	
	private Directory d = null;
	private NodesBlockReaderIO q_nbr = null;
	private NodesBlockReaderIO c_nbr = null;
	
	static final Logger _log = Logger.getLogger(Listener.class.getName());

	/**
	 * Servlet context is created.
	 */
	public void contextInitialized(ServletContextEvent event) {
		ServletContext ctx = event.getServletContext();
		
		String indexDir = getFullPath(ctx, ctx.getInitParameter("indexDir"));
		String keywordDir = indexDir+"/"+getFullPath(ctx, ctx.getInitParameter("keywordDir"));
		String quadIndex = indexDir+"/"+getFullPath(ctx, ctx.getInitParameter("quadIndex"));
		String quadSparse = indexDir+"/"+getFullPath(ctx, ctx.getInitParameter("quadSparse"));
		String concurIndex = indexDir+"/"+getFullPath(ctx, ctx.getInitParameter("concurIndex"));
		String concurSparse = indexDir+"/"+getFullPath(ctx, ctx.getInitParameter("concurSparse"));

		try{
			_log.info("Opening keyword directory at "+keywordDir);
			d = new NIOFSDirectory(new File(keywordDir));
			IndexReader ir = IndexReader.open(d);
			IndexSearcher kws = new IndexSearcher(ir);
			KeywordQueryProcessor kqp = new KeywordQueryProcessor(kws);
			ctx.setAttribute(KEYWORDQUERYPROCESSOR, kqp);
			_log.info("... opened keyword searcher.");
			
			_log.info("Opening quad index at "+quadIndex);
			q_nbr = new NodesBlockReaderIO(quadIndex);
			SparseIndex qsi = CallbackIndexerIO.loadSparseIndex(quadSparse,100000);
			NodesIndex qnbi = new NodesIndex(q_nbr, qsi);
			FocusQueryProcessor fqp = new FocusQueryProcessor(qnbi);
			ctx.setAttribute(FOCUSQUERYPROCESSOR, fqp);
			_log.info("... opened quad sparse index of size "+qsi.size());
			
			_log.info("Opening concurrence index at "+concurIndex);
			c_nbr = new NodesBlockReaderIO(concurIndex);
			SparseIndex csi = CallbackIndexerIO.loadSparseIndex(concurSparse,100000);
			NodesIndex cnbi = new NodesIndex(c_nbr, csi);
			ConcurrenceQueryProcessor cqp = new ConcurrenceQueryProcessor(cnbi, kqp);
			ctx.setAttribute(CONCURRENCEQUERYPROCESSOR, cqp);
			_log.info("... opened concurrence sparse index of size "+qsi.size());
		} catch (Exception e) {
			System.err.println("Error at startup!");
			e.printStackTrace();
			ctx.setAttribute(ERROR, e);
		}
	}

	private static String getFullPath(ServletContext ctx, String rel){
		if (rel.startsWith(".")) {
			String realpath = ctx.getRealPath(rel);

			if (realpath != null) {
				return realpath;
			} else return rel;
		}
		return rel;
	}


	/**
	 * Servlet context is about to be shut down.
	 */
	public void contextDestroyed(ServletContextEvent event) {
		;
	}
}
