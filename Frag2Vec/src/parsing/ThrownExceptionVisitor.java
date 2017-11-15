package parsing;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import utils.JavaASTUtil;

public class ThrownExceptionVisitor extends ASTVisitor {
	MethodDeclaration method;
	HashMap<String, String> thrownExceptions = new HashMap<>();
	
	public ThrownExceptionVisitor(MethodDeclaration method) {
		this.method = method;
	}

	@Override
	public boolean visit(ThrowStatement node) {
		if (node.getExpression() instanceof ClassInstanceCreation) {
			String condition = null;
			ASTNode p = node.getParent();
			if (p instanceof IfStatement)
				condition = ((IfStatement) p).getExpression().toString();
			else if (p instanceof Block) {
				if (p.getParent() == method && ((Block) p).statements().size() == 1)
					condition = "true";
				if (p.getParent() instanceof CatchClause)
					return false;
				if (p.getParent() instanceof IfStatement)
					condition = ((IfStatement) p.getParent()).getExpression().toString();
				else {
					List<?> l = ((Block) p).statements();
					int index = l.indexOf(node) - 1;
					while (index >= 0) {
						if (l.get(index) instanceof IfStatement && isTransferControl((IfStatement) l.get(index))) {
							if (condition == null)
								condition = "";
							else
								condition += " && ";
							condition += "!(" + ((IfStatement) (l.get(index))).getExpression().toString() + ")";
						}
						index--;
					}
				}
			} else if (p instanceof SwitchStatement) {
				SwitchStatement ss = (SwitchStatement) p;
				List<?> l = ss.statements();
				int index = l.indexOf(node) - 1;
				while (index > 0) {
					if (l.get(index) instanceof SwitchCase) {
						SwitchCase sc = (SwitchCase) l.get(index);
						if (sc.isDefault()) {
							for (int i = 0; i < ss.statements().size(); i++) {
								if (ss.statements().get(i) instanceof SwitchCase) {
									sc = (SwitchCase) ss.statements().get(i);
									if (sc.isDefault())
										break;
									if (condition == null)
										condition = ss.getExpression().toString() + " != " + sc.getExpression().toString();
									else
										condition += " && " + ss.getExpression().toString() + " != " + sc.getExpression().toString();
								}
							}
						} else {
							condition = ss.getExpression().toString() + " == " + sc.getExpression().toString();
						}
						break;
					}
					index--;
				}
			}
			String type = JavaASTUtil.getSimpleType(((ClassInstanceCreation) node.getExpression()).getType());
			if (condition == null)
				condition = "true";
			condition = JavaASTUtil.tokenize(condition);
			String fullCondition = thrownExceptions.get(type);
			if (fullCondition == null)
				fullCondition = condition;
			else
				fullCondition += " || " + condition;
			thrownExceptions.put(type, fullCondition);
		}
		return false;
	}

	private boolean isTransferControl(IfStatement is) {
		Statement s = null;
		if (is.getThenStatement() instanceof Block) {
			List<?> l = ((Block) is.getThenStatement()).statements();
			if (l.size() > 0)
				s = (Statement) l.get(l.size() - 1);
		} else
			s = is.getThenStatement();
		return isTransferStatement(s);
	}

	private boolean isTransferStatement(Statement s) {
		if (s == null)
			return false;
		if (s instanceof BreakStatement)
			return true;
		if (s instanceof ContinueStatement)
			return true;
		if (s instanceof ReturnStatement)
			return true;
		if (s instanceof ExpressionStatement) {
			ExpressionStatement es = (ExpressionStatement) s;
			if (es.getExpression() instanceof MethodInvocation) {
				MethodInvocation m = (MethodInvocation) es.getExpression();
				if (m.getName().getIdentifier().equals("exit") && m.arguments().size() == 1 && m.getExpression() != null && m.getExpression().toString().equals("System"))
					return true;
			}
		}
		return false;
	}
}
