// $Id: ConflictLog.java,v 1.2 2001/06/06 21:44:18 ctl Exp $

import java.util.List;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Iterator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ConflictLog {

  static final char PATHSEP='/';

  static final int UPDATE = 1;
  static final int DELETE = 2;
  static final int INSERT = 3;
  static final int MOVE = 4;

  final String[] TYPETAGS = {null,"update","delete","insert","move"};
  private LinkedList path = null;
  private int childPos = -1;
  private LinkedList conflicts = new LinkedList();
  private LinkedList warnings = new LinkedList();

  public ConflictLog() {
    resetContext();
  }

  private class ConflictEntry {
    String text = null;
    BaseNode b=null;
    BranchNode b1=null,b2=null;
    String mergePath = null;
    int type = 0;
  }

  public void addListConflict( int type, String text, BaseNode b, BranchNode ba, BranchNode bb ) {
    add( true, false, type,text,b,ba,bb);
  }

  public void addListWarning( int type, String text, BaseNode b, BranchNode ba, BranchNode bb ) {
    add( true, true, type,text,b,ba,bb);
  }

  public void addNodeConflict( int type, String text, BaseNode b, BranchNode ba, BranchNode bb ) {
    add( false, false,type, text,b,ba,bb);
  }

  public void addNodeWarning( int type, String text, BaseNode b, BranchNode ba, BranchNode bb ) {
    add( false, true, type,text,b,ba,bb);
  }

  protected void add( boolean list, boolean warning, int type, String text, BaseNode b, BranchNode ba, BranchNode bb ) {
    ConflictEntry ce = new ConflictEntry();
    ce.text = text;
    ce.type = type;
    ce.b = b;
    // let b1=left and b2=right
    if( ba == null ) {
      ba=bb;
      bb=null;
    }
    if( ba != null && ba.isLeftTree() ) {
      ce.b1 = ba;
      ce.b2 = bb;
    } else {
      ce.b1 = bb;
      ce.b2 = ba;
    }
    if( list ) {
      ce.mergePath=getPathString(path);
    } else
      ce.mergePath=getPathString(path)+PATHSEP+childPos;
    if( warning )
      warnings.addLast(ce);
    else
      conflicts.addLast(ce);
  }

  public void writeConflicts( ContentHandler ch ) throws SAXException {
    AttributesImpl atts = new AttributesImpl();
    ch.startElement("","","conflictlist",atts);
    if( !conflicts.isEmpty() ) {
      atts = new AttributesImpl();
      ch.startElement("","","conflicts",atts);
      for( Iterator i = conflicts.iterator();i.hasNext();)
        outputConflict( (ConflictEntry) i.next(),ch );
      ch.endElement("","","conflicts");
    }
    if( !warnings.isEmpty() ) {
      atts = new AttributesImpl();
      ch.startElement("","","warnings",atts);
      for( Iterator i = warnings.iterator();i.hasNext();)
        outputConflict( (ConflictEntry) i.next(),ch );
      ch.endElement("","","warnings");
    }
    ch.endElement("","","conflictlist");
    if( !conflicts.isEmpty() )
      System.out.println( "MERGE FAILED: " + conflicts.size() + " conflicts.");
    if( !warnings.isEmpty() )
      System.out.println( "Warning: " + warnings.size() + " conflict warnings.");
  }

  protected void outputConflict( ConflictEntry ce, ContentHandler ch ) throws SAXException {
    AttributesImpl atts = new AttributesImpl();
    ch.startElement("","",TYPETAGS[ce.type],atts);
    ch.characters(ce.text.toCharArray(),0,ce.text.length());
    atts = new AttributesImpl();
    atts.addAttribute("","","tree","CDATA","merged");
    atts.addAttribute("","","path","CDATA",ce.mergePath);
    ch.startElement("","","node",atts);
    ch.endElement("","","node");

    if( ce.b!= null ) {
      atts = new AttributesImpl();
      atts.addAttribute("","","tree","CDATA","base");
      atts.addAttribute("","","path","CDATA",getPathString(makePath(ce.b)));
      ch.startElement("","","node",atts);
      ch.endElement("","","node");
    }
    if( ce.b1!= null ) {
      atts = new AttributesImpl();
      atts.addAttribute("","","tree","CDATA","branch1");
      atts.addAttribute("","","path","CDATA",getPathString(makePath(ce.b1)));
      ch.startElement("","","node",atts);
      ch.endElement("","","node");
    }
    if( ce.b2!= null ) {
      atts = new AttributesImpl();
      atts.addAttribute("","","tree","CDATA","branch2");
      atts.addAttribute("","","path","CDATA",getPathString(makePath(ce.b2)));
      ch.startElement("","","node",atts);
      ch.endElement("","","node");
    }
    ch.endElement("","",TYPETAGS[ce.type]);
  }

/*
  public void add( int type, BranchNode conflictingNode, String text ) {
    System.out.println("CONFLICT:"+text);
    System.out.println("path="+getPath());
  }

  public void addWarning( int type, BranchNode conflictingNode, String text ) {
    System.out.println("CONFLICTW:"+text);
    System.out.println("path="+getPath());
  }
*/
  private String getPathString( LinkedList path ) {
    StringBuffer p = new StringBuffer();
    for( Iterator i=path.iterator();i.hasNext();) {
      p.append(PATHSEP);
      p.append(((Integer) i.next()).toString());
    }
    return p.toString();
  }

  private LinkedList makePath( Node n ) {
    LinkedList path = new LinkedList();
    do {
      path.addLast(new Integer(n.getChildPos()));
    } while( (n = n.getParentAsNode()) != null);
    path.removeLast(); // We don't want the artificial root node in the path
    Collections.reverse(path);
    return path;
  }

  public void resetContext() {
    path = new LinkedList();
    childPos = 0;
  }

  public void nextChild() {
    childPos++;
  }

  public void enterSubtree() {
    path.addLast(new Integer(childPos));
    childPos = 0;
  }

  public void exitSubtree() {
    Integer oldpos = (Integer) path.removeLast();
    childPos = oldpos.intValue();
  }


}