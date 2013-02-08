package org.deri.conker.build.cli;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.NIOFSDirectory;
import org.deri.conker.build.util.kw.KeywordIndexer;


/**
 * Use owl:sameAs relations in the data to consolidate/canonicalise
 * aliases.
 * 
 * @author aidhog
 *
 */
public class DumpKeywordDocument {
	static transient Logger _log = Logger.getLogger(DumpKeywordDocument.class.getName());
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
		Options options = org.semanticweb.yars.nx.cli.Main.getStandardOptions();
		
		Option kwO = new Option("kw", "keyword directory");
		kwO.setArgs(1);
		kwO.setRequired(true);
		options.addOption(kwO);
		
		Option rO = new Option("r", "resource to search");
		rO.setArgs(1);
		options.addOption(rO);

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("***ERROR: " + e.getClass() + ": " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options );
			return;
		}
		
		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options );
			return;
		}
		
		NIOFSDirectory dir = new NIOFSDirectory(new File(cmd.getOptionValue(kwO.getOpt())));
		IndexReader ir = IndexReader.open(dir);
		IndexSearcher kws = new IndexSearcher(ir);
		
		Query query = new TermQuery(new Term(KeywordIndexer.DocumentRepresentation.SAMEAS,cmd.getOptionValue(rO.getOpt())));

		ScoreDoc[] hits = kws.search(query, 10).scoreDocs;
		
		
		for(ScoreDoc dc:hits){
			Document d = kws.doc(dc.doc);
			System.out.println("================");
			System.out.println("=== ID "+dc+" ===");
			System.out.println("================");
			System.out.println("=== SUBJECT ===");
			System.out.println(d.get(KeywordIndexer.DocumentRepresentation.SUBJECT));
			System.out.println("=== SAMEAS ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.SAMEAS)));
			System.out.println("=== AUTH_COMMENTS ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.AUTH_COMMENTS)));
			System.out.println("=== COMMENTS ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.COMMENTS)));
			System.out.println("=== PREF_COMMENT ===");
			System.out.println(d.get(KeywordIndexer.DocumentRepresentation.PREF_COMMENT));
			System.out.println("=== AUTH_LABELS ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.AUTH_LABELS)));
			System.out.println("=== LABELS ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.LABELS)));
			System.out.println("=== PREF_LABEL ===");
			System.out.println(d.get(KeywordIndexer.DocumentRepresentation.PREF_LABEL));
			System.out.println("=== IMGs ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.IMG)));
			System.out.println("=== KEYWORDS ===");
			System.out.println(d.get(KeywordIndexer.DocumentRepresentation.KEYWORDS));
			System.out.println("=== RANK ===");
			System.out.println(parseDouble(d.get(KeywordIndexer.DocumentRepresentation.RANK)));
			System.out.println("=== AUTH_TYPES ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.AUTH_TYPES)));
			System.out.println("=== TYPES ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.TYPES)));
			System.out.println("=== DATES ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.DATES)));
			System.out.println("=== AUTH_DATES ===");
			System.out.println(append(d.getValues(KeywordIndexer.DocumentRepresentation.AUTH_DATES)));
			System.out.println("=== LAT ===");
			System.out.println(d.get(KeywordIndexer.DocumentRepresentation.LAT));
			System.out.println("=== LONG ===");
			System.out.println(d.get(KeywordIndexer.DocumentRepresentation.LONG));
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
