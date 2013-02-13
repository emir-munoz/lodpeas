package org.deri.conker.app.servlet.output;

import java.io.PrintWriter;
import java.util.Collection;

import org.deri.conker.app.lookup.results.ConcurrenceResult;
import org.deri.conker.app.lookup.results.ConcurrenceResult.ConcurrenceTuple;
import org.deri.conker.app.lookup.results.SnippetResult;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;

public class JSONWriter extends OutputWriter{
	public static String JSON_CONTENT_TYPE = "application/json";
	
	
	private int nestingLevel = 0; 
	
	private boolean comma = false;
	
	public JSONWriter(PrintWriter out){
		super(out);
	}

	public void startOutput() {
		out.println("{\n");
		nestingLevel++;
	}

	public void printQuery(String query) {
		nestLine();
		out.println("\"queryString:\"\t:\t\""+escape(query)+"\"");
		comma = true;
	}

	public void endOutput() {
		// TODO Auto-generated method stub
		out.println("}");
		nestingLevel--;
	}

	public void startConcurrenceSubject(SnippetResult sr) {
		printSnippet(sr);

		if(comma){
			comma();
		}
		nestLine();
		out.println("\"similarTo\"\t:");
		
		nestLine();
		out.println("[");
		nestingLevel++;
		comma = false;
	}
	
	public void printNoConcurrenceSubject(){
		if(comma){
			comma();
		}
		nestLine();
		out.println("noSubject");
		
		comma = true;
	}
	
	protected void printSnippet(SnippetResult sr){
		if(comma){
			comma();
		}
		
		if(sr.getSubject()!=null){
			nestLine();
			out.println("\"id\"\t:\t\""+escape(sr.getSubject().toN3())+"\",");
		}
		
		if(sr.getAliases()!=null) for(Node alias:sr.getAliases()){
			nestLine();
			out.println("\"alias\"\t:\t\""+escape(alias.toN3())+"\",");
		}
		
		if(sr.getLabel()!=null){
			nestLine();
			out.println("\"label\"\t:\t\""+escape(sr.getLabel().toString())+"\",");
		}
		
		if(sr.getComment()!=null){
			nestLine();
			out.println("\"comment\"\t:\t\""+escape(sr.getComment().toString())+"\",");
		}
		
//		if(sr.getComments()!=null) for(String comment:sr.getComments()){
//			nestLine();
//			out.println("\"comment\"\t:\t\""+escape(comment)+"\",");
//		}
		
		if(sr.getImages()!=null) for(String img:sr.getImages()){
			nestLine();
			out.println("\"image\"\t:\t\""+escape(img)+"\",");
		}
		
		if(sr.getRank()!=0){
			nestLine();
			out.println("\"prank\"\t:\t\""+escape(Double.toString(sr.getRank()))+"\",");
		}
		
		if(sr.getScore()!=0){
			nestLine();
			out.println("\"kwscore\"\t:\t\""+escape(Double.toString(sr.getScore()))+"\"");
		}
		
		comma = true;
	}

	public void endConcurrenceSubject() {
		nestingLevel--;
		nestLine();
		out.println("]");
		
		comma = true;
	}

	public void printConcurrenceObject(ConcurrenceResult cr) {
		SnippetResult sr = cr.getSnippet();
		
		if(comma)
			comma();
		
		nestLine();
		out.println("{");
		nestingLevel++;
		comma = false;
		
		printSnippet(sr);
		
		if(cr.getTuples()!=null && !cr.getTuples().isEmpty()){
			nestLine();
			out.println("\"sharedPairs\"\t:");
			
			nestLine();
			out.println("[");
			nestingLevel++;
			
			comma = false;
			
			for(ConcurrenceTuple ct : cr.getTuples()){
				printConcurrenceTuple(ct);
			}
			
			nestingLevel--;
			nestLine();
			out.println("]");
		}
		
		nestingLevel--;
		nestLine();
		out.println("}");
		
		comma = true;
	}
	
	public void printNoConcurrenceObjects(){
		if(comma){
			comma();
		}
		nestLine();
		out.println("noSimilar");
		
		comma = true;
	}
	
	protected void printConcurrenceTuple(ConcurrenceTuple ct){
		if(comma)
			comma();
		
		nestLine();
		out.println("{");
		nestingLevel++;
		
		nestLine();
		out.println("\"predId\"\t:\t\""+escape(ct.getPredicate().toN3())+"\",");
		
		nestLine();
		out.println("\"predLabel\"\t:\t\""+escape(ct.getPredicateLabel().toString())+"\",");
		
		nestLine();
		out.println("\"valueId\"\t:\t\""+escape(ct.getValue().toN3())+"\",");
		
		nestLine();
		out.println("\"valueLabel\"\t:\t\""+escape(ct.getValueLabel().toString())+"\",");
		
		nestLine();
		out.println("\"n\"\t:\t\""+escape(Integer.toString(ct.getN()))+"\",");
		
		nestLine();
		out.println("\"direction\"\t:\t\""+escape(ct.getDirection().toString())+"\"");
		
		nestingLevel--;
		nestLine();
		out.println("}");
		
		comma = true;
	}

