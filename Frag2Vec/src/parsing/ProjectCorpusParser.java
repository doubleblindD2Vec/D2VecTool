package parsing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import utils.FileIO;
import utils.JavaASTUtil;

public class ProjectCorpusParser {
	private static final boolean PARSE_INDIVIDUAL_SRC = false, SCAN_FILES_FRIST = false;
	static final String INHERIT_DOC_TAG = "@inheritDoc";
	private static final String PARAM_TAG = "@param", RETURNS_TAG = "@return";
	
	private String inPath;
	private PrintStream stLog;
	private boolean testing = false;
	private HashSet<String> badFiles = new HashSet<>();
	private HashMap<String, String> methodDescription = new HashMap<>(), typeDescription = new HashMap<>();
	private HashMap<String, String> locations = new HashMap<>();
	private HashMap<String, String> overriddenMethods = new HashMap<>();
	
	public ProjectCorpusParser(String inPath) {
		this.inPath = inPath;
	}
	
	public ProjectCorpusParser(String inPath, boolean testing) {
		this(inPath);
		this.testing = testing;
	}

	public int generateParallelCorpus(String outPath, boolean recursive) {
		ArrayList<String> rootPaths = getRootPaths();
		
		new File(outPath).mkdirs();
		try {
			stLog = new PrintStream(new FileOutputStream(outPath + "/log.txt"));
		} catch (FileNotFoundException e) {
			if (testing)
				System.err.println(e.getMessage());
			return 0;
		}
		int numOfSequences = 0;
		for (String rootPath : rootPaths) {
			String[] sourcePaths = getSourcePaths(rootPath, new String[]{".java"}, recursive);
			@SuppressWarnings("rawtypes")
			Map options = JavaCore.getOptions();
			options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setCompilerOptions(options);
			parser.setEnvironment(null, new String[]{}, new String[]{}, true);
			parser.setResolveBindings(true);
			parser.setBindingsRecovery(false);
			
			StatTypeFileASTRequestor r = new StatTypeFileASTRequestor();
			parser.createASTs(sourcePaths, null, new String[0], r, null);
			numOfSequences += r.numOfSequences;
		}
		inlineInheritedDocs();
		HashMap<String, String> descriptions = new HashMap<>(typeDescription);
		descriptions.putAll(methodDescription);
		ArrayList<String> list = new ArrayList<>(descriptions.keySet());
		Collections.sort(list);
		StringBuilder sbDocs = new StringBuilder(), sbLocations = new StringBuilder();
		for (String method : list) {
			String description = descriptions.get(method);
			sbDocs.append(method + ": " + description + "\n");
			sbLocations.append(locations.get(method) + "\n");
		}
		FileIO.writeStringToFile(sbDocs.toString(), outPath + "/docs.txt");
		FileIO.writeStringToFile(sbLocations.toString(), outPath + "/locations.txt");
		return numOfSequences;
	}
	
	private void inlineInheritedDocs() {
		HashSet<String> doneMethods = new HashSet<>();
		for (String method : new HashSet<String>(methodDescription.keySet())) {
			if (!doneMethods.contains(method))
				inlineInheritedDocs(method, doneMethods);
		}
	}

	private void inlineInheritedDocs(String method, HashSet<String> doneMethods) {
		if (doneMethods.contains(method))
			return;
		String description = methodDescription.get(method);
		if (description == null)
			return;
		int index = description.indexOf(INHERIT_DOC_TAG);
		if (index > -1) {
			String d = "";
			String overriddenMethod = overriddenMethods.get(method);
			if (overriddenMethod != null) {
				inlineInheritedDocs(overriddenMethod, doneMethods);
				d = methodDescription.get(overriddenMethod);
				if (d == null || d.equals(INHERIT_DOC_TAG))
					d = "";
			}
			description = description.replace(INHERIT_DOC_TAG, d);
			methodDescription.put(method, description);
		}
		doneMethods.add(method);
	}

	private class StatTypeFileASTRequestor extends FileASTRequestor {
		int numOfSequences = 0;
		
		@Override
		public void acceptAST(String sourceFilePath, CompilationUnit ast) {
			if (ast.getPackage() == null)
				return;
			if (testing)
				System.out.println(sourceFilePath);
			stLog.println(sourceFilePath);
			for (int i = 0; i < ast.types().size(); i++) {
				if (ast.types().get(i) instanceof TypeDeclaration) {
					TypeDeclaration td = (TypeDeclaration) ast.types().get(i);
					numOfSequences += generateSequence(td, sourceFilePath, ast.getPackage().getName().getFullyQualifiedName(), "");
				}
			}
		}
	}

