// $Id: EditLog.java,v 1.3 2001/06/13 13:37:16 ctl Exp $

import java.util.Vector;
import java.util.Stack;

import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public class EditLog {

  static final int INSERT = 0;
  static final int UPDATE = 1;
  static final int COPY = 2;
  static final int MOVE = 3;
  static final int DELETE = 4;

  static final String[] OPTAGS = {"insert","update","copy","move","delete"};

  private class EditEntry {
    int type = -1;
    BaseNode baseSrc=null;
    BranchNode branchSrc = null;
    String dstPath = null;

    EditEntry( int aType, BaseNode aBaseNode, BranchNode aBranchNode, String aDstPath ) {
      type = aType;
      baseSrc = aBaseNode;
      branchSrc = aBranchNode;
      dstPath = aDstPath;
    }
  }

  private Stack checkPoints = new Stack();
  private Vector edits = new Vector();
  private PathTracker pt = null;
  public EditLog(PathTracker apt) {
    pt = apt;
  }

  public void insert( BranchNode n, int childPos ) {
    edits.add( new EditEntry(INSERT,null,n,pt.getPathString(childPos)));
//    System.out.println("INSERT; " + pt.getPathString(childPos));
//    System.out.println(n.getContent().toString());
  }

  public void move( BranchNode n, int childPos ) {
    edits.add( new EditEntry(MOVE,n.getBaseMatch(),n,pt.getPathString(childPos)));
//    System.out.println("MOVE: " + PathTracker.getPathString(n)+"->" + pt.getPathString(childPos));
  }

  public void copy( BranchNode n, int childPos ) {
    edits.add( new EditEntry(COPY,n.getBaseMatch(),n,pt.getPathString(childPos)));
//    System.out.println("COPY: " + PathTracker.getPathString(n)+"->" + pt.getPathString(childPos));
  }

  public void update( BranchNode n ) {
    edits.add( new EditEntry(UPDATE,n.getBaseMatch(),n,pt.getFullPathString()));

//    System.out.println("UPDATE: " + pt.getFullPathString());
//    System.out.println(n.getContent().toString());
  }

  public void delete( BaseNode n, BranchNode originatingList ) {
    edits.add( new EditEntry(DELETE,n,originatingList,null));
//    System.out.println("DELETE: " + PathTracker.getPathString(n));
  }

  public void writeEdits( ContentHandler ch ) throws SAXException {
    ch.startDocument();
    AttributesImpl atts = new AttributesImpl();
    ch.startElement("","","edits",atts);
    for(int i=0;i<edits.size();i++)
      outputEdit((EditEntry) edits.elementAt(i),ch);
    ch.endElement("","","edits");
    ch.endDocument();
  }

  protected void outputEdit( EditEntry ee, ContentHandler ch ) throws SAXException {
    AttributesImpl atts = new AttributesImpl();
    if( ee.type != DELETE )
      atts.addAttribute("","","path","CDATA",ee.dstPath);
    if( ee.type != INSERT )
      atts.addAttribute("","","src","CDATA",PathTracker.getPathString(ee.baseSrc));
    atts.addAttribute("","","originTree","CDATA",ee.branchSrc.isLeftTree() ? "branch1" : "branch2");
    atts.addAttribute("","",(ee.type != DELETE ) ? "originNode" : "originList",
      "CDATA",PathTracker.getPathString(ee.branchSrc));
    ch.startElement("","",OPTAGS[ee.type],atts);
    ch.endElement("","",OPTAGS[ee.type]);
  }


  public void checkPoint() {
    checkPoints.push(new  Integer( edits.size() ) );
  }
  public void rewind() {
    int firstFree = ((Integer) checkPoints.pop()).intValue();
    edits.setSize(firstFree);
  }
  public void commit() {
    checkPoints.pop();
  }

}