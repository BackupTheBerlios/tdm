// $Id: Editgen.java,v 1.6 2002/10/30 10:11:48 ctl Exp $
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
import EditLog;
import java.util.Iterator;
import gnu.getopt.*;

public class Editgen {

  static java.util.Random rnd = new java.util.Random( 31415926L ); // repeatable runs!
  static int idCounter = 1000000;

  public static void main(String[] args) {
    Editgen e = new Editgen();
    e.editGen(/*"/home/ctl/fuego-core/xmlfs/3dm/usecases/shopping/"*/"L7.xml", "m.xml",
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
   EditLog mergeLog = new EditLog();
   // Make variants
   for( int iFile = 0; iFile < outfiles.length; iFile++ ) {
     System.err.println("Making "+outfiles[iFile]+"...");
     EditLog branchLog = new EditLog();
     BranchNode outRoot = clonedAndMatchedTree( docBase, false, true );
     ((MarkableBaseNode) docBase.getChild(0)).mark(); // Never edit root elem
     transform( (MarkableBaseNode) docBase.getChild(0),
                (MarkableBaseNode) docBase.getChild(0), mergeLog, branchLog);
     try {
       printTree( outRoot, new XMLPrinter( new java.io.FileOutputStream( outfiles[iFile] )));
     } catch (java.io.IOException x ) {
       System.err.println("Unable to write outfile "+outfiles[iFile] );
     }
     String editLogFile = "edits-"+outfiles[iFile];
     try {
       branchLog.writeEdits(new XMLPrinter( new java.io.FileOutputStream( editLogFile )));
     } catch (Exception x ) {
       System.err.println("Unable to write edit log "+editLogFile );
     }
   }
   // Write merge facit
   try {
     printTree( docMerged, new XMLPrinter( new java.io.FileOutputStream( mergeFile )));
   } catch (java.io.IOException x ) {
     System.err.println("Unable to write outfile "+mergeFile );
   }
   // Write combined log
   String mergeLogFile = "edits-"+mergeFile;
   try {
     mergeLog.writeEdits(new XMLPrinter( new java.io.FileOutputStream( mergeLogFile )));
   } catch (Exception x ) {
     System.err.println("Unable to write edit log "+mergeLogFile );
   }
  }

  int _visitCount = 0;

  public void transform( MarkableBaseNode base,MarkableBaseNode baseRoot,
                         EditLog mergeLog, EditLog branchLog ) {
    boolean editNode = false;
    // Decide if node should be edited
    if( ! base.isMarked() ) {
      _visitCount++;
      editNode = rnd.nextDouble() > 0.5;
    }
    BranchNode n = null; // used by edit ops
    boolean after = false;
    MarkableBaseNode dest = null;
    if( editNode ) {
      int op = 0; //(int) (rnd.nextDouble() *5.0);
//      if( op==1) op = 4;
//      if( op==0) op = 3;
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
          editTrees(dest,null,null,false,false,mergeLog,branchLog);
          break;
        case 1: // Insert node
          System.err.println("INS");
          org.xml.sax.helpers.AttributesImpl atts = new org.xml.sax.helpers.AttributesImpl();
          atts.addAttribute("","","id","CDATA",
             ""+(idCounter++) ); //  rnd.nextLong()+"@"+ System.currentTimeMillis());
          XMLElementNode content = new XMLElementNode("editgen:insert",atts);
          after = rnd.nextDouble() > 0.5;
          base.lock(!after,after);
          editTrees(null,base,new BranchNode( content),after,false,mergeLog,branchLog);
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

          editTrees(base,dest,null,after,true,mergeLog,branchLog);
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
          editTrees(null,dest,base,after,true,mergeLog,branchLog);
          break;
      }
    } // if edit node
    // recurse
    for( int i=0;i<base.getChildCount();i++)
      transform((MarkableBaseNode) base.getChild(i),baseRoot,mergeLog,branchLog);
  }

