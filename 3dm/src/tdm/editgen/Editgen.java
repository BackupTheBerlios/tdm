// $Id: Editgen.java,v 1.4 2002/10/29 09:01:55 ctl Exp $
package editgen;

import XMLNode;
import XMLElementNode;
import XMLTextNode;
import Node;
import NodeFactory;
import BaseNode;
import BranchNode;
import MatchedNodes;
import XMLParser;
import XMLNode;
import XMLPrinter;
import java.util.Iterator;

public class Editgen {

  static java.util.Random rnd = new java.util.Random( 31415926L ); // repeatable runs!
  static int idCounter = 1000000;

  public static void main(String[] args) {
    Editgen e = new Editgen();
    e.editGen(/*"/home/ctl/fuego-core/xmlfs/3dm/usecases/shopping/"*/"med.xml", "m.xml",
              new String[] {"1.xml","2.xml"});
  }

  public void editGen( String inFile, String mergeFile, String[] outfiles ) {
    // Parse infile
   MarkableBaseNode docBase=null;
   BranchNode docMerged=null;
   try {
     XMLParser p = new XMLParser();
     docBase = (MarkableBaseNode) p.parse( inFile,baseNodeFactory);
     countSubtreeSizes(docBase,0); // We ned subtree counts to alloc a random node
   } catch ( Exception e ) {
     System.err.println("XML Parse error in " + inFile +
                        ". Detailed exception info is:" );
     System.err.println( e.toString() );
     e.printStackTrace();
     return;
   }
   // Make merge clone
   // total merge is left tree, current working tree is right
   docMerged = clonedAndMatchedTree( docBase, true, true );
   // Make variants
   for( int iFile = 0; iFile < outfiles.length; iFile++ ) {
     System.err.println("Making "+outfiles[iFile]+"...");
     BranchNode outRoot = clonedAndMatchedTree( docBase, false, true );
     ((MarkableBaseNode) docBase.getChild(0)).mark(); // Never edit root elem
     transform( (MarkableBaseNode) docBase.getChild(0), outRoot.getChild(0), docMerged.getChild(0),
                (MarkableBaseNode) docBase.getChild(0));
     try {
       printTree( outRoot, new XMLPrinter( new java.io.FileOutputStream( outfiles[iFile] )));
     } catch (java.io.IOException x ) {
       System.err.println("Unable to write outfile "+outfiles[iFile] );
     }
   }
   // Write merge facit
   try {
     printTree( docMerged, new XMLPrinter( new java.io.FileOutputStream( mergeFile )));
   } catch (java.io.IOException x ) {
     System.err.println("Unable to write outfile "+mergeFile );
   }

  }

  int _visitCount = 0;

