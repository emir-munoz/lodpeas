package org.deri.conker.app.lookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.deri.conker.app.lookup.results.SnippetResult;
import org.deri.conker.build.util.kw.KeywordIndexer;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

public class KeywordQueryProcessor {
	private IndexSearcher _is;
	
	static Logger _log = Logger.getLogger(KeywordQueryProcessor.class.getName());
	
	public static QueryParser KW_QUERY_PARSER = null;
	
	public static float BOOST_TEXT = 1f;
	public static float BOOST_LABEL = 1000f;
	
	static{
//		HashMap<String,Float> boost = new HashMap<String,Float>();
//		boost.put(KeywordIndexer.DocumentRepresentation.KEYWORDS, BOOST_TEXT);
//		boost.put(KeywordIndexer.DocumentRepresentation.LABEL_TEXT, BOOST_LABEL);
//		
		KW_QUERY_PARSER = new MultiFieldQueryParser(
		Version.LUCENE_36, 
		new String[] { 
				KeywordIndexer.DocumentRepresentation.ENGLISH_LABEL_TEXT,
				KeywordIndexer.DocumentRepresentation.NON_ENGLISH_LABEL_TEXT,
				
		}, 
		new StandardAnalyzer(Version.LUCENE_36));
		KW_QUERY_PARSER.setDefaultOperator(Operator.AND);
		
		
//		KW_QUERY_PARSER = new QueryParser(
//		Version.LUCENE_36, 
//		KeywordIndexer.DocumentRepresentation.ENGLISH_LABEL_TEXT, 
//		new StandardAnalyzer(Version.LUCENE_36));
//		KW_QUERY_PARSER.setDefaultOperator(Operator.AND);
		

	}
	
	public KeywordQueryProcessor(IndexSearcher is){
		_is = is;
	}
	
	public SnippetResult getExternalNode(Node externalSub) throws ParseException, IOException, org.semanticweb.yars.nx.parser.ParseException{
		return getSnippet(searchExternal(externalSub));
	}
	
	public ArrayList<SnippetResult> getKeywordHits(String keywordQuery, int noOfResults) throws ParseException, IOException, org.semanticweb.yars.nx.parser.ParseException{
		ScoreDoc[] hits = searchKeyword(keywordQuery, noOfResults);
		return getSnippets(hits);
	}
	
	public HashMap<Node,SnippetResult> getInternalNodes(Collection<Node> internalSubs) throws ParseException, IOException, org.semanticweb.yars.nx.parser.ParseException{
		HashMap<Node,SnippetResult> snippets = new HashMap<Node,SnippetResult>();
		if(internalSubs!=null) for(Node internalSub:internalSubs){
			SnippetResult sr = getInternalNode(internalSub);
			if(sr!=null){
				snippets.put(internalSub, sr);
			}
		}
		return snippets;
	}
	
	public SnippetResult getInternalNode(Node internalSub) throws ParseException, IOException, org.semanticweb.yars.nx.parser.ParseException{
		return getSnippet(searchInternal(internalSub));
	}
	
	private ScoreDoc searchExternal(Node externalSub) throws ParseException, IOException{
		Query query = new TermQuery(new Term(KeywordIndexer.DocumentRepresentation.SAMEAS,externalSub.toN3()));
		ScoreDoc[] hits = _is.search(query, 1).scoreDocs;
		if(hits.length==1)
			return hits[0];
		return null;
	}
	
	private ScoreDoc searchInternal(Node internalSub) throws ParseException, IOException{
		Query query = new TermQuery(new Term(KeywordIndexer.DocumentRepresentation.SUBJECT,internalSub.toN3()));
		ScoreDoc[] hits = _is.search(query, 1).scoreDocs;
		if(hits.length==1)
			return hits[0];
		return null;
	}
	
	private ScoreDoc[] searchKeyword(String keywordQuery, int noOfResults) throws ParseException, IOException{
		Query query = KW_QUERY_PARSER.parse(keywordQuery);
		ScoreDoc[] hits = _is.search(query, noOfResults).scoreDocs;
		return hits;
	}
	
	private ArrayList<SnippetResult> getSnippets(ScoreDoc[] sds) throws CorruptIndexException, IOException, org.semanticweb.yars.nx.parser.ParseException{
		ArrayList<SnippetResult> snippets = new ArrayList<SnippetResult>();
		if(sds!=null) for(ScoreDoc sd:sds){
			snippets.add(getSnippet(sd));
		}
		return snippets;
	}
	
	private SnippetResult getSnippet(ScoreDoc sd) throws CorruptIndexException, IOException, org.semanticweb.yars.nx.parser.ParseException{
		if(sd==null) return null;
		Document d = _is.doc(sd.doc);
		SnippetResult sr = getSnippet(d);
		sr.setScore(sd.score);
		return sr;
	}
	
	private static SnippetResult getSnippet(Document d) throws CorruptIndexException, IOException, org.semanticweb.yars.nx.parser.ParseException{
		SnippetResult sr = new SnippetResult(parseNode(d.get(KeywordIndexer.DocumentRepresentation.SUBJECT)));

		sr.setComment(parseLiteral(d.get(KeywordIndexer.DocumentRepresentation.PREF_COMMENT)));
		sr.setLabel(parseLiteral(d.get(KeywordIndexer.DocumentRepresentation.PREF_LABEL)));
		sr.setRank(parseDouble(d.get(KeywordIndexer.DocumentRepresentation.RANK)));
		
		String[] imgs = d.getValues(KeywordIndexer.DocumentRepresentation.IMG);
		if(imgs!=null) for(String img:imgs){
			sr.addImage(img);
		}
		
		String[] comments = d.getValues(KeywordIndexer.DocumentRepresentation.COMMENTS);
		if(comments!=null) for(String comment:comments){
			sr.addComment(comment);
		}
		
		String[] aliases = d.getValues(KeywordIndexer.DocumentRepresentation.SAMEAS);
		if(aliases!=null) for(String alias:aliases){
			Node alias_n = parseNode(alias);
			if(alias_n!=null) sr.addAlias(alias_n);
		}
		
		return sr;
	}
	
	private static Literal parseLiteral(String l){
		try{
			Node n = parseNode(l);
			if(n !=null && n instanceof Literal){
				return (Literal)n;
			} else{
				return null;
			}
		} catch(Exception e){
			return null;
		}
	}
	
	public static Node parseNode(String s){
		try{
			//@SuppressWarnings("deprecation")
			return NxParser.parseNode(s);
		} catch(Exception e){
			return null;
		}
	}
	
	private static double parseDouble(String d){
		try{ 
			return Double.parseDouble(d);
		} catch(Exception e){
			_log.warning("Cannot parse rank value "+d);
			return 0d;
		}
	}
}