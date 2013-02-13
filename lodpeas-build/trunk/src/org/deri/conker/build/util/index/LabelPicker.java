package org.deri.conker.build.util.index;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.deri.conker.build.util.uri.RedirectsMap;
import org.deri.conker.build.util.uri.URIUtils;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.namespace.DC;
import org.semanticweb.yars.nx.namespace.DCTERMS;
import org.semanticweb.yars.nx.namespace.FOAF;
import org.semanticweb.yars.nx.namespace.RDFS;
import org.semanticweb.yars.nx.namespace.SIOC;
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.util.NxUtil;
import org.semanticweb.yars.stats.Count;

public class LabelPicker {

	static Logger _log = Logger.getLogger(LabelPicker.class.getName());

	public static double MIN_RANK = 0.000000000001d;

	public static Literal BLANK_LIT = new Literal("");

	public static Literal INTERNAL = new Literal("1");
	public static Literal NOINTERNAL = new Literal("0");

	public static String MARK_LNAME = ">>";

	public static final Node[] LABEL_PROPERTIES = new Node[]{
		RDFS.LABEL, DC.TITLE, DCTERMS.TITLE, new Resource("http://purl.org/rss/1.0/title"),
		SIOC.NAME, new Resource("http://rdf.freebase.com/ns/type.object.name"),
		FOAF.NAME,
		new Resource("http://rdf.freebase.com/ns/type.object.name"),
		new Resource("http://www.w3.org/2004/02/skos/core#altLabel"),
		new Resource("http://www.w3.org/2004/02/skos/core#prefLabel"),
		new Resource("http://www.w3.org/2004/02/skos/core#hiddenLabel"),
		new Resource("http://swrc.ontoware.org/ontology#title"),
		new Resource("http://ecowlim.tfri.gov.tw/lode/resource/lit/eco/speciesName"),
		new Resource("http://ecowlim.tfri.gov.tw/lode/resource/lit/flyhorse/speciesName"),
		new Resource("http://rdf.ookaboo.com/object/label"),
		new Resource("http://usefulinc.com/ns/doap#name"),
		new Resource("http://aims.fao.org/aos/geopolitical.owl#nameListEN")
	};

	private static final Set<Node> LABEL_PROPERTIES_SET = new HashSet<Node>();
	static{
		for(Node lp:LABEL_PROPERTIES){
			LABEL_PROPERTIES_SET.add(lp);
		}
	}

	public static void pickLabelsAndRanks(Iterator<Node[]> in, Iterator<Node[]> ranks, Callback cb){
		pickLabelsAndRanks(in, ranks, null, cb);
	}

	public static void pickLabelsAndRanks(Iterator<Node[]> in, Iterator<Node[]> ranks, RedirectsMap rm, Callback cb){
		Node oldSub = null;
		double rank = MIN_RANK;

		Node[] rna = null;
		LabelSet ls = new LabelSet();


		while (in.hasNext()) {	
			Node[] q = in.next();

			Node subject = q[0];

			if(oldSub==null || !subject.equals(oldSub)){
				if(oldSub!=null){
					Literal l = ls.chooseLabel();
					if(l==null && oldSub instanceof Resource){
						String lname = MARK_LNAME+parseLocalName((Resource)oldSub);
						if(lname!=null && !lname.isEmpty())
							l = new Literal(NxUtil.escapeForNx(lname));
					}

					if(l==null)	
						cb.processStatement(new Node[]{oldSub,BLANK_LIT,new Literal(Double.toString(rank)),INTERNAL});
					else
						cb.processStatement(new Node[]{oldSub,l,new Literal(Double.toString(rank)),INTERNAL});
					ls = new LabelSet();
				}

				rank = MIN_RANK;

				//				if (rna == null && ranks.hasNext()) {
				//					rna = ranks.next();
				//				}
				//				
				//				if (rna != null){
				//					comp = rna[0].compareTo(subject);
				//				}


				int comp =  Integer.MIN_VALUE;
				if(rna != null){
					comp = rna[0].compareTo(subject);
				}

				//				while (rna!=null && ranks.hasNext() && comp < 0) {
				while (ranks.hasNext() && comp < 0) {
					rna = ranks.next();
					comp =  rna[0].compareTo(subject);
					if(comp<0){
						if(rna[0] instanceof Resource){
							cb.processStatement(new Node[]{rna[0], new Literal(NxUtil.escapeForNx(MARK_LNAME+parseLocalName((Resource)rna[0]))), rna[1], NOINTERNAL});
						} else {
							cb.processStatement(new Node[]{rna[0], BLANK_LIT, rna[1], NOINTERNAL});
						}
					}
				}

				if(rna[0].equals(subject)){
					rank = Float.parseFloat(rna[1].toString());
				}
			}

			if(LABEL_PROPERTIES_SET.contains(q[1]) && q[2] instanceof Literal){
				if(rm==null || !URIUtils.isAuthoritative(q[0], q[3], rm)){
					ls.addLabel((Literal)q[2]);
				} else{
					ls.addAuthoritativeLabel((Literal)q[2]);
				}
			}

			oldSub = subject;
		}

		//do last
		Literal l = ls.chooseLabel();
		if(l==null && oldSub instanceof Resource){
			String lname = MARK_LNAME+parseLocalName((Resource)oldSub);
			if(lname!=null && !lname.isEmpty())
				l = new Literal(NxUtil.escapeForNx(MARK_LNAME+lname));
		}

		if(l==null)	
			cb.processStatement(new Node[]{oldSub,BLANK_LIT,new Literal(Double.toString(rank)),INTERNAL});
		else
			cb.processStatement(new Node[]{oldSub,l,new Literal(Double.toString(rank)),INTERNAL});

		while (ranks.hasNext()) {
			rna = ranks.next();
			if(rna[0] instanceof Resource){
				cb.processStatement(new Node[]{rna[0], new Literal(NxUtil.escapeForNx(MARK_LNAME+parseLocalName((Resource)rna[0]))), rna[1], NOINTERNAL});
			} else {
				cb.processStatement(new Node[]{rna[0], BLANK_LIT, rna[1], NOINTERNAL});
			}
		}
	}

