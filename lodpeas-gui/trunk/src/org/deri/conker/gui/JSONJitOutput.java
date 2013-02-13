package org.deri.conker.gui;

import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;

import org.deri.conker.app.lookup.results.ConcurrenceResult;
import org.deri.conker.app.lookup.results.ConcurrenceResult.ConcurrenceTuple;
import org.deri.conker.app.lookup.results.SnippetResult;
import org.deri.conker.app.servlet.output.OutputWriter;
import org.deri.conker.build.util.concur.ConcurrenceEngine;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;

public class JSONJitOutput extends OutputWriter
{

	private static final int		MAX_COMMENT_LENGTH	= 300;
	private static final int		MAX_TUPLES			= 10;

	boolean							comma				= false;

	ArrayList<ConcurrenceResult>	queue				= new ArrayList<ConcurrenceResult>();

	public JSONJitOutput(PrintWriter out)
	{
		super(out);
	}

	public void startOutput()
	{
		out.println("[");
	}

	public void printQuery(String arg0)
	{

	}

	public void endOutput()
	{
		for (ConcurrenceResult cr : queue)
		{
			if (comma)
				out.println(",");
			out.println("{");
			String[] colourSize = convertScoreToNodeColourSize(cr.getOverallScore());
			printSnippet(cr.getSnippet(), "circle", colourSize[0], colourSize[1]);
			out.println("}");
			comma = true;
		}
		out.println("]");
	}

	public void startConcurrenceSubject(SnippetResult arg0)
	{
		if(comma)
			out.println(",");
		out.println("{");
		printSnippet(arg0, "circle", "#00FF00", "20");
		out.println(",");
		out.println("\"adjacencies\": [");
		comma = false;
	}

	public void printNoConcurrenceSubject()
	{
		out.println("{");
		out.println("\"id\" : \"<no_result>\",");
		out.println("\"name\" : \"Nothing Found!\",");
		out.println("\"data\" : {");
		out.println("\"asciiname\" : \"Nothing Found!\",");
		out.println("\"$color\" : \"#FF0000\",");
		out.println("\"$type\" : \"square\",");
		out.println("\"$dim\" : \"18\"");
		out.println("}");
		out.println("}");
	}

	private void printSnippet(SnippetResult s, String type, String colour, String size)
	{
		out.println("\"id\" : \"" + escape(s.getSubject().toN3()) + "\",");
		if (s.getLabel() != null)
			out.println("\"name\" : \"" + escape(cleanUpString(s.getLabel().toString())) + "\",");
		out.println("\"data\" : {");
		if (s.getLabel() != null)
			out.println("\"asciiname\" : \"" + escape(cleanUpString(s.getLabel().toN3())) + "\",");
		if(colour!=null)
			out.println("\"$color\" : \"" + colour + "\",");
		if(type!=null)
			out.println("\"$type\" : \"" + type + "\",");
		if (s.getComment() != null)
		{
			String comm = makeComment(s.getComment());
			if (!comm.isEmpty())
			{
				out.println("\"comment\" : \"" + escape(comm) + "\",");
			}
		}

		if(size!=null){
			out.println("\"$dim\" : \"" + size + "\",");
		}

		if (s.getImages() != null && !s.getImages().isEmpty())
		{
			// out.println("\"image\" : \""+ JSONWriter.escape(arg0.getImages().iterator().next())+"\"");
			out.println("\"image\" : [ ");
			int i = 0;
			for (String img : s.getImages())
			{
				i++;
				img = img.substring(1, img.length() - 1);
				out.print("\"" + escape(img) + "\"");
				if (i != s.getImages().size())
				{
					out.println(" ,");
				} else
				{
					out.println("");
				}
			}
			out.println("] ,");
		} 
		
		if (s.getAliases() != null && !s.getAliases().isEmpty())
		{
			// out.println("\"image\" : \""+ JSONWriter.escape(arg0.getImages().iterator().next())+"\"");
			out.println("\"alias\" : [ ");
			int i = 0;
			for (Node alias : s.getAliases())
			{
				i++;
				out.print("\"" + escape(alias.toN3()) + "\"");
				if (i != s.getAliases().size())
				{
					out.println(" ,");
				} else
				{
					out.println("");
				}
			}
			out.println("] ,");
		} 
		
		out.println("\"kwscore\" : \"" + escape(Double.toString(s.getScore()))+ "\",");
		out.println("\"prank\" : \"" + escape(Double.toString(s.getRank()))+ "\"");

		out.println("}");
	}

	private static final String makeComment(Literal comment)
	{
		if (comment == null)
			return "";
		String c = comment.toString();
		if (c.length() > MAX_COMMENT_LENGTH)
		{
			int i = 0;
			for (i = MAX_COMMENT_LENGTH; i < MAX_COMMENT_LENGTH + 20 && i < c.length(); i++)
			{
				if (Character.isWhitespace(c.charAt(i)))
				{
					break;
				}
			}
			return c.substring(0, i) + " ...";
		}
		return c;
	}

	public void endConcurrenceSubject()
	{
		out.println("]");
		out.println("}");
		comma = true;
	}

	public void printConcurrenceObject(ConcurrenceResult cr)
	{
		// print details later
		queue.add(cr);

		if (comma)
		{
			out.println(",");
		} else
		{
			comma = true;
		}

		printAdjacency(cr, "#FF0000", "2");
	}