	private int generateSequence(TypeDeclaration td, String path, String packageName, String outer) {
		String name = outer.isEmpty() ? td.getName().getIdentifier() : outer + "." + td.getName().getIdentifier();
		Javadoc doc = td.getJavadoc();
		if (doc != null) {
			String description = null;
			if (doc.tags().size() > 0) {
				TagElement tagElement = (TagElement) doc.tags().get(0);
				if (tagElement.getTagName() == null) {
					StringBuilder sb = new StringBuilder();
					JavaDocVisitor jdv = new JavaDocVisitor(path, sb);
					tagElement.accept(jdv);
					description = DocumentationParser.normalize(sb.toString());
				}
			}
			if (description != null && !description.isEmpty()) {
				typeDescription.put(packageName + "." + name, description);
				locations.put(packageName + "." + name, path + "\t" + packageName + "\t" + name);
			}
		}
		int numOfSequences = 0;
		for (MethodDeclaration method : td.getMethods()) {
			String methodShortName = method.getName().getIdentifier() + "\t" + getParameters(method);
			String methodName = packageName + "." + name + "." + methodShortName;
			ITypeBinding tb = td.resolveBinding();
			if (tb != null)
				buildMethodHierarchy(methodName, methodShortName, tb, method);
			
			stLog.println(path + "\t" + name + "\t" + method.getName().getIdentifier() + "\t" + getParameters(method));
			doc = method.getJavadoc();
			if (doc == null)
				continue;
			String description = null;
			if (doc.tags().size() > 0) {
				TagElement tagElement = (TagElement) doc.tags().get(0);
				if (tagElement.getTagName() == null) {
					StringBuilder sb = new StringBuilder();
					JavaDocVisitor jdv = new JavaDocVisitor(path, sb);
					tagElement.accept(jdv);
					description = DocumentationParser.normalize(sb.toString());
				}
			}
			if (description == null)
				description = INHERIT_DOC_TAG;
//			HashMap<String, String> docExceptionCondition = new HashMap<>();
//			for (int i = 0; doc != null && i < doc.tags().size(); i++) {
//				TagElement tagElement = (TagElement) doc.tags().get(i);
//				if (EXCEPTION_TAGS.contains(tagElement.getTagName())) {
//					if (!tagElement.fragments().isEmpty() && tagElement.fragments().get(0) instanceof SimpleName) {
//						String exceptionName = ((SimpleName) tagElement.fragments().get(0)).getIdentifier();
//						if (exceptionName.endsWith("Exception")) {
//							boolean isInheritDoc = false;
//							final StringBuilder sb = new StringBuilder();
//							for (int j = 1; j < tagElement.fragments().size(); j++) {
//								ASTNode fragment = (ASTNode) tagElement.fragments().get(j);
//								if (fragment instanceof TagElement) {
//									TagElement tag = (TagElement) fragment;
//									if (tag.getTagName() != null && tag.getTagName().equals(INHERIT_DOC_TAG)) {
//										isInheritDoc = true;
//										break;
//									}
//								}
//								ASTNode pre = (ASTNode) tagElement.fragments().get(j-1);
//								if (pre.getStartPosition() + pre.getLength() < fragment.getStartPosition())
//									sb.append(" ");
//								if (fragment instanceof TagElement && ((TagElement) fragment).fragments().isEmpty())
//									j = ExceptionDocVisitor.handleEmptyTagElement((TagElement) fragment, j, tagElement, path, sb);
//								else {
//									ExceptionDocVisitor edv = new ExceptionDocVisitor(path, sb);
//									fragment.accept(edv);
//								}
//							}
//							if (isInheritDoc) {
//								String condition = docExceptionCondition.get(exceptionName);
//								if (condition == null)
//									condition = INHERIT_DOC_TAG;
//								else 
//									condition += " or " + INHERIT_DOC_TAG;
//								docExceptionCondition.put(exceptionName, condition);
//							} else {
//								System.out.println(sb.toString());
//								String flattenedSequence = sb.toString();
//								if (!flattenedSequence.isEmpty()) {
//									String condition = docExceptionCondition.get(exceptionName);
//									if (condition == null)
//										condition = flattenedSequence;
//									else 
//										condition += " or " + flattenedSequence;
//									docExceptionCondition.put(exceptionName, condition);
//								}
//							}
//						}
//					}
//				}
//			}
			methodDescription.put(methodName, description);
			locations.put(methodName, path + "\t" + packageName + "\t" + name + "\t" + method.getName().getIdentifier() + "\t" + getParameters(method));
		}
		for (TypeDeclaration inner : td.getTypes())
			numOfSequences += generateSequence(inner, path, packageName, name);
		return numOfSequences;
	}

