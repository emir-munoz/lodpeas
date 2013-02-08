package scratch;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.deri.conker.build.util.kw.KeywordIndexer;


public class KeywordLookups {
	public static String KW_DIR = "testdata/raw/kw/";
	
	static Logger _log = Logger.getLogger(KeywordLookups.class.getName());
	
	public static void main(String[] args) throws CorruptIndexException, IOException, ParseException{
		NIOFSDirectory dir = new NIOFSDirectory(new File(KW_DIR));
		IndexReader ir = IndexReader.open(dir);
		IndexSearcher kws = new IndexSearcher(ir);
		
		String keywordQ = "bbb";
		
		Analyzer sa = new StandardAnalyzer(Version.LUCENE_36);

		QueryParser qp = new MultiFieldQueryParser(
				Version.LUCENE_36, 
				new String[] { 
						KeywordIndexer.DocumentRepresentation.KEYWORDS,
						KeywordIndexer.DocumentRepresentation.LABEL_TEXT
				}, 
				sa);
		
		qp.setDefaultOperator(QueryParser.Operator.AND);

		Query query = qp.parse(keywordQ);

		ScoreDoc[] hits = kws.search(query, 10).scoreDocs;
		
		System.err.println("=== KEYWORD ===");
		for(ScoreDoc dc:hits){
			Document d = kws.doc(dc.doc);
			System.err.println(dc);
			System.err.println(d.get(KeywordIndexer.DocumentRepresentation.SUBJECT));
		}
		
		String subject = "<http://example.com/c>";
		
		query = new TermQuery(new Term(KeywordIndexer.DocumentRepresentation.SAMEAS,subject));

		hits = kws.search(query, 10).scoreDocs;
		
		System.err.println("=== ID ===");
		for(ScoreDoc dc:hits){
			Document d = kws.doc(dc.doc);
			System.err.println(dc);
			System.err.println(d.get(KeywordIndexer.DocumentRepresentation.SUBJECT));
			System.err.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.SAMEAS)));
			System.err.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.COMMENTS)));
			System.err.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.IMG)));
			System.err.println(d.get(KeywordIndexer.DocumentRepresentation.KEYWORDS));
			System.err.println(d.get(KeywordIndexer.DocumentRepresentation.LABEL_TEXT));
			System.err.println(d.get(KeywordIndexer.DocumentRepresentation.PREF_COMMENT));
			System.err.println(d.get(KeywordIndexer.DocumentRepresentation.PREF_LABEL));
			System.err.println(parseDouble(d.get(KeywordIndexer.DocumentRepresentation.RANK)));
		}
	}
	
	private static double parseDouble(String d){
		try{ 
			return Double.parseDouble(d);
		} catch(Exception e){
			_log.info("Cannot parse rank '"+d+"'");
			return 0d;
		}
	}
	
	public static String append(String[] in){
		if(in==null || in.length==0)
			return "{}";
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("{");
		for(String s:in){
			if(s!=null)
				sb.append("["+s+"]");
		}
		sb.append("}");
		return sb.toString();
	}
}