  public void transform( MarkableBaseNode base, BranchNode variant, BranchNode total,
                        MarkableBaseNode baseRoot ) {
    boolean editNode = false;
    // Decide if node should be edited
    if( ! base.isMarked() ) {
      _visitCount++;
      editNode = rnd.nextDouble() > 0.7;
    }
    BranchNode n = null; // used by edit ops
    boolean after = false;
    MarkableBaseNode dest = null;
    if( editNode ) {
      int op = 4;// (int) (rnd.nextDouble() *5.0);
      switch(op) {
        case 0: // Delete node
          System.err.println("DEL");
          dest = getLargestDelTree(base);
          if( dest == null ) {
            System.err.println("-- Nothing suitable to del found");
            break;
          }
          _checkNotMarked(dest);
          dest.lock();
          dest.lockSubtree();
          editTrees(dest,null,null,false,false);
          /*
          n = base.getLeft().getFullMatch();
          n.getParent().removeChild(n.getChildPos());
          n = base.getRight().getFullMatch();
          n.getParent().removeChild(n.getChildPos());
          */
          break;
        case 1: // Insert node
          System.err.println("INS");
          org.xml.sax.helpers.AttributesImpl atts = new org.xml.sax.helpers.AttributesImpl();
          atts.addAttribute("","","id","CDATA",
             ""+(idCounter++) ); //  rnd.nextLong()+"@"+ System.currentTimeMillis());
          XMLElementNode content = new XMLElementNode("editgen:insert",atts);
          after = rnd.nextDouble() > 0.5;
          base.lock(!after,after);
          editTrees(null,base,new BranchNode( content),after,false);
          /*
                    base = after ? base : (base.hasLeftSibling() ?
                                 (MarkableBaseNode) base.getLeftSibling() : base);
          n = base.getLeft().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,new BranchNode( content));
          n = base.getRight().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,new BranchNode( content));
          */
          break;
        case 2: // Update node
          System.err.println("UPD");
          break; // N/A for now...
        case 3: // Move subtree
          System.err.println("MOV");
          // Delete from base (src of move) (=delete code block)
          base.lock();
          dest = getRandomNode( baseRoot, true, base ); // NOTE! Dest must be fetched AFTER src is locked!
          after = rnd.nextDouble() > 0.5;
          dest.lock(!after,after);
          // Text node move check
          n = dest.getLeft().getFullMatch();
          Node n2 = after ? n.getRightSibling() : n.getLeftSibling();
          if( base.getContent() instanceof XMLTextNode &&
              ( (n.getContent() instanceof XMLTextNode) ||
               ( n2 != null && n2.getContent() instanceof XMLTextNode ))) {
            // NOTE: Unfortunately we have locked the dst and src, so they are no
            // longer eligable for other ops after the abort :( (unlocking is not trivial)
            System.err.println("-- Abort: copying text node adjacent to other text node not possible");
            break;
          }

          editTrees(base,dest,null,after,true);

          /*
          BranchNode l = base.getLeft().getFullMatch();
          l.getParent().removeChild(l.getChildPos());
          BranchNode r = base.getRight().getFullMatch();
          r.getParent().removeChild(r.getChildPos());
          // Insert at dest (=insert code block)
          dest.lock(!after,after);
          dest = after ? dest : (dest.hasLeftSibling() ?
                                 (MarkableBaseNode) dest.getLeftSibling() : dest);
          n = dest.getLeft().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,l);
          n = dest.getRight().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,r);*/
          break;
        case 4: // Copy subtree
          // NOTE: Will currently never copy as child of a node,
          // if the node does not already have children (always inserts in childlist)
          System.err.println("CPY");
          // Lock src of copy
          base.lock();
          // Insert at dest (=insert code block)
          dest = getRandomNode( baseRoot, true, base ); // NOTE! Dest must be fetched AFTER src is locked!
          after = rnd.nextDouble() > 0.5;
          dest.lock(!after,after);
          n = dest.getLeft().getFullMatch();
          n2 = after ? n.getRightSibling() : n.getLeftSibling();
          if( base.getContent() instanceof XMLTextNode &&
              ( (n.getContent() instanceof XMLTextNode) ||
               ( n2 != null && n2.getContent() instanceof XMLTextNode ))) {
            // NOTE: Unfortunately we have locked the dst and src, so they are no
            // longer eligable for other ops after the abort :( (unlocking is not trivial)
            System.err.println("-- Abort: copying text node adjacent to other text node not possible");
            break;
          }

          editTrees(null,dest,base,after,true);
/*
          n.getParent().addChild(n.getChildPos()+offset,
                                 clonedAndMatchedTree(base,true,false));
          n = dest.getRight().getFullMatch();
          n.getParent().addChild(n.getChildPos()+offset,
                                 clonedAndMatchedTree(base,false,false));*/
          break;
      }
    } // if edit node
    // recurse
    for( int i=0;i<base.getChildCount();i++)
      transform((MarkableBaseNode) base.getChild(i),variant,total,baseRoot);
  }

  public MarkableBaseNode getRandomNode( MarkableBaseNode root, boolean isUnmarked,
     MarkableBaseNode forbiddenTree ) {
    int pos = (int) (rnd.nextDouble() * (root.getSubteeSize() - forbiddenTree.getSubteeSize() ));
    MarkableBaseNode found = doGetRandomNode( pos, root, isUnmarked, forbiddenTree );
    if( found == null && isUnmarked )
      found=doGetRandomNode(SCAN_UNMARKED,root,isUnmarked, forbiddenTree ); // Unmarked scann reached end of tree, start from top
// Forbiddentreecheck
    MarkableBaseNode _n = found;
    while( _n != null ) {
      if( _n == forbiddenTree )
        throw new IllegalStateException("found in forbidden tree");
      _n = (MarkableBaseNode) _n.getParent();
    }
// endcheck
    return found;
  }