	private void buildMethodHierarchy(String methodName, String methodShortName, ITypeBinding tb, MethodDeclaration method) {
		if (tb.getSuperclass() != null) {
			ITypeBinding stb = tb.getSuperclass().getTypeDeclaration();
			for (IMethodBinding mb : stb.getDeclaredMethods()) {
				if (method.resolveBinding() != null && method.resolveBinding().overrides(mb)) {
					String name = mb.getDeclaringClass().getQualifiedName() + "." + methodShortName;
					overriddenMethods.put(methodName, name);
					return;
				}
			}
			buildMethodHierarchy(methodName, methodShortName, stb, method);
			if (this.overriddenMethods.containsKey(methodName))
				return;
		}
		for (ITypeBinding itb : tb.getInterfaces()) {
			for (IMethodBinding mb : itb.getTypeDeclaration().getDeclaredMethods()) {
				if (method.resolveBinding() != null && method.resolveBinding().overrides(mb)) {
					String name = mb.getDeclaringClass().getQualifiedName() + "." + methodShortName;
					overriddenMethods.put(methodName, name);
					return;
				}
			}
			buildMethodHierarchy(methodName, methodShortName, itb.getTypeDeclaration(), method);
			if (this.overriddenMethods.containsKey(methodName))
				return;
		}
	}
	
	private String getParameters(MethodDeclaration method) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i = 0; i < method.parameters().size(); i++) {
			SingleVariableDeclaration d = (SingleVariableDeclaration) (method.parameters().get(i));
			String type = JavaASTUtil.getSimpleType(d.getType());
			sb.append("\t" + type);
		}
		sb.append("\t)");
		return sb.toString();
	}

	private String[] getSourcePaths(String path, String[] extensions, boolean recursive) {
		HashSet<String> exts = new HashSet<>();
		for (String e : extensions)
			exts.add(e);
		HashSet<String> paths = new HashSet<>();
		getSourcePaths(new File(path), paths, exts, recursive);
		paths.removeAll(badFiles);
		return (String[]) paths.toArray(new String[0]);
	}

	private void getSourcePaths(File file, HashSet<String> paths, HashSet<String> exts, boolean recursive) {
		if (file.isDirectory()) {
			if (paths.isEmpty() || recursive)
				for (File sub : file.listFiles())
					getSourcePaths(sub, paths, exts, recursive);
		} else if (exts.contains(getExtension(file.getName())))
			paths.add(file.getAbsolutePath());
	}

	private Object getExtension(String name) {
		int index = name.lastIndexOf('.');
		if (index < 0)
			index = 0;
		return name.substring(index);
	}

	private ArrayList<String> getRootPaths() {
		ArrayList<String> rootPaths = new ArrayList<>();
		if (PARSE_INDIVIDUAL_SRC)
			getRootPaths(new File(inPath), rootPaths);
		else {
			if (SCAN_FILES_FRIST)
				getRootPaths(new File(inPath), rootPaths);
			rootPaths = new ArrayList<>();
			rootPaths.add(inPath);
		}
		return rootPaths;
	}

	private void getRootPaths(File file, ArrayList<String> rootPaths) {
		if (file.isDirectory()) {
			System.out.println(rootPaths);
			for (File sub : file.listFiles())
				getRootPaths(sub, rootPaths);
		} else if (file.getName().endsWith(".java")) {
			@SuppressWarnings("rawtypes")
			Map options = JavaCore.getOptions();
			options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setCompilerOptions(options);
			parser.setSource(FileIO.readStringFromFile(file.getAbsolutePath()).toCharArray());
			try {
				CompilationUnit ast = (CompilationUnit) parser.createAST(null);
				if (ast.getPackage() != null && !ast.types().isEmpty() && ast.types().get(0) instanceof TypeDeclaration) {
					String name = ast.getPackage().getName().getFullyQualifiedName();
					name = name.replace('.', '\\');
					String p = file.getParentFile().getAbsolutePath();
					if (p.endsWith(name))
						add(p.substring(0, p.length() - name.length() - 1), rootPaths);
				} /*else 
					badFiles.add(file.getAbsolutePath());*/
			} catch (Throwable t) {
				badFiles.add(file.getAbsolutePath());
			}
		}
	}

	private void add(String path, ArrayList<String> rootPaths) {
		int index = Collections.binarySearch(rootPaths, path);
		if (index < 0) {
			index = - index - 1;
			int i = rootPaths.size() - 1;
			while (i > index) {
				if (rootPaths.get(i).startsWith(path))
					rootPaths.remove(i);
				i--;
			}
			i = index - 1;
			while (i >= 0) {
				if (path.startsWith(rootPaths.get(i)))
					return;
				i--;
			}
			rootPaths.add(index, path);
		}
	}

}
