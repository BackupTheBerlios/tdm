// $Id: Patch.java,v 1.5 2001/09/05 21:22:29 ctl Exp $ D

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.LinkedList;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;

/** Patching algorithm.
 *  See the {@link patch(BaseNode, BranchNode, org.xml.sax.ContentHandler)
 *  patch} method for usage.
 */

public class Patch {

/// /***COMMON TO DIFF--consider moving!*/
  private static final Set RESERVED;
  private static final String DIFF_NS ="diff:";
  static {
      RESERVED = new HashSet();
      RESERVED.add(DIFF_NS+"copy");
      RESERVED.add(DIFF_NS+"insert");
      RESERVED.add(DIFF_NS+"esc");
  }
/// //ENDCOMMON

  public Patch() {
  }

  /** Patch a tree.
   *  @param base Tree to patch
   *  @param diff Parse tree of XML diff file, as produced by the Diff class
   *  @param ch Content handler that the patched tree is output to
   */

  public void patch( BaseNode base, BranchNode diff, ContentHandler ch )
    throws ParseException, SAXException {
    BranchNode patched = patch( base, diff );
    ch.startDocument();
    dumpTree( patched.getChild(0), ch );
    ch.endDocument();
  }

  private void dumpTree( BranchNode n, ContentHandler ch ) throws SAXException {
    if( n.getContent() instanceof XMLTextNode ) {
      char [] text = ((XMLTextNode) n.getContent()).getText();
      ch.characters(text,0,text.length);
    } else {
      XMLElementNode en = (XMLElementNode) n.getContent();
      ch.startElement("","",en.getQName(),en.getAttributes());
      for( int i=0;i<n.getChildCount();i++)
        dumpTree(n.getChild(i),ch );
      ch.endElement("","",en.getQName());
    }
  }

  protected BranchNode patch( BaseNode base, BranchNode diff ) throws
          ParseException  {
    initLookup(base,nodeLookup);
    BranchNode patch = new BranchNode( new XMLElementNode("$ROOT$",
      new org.xml.sax.helpers.AttributesImpl() ) );
      // Getchild(0) to skip diff tag
    copy( patch,diff.getChild(0),base.getChild(0));
    return patch;
  }

  // diff => a command or just some nodes to insert
  // patch = the parent node, under which the subtree produced by the command
  // shall be inserted

  protected void insert( BranchNode patch, BranchNode diff) throws
    ParseException {
    XMLNode cmdcontent = diff.getContent();
    if( cmdcontent instanceof XMLTextNode ||
      !RESERVED.contains(((XMLElementNode) cmdcontent).getQName())) {
      // Simple insert operation
      BranchNode node = new BranchNode( cmdcontent );
      patch.addChild(node);
      for( int i=0;i<diff.getChildCount();i++)
        insert(node,diff.getChild(i)); // Recurse to next level
    } else {
      // Other ops..
      XMLElementNode ce = (XMLElementNode) cmdcontent;
      if( ce.getQName().equals(DIFF_NS+"esc") ||
          ce.getQName().equals(DIFF_NS+"insert") ) {
        if( diff.getChildCount() == 0)
            throw new ParseException("DIFFSYNTAX: insert/esc has no subtree " +
                                      ce.toString() );
        insert( patch, diff.getChild(0) );
      } else {
        // Copy operation
        BaseNode srcRoot = null;
        try {
          srcRoot = locateNode(
                        Integer.parseInt(ce.getAttributes().getValue("src"))) ;
        } catch ( Exception e ) {
            throw new ParseException(
              "DIFFSYNTAX: Invalid parameters in command " + ce.toString() );
        }
        copy( patch, diff, srcRoot );
      } // Copyop
    }
  }

  protected void copy( BranchNode patch, BranchNode diff, BaseNode srcRoot )
                      throws ParseException {
    // Gather the stopnodes for the copy
    Vector dstNodes = new Vector();
    Map stopNodes = new HashMap();
    for( int i = 0; i < diff.getChildCount(); i++ ) {
      XMLNode content = diff.getChild(i).getContent();
      Node stopNode = null;
      // Sanity check
      if( content instanceof XMLTextNode || (
        !((XMLElementNode) content).getQName().equals(DIFF_NS+"copy") &&
        !((XMLElementNode) content).getQName().equals(DIFF_NS+"insert") ) )
        throw new ParseException("DIFFSYNTAX: Only copy or insert commands may"+
                                 " appear below a copy command");
      XMLElementNode stopCommand = (XMLElementNode) content;
      try {
        // PATCH here when implementing RUN attribute...
        stopNode = locateNode( Integer.parseInt(
                                stopCommand.getAttributes().getValue("dst")));
      } catch ( Exception e ) {
        throw new ParseException("DIFFSYNTAX: Invalid parameters in command " +
                                 stopNode.toString() );
      }
      dstNodes.add( stopNode );
      if( !stopNodes.containsKey(stopNode) )
        stopNodes.put(stopNode,null);
    }
    // Run copy. stopNode values are at the same time filled in to point to the
    // created nodes
    dfsCopy( patch, srcRoot , stopNodes );
    // Recurse for each diff child
    for( int i = 0; i < diff.getChildCount(); i++ ) {
      insert( (BranchNode) stopNodes.get( dstNodes.elementAt(i) ),
              diff.getChild(i));
    }
  }

  protected void dfsCopy( BranchNode dst, BaseNode src, Map stopNodes ) {
    BranchNode copied = new BranchNode( src.getContent() );
    dst.addChild( copied );
    if( stopNodes.containsKey(src) ) {
      stopNodes.put(src,copied);
      return; // We're done in this branch
    }
    for( int i =0;i<src.getChildCount();i++)
      dfsCopy( copied, src.getChild(i), stopNodes );
  }

  private Vector nodeLookup = new Vector();

  protected BaseNode locateNode( int ix ) {
    return (BaseNode) nodeLookup.elementAt(ix);
  }

  // BFS Enumeration of nodes. Useful beacuse adjacent nodes have subsequent ids =>
  // diff can use the "run" attribute more often
  // NOTE: Copied from diff, should really be moved to a common base class for
  // patch & diff
  protected void initLookup( Node start, Vector table) {
    LinkedList queue = new LinkedList();
    queue.add(start);
    while( !queue.isEmpty() ) {
      Node n = (Node) queue.removeFirst();
      table.add(n);
      for( int i=0;i<n.getChildCount();i++)
        queue.add(n.getChildAsNode(i));
    }
  }
}