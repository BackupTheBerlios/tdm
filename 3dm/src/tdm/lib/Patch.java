// $Id: Patch.java,v 1.1 2001/04/27 16:59:11 ctl Exp $

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.LinkedList;

public class Patch {

/***COMMON TO DIFF--consider movinG!*/
  private static final Set RESERVED;
  static final String DIFF_NS ="diff:";
  static {
      RESERVED = new HashSet();
      RESERVED.add(DIFF_NS+"copy");
      RESERVED.add(DIFF_NS+"insert");
      RESERVED.add(DIFF_NS+"esc");
  }
///////ENDCOMMON

  public Patch() {
  }

  public BranchNode patch( BaseNode base, BranchNode diff ) throws ParseException  {
    initLookup(base,nodeLookup);
    BranchNode patch = new BranchNode( new XMLElementNode("$ROOT$",
      new org.xml.sax.helpers.AttributesImpl() ) );
    runCopy( patch,diff.getChild(0),base.getChild(0)); // Getchild0 to skip diff tag
    return patch;
  }

  // diff => a command or just some nodes to insert
  // patch = the parent node, under which the subtree produced by the command shall be inserted

  protected void patch( BranchNode patch, BranchNode diff) throws ParseException {
    XMLNode cmdcontent = diff.getContent();
    if( cmdcontent instanceof XMLTextNode || !RESERVED.contains(((XMLElementNode) cmdcontent).getQName())) {
      // Simple insert operation
      BranchNode node = new BranchNode( cmdcontent );
      patch.addChild(node);
      for( int i=0;i<diff.getChildCount();i++)
        patch(node,diff.getChild(i)); // Recurse to nect level
    } else {
      // Other ops..
      XMLElementNode ce = (XMLElementNode) cmdcontent;
      if( ce.getQName().equals(DIFF_NS+"esc") || ce.getQName().equals(DIFF_NS+"insert") ) {
        if( diff.getChildCount() == 0)
            throw new ParseException("DIFFSYNTAX: insert/esc has no subtree " + ce.toString() );
        patch( patch, diff.getChild(0) );
      } else {
        // Copy operation
        BaseNode srcRoot = null;
        try {
          srcRoot = locateNode( Integer.parseInt(ce.getAttributes().getValue("src"))) ;
        } catch ( Exception e ) {
            throw new ParseException("DIFFSYNTAX: Invalid parameters in command " + ce.toString() );
        }
        runCopy( patch, diff, srcRoot );
      } // Copyop
    }
  }

  protected void runCopy( BranchNode patch, BranchNode diff, BaseNode srcRoot ) throws ParseException {
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
        throw new ParseException("DIFFSYNTAX: Only copy or insert commands may appear below a copy command");
      XMLElementNode stopCommand = (XMLElementNode) content;
      try {
        // PATCH here for RUN attribute
        stopNode = locateNode( Integer.parseInt(stopCommand.getAttributes().getValue("dst"))) ;
      } catch ( Exception e ) {
        throw new ParseException("DIFFSYNTAX: Invalid parameters in command " + stopNode.toString() );
      }
      dstNodes.add( stopNode );
      if( !stopNodes.containsKey(stopNode) )
        stopNodes.put(stopNode,null);
    }
    // Run copy. stopNode values are at the same time filled in to point to the created nodes
    dfsCopy( patch, srcRoot , stopNodes );
    // Recurse for each diff child
    for( int i = 0; i < diff.getChildCount(); i++ ) {
      patch( (BranchNode) stopNodes.get( dstNodes.elementAt(i) ), diff.getChild(i));
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

/*******COPIED CODE, MOVE TO COMMON*/
  // BFS Enumeration of nodes. Useful beacuse adjacent nodes have subsequent ids =>
  // diff can use the "run" attribute more often
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
/************************/
}