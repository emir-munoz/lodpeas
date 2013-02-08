package org.deri.conker.app.lookup.results;

import java.util.HashSet;

import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;

public class SnippetResult {
	private Node subject = null;
	private Literal comment = null;
	private Literal label = null;
	private HashSet<Node> aliases = null;

	private HashSet<String> images = null;
	private HashSet<String> comments = null;
	private double rank = 0d;
	private double score = 0d;
	
	
	
	public SnippetResult(Node subject){
		this.subject = subject;
	}
	
	public Node getSubject() {
		return subject;
	}

	public Literal getComment() {
		return comment;
	}

	public void setComment(Literal comment) {
		this.comment = comment;
	}

	public Node getLabel() {
		return label;
	}

	public void setLabel(Literal label) {
		this.label = label;
	}

	public double getRank() {
		return rank;
	}

	public void setRank(double rank) {
		this.rank = rank;
	}
	
	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public HashSet<String> getImages() {
		return images;
	}

	public boolean addImage(String image) {
		if(images==null)
			images = new HashSet<String>();
		return images.add(image);
	}
	
	public HashSet<String> getComments() {
		return comments;
	}

	public boolean addComment(String comment) {
		if(comments==null)
			comments = new HashSet<String>();
		return comments.add(comment);
	}

	public HashSet<Node> getAliases() {
		return aliases;
	}

	public boolean addAlias(Node alias) {
		if(aliases==null)
			aliases = new HashSet<Node>();
		return aliases.add(alias);
	}
}
