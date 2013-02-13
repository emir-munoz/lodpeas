package org.deri.conker.build.util.uri;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.tld.TldManager;

/**
 * Static helper methods to process URIs strings.
 * 
 * @author aidhog
 *
 */
public class URIUtils {

	/**
	 * Manages TLDs used to deduce PLDs.
	 */
	private static final TldManager TLDM; 
	static{
		try{
			TLDM = new TldManager();
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}

	/**
	 * Default value if no PLD can be found
	 */
	public static final String NO_PLD = "no_pld";

	/**
	 * Extract PLD from a resource or a contextual bnode.
	 * @param n A resource or a contextual bnode.
	 * @return The PLD of the node, or NO_PLD if not found.
	 */
	public static String getPld(Node n){
		if(n instanceof Resource){
			return getPld((Resource)n);
		} else if(n instanceof BNode){
			return getPld((BNode)n);
		} else{
			return NO_PLD;
		}
	}


	/**
	 * Extract PLD from a resource.
	 * @param r The resource
	 * @return The PLD of the resource, or NO_PLD if not found.
	 */
	public static String getPld(Resource r){
		try{
			return getPld(r.toString());
		} catch(Exception ue){
			return NO_PLD;
		}
	}

	/**
	 * Extract PLD from a contextual BNode
	 * @param b The blank node
	 * @return The PLD of the blank node, or NO_PLD if not found.
	 */
	public static String getPld(BNode b){
		try{
			String[] split = BNode.parseContextualBNode(b);
			if(split != null && split.length>0){
				getPld(split[0]);
			}
			return NO_PLD;
		} catch(Exception pe){
			return NO_PLD;
		}
	}

	/**
	 * Extract PLD from a URI string
	 * @param s A URI string
	 * @return The PLD of the string, or NO_PLD if not found.
	 */
	public static String getPld(String s){
		try{
			URI u = new URI(s);
			return getPld(u);
		} catch(Exception ue){
			return NO_PLD;
		}
	}

	/**
	 * Extract PLD from a URI
	 * @param u A URI
	 * @return The PLD of the URI, or NO_PLD if not found.
	 */
	public static String getPld(URI u){
		try{
			String pld = TLDM.getPLD(u);
			if(pld==null)
				pld = NO_PLD;
			return pld;
		} catch(Exception ue){
			return NO_PLD;
		}
	}
	
	/**
	 * Wraps {@link #normalise(URI u) normalise} method with
	 * String argument and return value. If URISyntaxException 
	 * is thrown during the normalise method, the input string
	 * is returned. 
	 * 
	 * @param u
	 * @return
	 */
	public static String normalise(String u){
		try{
			URI uu = new URI(u);
			return normalise(uu).toString();
		} catch(Exception e){
			return u;
		}
	}

	/**
	 * Maps a data URI to a document URL, as a crawler
	 * would do. Primarily, the fragment is removed from
	 * the URI. The case of the domain can also be dropped.
	 * 
	 * This code taken specifically from LDspider.
	 * 
	 * (I have my doubts about removing index* suffixes.)
	 * 
	 * @param u
	 * @return A URI u with common /index* suffixes removed, 
	 * then fragments removed, a slash added for empty path,
	 * and with scheme and host lowercased.
	 */
	public static URI normalise(URI u) throws URISyntaxException {
		String path = u.getPath();
		if (path == null || path.length() == 0) {
			path = "/";
		} else if (path.endsWith("/index.html")) {
			path = path.substring(0, path.length()-10);
		} else if (path.endsWith("/index.htm") || path.endsWith("/index.php") || path.endsWith("/index.asp")) {
			path = path.substring(0, path.length()-9);
		}

		if (u.getHost() == null) {
			throw new URISyntaxException(u.toString(), "no host in");
		}

		// remove fragment
		URI norm = new URI(u.getScheme().toLowerCase(),
				u.getUserInfo(), u.getHost().toLowerCase(), u.getPort(),
				path, u.getQuery(), null);

		return norm.normalize();
	}
	
	public static boolean isAuthoritative(Node term, Node context, RedirectsMap rm){
		if(context instanceof Resource){
			if(term instanceof Resource){
				return isAuthoritative((Resource)term, (Resource)context, rm);
			} else if(term instanceof BNode){
				return true;
			}
		} 
		return false;
	}
	
	public static boolean isAuthoritative(Resource term, Resource context, RedirectsMap rm){
		String con = context.toString().toLowerCase().trim();

		String namespaceS = getNamespace(term.toString());
		String namespaceSL = namespaceS.toLowerCase().trim();
				
		if(coincides(namespaceSL, con)){
			return true;
		}
					
		Resource redirect = rm.get(term);
		
		if(redirect!=null){
			String namespaceR = getNamespace(redirect.toString());
			String namespaceRL = namespaceR.toLowerCase().trim();
			
			if(coincides(namespaceRL,con)){
				return true;
			}
		}
		
		redirect = rm.get(new Resource(namespaceS));
		
		if(redirect!=null){
			String namespaceR = getNamespace(redirect.toString());
			String namespaceRL = namespaceR.toLowerCase().trim();
			
			if(coincides(namespaceRL,con)){
				return true;
			}
		}
		
		return false;
	}
	
	public static String getNamespace(String r){
		int hash, slash, end;
		hash = r.lastIndexOf("#");
		slash = r.lastIndexOf("/");
		end = Math.max(hash, slash);
		if(end <= 0)
			return r;
		else if(end == hash)
			return r.substring(0,end);
		else
			return r.substring(0,end+1);
	}
	
	public static String removeWWW(String in){
		if(!in.contains("www.")){
			return in;
		}
		int index = in.indexOf("www.");
		in = in.substring(0, index) + in.substring(index+4);
		return in;
	}
	
	private static boolean coincides(String ns, String c){
		String sns = ns.substring(0,ns.length()-1) ;
		return c.startsWith(sns) || removeWWW(c).startsWith(removeWWW(ns));
	}
}