  protected static final int SCAN_UNMARKED = Integer.MIN_VALUE;
  protected MarkableBaseNode doGetRandomNode( int pos, MarkableBaseNode n, boolean isUnmarked,
     MarkableBaseNode forbiddenTree ) {
    if( n == forbiddenTree )
      return null;
    if( pos == SCAN_UNMARKED ) {
      if( !n.isMarked() )
        return n;
      else {
        MarkableBaseNode found = null;
        for( int i=0;i<n.getChildCount() && found == null;i++)
          found = doGetRandomNode(pos,(MarkableBaseNode) n.getChild(i),isUnmarked,forbiddenTree);
        return found;
      }
    }
    if( pos == 0 ) {
      if( isUnmarked && n.isMarked() )
        return doGetRandomNode(SCAN_UNMARKED,n,isUnmarked, forbiddenTree);
      else
        return n;
    }
    int stSize = 0;
    MarkableBaseNode child= null;
    for( int i=0;i<n.getChildCount() && pos>=0;i++) {
      child = ((MarkableBaseNode) n.getChild(i));
      stSize = child == forbiddenTree ? 0 : child.getSubteeSize();
      pos-= stSize;
    }
    return doGetRandomNode( pos + stSize, child, isUnmarked, forbiddenTree );
  }

  // Get root of largest unmarked subtree of n

  protected MarkableBaseNode getLargestDelTree( MarkableBaseNode n ) {
    boolean allSubtreesDeletable = true;
    MarkableBaseNode largest = null;
    // find largest deltree from children
    for( int i=0;i<n.getChildCount();i++) {
      MarkableBaseNode delRoot = getLargestDelTree((MarkableBaseNode) n.getChild(i));
      allSubtreesDeletable &= delRoot == n.getChild(i);
      if( largest == null || (delRoot != null &&  largest.getSubteeSize() < delRoot.getSubteeSize() ))
        largest = delRoot;
    }
    if( allSubtreesDeletable && !n.isMarked() )
      return n;
    else
      return largest;
  }

  // Safety check
  private void _checkNotMarked( MarkableBaseNode n ) {
    if( n.isMarked() )
      throw new RuntimeException("ASSERT FAILED");
    for(int i=0;i<n.getChildCount();i++)
      _checkNotMarked((MarkableBaseNode) n.getChild(i));
  }

/*  protected static final Node DELETE_TREE = new BranchNode(null);
  protected static final Node MOVE_TREE = new BranchNode(null);
*/
  // if insTree = DELETE_TREE, the delete subtrees that b matches
  // != null, add insTree after/before b (after if after is set)
  // if cloneInsTree is set, a clone of the subtree rooted at insTree is added,
  // otherwise we just attach insTree.
  // NOTE: insTree must be BaseNodes if clone, otherwise BranchNodes
  protected void editTrees( MarkableBaseNode src, MarkableBaseNode dest, Node insTree,
                            boolean after, boolean cloneInsTree ) {
    doEditTrees( src,dest, after, insTree, cloneInsTree, true );
    doEditTrees( src,dest, after, insTree, cloneInsTree, false );
  }

