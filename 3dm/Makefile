# 3DM makefile
# $Id: Makefile,v 1.1 2001/09/26 19:36:43 ctl Exp $

SRCDIR = src/
CLASSDIR = build
JAVAC = javac
JAR = jar
CLASSPATH=../../../ubidoc-support/lib/java-getopt-1.0.8.jar\;../../../ubidoc-support/lib/xerces.jar\;../../../ubidoc-support/lib/sax2.jar\;$(CLASSDIR)

# 3DM source files
FILES= BaseNode MatchedNodes Patch XMLElementNode BranchNode Matching PathTracker XMLNode \
ConflictLog Measure XMLParser Diff Merge TreeDiffMerge XMLPrinter \
DiffMatching Node XMLTextNode EditLog NodeFactory MatchArea ParseException TriMatching

CLASSES = $(FILES)

$(FILES): 
	$(JAVAC) -classpath $(CLASSPATH) -sourcepath $(SRCDIR) -d $(CLASSDIR) \
	 $(SRCDIR)/$@.java 


3dm:	$(CLASSES)

jar:	
	$(JAR) cvf 3dm.jar -C $(CLASSDIR) .

clean:
	rm -f $(CLASSDIR)/*.class
