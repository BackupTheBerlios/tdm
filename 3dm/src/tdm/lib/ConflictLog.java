// $Id: ConflictLog.java,v 1.6 2001/09/05 13:21:25 ctl Exp $ D

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/** Class for logging conflicts and conflict warnings.
 */
public class ConflictLog {

  // Types of conflicts (used in add... functions)
  public static final int UPDATE = 1;
  public static final int DELETE = 2;
  public static final int INSERT = 3;
  public static final int MOVE = 4;

  private final String[] TYPETAGS = {null,"update","delete","insert","move"};
  private LinkedList conflicts = new LinkedList();
  private LinkedList warnings = new LinkedList();
  private PathTracker pt = null;

  public ConflictLog(PathTracker apt) {
    pt=apt;
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

  public void addListWarning( int type, String text, BaseNode b, BranchNode ba,
                              BranchNode bb ) {
    add( true, true, type,text,b,ba,bb);
  }

  public void addNodeConflict( int type, String text, BaseNode b, BranchNode ba,
                               BranchNode bb ) {
    add( false, false,type, text,b,ba,bb);
  }

  public void addNodeWarning( int type, String text, BaseNode b, BranchNode ba,
                              BranchNode bb ) {
    add( false, true, type,text,b,ba,bb);
  }

  protected void add( boolean list, boolean warning, int type, String text,
                      BaseNode b, BranchNode ba, BranchNode bb ) {
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
      ce.mergePath=pt.getPathString();
    } else
      ce.mergePath=pt.getFullPathString();
    if( warning )
      warnings.addLast(ce);
    else
      conflicts.addLast(ce);
  }

  /** Output conflict list as XML.
   * @param ch Content handler to output the conflict to.
   */
  public void writeConflicts( ContentHandler ch ) throws SAXException {
    AttributesImpl atts = new AttributesImpl();
    ch.startDocument();
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
      System.out.println( "Warning: " + warnings.size() +" conflict warnings.");
    ch.endDocument();
  }

  protected void outputConflict( ConflictEntry ce, ContentHandler ch )
    throws SAXException {
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
      atts.addAttribute("","","path","CDATA",PathTracker.getPathString(ce.b));
      ch.startElement("","","node",atts);
      ch.endElement("","","node");
    }
    if( ce.b1!= null ) {
      atts = new AttributesImpl();
      atts.addAttribute("","","tree","CDATA","branch1");
      atts.addAttribute("","","path","CDATA",PathTracker.getPathString(ce.b1));
      ch.startElement("","","node",atts);
      ch.endElement("","","node");
    }
    if( ce.b2!= null ) {
      atts = new AttributesImpl();
      atts.addAttribute("","","tree","CDATA","branch2");
      atts.addAttribute("","","path","CDATA",PathTracker.getPathString(ce.b2));
      ch.startElement("","","node",atts);
      ch.endElement("","","node");
    }
    ch.endElement("","",TYPETAGS[ce.type]);
  }
}