# 3DM makefile
# $Id: Makefile,v 1.3 2002/06/01 19:41:18 ctl Exp $

GPL_FILE = COPYING
DISTDIR = doc
SRCDIR = src
CLASSDIR = build
JAVAC = javac
JAR = jar
CLASSPATH=lib/java-getopt-1.0.8.jar\;lib/xerces.jar\;lib/sax2.jar\;$(CLASSDIR)

# 3DM source files
FILES= BaseNode MatchedNodes Patch XMLElementNode BranchNode Matching PathTracker XMLNode \
ConflictLog Measure XMLParser Diff Merge TreeDiffMerge XMLPrinter \
DiffMatching Node XMLTextNode EditLog NodeFactory MatchArea ParseException TriMatching

CLASSES = $(FILES)

$(FILES): 
	$(JAVAC) -classpath $(CLASSPATH) -sourcepath $(SRCDIR) -d $(CLASSDIR) \
	 $(SRCDIR)/$@.java 


3dm:	$(CLASSES)

3dm.jar: 
	rm -f 3dm.jar
	$(JAR) cvf 3dm.jar -C $(CLASSDIR) .

clean:
	rm -f $(CLASSDIR)/*.class

dist:	3dm.jar
	rm $(DISTDIR)/3dm_src.tar.gz
	rm $(DISTDIR)/3dm_src.zip
	cp 3dm.jar $(DISTDIR)
	tar -cf $(DISTDIR)/3dm_src.tar $(addprefix $(SRCDIR),$(addsuffix .java, $(FILES))) $(GPL_FILE)
	gzip $(DISTDIR)/3dm_src.tar	
	zip $(DISTDIR)/3dm_src.zip $(addprefix $(SRCDIR),$(addsuffix .java, $(FILES))) $(GPL_FILE)