  /** Return a random node from a tree. If isUnmarked is set, only returns unmarked
   * nodes. Never returns nodes inside forbiddenTree.
   */

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

  /** Function for manipulating trees. Use as described below (* means arg not used).
   * The base tree is never edited (but matches are added/removed). All operations
   * operate on entire subtrees. This function only works properly when all copied
   * subtrees are kept identical. (The function itself preserves this property)
   * DEL: editTrees(nodeToDelete,null,*,*,*);
   *   All matches for nodeToDelete in left & right are deleted, and matchings to
   *   the deleted nodes are removed.
   * INS: editTrees(null,siblingOfInsNode,BranchNode nodeToInsert,after,false);
   *   nodeToInsert is attached to both branches, either before (after flag false)
   *   or after (after flag true) siblingOfInsNode. No matchings are added.
   * MOV: editTrees(nodeToMove,targetNode,null,after,true);
   *  For both branches, the matches for nodeToMove are moved to before/after
   *  all matches for targetNode (before/after depending on the after flag)
   * CPY: editTrees(null,targetNode,BaseNode nodeToCopy,after,true);
   *  For both branches, a full match for nodeToCopy is copied to before/after
   *  all matches for targetNode (before/after depending on the after flag)
  */

  protected void editTrees( MarkableBaseNode src, MarkableBaseNode dest, Node insTree,
                            boolean after, boolean cloneInsTree, EditLog mergeLog,
                            EditLog branchLog ) {
    doEditTrees( src,dest, after, insTree, cloneInsTree, true, mergeLog );
    doEditTrees( src,dest, after, insTree, cloneInsTree, false, branchLog );
  }

  private void doEditTrees( MarkableBaseNode src,
   MarkableBaseNode dest, boolean after, Node insTree, boolean cloneInsTree,
                           boolean left, EditLog log ) {
    MatchedNodes m = null;
    if( src != null ) {
      // We should detach all matches of src
      // NOTE! for move with multiple matches, assumes all matches are identical!
      // (we only keep track of 1 deleted node, and reattach it to every copy)
      m = left ? src.getLeft() : src.getRight();
      if( dest == null )
        log.delete(src,m.getFullMatch().getParent());
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
          if( src != null )
            throw new IllegalStateException("Currently unneccessary/unimplemented state");
          match.getParent().addChild(ix,insTree);
        } else {
          BranchNode insRoot = clonedAndMatchedTree(
          insTree instanceof BaseNode ? (left ? ((BaseNode) insTree).getLeft().getFullMatch() :
          ((BaseNode) insTree).getRight().getFullMatch() ) : insTree
          ,left,false);
          match.getParent().addChild(ix,insRoot);
          if( src == null ) {
            if( insTree instanceof BaseNode )
              log.copy(insRoot);
            else
              log.insert(insRoot);
          } else
            log.move(insRoot);
        }
      }
    }
  }

  protected BranchNode clonedAndMatchedTree( Node n, boolean left, boolean resetMatches ) {
    BaseNode b = n instanceof BaseNode ? (BaseNode) n : ((BranchNode) n).getBaseMatch();
    BranchNode nc = new BranchNode( n.getContent() );
    if( b != null ) { // Set matches for non-inserted nodes
      nc.setBaseMatch(b,BranchNode.MATCH_FULL);
      MatchedNodes mn = left ? b.getLeft() : b.getRight();
      if( resetMatches )
        mn.clearMatches();
      mn.addMatch(nc);
    }
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
    if( id.equals(_getId(n)) )
        nodes.add(n);
    for(int i=0;i<n.getChildCount();i++)
      _getByIds(n.getChildAsNode(i),id,nodes);
  }

  private String _getId( Node n ) {
    XMLNode c = n.getContent();
    if( c instanceof XMLElementNode ) {
      XMLElementNode ce = (XMLElementNode) c;
      return ce.getAttributes().getValue("id");
    } else {
      return ((XMLTextNode) c).toString();
    }
  }


}