	private void printAdjacency(ConcurrenceResult cr, String colour, String size)
	{
		SnippetResult s = cr.getSnippet();

		int outward = 0;
		int inward = 0;

		out.println("{");
		out.println("\"nodeTo\" : \"" + escape(s.getSubject().toN3()) + "\",");
		out.println("\"data\" :  {");
		out.println("\"$color\" : \"" + colour + "\",");
		out.println("\"$dim\" : \"" + size + "\",");
		out.print("\"score\" : \"" + cr.getOverallScore() + "\"");
		if (cr.getTuples() != null && !cr.getTuples().isEmpty())
		{
			out.println(",");
			out.println("\"common\" : [");

			for (int i = 0; i < cr.getTuples().size(); i++)
			{
				ConcurrenceTuple ct = cr.getTuples().get(i);

				if (ct.getDirection().equals(ConcurrenceEngine.SP_MARKER))
				{
					inward++;
					if (inward > MAX_TUPLES)
					{
						continue;
					}
				} else
				{
					outward++;
					if (outward > MAX_TUPLES)
					{
						continue;
					}
				}

				if (i > 0)
					out.println(",");

				out.println("{");
				out.println("\"plabel\" : \"" + escape(cleanUpString(ct.getPredicateLabel().toString())) + "\",");
				out.println("\"pnode\" : \"" + escape(ct.getPredicate().toN3()) + "\",");
				out.println("\"vlabel\" : \"" + escape(cleanUpString(ct.getValueLabel().toString())) + "\",");
				out.println("\"vnode\" : \"" + escape(ct.getValue().toString()) + "\",");
				out.println("\"n\" : \"" + escape(Integer.toString(ct.getN())) + "\",");
				out.println("\"dir\" : \"" + escape(ct.getDirection().toString()) + "\"");
				out.println("}");
			}
			out.println("]");
		} else
		{
			out.println();
		}
		out.println("}");
		out.println("}");
	}

	public void printNoConcurrenceObjects()
	{
		;
	}

	public String getOutputContentType()
	{
		return "application/json";
	}

	public void printDisamibuationResults(Collection<SnippetResult> arg0)
	{
		if(comma)
			out.println(",");
		
		out.println("{");
		out.println("\"disambiguation\"\t:");
		out.println("[");
		
		boolean first = true;
		for(SnippetResult sr : arg0){
			if(!first){
				out.println(",");
			}
			first = false;
			
			out.println("{");
			comma = false;
			printSnippet(sr,null,null,null);
			out.println("}");
		}
		
		out.println("]");
		out.println("}");
		
		comma = true;
		
	}
	
	public void startFocusResults(Node arg0, Node arg1, Node arg2, Node arg3)
	{

	}

	public void printNoFocusResult()
	{

	}

	@Override
	public void printFocusTuple(Node[] arg0)
	{

	}

	public void endFocusResults()
	{
		;
	}
	
	private static String cleanUpString(String s){
		if(s!=null && s.startsWith(">>")){
			try{
				String clean = s.substring(2);
				clean = URLDecoder.decode(clean,"UTF-8");
				clean = clean.replaceAll("_", " ");
				return clean;
			} catch(Exception e){
				return s;
			}
		}
		return s;
	}

	private static String[] convertScoreToNodeColourSize(double score)
	{
		if (score > 0.9)
		{
			return new String[] { "#00FF00", "20" };
		} else if (score > 0.8)
		{
			return new String[] { "#22FF22", "18" };
		} else if (score > 0.7)
		{
			return new String[] { "#44FF44", "16" };
		} else if (score > 0.6)
		{
			return new String[] { "#66FF66", "14" };
		} else if (score > 0.5)
		{
			return new String[] { "#77FF77", "12" };
		} else if (score > 0.4)
		{
			return new String[] { "#88FF88", "10" };
		} else if (score > 0.3)
		{
			return new String[] { "#99FF99", "8" };
		} else if (score > 0.2)
		{
			return new String[] { "#AAFFAA", "6" };
		} else if (score > 0.1)
		{
			return new String[] { "#BBFFBB", "4" };
		} else
		{
			return new String[] { "#CCFFCC", "2" };
		}
	}

	public static String escape(String in)
	{
		StringBuffer result = new StringBuffer();

		for (int i = 0; i < in.length(); i++)
		{
			int cp = in.codePointAt(i);
			char c;

			if (!Character.isSupplementaryCodePoint(cp))
			{
				c = (char) (cp);
				switch (c)
				{
				case '\\':
					result.append("\\\\");
					break;
				case '"':
					result.append("\\\"");
					break;
				// case '/':
				// result.append("\\/");
				// break;
				case '\n':
					result.append("\\n");
					break;
				case '\r':
					result.append("\\r");
					break;
				case '\b':
					result.append("\\b");
					break;
				case '\f':
					result.append("\\f");
					break;
				case '\t':
					result.append("\\t");
					break;
				default:
					if (c >= 0x0 && c <= 0x8 || c == 0xB || c == 0xC || c >= 0xE && c <= 0x1F || c >= 0x7F
							&& c <= 0xFFFF)
					{
						result.append("\\u");
						result.append(toHexString(c, 4));
					} else
					{
						result.append(c);
					}
				}
			} else
			{
				String pair = toHexString(cp, 8);
				String pair1 = pair.substring(0, 4);
				String pair2 = pair.substring(4, 8);
				result.append("\\u" + pair1 + "\\u" + pair2);
				++i;
			}

		}

		return result.toString();
	}

	/**
	 * Converts a decimal value to a hexadecimal string represention of the
	 * specified length. For unicode escaping.
	 * 
	 * @param decimal
	 *            A decimal value.
	 * @param stringLength
	 *            The length of the resulting string.
	 **/
	private static String toHexString(int decimal, int stringLength)
	{
		return String.format("%0" + stringLength + "X", decimal);
	}
}