  private void doEditTrees( MarkableBaseNode src,
   MarkableBaseNode dest, boolean after, Node insTree, boolean cloneInsTree,
                           boolean left ) {
    MatchedNodes m = null;
    //BranchNode deletedNode = null;
    if( src != null ) {
      // We should detach all matches of src
      // NOTE! for move with multiple matches, assumes all matches are identical!
      // (we only keep track of 1 deleted node, and reattach it to every copy)
      m = left ? src.getLeft() : src.getRight();
      for( Iterator i = m.getMatches().iterator();i.hasNext();) {
        BranchNode match = (BranchNode) i.next();
        insTree = match; // DONT USE .getBaseMatch(), as there may be changes in the Branch!
        match.getParent().removeChild(match.getChildPos());
      }
      m.getMatches().clear();
    }
    if( dest != null ) {
      m = left ? dest.getLeft() : dest.getRight();
      for( Iterator i = m.getMatches().iterator();i.hasNext();) {
        BranchNode match = (BranchNode) i.next();
        int ix = match.getChildPos() + (after ? 1: 0);
        if (!cloneInsTree) {
          if( src != null ) {
            ((BranchNode) insTree).setBaseMatch( src, BranchNode.MATCH_FULL );
            (left ? src.getLeft() : src.getRight()).addMatch((BranchNode)insTree);
          }
          match.getParent().addChild(ix,insTree);
        } else {
          match.getParent().addChild(ix,clonedAndMatchedTree(insTree,left,false));
        }
      }
    }
  }

  protected BranchNode clonedAndMatchedTree( Node n, boolean left, boolean resetMatches ) {
    BaseNode b = n instanceof BaseNode ? (BaseNode) n : ((BranchNode) n).getBaseMatch();
    BranchNode nc = new BranchNode( n.getContent() );
    nc.setBaseMatch(b,BranchNode.MATCH_FULL);
    MatchedNodes mn = left ? b.getLeft() : b.getRight();
    if( resetMatches )
      mn.clearMatches();
    mn.addMatch(nc);
    for( int i=0;i<n.getChildCount();i++)
      nc.addChild(clonedAndMatchedTree(n.getChildAsNode(i),left,resetMatches));
    return nc;
  }

  void printTree( Node n, XMLPrinter p ) {
    p.startDocument();
    doPrintTree(n.getChildAsNode(0),p);
    p.endDocument();
  }

  void doPrintTree( Node n, XMLPrinter p ) {
    XMLNode c = n.getContent();
    if( c instanceof XMLTextNode ) {
      XMLTextNode ct = (XMLTextNode) c;
      p.characters(ct.getText(),0,ct.getText().length);
    } else {
      XMLElementNode ce = (XMLElementNode) c;
      p.startElement(ce.getNamespaceURI(),ce.getLocalName(),ce.getQName(),ce.getAttributes());
      for( int i=0;i<n.getChildCount();i++)
        doPrintTree(n.getChildAsNode(i),p);
      p.endElement(ce.getNamespaceURI(),ce.getLocalName(),ce.getQName());
    }
  }

  public int countSubtreeSizes( MarkableBaseNode n, int count ) {
    int stSize = 0;
    for( int i=0;i<n.getChildCount();i++)
      stSize = countSubtreeSizes((MarkableBaseNode)n.getChild(i),stSize);
    n.setSubtreeSize(stSize+1);
    return stSize;
  }

  protected void markSubtree( MarkableBaseNode b )  {
    b.mark();
    for(int i=0;i<b.getChildCount();i++)
      markSubtree((MarkableBaseNode) b.getChild(i));
  }

  protected void unmarkSubtree( MarkableBaseNode b )  {
    b.unmark();
    for(int i=0;i<b.getChildCount();i++)
      unmarkSubtree((MarkableBaseNode) b.getChild(i));
  }

    // Factory for BaseNode:s
  private static NodeFactory baseNodeFactory =  new NodeFactory() {
            public Node makeNode(  XMLNode content ) {
              return new MarkableBaseNode( content  );
            }
        };

  private static NodeFactory branchNodeFactory =  new NodeFactory() {
            public Node makeNode(  XMLNode content ) {
              return new BranchNode(  content  );
            }
        };

  private void _getByIds( Node n, String id, java.util.Set nodes ) {
    XMLNode c = n.getContent();
    if( c instanceof XMLElementNode &&
        id.equals( ((XMLElementNode) c).getAttributes().getValue("id") ) )
        nodes.add(n);
    for(int i=0;i<n.getChildCount();i++)
      _getByIds(n.getChildAsNode(i),id,nodes);
  }

}