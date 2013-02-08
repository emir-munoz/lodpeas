package org.deri.conker.build.util.uri;

import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.namespace.DC;
import org.semanticweb.yars.nx.namespace.FOAF;
import org.semanticweb.yars.nx.namespace.RDF;
import org.semanticweb.yars.nx.namespace.RDFS;
import org.semanticweb.yars.nx.parser.ParseException;

public class RedirectsAuthorityInspectorTest extends TestCase {
	public static final Resource[][] REDIRS = new Resource[][]{
		{FOAF.NAME,  new Resource("http://xmlns.com/foaf/spec/")},
		{DC.DESCRIPTION,  new Resource("http://dublincore.org/2010/10/11/dcelements.rdf")},
		{new Resource("http://purl.org/dc/terms/created"),  new Resource("http://dublincore.org/2010/10/11/dcterms.rdf")},
		{new Resource("http://owl.mindswap.org/2003/ont/owlweb.rdf"), new Resource("http://www.mindswap.org/2003/ont/owlweb.rdf")},
		{new Resource("http://www.mpii.de/yago/resource/yagoMonetaryValue"), new Resource("http://www.mpi-inf.mpg.de/yago/resource/yagoMonetaryValue")},
		{new Resource("http://www.mpi-inf.mpg.de/yago/resource/yagoMonetaryValue"), new Resource("http://yago.zitgist.com/yagoMonetaryValue")},
		{new Resource("http://yago.zitgist.com/yagoMonetaryValue"), new Resource("http://yago.zitgist.com/yagoMonetaryValue.rdf")},
		{new Resource("http://purl.org/ontology/mo/MusicArtist"), new Resource("http://motools.sourceforge.net/mo/MusicArtist")},
		{new Resource("http://purl.org/ontology/mo/Composer"), new Resource("http://motools.sourceforge.net/doc/musicontology.rdfs")},
		{new Resource("http://purl.org/ontology/mo/composer"), new Resource("http://motools.sourceforge.net/doc/musicontology.rdfs")},
		{new Resource("http://purl.org/ontology/mo/Torrent"), new Resource("http://motools.sourceforge.net/mo/Torrent")},
		{new Resource("http://purl.org/ontology/mo/baritone"), new Resource("http://motools.sourceforge.net/mo/baritone")},
		{new Resource("http://purl.org/NET/c4dm/event.owl#Factor"), new Resource("http://motools.sourceforge.net/event/")},
		{new Resource("http://motools.sourceforge.net/event/"), new Resource("http://motools.sourceforge.net/event/event.rdf")}
	};
	
	public void testCompressedFileRedirectsAuthority() throws ParseException, IOException{
		ArrayList<Node[]> redirs = new ArrayList<Node[]>();
		for(Resource[] redir:REDIRS){
			redirs.add(redir);
		}
		RedirectsMap rm = RedirectsMap.load(redirs.iterator());
		rm.forwardAll();
		
		checkAuthority(rm);
	}
	
	private static void checkAuthority(RedirectsMap rm){
		assertTrue(URIUtils.isAuthoritative(RDFS.LABEL, new Resource(RDFS.NS),rm));
		assertTrue(URIUtils.isAuthoritative(RDF.TYPE, new Resource(RDF.NS),rm));
		assertTrue(URIUtils.isAuthoritative(FOAF.NAME, new Resource("http://xmlns.com/foaf/spec/"),rm));
		assertTrue(URIUtils.isAuthoritative(FOAF.NAME, new Resource("http://xmlns.com/foaf/spec/index.rdf"),rm));
		assertTrue(URIUtils.isAuthoritative(DC.TITLE, new Resource(DC.NS),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://www.deri.org/Example"),new Resource("http://deri.org/"),rm));
		assertTrue(URIUtils.isAuthoritative(DC.DESCRIPTION, new Resource("http://dublincore.org/2010/10/11/dcelements.rdf"),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://demo.openlinksw.com/schemas/northwind#Address"), new Resource("http://demo.openlinksw.com/schemas/northwind"),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://b4mad.net/ns/foaf-ext/preferredMeansOfContact"), new Resource("http://b4mad.net/ns/foaf-ext/index.rdf"),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://owl.mindswap.org/2003/ont/owlweb.rdf#Bookmark"), new Resource("http://www.mindswap.org/2003/ont/owlweb.rdf"),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://dig.csail.mit.edu/2007/wiki/Projects.rdf#OpenLinkedDataProject"), new Resource("http://dig.csail.mit.edu/2007/wiki/projects.rdf"),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://purl.org/dc/terms/created"), new Resource("http://dublincore.org/2010/10/11/dcterms.rdf"),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://aidanhogan.com/vocab.rdf#sibling"), new Resource("http://aidanhogan.com/vocab.rdf"),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://www.mpii.de/yago/resource/yagoMonetaryValue"), new Resource("http://yago.zitgist.com/yagoMonetaryValue.rdf"),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://purl.org/ontology/mo/Composer"), new Resource("http://motools.sourceforge.net/doc/musicontology.rdfs"),rm));
		assertTrue(URIUtils.isAuthoritative(new Resource("http://purl.org/NET/c4dm/event.owl#Factor"), new Resource("http://motools.sourceforge.net/event/event.rdf"),rm));
		
		System.err.println(URIUtils.isAuthoritative(new Resource("http://purl.org/NET/c4dm/event.owl#Factor"), new Resource("http://motools.sourceforge.net/event/event.rdf"),rm));
		
		assertFalse(URIUtils.isAuthoritative(new Resource("http://www.deri.org/foaf/"), new Resource("http://deri.org/"),rm));
		assertFalse(URIUtils.isAuthoritative(new Resource("http://www.deri.org/Example"), new Resource("http://sw.deri.org/"),rm));
		assertFalse(URIUtils.isAuthoritative(FOAF.NAME, new Resource(DC.NS),rm));
		assertFalse(URIUtils.isAuthoritative(RDFS.DOMAIN, new Resource(FOAF.NS),rm));
	}
}
