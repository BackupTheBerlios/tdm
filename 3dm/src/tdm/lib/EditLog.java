// $Id: EditLog.java,v 1.4 2001/09/05 13:21:25 ctl Exp $ D

import java.util.Vector;
import java.util.Stack;

import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/** Logs edit operations perfomed by the merge algorithm. */

public class EditLog {

  // Inyternal edit op codes
  private static final int INSERT = 0;
  private static final int UPDATE = 1;
  private static final int COPY = 2;
  private static final int MOVE = 3;
  private static final int DELETE = 4;

  // Edit tag names
  private static final String[] OPTAGS = {"insert","update","copy","move",
                                          "delete"};

  // Class for storing an edit operation in memory.
  private class EditEntry {
    int type = -1;
    BaseNode baseSrc=null;
    BranchNode branchSrc = null;
    String dstPath = null;

    EditEntry( int aType, BaseNode aBaseNode, BranchNode aBranchNode,
                String aDstPath ) {
      type = aType;
      baseSrc = aBaseNode;
      branchSrc = aBranchNode;
      dstPath = aDstPath;
    }
  }

  private Stack checkPoints = new Stack();
  // Edits in the log. A list of EditEntries.
  private Vector edits = new Vector();
  private PathTracker pt = null;

  /** Construct edit log. The PathTracker given as argument is queried for the
   *  current position in the merge treeeach time each time an edit operation is
   *  added.
   *  @param apt PathTracker that tracks the current position in the merged tree
   */
  public EditLog(PathTracker apt) {
    pt = apt;
  }

  /** Add insert operation.
   *  @param n Node that is inserted
   *  @param childPos position in the current child list of the merge tree */
  public void insert( BranchNode n, int childPos ) {
    edits.add( new EditEntry(INSERT,null,n,pt.getPathString(childPos)));
  }

  /** Add move operation.
   *  @param n Node that is moved
   *  @param childPos position in the current child list of the merge tree */
  public void move( BranchNode n, int childPos ) {
    edits.add( new EditEntry(MOVE,n.getBaseMatch(),n,
                                  pt.getPathString(childPos)));
  }

  /** Add copy operation.
   *  @param n Node that is copied
   *  @param childPos position in the current child list of the merge tree */
  public void copy( BranchNode n, int childPos ) {
    edits.add( new EditEntry(COPY,n.getBaseMatch(),n,
                                  pt.getPathString(childPos)));
  }

  /** Add move operation.
   *  @param n Node that is upated.
   *  @param childPos position in the current child list of the merge tree */
  public void update( BranchNode n ) {
    edits.add( new EditEntry(UPDATE,n.getBaseMatch(),n,pt.getFullPathString()));
  }

  /** Add delete operation.
   *  @param n Node that is deleted
   *  @param originatingList Node, whose child list originated the delete.
   */
  public void delete( BaseNode n, BranchNode originatingList ) {
    edits.add( new EditEntry(DELETE,n,originatingList,null));
  }

  /** Write out the edit log. */
  public void writeEdits( ContentHandler ch ) throws SAXException {
    ch.startDocument();
    AttributesImpl atts = new AttributesImpl();
    ch.startElement("","","edits",atts);
    for(int i=0;i<edits.size();i++)
      outputEdit((EditEntry) edits.elementAt(i),ch);
    ch.endElement("","","edits");
    ch.endDocument();
  }

  protected void outputEdit( EditEntry ee, ContentHandler ch )
                              throws SAXException {
    AttributesImpl atts = new AttributesImpl();
    if( ee.type != DELETE )
      atts.addAttribute("","","path","CDATA",ee.dstPath);
    if( ee.type != INSERT )
      atts.addAttribute("","","src","CDATA",PathTracker.getPathString(ee.baseSrc));
    atts.addAttribute("","","originTree","CDATA",ee.branchSrc.isLeftTree() ?
                      "branch1" : "branch2");
    atts.addAttribute("","",(ee.type != DELETE ) ? "originNode" : "originList",
      "CDATA",PathTracker.getPathString(ee.branchSrc));
    ch.startElement("","",OPTAGS[ee.type],atts);
    ch.endElement("","",OPTAGS[ee.type]);
  }

  /** Mark a checkpoint in the edit log. */
  public void checkPoint() {
    checkPoints.push(new  Integer( edits.size() ) );
  }

  /** Remove all edits added after the last checkpoint. */
  public void rewind() {
    int firstFree = ((Integer) checkPoints.pop()).intValue();
    edits.setSize(firstFree);
  }

  /** Commit edits made after the last checkpoint. */
  public void commit() {
    checkPoints.pop();
  }
}