	public void printDisamibuationResults(Collection<SnippetResult> results) {
		if(comma)
			comma();
		
		nestLine();
		out.println("\"dismbiguation\"\t:");
		
		nestLine();
		out.println("[");
		nestingLevel++;
		
		boolean first = true;
		for(SnippetResult sr : results){
			if(!first){
				nestLine();
				out.println(",");
			}
			first = false;
			
			nestLine();
			out.println("{");
			nestingLevel++;
			comma = false;
			
			printSnippet(sr);
			nestingLevel--;
			nestLine();
			out.println("}");
		}
		
		nestingLevel--;
		nestLine();
		out.println("]");
		
		comma = true;
	}
	
	public void startFocusResults(Node subject, Node subLabel, Node subRank, Node subInternal) {
		if(comma){
			comma();
		}
		
		nestLine();
		out.println("\"sub\"\t:\t\""+escape(subject.toN3())+"\",");
		out.println("\"subLabel\"\t:\t\""+escape(subLabel.toString())+"\",");
		out.println("\"subRank\"\t:\t\""+escape(subRank.toString())+"\",");
		out.println("\"subInternal\"\t:\t\""+escape(subInternal.toString())+"\",");
		out.println("\"edge\"\t:");
		
		nestLine();
		out.println("[");
		nestingLevel++;

		comma = false;
	}
	
	public void printNoFocusResult(){
		if(comma){
			comma();
		}
		nestLine();
		out.println("\"noFocus\"");
		
		comma = true;
	}

	public void printFocusTuple(Node[] tuple) {
		if(comma)
			comma();
		
		nestLine();
		out.println("{");
		nestingLevel++;
		
		nestLine();
		out.println("\"pred\"\t:\t\""+escape(tuple[1].toN3())+"\",");
		nestLine();
		out.println("\"predLabel\"\t:\t\""+escape(tuple[7].toN3())+"\",");
		nestLine();
		out.println("\"predRank\"\t:\t\""+escape(tuple[8].toString())+"\",");
		nestLine();
		out.println("\"predInternal\"\t:\t\""+escape(tuple[9].toString())+"\",");
		nestLine();
		out.println("\"obj\"\t:\t\""+escape(tuple[2].toN3())+"\",");
		nestLine();
		out.println("\"objType\"\t:\t\""+type(tuple[2])+"\",");
		nestLine();
		out.println("\"objLabel\"\t:\t\""+escape(tuple[10].toN3())+"\",");
		nestLine();
		out.println("\"objRank\"\t:\t\""+escape(tuple[11].toString())+"\",");
		nestLine();
		out.println("\"objInternal\"\t:\t\""+escape(tuple[12].toString())+"\",");
		nestLine();
		out.println("\"context\"\t:\t\""+escape(tuple[3].toString())+"\"");
		
		nestingLevel--;
		nestLine();
		out.println("}");
		
		comma = true;
	}

	public void endFocusResults() {
		nestingLevel--;
		out.println("]");
		comma = false;
	}

	public String getOutputContentType() {
		return JSON_CONTENT_TYPE;
	}
	
	private void comma(){
		nestLine();
		out.println(',');
	}
	
	private void nestLine(){
		for(int i=0; i<nestingLevel; i++){
			out.print('\t');
		}
	}
	
	public static String type(Node n){
		if(n instanceof Literal)
			return "literal";
		else if(n instanceof Resource)
			return "uri";
		else return "bnode";
	}
	
	public static String escape(String in){
		StringBuffer result = new StringBuffer();

		for (int i = 0; i < in.length(); i++) {

			int cp = in.codePointAt(i);
			char c;

			if (!Character.isSupplementaryCodePoint(cp)) {
				c = (char) (cp);
				switch (c) {
				case '\\':
					result.append("\\\\");
					break;
				case '"':
					result.append("\\\"");
					break;
				case '/':
					result.append("\\/");
					break;
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
					if (c >= 0x0 && c <= 0x8 || c == 0xB || c == 0xC
							|| c >= 0xE && c <= 0x1F || c >= 0x7F
							&& c <= 0xFFFF) {
						result.append("\\u");
						result.append(toHexString(c, 4));
					} else {
						result.append(c);
					}
				}
			} else {
				result.append("\\U");
				result.append(toHexString(cp, 8));
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
	private static String toHexString(int decimal, int stringLength) {
		return String.format("%0" + stringLength + "X", decimal);
	}
}