	public static String parseLocalName(Resource r){
		String sr = r.toString();
		int hash = sr.lastIndexOf('#');
		int slash = sr.lastIndexOf('/');

		int cut = hash;
		if(slash>hash) cut = slash;

		if(cut>0){
			return sr.substring(cut+1);
		} else{
			return "";
		}
	}

	public static class LabelSet {
		Count<String> dist;
		Set<Literal> labelSet;
		Set<Literal> authLabelSet;

		public LabelSet(){
			dist = new Count<String>();
			labelSet = new HashSet<Literal>();
			authLabelSet = new HashSet<Literal>();
		}

		public void addLabel(Literal l){
			labelSet.add(l);
			dist.add(l.getData());
		}

		public void addAuthoritativeLabel(Literal l){
			authLabelSet.add(l);
			dist.add(l.getData());
		}

		public Literal chooseLabel(){
			Literal pref = null;
			if(authLabelSet!=null && !authLabelSet.isEmpty()){
				pref = choose(authLabelSet,dist);
				if(pref!=null){
					String lang = pref.getLanguageTag();
					if(lang==null || lang.startsWith("en")){
						return pref;
					}
				}
			}

			if(labelSet!=null && !labelSet.isEmpty()){
				pref = choose(labelSet,dist);
			}
			return pref;
		}

		private static Literal choose(Set<Literal> lits, Count<String> dist){
			Literal pref_l = null;
			boolean pref_eng = false;
			String pref_s = null;


			for(Literal l:lits){
				if(l==null)
					continue;
				
				String lang = l.getLanguageTag();
				boolean nolang = lang==null;
				boolean eng = false;
				String s = l.getData();
				
				if(s==null)
					continue;

				if(pref_l==null){
					pref_l = l;
					pref_eng = eng;
					pref_s = s;
				} else if(!pref_l.equals(l)){
					if(!nolang && lang.toLowerCase().startsWith("en"))
						eng = true;


					if(!s.isEmpty() && pref_s.toString().isEmpty()){ //prefer non-empty to empty
						pref_l = l;
						pref_eng = eng;
						pref_s = s;
					} else if (!pref_l.toString().isEmpty() && l.toString().isEmpty()){ //do nothing
						;
					} else if(eng && !pref_eng){ //prefer english to no lang or non-english
						pref_l = l;
						pref_eng = eng;
						pref_s = s;
					} else if(pref_eng && !eng){ //do nothing
						;
					} else if(l.getDatatype()==null && pref_l.getDatatype()!=null){ //prefer plain to datatype
						pref_l = l;
						pref_eng = eng;
						pref_s = s;
					} else if(pref_l.getDatatype()==null && l.getDatatype()!=null){ //do nothing
						;
					} else if(l.getLanguageTag()==null && pref_l.getLanguageTag()!=null){ //prefer no lang to non-english
						pref_l = l;
						pref_eng = eng;
						pref_s = s;
					} else if((pref_l.getLanguageTag()==null) && l.getLanguageTag()!=null){ //do nothing
						;
					} else if(s.contains(",") && !pref_s.contains(",")){ //prefer no comma to comma
						pref_l = l;
						pref_eng = eng;
						pref_s = s;
					} else if(!pref_s.contains(",") && s.contains(",")){ //do nothing
						;
					} else { //both english || both datatype || both non-lang plain || both non-english || both (no-) comma
						int countp = 0;
						Integer icountp = dist.get(pref_s);
						if(icountp==null){
							_log.warning("Missing count for literal "+pref_s);
						} else{
							countp = icountp;
						}

						int countl = 0;
						Integer icountl = dist.get(s);
						if(icountl==null){
							_log.warning("Missing count for literal "+s);
						} else{
							countl = icountl;
						}

						if(countl>countp){ //prefer more common
							pref_l = l;
							pref_eng = eng;
							pref_s = s;
						} else if(countl==countp && pref_s.length()>s.length()){//prefer longer
							pref_l = l;
							pref_eng = eng;
							pref_s = s;
						}
					}
				}
			}

			return pref_l;
		}
	}
}
