package org.deri.conker.build.util.kw;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.deri.conker.build.util.index.LabelPicker;
import org.deri.conker.build.util.index.LabelPicker.LabelSet;
import org.deri.conker.build.util.uri.RedirectsMap;
import org.deri.conker.build.util.uri.URIUtils;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.namespace.DC;
import org.semanticweb.yars.nx.namespace.DCTERMS;
import org.semanticweb.yars.nx.namespace.GEO;
import org.semanticweb.yars.nx.namespace.OWL;
import org.semanticweb.yars.nx.namespace.RDF;
import org.semanticweb.yars.nx.namespace.RDFS;
import org.semanticweb.yars.nx.namespace.SIOC;
import org.semanticweb.yars.nx.namespace.XSD;
import org.semanticweb.yars.nx.parser.ParseException;
import org.semanticweb.yars.util.CheckSortedIterator;

public class KeywordIndexer {
	static Logger _log = Logger.getLogger(KeywordIndexer.class.getName());

	public static final int MERGE_FACTOR = 20;

	public static double MIN_RANK = 0.0000001d;

	public static int RANK_INDEX = 5;

	public static final String[] IMG_FILE_EXTENSIONS = new String[]{
		".jpg", ".jpeg", ".gif", ".png", ".bmp", ".tif", ".tiff"
	};

	public static final Node[] LABEL_PROPERTIES = LabelPicker.LABEL_PROPERTIES;

	private static final Set<Node> LABEL_PROPERTIES_SET = new HashSet<Node>();

	static{
		for(Node n:LABEL_PROPERTIES){
			LABEL_PROPERTIES_SET.add(n);
		}
	}

	public static final Node[] COMMENT_PROPERTIES = new Node[]{
		RDFS.COMMENT, 
		DC.DESCRIPTION, 
		new Resource("http://purl.org/rss/1.0/description"),
		SIOC.CONTENT, 
		DCTERMS.DESCRIPTION, 
		new Resource("http://purl.org/vocab/bio/0.1/olb"),
		new Resource("http://dbpedia.org/ontology/abstract")
	};

	private static final Set<Node> COMMENT_PROPERTIES_SET = new HashSet<Node>();

	static{
		for(Node n:COMMENT_PROPERTIES){
			COMMENT_PROPERTIES_SET.add(n);
		}
	}

	public static final Node[] DATE_PROPERTIES = new Node[]{
		DC.DATE
	};

	private static final Set<Node> DATE_PROPERTIES_SET = new HashSet<Node>();

	static{
		for(Node n:DATE_PROPERTIES){
			DATE_PROPERTIES_SET.add(n);
		}
	}

	public static void buildLucene(Iterator<Node[]> in, String dir) throws CorruptIndexException, LockObtainFailedException, IOException, ParseException{
		buildLucene(in, null, dir);
	}

	public static void buildLucene(Iterator<Node[]> in, RedirectsMap rm, String dir) throws CorruptIndexException, LockObtainFailedException, IOException, ParseException{
		Analyzer sa = new StandardAnalyzer(Version.LUCENE_36);

		File f = new File(dir);
		NIOFSDirectory fsd = new NIOFSDirectory(f);

		LogMergePolicy lmp = new LogDocMergePolicy();
		lmp.setMergeFactor(MERGE_FACTOR);

		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_36, sa);
		conf.setMergePolicy(lmp);

		IndexWriter writer = new IndexWriter(fsd, conf);     //new IndexWriter(fsd, sa, IndexWriter.MaxFieldLength.LIMITED);

		buildKeyword(in, rm, writer);

