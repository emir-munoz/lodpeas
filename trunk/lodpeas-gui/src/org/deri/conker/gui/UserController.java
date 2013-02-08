package org.deri.conker.gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.deri.conker.app.lookup.ConcurrenceQueryProcessor;
import org.deri.conker.app.lookup.FocusQueryProcessor;
import org.deri.conker.app.lookup.KeywordQueryProcessor;
import org.deri.conker.app.lookup.results.ConcurrenceResult;
import org.deri.conker.app.lookup.results.SnippetResult;
import org.semanticweb.nxindex.NodesIndex;
import org.semanticweb.nxindex.block.NodesBlockReaderIO;
import org.semanticweb.nxindex.block.util.CallbackIndexerIO;
import org.semanticweb.nxindex.sparse.SparseIndex;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UserController
{

	private static final boolean		DEBUG			= false;
	
	private static String			KW;
	private static String			QUAD_NI;
	private static String			QUAD_SP;
	private static String			CONCUR_NI;
	private static String			CONCUR_SP;	

	private KeywordQueryProcessor		kqp				= null;
	@SuppressWarnings("unused")
	private FocusQueryProcessor			fqp				= null;
	private ConcurrenceQueryProcessor	cqp				= null;

	private static int					TOP_K			= 20;
	private static int					DISAMB			= 10;

	private int							queryCounter	= 0;

	static final Logger					_log			= Logger.getLogger(UserController.class.getName());

	public UserController() throws IOException, ParseException
	{
		_log.info("Setting properties from config.props ");
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("../config.props");
        Properties properties = new Properties();
        properties.load(inputStream);  
  
		KW = properties.getProperty("KW");
		QUAD_NI = properties.getProperty("QUAD_NI");
		QUAD_SP = properties.getProperty("QUAD_SP");
		CONCUR_NI = properties.getProperty("CONCUR_NI");
		CONCUR_SP = properties.getProperty("CONCUR_SP");
		if (!DEBUG)
		{
			_log.info("Opening keyword directory at " + KW);
			Directory d = new NIOFSDirectory(new File(KW));
			IndexReader ir = IndexReader.open(d);
			IndexSearcher kws = new IndexSearcher(ir);
			this.kqp = new KeywordQueryProcessor(kws);
			_log.info("... opened keyword searcher.");

			_log.info("Opening quad index at " + QUAD_NI);
			NodesBlockReaderIO q_nbr = new NodesBlockReaderIO(QUAD_NI);
			SparseIndex qsi = CallbackIndexerIO.loadSparseIndex(QUAD_SP, 100000);
			NodesIndex qnbi = new NodesIndex(q_nbr, qsi);
			this.fqp = new FocusQueryProcessor(qnbi);
			_log.info("... opened quad sparse index of size " + qsi.size());

			_log.info("Opening concurrence index at " + CONCUR_NI);
			NodesBlockReaderIO c_nbr = new NodesBlockReaderIO(CONCUR_NI);
			SparseIndex csi = CallbackIndexerIO.loadSparseIndex(CONCUR_SP, 100000);
			NodesIndex cnbi = new NodesIndex(c_nbr, csi);
			this.cqp = new ConcurrenceQueryProcessor(cnbi, kqp);
			_log.info("... opened concurrence sparse index of size " + qsi.size());
		}
	}

	@RequestMapping(value = "/index", method = RequestMethod.GET, headers = "Accept=*/*")
	public String getIndex() throws Exception
	{
		return "jittest";
	}

	@RequestMapping(value = "/search", method = RequestMethod.GET, headers = "Accept=*/*")
	public @ResponseBody
	String getName(@RequestParam("type") String type, @RequestParam("term") String query) throws Exception
	{
		if (type.equals("normal"))
			_log.info("Normal query");
		else if (type.equals("uri"))
			_log.info("URI query");
		
		int qc = -1;

		synchronized (this)
		{
			queryCounter++;
			qc = queryCounter;
		}

		query = URLDecoder.decode(query, "UTF-8");

		_log.info("query= " + query);

		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);

		JSONJitOutput ow = new JSONJitOutput(out);
		ow.startOutput();

		ow.printQuery(query);

		long b4 = System.currentTimeMillis();

		
		SnippetResult result = null;
		ArrayList<SnippetResult> results = null;
		
		if(type.equals("normal")){
			results = kqp.getKeywordHits(query, DISAMB);
			if (results == null || results.isEmpty())
			{
				_log.info("Query " + qc + " no keyword matched after  : " + (System.currentTimeMillis() - b4) + " ms.");
				ow.printNoConcurrenceSubject();
			} else
			{
				result = results.get(0);
				_log.info("Query " + qc + " keyword matched to " + result.getSubject().toN3() + " after  : "
						+ (System.currentTimeMillis() - b4) + " ms.");
			}
		} else{
			result = kqp.getInternalNode(KeywordQueryProcessor.parseNode(query));
			if(result==null){
				_log.info("Lookup " + qc + " no internal node after  : " + (System.currentTimeMillis() - b4) + " ms.");
				ow.printNoConcurrenceSubject();
			} else{
				_log.info("Lookup " + query + " internal node query matched to " + result.getSubject().toN3() + " after  : "
						+ (System.currentTimeMillis() - b4) + " ms.");
			}
		}

		
		if(result!=null){
			if(results!=null && results.size()>1){
				ow.printDisamibuationResults(results);
			}
			
			
			Node sub = result.getSubject();
	
			ow.startConcurrenceSubject(result);
			_log.info("Query " + qc + " subject " + sub.toN3() + " serialised after  : "
					+ (System.currentTimeMillis() - b4) + " ms.");
	
			ArrayList<ConcurrenceResult> oresults = cqp.getConcurrenceResults(sub, TOP_K);
	
			if (oresults == null || oresults.isEmpty())
			{
				ow.printNoConcurrenceObjects();
				_log.info("Query " + qc + " no concurrence matched after  : " + (System.currentTimeMillis() - b4) + " ms.");
			} else
			{
				_log.info("Query " + qc + " concurrence [results | tuples] [" + oresults.size() + 
						"] matched after  : " + (System.currentTimeMillis() - b4) + " ms.");
				int tuples = 0;
				for (ConcurrenceResult cr : oresults)
				{
					tuples += cr.getTuples().size();
					ow.printConcurrenceObject(cr);
				}
				_log.info("Query " + qc + " concurrence [results | tuples] [" + oresults.size() + " | " + tuples
						+ "] serialised after  : " + (System.currentTimeMillis() - b4) + " ms.");
			}
	
			ow.endConcurrenceSubject();
		}

		ow.endOutput();

		out.flush();
		String response = sw.toString();

		_log.info("Query " + qc + " finished after : " + (System.currentTimeMillis() - b4) + " ms.");
		_log.info(response);

//		response = "{"
//				+ "\"queryString:\"	:	\"d=deri\","
//				+ "\"disambiguation\"	:"
//				+ "["
//				+ "{"
//				+ "\"id\"	:	\"<http://data.linkedmdb.org/resource/film/4074>\","
//				+ "\"alias\"	:	[ \"<http://data.linkedmdb.org/resource/film/4074>\" , \"<http://dbpedia.org/resource/The_Texas_Chainsaw_Massacre_3>\" ] ,"
//				+ "\"label\"	:	\"Leatherface: Texas Chainsaw Massacre III\","
//				+ "\"comment\"	:	\"Leatherface: The Texas Chainsaw Massacre III is the second sequel to the 1974 film The Texas Chain Saw Massacre and was directed by Jeff Burr. It was released by New Line Cinema on January 12, 1990. The film is both a sequel and a reboot to the previous movies, as the original Sawyer family and apparently the Leatherface character died in the previous film. Leatherface stars Kate Hodge, Ken Foree, William Butler, and a then-unknown Viggo Mortensen. At first, New Line Cinema intended to produce the film as the first of several sequels in the series. However, the film did not prove a financial success, although Jeff Burr did receive a nomination for the International Fantasy Film Award at the Fantasporto film festival in 1990. Leatherface gained a certain amount of notoriety prior to release due to a battle between New Line Cinema and the MPAA, which initially rated the film an X because of its graphic violence. The studio eventually relented, and trimmed the more graphic elements, however, in 2003 it released the uncut version in VHS and DVD formats. It was the final film to receive this classification before the MPAA replaced X with NC-17. The tag line The Saw is Family is derived from a line spoken by Drayton Sawyer in the previous film.\","
//				+ "\"image\"	:	 [ \"<http://upload.wikimedia.org/wikipedia/commons/4/48/TCM3.jpg>\" , \"<http://upload.wikimedia.org/wikipedia/commons/thumb/4/48/TCM3.jpg/200px-TCM3.jpg>\" ] ,"
//				+ "\"prank\"	:	\"2.785099968605209E-5\","
//				+ "\"kwscore\"	:	\"48.831764221191406\""
//				+ "}"
//				+ ","
//				+ "{"
//				+ "\"id\"	:	\"<http://dbpedia.org/resource/Deri_RFC>\","
//				+ "\"alias\"	: [ \"<http://www4.wiwiss.fu-berlin.de/flickrwrappr/photos/Deri_RFC>\" , \"<http://mpii.de/yago/resource/Deri_RFC>\" , \"<http://dbpedia.org/resource/Deri_RFC>\"] ,"
//				+ "\"label\"	:	\"Deri RFC\","
//				+ "\"comment\"	:	\"Deri Rugby Football Club is a Welsh rugby union team based in Deri, Caerphilly in Wales. The club is a member of the Welsh Rugby Union and is a feeder club for the Newport Gwent Dragons.\","
//				+ "\"prank\"	:	\"6.76409808875178E-6\","
//				+ "\"kwscore\"	:	\"24.415882110595703\""
//				+ "},"
//				+ "{"
//				+ "\"id\"	:	\"<http://data.semanticweb.org/organization/deri-nui-galway>\","
//				+ "\"alias\"	:	[ \"<http://data.semanticweb.org/organization/deri-nui-galway>\" , \"<http://ontoworld.org/wiki/Special:URIResolver/DERI_NUI_Galway>\" , \"<http://social.semantic-2dweb.at/wiki/index.php/_DERI_Galway>\" ] ,"
//				+ "\"prank\"	:	\"2.5083316359086893E-5\","
//				+ "\"kwscore\"	:	\"15.628495216369629\""
//				+ "},"
//				+ "{"
//				+ "\"id\"	:	\"<http://my.opera.com/deriramdhani/xml/foaf>\","
//				+ "\"alias\"	:	[ \"<http://my.opera.com/deriramdhani/xml/foaf>\" ] ,"
//				+ "\"label\"	:	\"FoaF Document for Deri ramdhani\","
//				+ "\"comment\"	:	\"Friend-of-a-Friend description of Deri ramdhani\","
//				+ "\"prank\"	:	\"2.4620996555313468E-5\","
//				+ "\"kwscore\"	:	\"14.242597579956055\""
//				+ "},"
//				+ "{"
//				+ "\"id\"	:	\"<http://dbpedia.org/resource/Dermochelidae>\","
//				+ "\"alias\"	:	[ \"<http://www4.wiwiss.fu-berlin.de/flickrwrappr/photos/Leatherback_sea_turtle>\" , \"<http://dbpedia.org/resource/Dermochelidae>\" ] ,"
//				+ "\"label\"	:	\"Leatherback sea turtle\","
//				+ "\"comment\"	:	\"The leatherback sea turtle (Dermochelys coriacea) is the largest of all living sea turtles and the fourth largest modern reptile behind three crocodilians. It is the only living species in the genus Dermochelys. It can easily be differentiated from other modern sea turtles by its lack of a bony shell. Instead, its carapace is covered by skin and oily flesh. Dermochelys coriacea is the only extant member of the family Dermochelyidae.\","
//				+ "\"image\"	:	[ \"<http://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/LeatherbackTurtle.jpg/200px-LeatherbackTurtle.jpg>\" , \"<http://upload.wikimedia.org/wikipedia/commons/9/9b/LeatherbackTurtle.jpg>\" ],"
//				+ "\"prank\"	:	\"1.3179951565689407E-6\","
//				+ "\"kwscore\"	:	\"5.086641788482666\""
//				+ "},"
//				+ "{"
//				+ "\"id\"	:	\"<http://www.ecs.soton.ac.uk/~dt2/dlstuff/www2006_data#deri>\","
//				+ "\"alias\"	:	[ \"<http://www.ecs.soton.ac.uk/~dt2/dlstuff/www2006_data#deri>\" ],"
//				+ "\"label\"	:	\"DERI\","
//				+ "\"prank\"	:	\"1.3141716408426873E-5\","
//				+ "\"kwscore\"	:	\"4.0693135261535645\""
//				+ "},"
//				+ "{"
//				+ "\"id\"	:	\"<http://dbpedia.org/resource/Deri,_Caerphilly>\","
//				+ "\"alias\"	:	[ \"<http://dbpedia.org/resource/Deri,_Caerphilly>\" , \"<http://rdf.freebase.com/ns/m.0ds08r5>\" ] ,"
//				+ "\"label\"	:	\"Deri, Caerphilly\","
//				+ "\"prank\"	:	\"6.7481382757250685E-6\","
//				+ "\"kwscore\"	:	\"3.051985263824463\""
//				+ "},"
//				+ "{"
//				+ "\"id\"	:	\"<http://www.ecs.soton.ac.uk/~dt2/dlstuff/www2006_data#deri_innsbruck>\","
//				+ "\"alias\"	:	[ \"<http://www.ecs.soton.ac.uk/~dt2/dlstuff/www2006_data#deri_innsbruck>\" ],"
//				+ "\"label\"	:	\"DERI Innsbruck\","
//				+ "\"prank\"	:	\"1.3141716408426873E-5\","
//				+ "\"kwscore\"	:	\"2.543320894241333\""
//				+ "},"
//				+ "{"
//				+ "\"id\"	:	\"<http://data.nytimes.com/15567942791571858143>\","
//				+ "\"alias\"	:	[ \"<http://rdf.freebase.com/ns/en.aryeh_deri>\" , \"<http://dbpedia.org/resource/Aryeh_Deri>\" , \"<http://data.nytimes.com/deri_aryeh_per>\" , \"<http://data.nytimes.com/15567942791571858143>\" ],"
//				+ "\"label\"	:	\"Deri, Aryeh\","
//				+ "\"prank\"	:	\"2.1804612515552435E-6\","
//				+ "\"kwscore\"	:	\"1.7983994483947754\""
//				+ "},"
//				+ "{"
//				+ "\"id\"	:	\"<http://dbpedia.org/resource/Cattle_hide>\","
//				+ "\"alias\"	:	[ \"<http://dbpedia.org/resource/Cattle_hide>\" , \"<http://dbpedia.org/resource/Leather_making>\" ],"
//				+ "\"label\"	:	\"Leather\","
//				+ "\"comment\"	:	\"Leather is a durable and flexible material created via the tanning of putrescible animal rawhide and skin, primarily cattlehide. It can be produced through different manufacturing processes, ranging from cottage industry to heavy industry.\","
//				+ "\"image\"	:	[ \"<http://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Leathertools.jpg/200px-Leathertools.jpg>\" , \"<http://upload.wikimedia.org/wikipedia/commons/5/5c/Leathertools.jpg>\" ],"
//				+ "\"prank\"	:	\"4.927736085846846E-7\"," + "\"kwscore\"	:	\"1.7803246974945068\"" + "}" + "]" + "}";

		return response;
	}
}
