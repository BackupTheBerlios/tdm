// $Id: Editgen.java,v 1.1 2002/10/25 11:44:47 ctl Exp $
package editgen;

import XMLNode;
import XMLElementNode;
import XMLTextNode;
import Node;
import NodeFactory;
import BaseNode;
import BranchNode;
import XMLParser;
import XMLNode;
import XMLPrinter;

public class Editgen {

  static java.util.Random rnd = new java.util.Random( 31415926L ); // repeatable runs!

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
   // Make variants
   for( int iFile = 0; iFile < outfiles.length; iFile++ ) {
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
      editNode = _visitCount % 4 == 0 ;
    }
    BranchNode n = null; // used by edit ops
    boolean after = false;
    MarkableBaseNode dest = null;
    if( editNode ) {
      int op = 1;// (int) (rnd.nextDouble() *5.0);
      switch(op) {
        case 0: // Delete node
          System.err.println("DEL");
          base = getLargestDelTree(base);
          _checkNotMarked(base);
          base.lock();
          base.lockSubtree();
          n = base.getLeft().getFullMatch();
          n.getParent().removeChild(n.getChildPos());
          n = base.getRight().getFullMatch();
          n.getParent().removeChild(n.getChildPos());
          break;
        case 1: // Insert node
          System.err.println("INS");
          XMLTextNode content = new XMLTextNode("!INSERT!"+System.currentTimeMillis());
          after = rnd.nextDouble() > 0.5;
          base.lock(!after,after);
          base = after ? base : (base.hasLeftSibling() ?
                                 (MarkableBaseNode) base.getLeftSibling() : base);
          n = base.getLeft().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,new BranchNode( content));
          n = base.getRight().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,new BranchNode( content));
          break;
        case 2: // Update node
          System.err.println("UPD");
          break; // N/A for now...
        case 3: // Move subtree
          System.err.println("MOV");
          // Delete from base (src of move) (=delete code block)
          base.lock();
          BranchNode l = base.getLeft().getFullMatch();
          l.getParent().removeChild(l.getChildPos());
          BranchNode r = base.getRight().getFullMatch();
          r.getParent().removeChild(r.getChildPos());
          // Insert at dest (=insert code block)
          dest = getRandomNode( baseRoot, true ); // NOTE! Dest must be fetched AFTER src is locked!
          after = rnd.nextDouble() > 0.5;
          dest.lock(!after,after);
          dest = after ? dest : (dest.hasLeftSibling() ?
                                 (MarkableBaseNode) dest.getLeftSibling() : dest);
          n = dest.getLeft().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,l);
          n = dest.getRight().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,r);
          break;
        case 4: // Copy subtree
          System.err.println("CPY");
          // Lock src of copy
          base.lock();
          // Insert at dest (=insert code block)
          dest = getRandomNode( baseRoot, true ); // NOTE! Dest must be fetched AFTER src is locked!
          after = rnd.nextDouble() > 0.5;
          dest.lock(!after,after);
          dest = after ? dest : (dest.hasLeftSibling() ?
                                 (MarkableBaseNode) dest.getLeftSibling() : dest);
          n = dest.getLeft().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,
                                 clonedAndMatchedTree(base,true,false));
          n = dest.getRight().getFullMatch();
          n.getParent().addChild(n.getChildPos()+1,
                                 clonedAndMatchedTree(base,false,false));
          break;
      }
    } // if edit node
    // recurse
    for( int i=0;i<base.getChildCount();i++)
      transform((MarkableBaseNode) base.getChild(i),variant,total,baseRoot);
  }

  public MarkableBaseNode getRandomNode( MarkableBaseNode root, boolean isUnmarked ) {
    int pos = (int) (rnd.nextDouble() * root.getSubteeSize());
    MarkableBaseNode found = doGetRandomNode( pos, root, isUnmarked );
    if( found == null && isUnmarked )
      found=doGetRandomNode(SCAN_UNMARKED,root,isUnmarked); // Unmarked scann reached end of tree, start from top
    return found;
  }

  protected static final int SCAN_UNMARKED = Integer.MIN_VALUE;
  protected MarkableBaseNode doGetRandomNode( int pos, MarkableBaseNode n, boolean isUnmarked ) {
    if( pos == SCAN_UNMARKED ) {
      if( !n.isMarked() )
        return n;
      else {
        MarkableBaseNode found = null;
        for( int i=0;i<n.getChildCount() && found == null;i++)
          found = doGetRandomNode(pos,(MarkableBaseNode) n.getChild(i),isUnmarked);
        return found;
      }
    }
    if( pos == 0 ) {
      if( isUnmarked && n.isMarked() )
        return doGetRandomNode(SCAN_UNMARKED,n,isUnmarked);
      else
        return n;
    }
    int stSize = 0;
    MarkableBaseNode child= null;
    for( int i=0;i<n.getChildCount() && pos>=0;i++) {
      child = ((MarkableBaseNode) n.getChild(i));
      stSize = child.getSubteeSize();
      pos-= stSize;
    }
    return doGetRandomNode( pos + stSize, child, isUnmarked );
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

  protected BranchNode clonedAndMatchedTree( BaseNode n, boolean left, boolean setMatches ) {
    BranchNode nc = new BranchNode( n.getContent() );
    nc.setBaseMatch(n,BranchNode.MATCH_FULL);
    if( setMatches ) {
      if( left ) {
        n.getLeft().clearMatches();
        n.getLeft().addMatch(nc);
      } else {
        n.getRight().clearMatches();
        n.getRight().addMatch(nc);
      }
    }
    for( int i=0;i<n.getChildCount();i++)
      nc.addChild(clonedAndMatchedTree(n.getChild(i),left,setMatches));
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


}