		_log.info("Closing index...");
		long b4 = System.currentTimeMillis();
		writer.close();
		_log.info("closed index in "+ (System.currentTimeMillis()-b4)+" ms.");
		_log.info("...done!");
	}

	protected static void buildKeyword(Iterator<Node[]> it, RedirectsMap rm, IndexWriter writer) throws IOException, ParseException {
		Node oldSub = null;
		Node[] q = null;
		Node subject = null;

		_log.info("Performing keyword build...");

		DocumentRepresentation dr = null;

		double rank;

		long b4 = System.currentTimeMillis();

		CheckSortedIterator vs = new CheckSortedIterator(it);
		it = vs;

		boolean index = false;
		while (it.hasNext()) {	
			q = it.next();

			if(!vs.isOkay())
				throw new RuntimeException(vs.getException());

			subject = q[0];

			// need to create a Lucene document with all information per subject
			if(oldSub==null || !subject.equals(oldSub)){
				if(dr!=null && index){
					store(dr, writer);
				}

				try{
					rank = Double.parseDouble(q[RANK_INDEX].toString());
				} catch(Exception e){
					rank = MIN_RANK;
					_log.severe("Error finding rank from index "+ RANK_INDEX + " in tuple "+Nodes.toN3(q));
				}

				dr = new DocumentRepresentation(subject, rank);
				index = false;
			}

			if(!q[1].equals(OWL.SAMEAS)){
				index = true;
			}

			if(rm==null || q.length<=3){
				dr.addStatement(q);
			} else{
				dr.addStatement(q,URIUtils.isAuthoritative(q[0],q[3],rm));
			}

			oldSub = subject;
		}

		//write last
		if(dr!=null && index){
			store(dr, writer);
		}

		_log.info("...finished keyword build in "+(System.currentTimeMillis()-b4)+" ms.");
	}

	static String toStr(Set<String> set) {
		StringBuffer sb = new StringBuffer();

		for (String s: set) {
			sb.append(s);
			sb.append(" ");
		}

		return sb.toString();
	}

	static StringBuffer tokenize(Node n) {
		StringBuffer sb = new StringBuffer();

		if (!(n instanceof Resource)) {
			return sb;
		}

		try {
			URL u = new URL(n.toString());
			StringTokenizer tok = new StringTokenizer(u.getHost());
			while (tok.hasMoreElements()) {
				sb.append(tok.nextToken());
				sb.append(" ");
			}
			tok = new StringTokenizer(u.getPath());
			while (tok.hasMoreElements()) {
				sb.append(tok.nextToken());
				sb.append(" ");
			}
		} catch (MalformedURLException mue) {
			System.err.println("no host for " + n.toString());
		}

		return sb;
	}

	static Set<String> tokenizeSet(Node n) {
		HashSet<String> sb = new HashSet<String>();

		if (!(n instanceof Resource)) {
			return sb;
		}

		try {
			URL u = new URL(n.toString());
			StringTokenizer tok = new StringTokenizer(u.getHost());
			while (tok.hasMoreElements()) {
				sb.add(tok.nextToken());
			}
			tok = new StringTokenizer(u.getPath());
			while (tok.hasMoreElements()) {
				sb.add(tok.nextToken());
			}
		} catch (MalformedURLException mue) {
			System.err.println("no host for " + n.toString());
		}

		return sb;
	}

	static void store(DocumentRepresentation dr, IndexWriter iw) throws CorruptIndexException, IOException {
		//		Node sub = dr.getSub();
		//		Resource debugF = new Resource("http://purl.org/dc/terms/Agent");
		//		if(sub.equals(debugF)){
		//			System.err.println("Agent storing.");
		//		}

		//		if(sub instanceof Resource){
		//			try {
		//				URL u = new URL(sub.toString());
		//				StringTokenizer tok = new StringTokenizer(u.getHost());
		//				while (tok.hasMoreElements()) {
		//					dr.addText(tok.nextToken());
		//				}
		//				tok = new StringTokenizer(u.getPath());
		//				while (tok.hasMoreElements()) {
		//					dr.addText(tok.nextToken());
		//				}
		//			} catch (MalformedURLException mue) {
		////				System.err.println("no host for " + sub.toString());
		//			}
		//		}

		Document doc = DocumentRepresentation.toDocument(dr);
		iw.addDocument(doc);
	}

	public static class DocumentRepresentation{

		public static final int MAX_LITERAL_LENGTH = 100000;
		public static final float ENG_LABEL_BOOST = 2000f;
		public static final float NON_ENG_LABEL_BOOST = 20f;

		public static final float HAS_LABEL_BOOST = 10f;
		public static final float HAS_IMAGE_BOOST = 4f;
		public static final float HAS_COMMENT_BOOST = 4f;

		public static final String SUBJECT = "subject";

		public static final String KEYWORDS = "keywords";

		public static final String NON_ENGLISH_LABEL_TEXT = "nelabeltext";
		public static final String ENGLISH_LABEL_TEXT = "elabeltext";

		public static final String LABELS = "labels";
		public static final String AUTH_LABELS = "authlabels";
		public static final String PREF_LABEL = "preflabel";

		public static final String COMMENTS = "comments";
		public static final String AUTH_COMMENTS = "authcomments";
		public static final String PREF_COMMENT = "prefcomment";

		public static final String TYPES = "types";
		public static final String AUTH_TYPES = "authtypes";

		public static final String DATES = "dates";
		public static final String AUTH_DATES = "authdates";

		public static final String RANK = "rank";

		public static final String SAMEAS = "sameas";

		public static final String LAT = "lat";
		public static final String LONG = "long";
		public static final String IMG = "img";
		public static final String LABEL_TEXT = null;

		private Node _sub = null;
		private double _rank = MIN_RANK;
		private StringBuffer _text = null;
		private StringBuffer _enLabelText = null;
		private StringBuffer _nonEnglishLabelText = null;

		private Set<Literal> _labels = null;
		private Set<Literal> _authLabels = null;

		private Set<Literal> _comments = null;
		private Set<Literal> _authComments = null;

		private Set<Literal> _authDates = null;
		private Set<Literal> _dates = null;

		private Set<Node> _authTypes = null;
		private Set<Node> _types = null;

		private Set<Resource> _sameas = null;

		private Set<Resource> _imgs = null;
		private Literal _lat = null, _lon = null;

		private LabelSet _pickLabels = null;
		private LabelSet _pickComments = null;

		public DocumentRepresentation(Node sub, double rank){
			_sub = sub;
			_rank = rank;
		}

		public Node getSub() {
			return _sub;
		}

		public double getRank() {
			return _rank;
		}

		public String getText() {
			if(_text==null)
				return null;
			return _text.toString().trim();
		}

		public void addText(Literal text) {
			if(_text==null){
				_text = new StringBuffer();
			}
			_text.append(text);
			_text.append(" ");
		}

		public void addText(String text) {
			if(_text==null){
				_text = new StringBuffer();
			}
			_text.append(text);
			_text.append(" ");
		}

		public String getNonEnglishLabelText() {
			if(_nonEnglishLabelText==null)
				return null;
			return _nonEnglishLabelText.toString().trim();
		}

		public String getEnglishLabelText() {
			if(_enLabelText==null)
				return null;
			return _enLabelText.toString().trim();
		}

		public void addNonEnglishLabelText(Literal label) {
			if(_nonEnglishLabelText==null)
				_nonEnglishLabelText = new StringBuffer();
			_nonEnglishLabelText.append(label);
			_nonEnglishLabelText.append(" ");
		}

		public void addEnglishLabelText(Literal label) {
			if(_enLabelText==null)
				_enLabelText = new StringBuffer();
			_enLabelText.append(label);
			_enLabelText.append(" ");
		}

		public Set<Literal> getLabels() {
			return _labels;
		}

		public void addLabel(Literal label) {
			if(_labels==null){
				_labels = new HashSet<Literal>();
			}
			if(_pickLabels==null){
				_pickLabels = new LabelSet(); 
			}
			_labels.add(label);
			_pickLabels.addLabel(label);
		}

		public Literal pickLabel(){
			if(_pickLabels!=null)
				return _pickLabels.chooseLabel();
			return null;
		}

		public Set<Node> getTypes() {
			return _types;
		}

		public void addType(Node type) {
			if(_types==null){
				_types = new HashSet<Node>();
			}
			_types.add(type);
		}

		public Set<Resource> getSameAs() {
			return _sameas;
		}

		public void addSameAs(Resource sameas) {
			if(_sameas==null){
				_sameas = new HashSet<Resource>();
			}
			_sameas.add(sameas);
		}

		public Set<Node> getAuthTypes() {
			return _authTypes;
		}

		public void addAuthType(Node type) {
			if(_authTypes==null){
				_authTypes = new HashSet<Node>();
			}
			_authTypes.add(type);
		}

		public Set<Literal> getAuthLabels() {
			return _authLabels;
		}

		public void addAuthLabel(Literal authLabel) {
			if(_authLabels==null){
				_authLabels = new HashSet<Literal>();
			}
			if(_pickLabels==null){
				_pickLabels = new LabelSet(); 
			}
			_pickLabels.addAuthoritativeLabel(authLabel);
			_authLabels.add(authLabel);
		}

		public Set<Literal> getComments() {
			return _comments;
		}

		public void addComment(Literal comment) {
			if(_comments==null){
				_comments = new HashSet<Literal>();
			}
			if(_pickComments==null){
				_pickComments = new LabelSet(); 
			}
			_comments.add(comment);
			_pickComments.addLabel(comment);
		}

		public Literal pickComment(){
			if(_pickComments!=null)
				return _pickComments.chooseLabel();
			return null;
		}

		public Set<Literal> getAuthComments() {
			return _authComments;
		}

		public void addAuthComment(Literal authComment) {
			if(_authComments==null){
				_authComments = new HashSet<Literal>();
			}
			if(_pickComments==null){
				_pickComments = new LabelSet(); 
			}
			_authComments.add(authComment);
			_pickComments.addAuthoritativeLabel(authComment);
		}

		public Set<Literal> getDates() {
			return _dates;
		}

		public void addDate(Literal date) {
			if(_dates==null){
				_dates = new HashSet<Literal>();
			}
			_dates.add(date);
		}

		public Set<Literal> getAuthDates() {
			return _authDates;
		}

		public void addAuthDate(Literal authDate) {
			if(_authDates==null){
				_authDates = new HashSet<Literal>();
			}
			_authDates.add(authDate);
		}

		public Set<Resource> getImages() {
			return _imgs;
		}

		public void addImage(Resource img) {
			if(_imgs==null){
				_imgs = new HashSet<Resource>();
			}
			_imgs.add(img);
		}

		public Literal getLat() {
			return _lat;
		}

		public void setLat(Literal lat) {
			_lat = lat;
		}

		public Literal getLong() {
			return _lon;
		}

		public void setLong(Literal lon) {
			_lon = lon;
		}

		public void addStatement(Node[] na){
			addStatement(na, false);
		}

		public void addStatement(Node[] na, boolean subjAuth){
			Node object = na[2];
			if(object instanceof Literal){
				String str = object.toString();
				if (str.length() < MAX_LITERAL_LENGTH) {
					Literal ol = (Literal) object;
					if(LABEL_PROPERTIES_SET.contains(na[1])){
						if(subjAuth){
							addAuthLabel(ol);
						} else{
							addLabel(ol);
						}

						String lt = ol.getLanguageTag();
						if(lt==null || lt.toLowerCase().startsWith("en")){
							addEnglishLabelText(ol);
						} else {
							addNonEnglishLabelText(ol);
						}
					} else if(COMMENT_PROPERTIES_SET.contains(na[1])){
						if(subjAuth){
							addAuthComment(ol);
						} else{
							addComment(ol);
						}
						addText(ol);
					} else if(DATE_PROPERTIES_SET.contains(na[1]) || (ol.getDatatype()!=null
							&& (ol.getDatatype().equals(XSD.DATE) || ol.getDatatype().equals(XSD.DATETIME)
									|| ol.getDatatype().equals(XSD.DATETIMESTAMP)))){
						if(subjAuth){
							addAuthDate(ol);	
						} else{
							addDate(ol);
						}
						addText(ol);
					} else if(na[1].equals(GEO.LAT)){
						_lat = ol;
						addText(ol);
					} else if(na[1].equals(GEO.LONG)){
						_lon = ol;
						addText(ol);
					} else{
						addText(ol);
					}
				}
			} else if(object instanceof Resource){
				if(na[1].equals(RDF.TYPE)){
					if(subjAuth){
						addAuthType(na[2]);
					} else{
						addType(na[2]);
					}
				} else if(na[1].equals(OWL.SAMEAS)){
					addSameAs((Resource)na[2]);
				} else{
					String r = object.toString().toLowerCase();
					if(r.startsWith("http")){
						for(String fe:IMG_FILE_EXTENSIONS){
							if(r.endsWith(fe)){
								addImage((Resource)object);
							}
						}
					}
				}
			}
		}

		static Document toDocument(DocumentRepresentation dr) throws java.io.FileNotFoundException, UnsupportedEncodingException {
			// make a new, empty document
			Document doc = new Document();

			int aliasDomains = 1;
			if(dr.getSameAs()!=null) {
				aliasDomains+=getAliasePlds(dr.getSameAs()).size();
			}
			float boost =  (float)dr.getRank() * (float)aliasDomains;

			doc.add(new Field(SUBJECT, dr.getSub().toN3(), Field.Store.YES, Field.Index.NOT_ANALYZED));

			String nonEngLabelText = dr.getNonEnglishLabelText();
			if(nonEngLabelText!=null && !nonEngLabelText.isEmpty()){
				Field f = new Field(NON_ENGLISH_LABEL_TEXT, nonEngLabelText, Field.Store.YES, Field.Index.ANALYZED);
				f.setBoost(NON_ENG_LABEL_BOOST);
				doc.add(f);
			}

			String engLabelText = dr.getEnglishLabelText();
			if(engLabelText!=null && !engLabelText.isEmpty()){
				Field f = new Field(ENGLISH_LABEL_TEXT, engLabelText, Field.Store.YES, Field.Index.ANALYZED);
				f.setBoost(ENG_LABEL_BOOST);
				doc.add(f);
			}

			//			don't index text
			//			String text = dr.getText();
			//			if(text!=null && !text.isEmpty()){
			//				Field f = new Field(KEYWORDS, text, Field.Store.YES, Field.Index.ANALYZED);
			//				doc.add(f);
			//			}

			doc.add(new Field(RANK, Double.toString(dr.getRank()), Field.Store.YES, Field.Index.NO));

			boolean lb = false;
			Set<Literal> labels = dr.getLabels();
			if(labels!=null) {
				for(Literal l:labels){
					if(!l.toString().isEmpty()){
						lb = true;
						doc.add(new Field(LABELS, l.toN3(), Field.Store.YES, Field.Index.NO));
					}
				}
			}

			Set<Literal> authlabels = dr.getAuthLabels();
			if(authlabels!=null) {
				for(Literal l:authlabels){
					if(!l.toString().isEmpty()){
						lb = true;
						doc.add(new Field(AUTH_LABELS, l.toN3(), Field.Store.YES, Field.Index.NO));
					}
				}
			}
			
			if(lb){
				boost *= HAS_LABEL_BOOST;
			}

			Literal prefLabel = dr.pickLabel();
			if(prefLabel!=null){
				doc.add(new Field(PREF_LABEL, prefLabel.toN3(), Field.Store.YES, Field.Index.NO));
			} else if(lb){
				_log.warning("No preferred label found for "+dr.getSub()+" though labels exist... AUTH: "+authlabels+" NA: "+labels);
			}


			boolean cb = false;
			Set<Literal> comments = dr.getComments();
			if(comments!=null) {
				for(Literal l:comments){
					if(!l.toString().isEmpty()){
						doc.add(new Field(COMMENTS, l.toN3(), Field.Store.YES, Field.Index.NO));
						cb = true;
					}
				}
			}

			Set<Literal> authcomments = dr.getAuthComments();
			if(authcomments!=null) {
				for(Literal l:authcomments){
					if(!l.toString().isEmpty()){
						doc.add(new Field(AUTH_COMMENTS, l.toN3(), Field.Store.YES, Field.Index.NO));
						cb = true;
					}
				}
			}
			
			if(cb)
				boost *= HAS_COMMENT_BOOST;

			Literal prefComment = dr.pickComment();
			if(prefComment!=null){
				doc.add(new Field(PREF_COMMENT, prefComment.toN3(), Field.Store.YES, Field.Index.NO));
			} else if(cb){
				_log.warning("No preferred comment found for "+dr.getSub()+" though comments exist... AUTH: "+authcomments+" NA: "+comments);
			}


			Set<Literal> dates = dr.getDates();
			if(dates!=null) for(Literal l:dates){
				doc.add(new Field(DATES, l.toN3(), Field.Store.YES, Field.Index.NO));
			}

			Set<Literal> authdates = dr.getAuthDates();
			if(authdates!=null) for(Literal l:authdates){
				doc.add(new Field(AUTH_DATES, l.toN3(), Field.Store.YES, Field.Index.NO));
			}

			Set<Node> types = dr.getTypes();
			if(types!=null) for(Node t:types){
				doc.add(new Field(TYPES, t.toN3(), Field.Store.YES, Field.Index.NO));
			}

			Set<Node> authtypes = dr.getAuthTypes();
			if(authtypes!=null) for(Node t:authtypes){
				doc.add(new Field(AUTH_TYPES, t.toN3(), Field.Store.YES, Field.Index.NO));
			}

			if(dr.getLat()!=null && dr.getLong()!=null){
				doc.add(new Field(LAT, dr.getLat().toN3(), Field.Store.YES, Field.Index.NO));
				doc.add(new Field(LONG, dr.getLong().toN3(), Field.Store.YES, Field.Index.NO));
			}

			Set<Resource> imgs = dr.getImages();
			if(imgs!=null && !imgs.isEmpty()) {
				boost *= HAS_IMAGE_BOOST;
				for(Resource img:imgs){
					doc.add(new Field(IMG, img.toN3(), Field.Store.YES, Field.Index.NO));
				}
			}

			Set<Resource> sameas = dr.getSameAs();
			if(sameas!=null) for(Resource sa:sameas){
				doc.add(new Field(SAMEAS, sa.toN3(), Field.Store.YES, Field.Index.NOT_ANALYZED));
			}
			doc.add(new Field(SAMEAS, dr.getSub().toN3(), Field.Store.YES, Field.Index.NOT_ANALYZED));


			doc.setBoost(boost);
			// return the document
			return doc;
		}

		private static HashSet<String> getAliasePlds(Set<Resource> sameAs2) {
			HashSet<String> plds = new HashSet<String>();
			for(Resource r:sameAs2){
				try{
					String pld = URIUtils.getPld(r);
					if(pld!=null && !pld.equals(URIUtils.NO_PLD)){
						plds.add(pld);
					}
				} catch(Exception e){
					//to be sure.
				}
			}
			return plds;
		}
	}
}
