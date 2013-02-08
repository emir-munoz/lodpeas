package org.deri.conker.app.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.conker.app.lookup.ConcurrenceQueryProcessor;
import org.deri.conker.app.lookup.FocusQueryProcessor;
import org.deri.conker.app.lookup.KeywordQueryProcessor;
import org.deri.conker.app.lookup.results.ConcurrenceResult;
import org.deri.conker.app.lookup.results.SnippetResult;
import org.deri.conker.app.servlet.output.JSONWriter;
import org.deri.conker.app.servlet.output.OutputWriter;
import org.semanticweb.yars.nx.Node;


/**
 * Servlet.
 * 
 * @author aidhog
 */
public class Servlet extends HttpServlet  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static Logger _log = Logger.getLogger(Servlet.class.getName());

	private int queryCounter = 0;

	public static int DEFAULT_DISAMB_RESULTS = 10;

	/**
	 * GET for asking queries.
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		int qc = -1;

		synchronized(this){
			queryCounter++;
			qc = queryCounter;
		}

		ServletContext ctx = getServletContext();

		//OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream(), "UTF8");
		PrintWriter out = response.getWriter();

		// check for error during startup
		Exception ioex = (Exception)ctx.getAttribute(Listener.ERROR);
		if (ioex != null) {
			throw new ServletException(ioex.getMessage());
		}

		String keywordQ = request.getParameter("k");
		String disambQ = request.getParameter("d");
		String internalQ = request.getParameter("i");
		String externalQ = request.getParameter("q");
		String focusQ = request.getParameter("f");

		String charenc = request.getCharacterEncoding();

		if (charenc == null) {
			charenc = "ISO-8859-1";
		}

		OutputWriter ow = new JSONWriter(out);
		response.setContentType(ow.getOutputContentType());

		ow.startOutput();
		ow.printQuery(request.getQueryString());

		_log.info("Query "+qc+" query string : "+request.getQueryString());
		long b4 = System.currentTimeMillis();

		if(keywordQ!=null || internalQ!=null || externalQ!=null){
			String f = request.getParameter("n");

			int topK = -1;
			try {
				keywordQ = new String(keywordQ.getBytes(charenc),"UTF8");
				topK = Integer.parseInt(f);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}


			SnippetResult result = null;
			KeywordQueryProcessor kqp = (KeywordQueryProcessor)ctx.getAttribute(Listener.KEYWORDQUERYPROCESSOR);

			try {
				if(keywordQ!=null){
					ArrayList<SnippetResult> results = kqp.getKeywordHits(keywordQ, 1);

					if(results==null || results.isEmpty()){
						_log.info("Query "+qc+" no keyword matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
						ow.printNoConcurrenceSubject();
					} else{
						_log.info("Query "+qc+" keyword matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
						result = results.get(0);
					}
				} else if(internalQ!=null){
					result = kqp.getInternalNode(KeywordQueryProcessor.parseNode(internalQ));

					if(result==null){
						_log.info("Query "+qc+" no internal node matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
						ow.printNoConcurrenceSubject();
					} else{
						_log.info("Query "+qc+" internal node matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
					}
				} else{
					result = kqp.getExternalNode(KeywordQueryProcessor.parseNode(externalQ));

					if(result==null){
						_log.info("Query "+qc+" no external node matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
						ow.printNoConcurrenceSubject();
					} else{
						_log.info("Query "+qc+" external node matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
				throw new ServletException(e1);
			}



			try{
				if(result!=null){
					Node sub = result.getSubject();

					ow.startConcurrenceSubject(result);
					_log.info("Query "+qc+" subject serialised after  : "+(System.currentTimeMillis()-b4)+" ms.");

					ConcurrenceQueryProcessor cqp = (ConcurrenceQueryProcessor)ctx.getAttribute(Listener.CONCURRENCEQUERYPROCESSOR);
					ArrayList<ConcurrenceResult> results = cqp.getConcurrenceResults(sub, topK);

					if(results==null || results.isEmpty()){
						ow.printNoConcurrenceObjects();
						_log.info("Query "+qc+" no concurrence matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
					} else{
						_log.info("Query "+qc+" concurrence matched ["+results.size()+"] after  : "+(System.currentTimeMillis()-b4)+" ms.");
						int tuples = 0;
						for(ConcurrenceResult cr: results){
							tuples+=cr.getTuples().size();
							ow.printConcurrenceObject(cr);
						}
						_log.info("Query "+qc+" concurrence [results | tuples] ["+results.size()+" | "+tuples+"] serialised after  : "+(System.currentTimeMillis()-b4)+" ms.");
					}

					ow.endConcurrenceSubject();
				}
			} catch (Exception e1) {
				e1.printStackTrace();
				throw new ServletException(e1);
			}
		} else if(disambQ!=null){
			String f = request.getParameter("n");

			int topK = DEFAULT_DISAMB_RESULTS;
			if(f!=null){
				try {
					disambQ = new String(disambQ.getBytes(charenc),"UTF8");
					topK = Integer.parseInt(f);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			try {
				KeywordQueryProcessor kqp = (KeywordQueryProcessor)ctx.getAttribute(Listener.KEYWORDQUERYPROCESSOR);
				ArrayList<SnippetResult> result = kqp.getKeywordHits(disambQ, topK);

				if(result==null || result.isEmpty()){
					_log.info("Query "+qc+" no disambiguation keyword matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
					ow.printNoConcurrenceSubject();
				} else{
					_log.info("Query "+qc+" disambiguation keyword matched ["+result.size()+"]  after  : "+(System.currentTimeMillis()-b4)+" ms.");
					ow.printDisamibuationResults(result);
					_log.info("Query "+qc+" disambiguation keyword results ["+result.size()+"]  serialised after  : "+(System.currentTimeMillis()-b4)+" ms.");
				}
			} catch (Exception e1) {
				e1.printStackTrace();
				throw new ServletException(e1);
			}
		} else if(focusQ!=null){
			FocusQueryProcessor fqp = (FocusQueryProcessor)ctx.getAttribute(Listener.FOCUSQUERYPROCESSOR);

			try{
				Node f = KeywordQueryProcessor.parseNode(focusQ);
				Iterator<Node[]> results = fqp.getFocusResult(f);
				if(!results.hasNext()){
					_log.info("Query "+qc+" no focus node matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
					ow.printNoFocusResult();
				} else{
					_log.info("Query "+qc+" focus node matched after  : "+(System.currentTimeMillis()-b4)+" ms.");
					int tuples = 0;
					Node[] first = results.next();
					ow.startFocusResults(first[0], first[4], first[5], first[6]);
					ow.printFocusTuple(first);
					while(results.hasNext()){
						ow.printFocusTuple(results.next());
						tuples++;
					}
					ow.endFocusResults();
					_log.info("Query "+qc+" focus node results ["+tuples+"]  serialised after  : "+(System.currentTimeMillis()-b4)+" ms.");
				}
			}  catch (Exception e1) {
				e1.printStackTrace();
				throw new ServletException(e1);
			}
		} else {
			throw new ServletException("please specify parameter 'k|d|i|q|f'");
		}

		_log.info("Query "+qc+" finished after : "+(System.currentTimeMillis()-b4)+" ms.");

		ow.endOutput();
		out.close();
	}


	/**
	 * POST to register queries
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		throw new ServletException("post not supported, use get");
	}

	/**
	 * PUT is for adding quads.
	 */
	public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {   	
		throw new ServletException("put not supported, use get");
	}

	/**
	 * DELETE for removing quads.
	 */
	public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {   	
		throw new ServletException("delete not supported, use get");
	}
}