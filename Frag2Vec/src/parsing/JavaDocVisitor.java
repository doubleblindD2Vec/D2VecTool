package parsing;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;

import utils.FileIO;

public class JavaDocVisitor extends ASTVisitor {
	private String path, source;
	private StringBuilder doc = new StringBuilder();
	
	public JavaDocVisitor(String path, StringBuilder sb) {
		this.path = path;
		this.doc = sb;
	}
	
	@Override
	public boolean visit(MemberRef node) {
		doc.append(node.getName().getIdentifier());
		return false;
	}
	
	@Override
	public boolean visit(MethodRef node) {
		doc.append(node.getName().getIdentifier() + "()");
		return false;
	}
	
	@Override
	public boolean visit(QualifiedName node) {
		doc.append(node.getFullyQualifiedName());
		return false;
	}
	@Override
	public boolean visit(SimpleName node) {
		doc.append(node.getIdentifier());
		return false;
	}
	
	@Override
	public boolean visit(TagElement node) {
		String tag = null;
		if (node.getTagName() != null) {
			tag = node.getTagName();
			if (doc.length() > 0 && Character.isJavaIdentifierPart(doc.charAt(doc.length()-1)))
				doc.append(" ");
			if (tag.equals(ProjectCorpusParser.INHERIT_DOC_TAG)) {
				doc.append(ProjectCorpusParser.INHERIT_DOC_TAG);
				return false;
			}
		}
		for (int k = 0; k < node.fragments().size(); k++) {
			ASTNode e = (ASTNode) node.fragments().get(k);
			if (k == 0) {
				if (e instanceof TagElement && missFragment((TagElement) e))
					k = handleEmptyTagElement((TagElement) e, k, node, path, doc);
				else {
					if (node.getTagName() != null) {
						StringBuilder subsb = new StringBuilder();
						JavaDocVisitor subvisitor = new JavaDocVisitor(path, subsb);
						e.accept(subvisitor);
						while (subsb.length() > 0 && Character.isWhitespace(subsb.charAt(0)))
							subsb.deleteCharAt(0);
						doc.append(subsb.toString());
					} else
						e.accept(this);
				}
			} else {
				ASTNode pre = (ASTNode) node.fragments().get(k-1);
				if (pre.getStartPosition() + pre.getLength() < e.getStartPosition())
					doc.append(" ");
				if (e instanceof TextElement) {
					String text = ((TextElement) e).getText().trim();
					if (pre instanceof MemberRef && ((MemberRef) pre).getName().getIdentifier().equals(text))
						continue;
					if (pre instanceof MethodRef && ((MethodRef) pre).getName().getIdentifier().equals(text))
						continue;
				}
				if (e instanceof TagElement && ((TagElement) e).fragments().isEmpty())
					k = handleEmptyTagElement((TagElement) e, k, node, path, doc);
				else
					e.accept(this);
			}
		}
		return false;
	}
	
	private boolean missFragment(TagElement tag) {
		if (tag.fragments().isEmpty()) {
			if (source == null)
				source = FileIO.readStringFromFile(path);
			if (source.charAt(tag.getStartPosition()) == '{' && source.charAt(tag.getStartPosition() + tag.getLength() - 1) != '}')
				return true;
		}
		return false;
	}

	private int handleEmptyTagElement(TagElement node, int pos, TagElement parent, String path, StringBuilder doc) {
		int i = pos + 1;
		while (i < parent.fragments().size()) {
			if (!(parent.fragments().get(i) instanceof TextElement))
				break;
			if (parent.fragments().get(i-1) instanceof TextElement) {
				ASTNode pre = (ASTNode) parent.fragments().get(i-1), cur = (ASTNode) parent.fragments().get(i);
				if (cur.getStartPosition() - pre.getStartPosition() - pre.getLength() > 2)
					break;
			}
			i++;
		}
		i--;
		ASTNode start = (ASTNode) parent.fragments().get(pos+1), end = (ASTNode) parent.fragments().get(i);
		if (source == null)
			source = FileIO.readStringFromFile(path);
		String text = source.substring(start.getStartPosition(), end.getStartPosition() + end.getLength());
		int j = 0, opens = 1;
		while (j < text.length()) {
			char ch = text.charAt(j);
			if (ch == '{')
				opens++;
			else if (ch == '}')
				opens--;
			if (opens == 0)
				break;
			j++;
		}
		String tag = node.getTagName();
		if (tag != null) {
			if (doc.length() > 0 && Character.isJavaIdentifierPart(doc.charAt(doc.length()-1)))
				doc.append(" ");
			if (tag.equals(ProjectCorpusParser.INHERIT_DOC_TAG))
				doc.append(ProjectCorpusParser.INHERIT_DOC_TAG);
		}
		if (tag != null)
			doc.append(text.substring(0, j).trim());
		else
			doc.append(text.substring(0, j));
		if (j + 1 < text.length())
			doc.append(text.substring(j+1));
		return i;
	}

	@Override
	public boolean visit(TextElement node) {
		doc.append(node.getText());
		return false;
	}
}
