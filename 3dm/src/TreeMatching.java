// $Id: TreeMatching.java,v 1.1 2001/03/14 08:23:54 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE
/**
 * Title:        Tree Diff and Merge quick proto 1
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:      HUT
 * @author Tancred Lindholm
 * @version 1.0
 */
/*
import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.io.PrintWriter;

public class TreeMatching {


  //public MapNode mapRoot;
  private HashMap baseDocToMap = new HashMap();

  public TreeMatching() {
  }

  // Build a matching between docs rooted at A and B
/*  public TreeMatching( Node docA, Node docB, String aName ) {
    System.out.println("Finding corresponing nodes in the trees...");
    match( docA, docB );
    System.out.println("Building nodemap...");
//    mapRoot = buildMapping( null, docB );
    name = aName;
  }
/*
  public MapNode buildMapping( MapNode mapParent, Node docNode ) {
    MapNode map = new MapNode( docNode );
    Node partner = getFirstMapping(null,docNode);
    map.partner = partner;
    addBaseDocToMap( partner, map);
    // Make mapping operation
    if( mapParent != null ) { // Not root of mapping
      // partner in Src3
      if( partner == null ) {
        // No matching node, this node is inserted
        map.addOperation( new InsertOp( docNode ) );
      } else {
        if( getNextMapping() != null )
          throw new RuntimeException("buildMapping(): Cannot Handle GLU operations (many(docA)->one(docB)" );
        // Copy/Modify operation
        if( partner.contentEquals(docNode) )
          map.addOperation( new CopyOp( new TreePath( mapParent.partner, partner ), partner ) );
        else
          map.addOperation( new UpdateOp( new TreePath( mapParent.partner, partner ), docNode ) ) ;
        // Check for deletion Ops
        // iterate over partner's children and see if they are any unmapped (=deleted)
        if( partner instanceof ElementNode ) {
          for( int i=0;i<((ElementNode) partner).children.size();i++) {
            if( getFirstMapping( null,(Node) ((ElementNode) partner).children.elementAt(i) ) == null )
              buildDeletia( map, (Node) ((ElementNode) partner).children.elementAt(i) );
          }
        }
      }
    } else {
      // Making the root mapping; currently no mapping
      ;
    }
    // Mapping Node built, now go one level down
    if( docNode instanceof ElementNode ) {
      for( int i=0;i<((ElementNode) docNode).children.size();i++)
        map.addChild( buildMapping( map, (Node) ((ElementNode) docNode).children.elementAt(i) ) );
    }
    return map;
  }

  public void buildDeletia( MapNode mapNode, Node docNode ) {
    if( getFirstMapping( null, docNode ) == null ) {
      // Node is deleted
      mapNode.addOperation( new DeleteOp( docNode ) );
      if( docNode instanceof ElementNode ) {
        // Check for deleted children
        for( int i=0;i<((ElementNode) docNode).children.size();i++)
          buildDeletia( mapNode, (Node) ((ElementNode) docNode).children.elementAt(i) );
      }
    }
    // else terminate recursion
  }
*/
/*  public void match( Node docA, Node docB ) {
  // NO GLU MAPPINGS  buildExactMatching( docA, docB ); // Won't make copy mappings
    buildExactMatching( docB, docA ); // So we need to run it 'backwards' as well
    matchSamePosUnmatched( docA, docB);

    // Sometimes in the future: buildHeuristicMatchingOfUnmatched();
  }


  // !!!! Assumes target,a and b subtrees match EXACTLY!
  // The distance otherwise returned in case of recursion (see **) is meaningless
  Node minDist( Node target, Node a, Node b ) {

/*    if( target.toString().startsWith("hr"))
    System.out.println("Mindisting:");
    System.out.println("a=" + a.toString() );
    System.out.println("t=" + target.toString() );
    System.out.println("b=" + b.toString() );

    target.printTree(0);
/*    System.out.println("with");
    a.printTree(0);
    System.out.println("and");
    b.printTree(0);
*/
/*    Vector tpath = TreePath.buildNodePath( target ), apath = TreePath.buildNodePath( a ),
                  bpath = TreePath.buildNodePath( b );
    // find node with longest common path (NOTE! node addresses, not equality)
    int imax = tpath.size() -1 ;
//    System.out.println("Walking: (a,t,b)");
    for( int i = 1;i<=imax;i++) {
      // NOTE! Be VERY careful NOT to use the Vectors indexOf method as it compares using
      // equals, not ptr
      int tpos = ((ElementNode) tpath.elementAt(i)).childNo;
      int apos = Integer.MAX_VALUE, bpos =Integer.MAX_VALUE;
      try {
        apos = ((ElementNode) apath.elementAt(i)).childNo;
      } catch (IndexOutOfBoundsException e ) {
      }
      try {
        bpos = ((ElementNode) bpath.elementAt(i)).childNo;
      } catch (IndexOutOfBoundsException e ) {
      }

//      System.out.println(apos+","+tpos+","+bpos);
      if( tpos == apos && tpos == bpos && i < imax)
        continue; // All still matching, and still nodes to match
      else if ( tpos == apos && tpos == bpos ) {
        // Last node in target, and both paths have matched; return the shortest one
        // (=the one with min "extra" length)
        int asize = apath.size(), bsize =  bpath.size();
        if( asize == bsize && asize == tpath.size()
            // **) Here we use the assumption that target, a and b match exactly
            // => only on test for type and child Vector
            && target instanceof ElementNode && ((ElementNode) target).children.size()>0 &&
            matches((Node) ((ElementNode) target).children.elementAt(0),
                        (Node) ((ElementNode) a).children.elementAt(0)) &&
            matches((Node) ((ElementNode) target).children.elementAt(0),
                        (Node) ((ElementNode) b).children.elementAt(0))
          ) {
          // Paths of target, a and b match; use the leftmost children (if any) to resolve
          // NOTE! This may recurse to maximum depth
          System.out.println("Target = " + target.toString() );
          System.out.println("a = " + target.toString() );
          System.out.println("b = " + target.toString() );
          System.out.flush();
          return minDist((Node) ((ElementNode) target).children.elementAt(0),
                        (Node) ((ElementNode) a).children.elementAt(0),
                        (Node) ((ElementNode) b).children.elementAt(0) );
        } else
          return asize < bsize ? a : b;
      } else {
        // At least one does not match, return the "closest" (minimum distance btw. siblings)
        return Math.abs(apos-tpos) < Math.abs(bpos-tpos) ? a : b;
      }
    }
    throw new RuntimeException("minDist(): Should never get here");
  }

  private void buildExactMatching( Node a, Node rootOfB ) {
    if( getFirstMapping( null, a ) == null ) {
      // Node isn't matched, let's do it
      HashSet candidates = new HashSet();
      findCandidates( candidates, a, rootOfB );
      Node bestMatch = null;
      int bestCount = 0;
      for( Iterator i = candidates.iterator(); i.hasNext(); ) {
        Node candidate = (Node) i.next();
        int thisCount = dfsExactMatch( a, candidate , false, 0 );
        if( thisCount > bestCount ) {
          bestCount = thisCount;
          bestMatch = candidate;
        } else if ( thisCount > 0 && thisCount == bestCount ) {
          bestMatch = minDist( a, bestMatch, candidate );
        }
      }

      // DEBUG CODE
/*      if( a.toString().startsWith("ul") ) {
        System.out.println("best count = " + bestCount );
        System.out.println("Node = " + bestMatch.toString() );
      }
*/
/*      // Build matching
      if( bestMatch != null )
        dfsExactMatch( a, bestMatch, true , 0);
    }
    // Process children
    if( a instanceof ElementNode ) {
      Vector children = ((ElementNode) a).children;
      for(int i=0;i<children.size();i++)
        buildExactMatching((Node) children.elementAt(i), rootOfB );
    }
  }
*/
/*  private void findCandidates( HashSet candidates, Node key, Node treeRoot ) {

    // DEBUG CODE
    __findCandidates(candidates,key,treeRoot);
    if( key.toString().startsWith("ul") ) {
      System.out.println("Matching:");
      key.printTree(0);
      System.out.println("Matches = " + candidates.size() );
    }
  }
*/

/*
  private int dfsExactMatch(Node docA, Node docB, boolean addMatchings, int count ) {
    if( !matches(docA,docB) )
      return count;
    else {
      // matches!
      if( addMatchings ) {
        addMatching( docA,docB );
        addMatching( docB, docA );
      }
      // recursively iter trough children
      if( docA instanceof ElementNode ) {
        Vector children = ((ElementNode) docA).children;
        if( children.size() == ((ElementNode) docB).children.size() ) {
          // Only match children, if there are equally many
          for( int i=0; i<children.size(); i ++ )
            count += dfsExactMatch( (Node) ((ElementNode) docA).children.elementAt(i),
                                    (Node) ((ElementNode) docB).children.elementAt(i), addMatchings, 0 );
        }
      }
      return count + 1; // +1 because we matched docA to docB
    }
  }

  /********************************************************************************/
  /* Merging routines
  *********************************************************************************/


/*  public MapNode merge( TreeMatching m2) {
    System.out.println("Merging trees...");
    if( m2.mapRoot.partner != mapRoot.partner )
      throw new RuntimeException("Can only merge trees based on a common version!");
    initOpStates(mapRoot, true, m2);
    initOpStates(m2.mapRoot, false, this );
    MapNode mergeRoot = new MapNode(mapRoot.partner);
    mergeNode( mergeRoot, mapRoot, m2.mapRoot, m2 );
    return mergeRoot;
  }
*/
/*
  public MergedNode merge2( Node a, Node b, TreeMatching m ) {
///    initCrossMap(getFirstMapping(a),m.getFirstMapping(b));
    initMatchTag(a,this);
///    Node ap = getFirstMapping(a);
///    initMatchTag(ap /*getFirstMapping(a)*//*,this);
    initMatchTag(b,m);
///    initMatchTag(m.getFirstMapping(b),m);

///    Node bp = m.getFirstMapping(b);
///    if( ap == bp ) {
///      System.out.println("Something seriously fucked uP!!!!");
///      System.exit(-1);
///    }
     MergedNode root = new MergedNode( new TextNode("$") );
/*    mergeNode(root,(ElementNode) ((ElementNode) a).getChild(0),
      (ElementNode) ((ElementNode) b).getChild(0),m);
*//*
    mergeNode(root,(ElementNode)a,(ElementNode)b,m);
    java.io.PrintWriter pw =  new java.io.PrintWriter( System.out );
    root.getChild(0).printXML( pw, 0);
    pw.close();
  try {
  pw =  new java.io.PrintWriter( new java.io.FileOutputStream("mf.xml" ) );
    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    root.getChild(0).printXML( pw, 0);
    pw.close();
  } catch (Exception e) { System.err.println("I/O error writing merged"); }
//    root.printTree(0);
    return root;
  }

/*  void initCrossMap( Node a, Node b ) {
    crossMap.put(a,b);
    crossMap.put(b,a);
      if( a.getChildCount() != b.getChildCount() ) throw new RuntimeException("Crossmap init failed!");
    for(int i=0;i<a.getChildCount();i++)
      initCrossMap( a.getChild(i), b.getChild(i) );
  }
*//*
  void initMatchTag( Node a, TreeMatching m ) {
    a.matchTag = m;
    for(int i=0;i<a.getChildCount();i++)
      initMatchTag( a.getChild(i), m );
  }

//  private HashMap crossMap = new HashMap();

  class HangonEntry {
    Node node = null;
    boolean updated = false;
    HangonEntry( Node an, boolean u ) {
      node = an;
      updated = u;
    }

    public String toString() {
      return (updated ? '*' : ' ')+node.toString();
    }
  }

  class MergeEntry {
    Node node = null;
    Node basePartner = null;
    Vector inserts = new Vector();
    boolean locked = false;
    boolean updated = false;
    void print() {
      System.out.print(updated ? 'x' : ' ');
      System.out.print(locked ? '*' : ' ');
      System.out.print(' ' + node.toString() + ' ');
      System.out.println( inserts.toString() );
    }

    int getHangonCount() {
      return inserts.size();
    }

    HangonEntry getHangon( int ix ) {
      return (HangonEntry) inserts.elementAt(ix);
    }
  }

  class MergeList {
    private Vector list = new Vector();
    int currentPos = -1; // A better name would be current tail pos
    void add( Node n, Node basePartner, boolean updated ) {
      currentPos++;
      ensureCapacity( currentPos + 1);
      ((MergeEntry) list.elementAt(currentPos)).node = n;
      ((MergeEntry) list.elementAt(currentPos)).basePartner = basePartner;
      ((MergeEntry) list.elementAt(currentPos)).updated = updated;
    }

    void addHangOn( Node n, boolean updated ) {
      ((MergeEntry) list.elementAt(currentPos)).inserts.add( new HangonEntry( n, updated ) );
    }


    int getEntryCount() {
      return currentPos + 1;
    }

    MergeEntry getEntry( int ix ) {
      return (MergeEntry) list.elementAt(ix);
    }

    void removeEntryAt( int ix ) {
      list.removeElementAt(ix);
      currentPos--;
    }

    void lockNeighborhood(  int left, int right ) {
      lockNeighborhood( currentPos, left, right );
    }

    void lockNeighborhood( int acurrentPos, int left, int right ) {
      ensureCapacity( acurrentPos + right + 1);
      for( int i = acurrentPos - left; i<=acurrentPos + right; i++)
        ((MergeEntry) list.elementAt(i)).locked = true;
    }

    private void ensureCapacity( int size ) {
      for( int i = list.size(); i < size; i ++ )
        list.add( new  MergeEntry() );
    }

    void print() {
      int pos = 0;
      MergeEntry me = null;
      do {
        me = (MergeEntry) list.elementAt(pos);
        me.print();
        pos++;
      } while( me.node != END );
    }
  }

  static Node START = new TextNode("__start__");
  static Node END = new TextNode("__end__");

  // docA and docB are merge partners
  // GLU mappings forbidden
  // Mental note: Reading source code the Victor Borge way would probably be funny
  protected void mergeNode( MergedNode mergeTree, ElementNode docA, ElementNode docB, TreeMatching docBMatching ) {
    Vector merged = new Vector();

    MergeList mlistA = docA != null ? makeMergeList( docA ) : null;
    MergeList mlistB = docB != null ? docBMatching.makeMergeList( docB ) : null;
    if( mlistA != null && mlistB != null ) {
      mergeDeleted( docA, docB, mlistA, mlistB, docBMatching );
      // Now we should have exactly the same entries in mlistA and mlistB
      // quick check
        if( mlistA.getEntryCount() != mlistB.getEntryCount() ) throw new RuntimeException("mergeNode(): lists different lengths!");

/*      System.out.println("DocA list:");
      mlistA.print();
      System.out.println("DocB list:");
      mlistB.print();
*//*
      // So let's do the merge
      // Some things to prove:
      // No node can get "skipped" (deleted)
      // No node gets extra copies
      // Sequencing isn't violated if merge is successful
      int posA = 0, posB = 0, nextA=-1,nextB=-1;
      while(true) {
        // pos is set up so that ea and eb are merge-partners
        MergeEntry ea = mlistA.getEntry(posA), eb= mlistB.getEntry(posB);
        // Merge the entries, for now just pick the locked/changed one
        if( ea.updated && eb.updated ) {
            if( !matches(ea.node,eb.node) ) {
              System.out.println("CONFLICT; both nodes updated- picked A; nodes follow");
              ea.print();
              eb.print();
            } else
              System.out.println("CONFLICTW; Node updated in both branches, but updates are equal");

            eb.updated = false;
        }
        MergeEntry chosenEn=null,otherEn=null;
        if( ea.updated ) {
          merged.add( ea );
          chosenEn = ea;
          otherEn = eb;
        } else {
          merged.add( eb );
          chosenEn = eb;
          otherEn = ea;
        }

        // Hangon nodes
        if( otherEn.inserts.size() > 0 ) {
          if( chosenEn.inserts.size() > 0 ) {
            // Both have hangons, this needs more work. Insertions can be chained, but whatabout
            // nodes from elsewhere and n:th copies?
            System.out.println("CONFLICTW; both nodes have hangons; sequencing them as updated(or A if no update)-Other");
            System.out.println("DocA list:");
            mlistA.print();
            System.out.println("DocB list:");
            mlistB.print();
          }
          for( int i=0;i<otherEn.inserts.size();i++)
            chosenEn.inserts.add( otherEn.inserts.elementAt(i) );
        }
        // See if we're done
        if( (ea.locked && ea.node==END) || (eb.locked && eb.node==END) || (ea.node==END && eb.node==END) )
          break;
        // figure out the next one
        nextA = ea.locked && mlistA.getEntry(posA+1).locked ? posA + 1 : -1; // -1 means free
        nextB = eb.locked && mlistB.getEntry(posB+1).locked ? posB + 1 : -1;
        if( nextA == -1 && nextB == -1 ) { // No locking, just let both go forward
          nextA = posA + 1;
          nextB = posB + 1;
        }
        if( nextB == -1 )
          nextB = getPartnerPos( mlistA.getEntry(nextA), mlistB, docB, docBMatching );
        else if (nextA == -1 )
          nextA = getPartnerPos( mlistB.getEntry(nextB), mlistA, docA, this );
        else if (nextB != getPartnerPos( mlistA.getEntry(nextA), mlistB, docB, docBMatching ) ) {
          System.out.println("CONFLICT: Sequencing conflict, using docA's sequencing");
          nextB = getPartnerPos( mlistA.getEntry(nextA), mlistB, docB, docBMatching);
        }
        posA = nextA;
        posB = nextB;
      }
/*      System.out.println("Merged list:");
      for(int i=0;i<merged.size();i++)
        ((MergeEntry) merged.elementAt(i)).print();*//*
    } // end if merging two lists e
    else if( mlistA == null ) {
      for( int i=0;i<mlistB.getEntryCount();i++)
        merged.add( mlistB.getEntry(i) );
    } else if ( mlistB == null ) {
      for( int i=0;i<mlistA.getEntryCount();i++)
        merged.add( mlistA.getEntry(i) );
    } else
      throw new RuntimeException("merge(): both docA and docB were NULL!");

    //
    // Now just add the new nodes to mergeTree and recurse for all ElementNodes
    //
    for(int i=0;i<merged.size()-1;i++) { // -1 because END is not needed
      MergeEntry me = (MergeEntry) merged.elementAt(i);
      if( me.node != START ) {
        MergedNode n = new MergedNode( me.node );
        mergeTree.addChild(n);
        if( n.getNode() instanceof ElementNode ) // Only recurse on element nodes
          mergeNode( n,
            (ElementNode) getPartnerA(me.node),
            (ElementNode) getPartnerB(me.node,docBMatching),docBMatching );
      }
      // and hangons
      for( int ih=0;ih<me.getHangonCount();ih++) {
        MergedNode n = new MergedNode( me.getHangon(ih).node ); //   (Node) ((Node) me.inserts.elementAt(ih)).clone();
        mergeTree.addChild(n);
        if( n.getNode() instanceof ElementNode ) // Only recurse on element nodes
          mergeNode( n,
            (ElementNode) getPartnerA( me.getHangon(ih).node),
            (ElementNode) getPartnerB( me.getHangon(ih).node,docBMatching),docBMatching );
      }
    }
  }

  // Returns partner of n in this tree, or simply n if it's already in this tree
  private Node getPartnerA( Node n ) {
    if( n.matchTag == this )
      return n;
    Node base = n.matchTag.getFirstMapping(n);
    if( base == null )
      return null; // It was an insert node on the other side, and has no mapping
/*    Node thisBase = (Node) crossMap.get(otherBase);
*//*
    Node partner = getFirstMapping( base );
    // Make sure all partners are identical
    Node n2 = getNextMapping();
    if(n2!=null)
      System.out.println("WARNING: Possible multiple models");
    return partner;
  }

  private Node getPartnerB( Node n, TreeMatching m ) {
    // Question: should we check for multiple models here?
    // Don't we actually know which copy is being used at this point?
    // Would ignoring the check break merge symmetry?
    // The same comment goes for getPartnerA, of course
    if( n.matchTag != this )
      return n;
    Node base = getFirstMapping(n);
    if( base == null )
      return null; // It was an insert node on the other side, and has no mapping
//    Node otherBase = (Node) crossMap.get(thisBase);
    Node partner = m.getFirstMapping( base );
    // Make sure all partners are identical
    Node n2 = m.getNextMapping();
    if(n2!=null)
      System.out.println("WARNING: Possible multiple models");
    return partner;
  }


  int getPartnerPos( MergeEntry e, MergeList bList,  ElementNode parentB, TreeMatching mb ) {
    // Special cases: start and end markers
    if( e.node == START )
      return 0;
    if( e.node == END )
      return bList.getEntryCount() - 1;
    Node baseParent = mb.getFirstMapping( parentB );
    Node partnerB = baseParent.getChild( e.basePartner.childNo );
    for( int i = 1;i<bList.getEntryCount()-1;i++) {
      if( bList.getEntry(i).basePartner == partnerB )
        return i;
    }
    throw new RuntimeException("getpartnerPos(): partner not found!");
  }

  void mergeDeleted( ElementNode parentA, ElementNode parentB, MergeList mlA, MergeList mlB,
    TreeMatching bMatching ) {
    // Here could be some assertions about parentpartner and parentPartnerB containing
    // _exactly_ the same nodes
    Node parentPartner = getFirstMapping( parentA );
    Node parentPartnerB = bMatching.getFirstMapping( parentB );

    HashSet usedNodesA = new HashSet(),usedNodesB = new HashSet();
    // usedNodes contains all nodes not deleted (because they occur in the mergeList)
    // NOTE! Hang-on nodes need not be added to the list, because they are
    // 1) inserts = no partner, or
    // 2) n:th copies (1:st copy is present in the list
    // 3) from elesewhere (not same parent)
    for( int i=1;i<mlA.getEntryCount()-1;i++) // strange loop ixes becaus eof START & END markers
      usedNodesA.add( mlA.getEntry(i).basePartner );
    for( int i=1;i<mlB.getEntryCount()-1;i++)
      usedNodesB.add( mlB.getEntry(i).basePartner );
//    System.out.println(usedNodesA.toString());
//    System.out.println(usedNodesB.toString());
    for( int i=0;i<parentPartner.getChildCount();i++ ) {
      boolean inA = usedNodesA.contains(parentPartner.getChild(i)),
              inB = usedNodesB.contains(parentPartnerB.getChild(i));
      if( !inA || !inB ) {
        // The node is deleted in either/both branches, must be removed from the other
        if( inA ) // Delete from A
          deleteNode( mlA, parentPartner.getChild(i) );
        if (inB )
          bMatching.deleteNode( mlB, parentPartnerB.getChild(i) );
      } // present in both
    }
  }

  // Delete node in mergelist, id'd by basePartner
  void deleteNode( MergeList ml, Node basePartner ) {
    boolean deleted = false;
    for( int i=0;i<ml.getEntryCount();i++) {
      MergeEntry me = ml.getEntry(i);
      if( me.basePartner == basePartner ) {
        deleted = true;
        if( me.locked || me.updated )
          System.out.println("CONFLICT: Moved/updated node deleted!");
        // Other checks, such that all nodes below are not changed...plahplah
        ml.removeEntryAt( i );
        ml.lockNeighborhood( i, 1, 0); // Lock neignhborhood of deleted node
        break;
      }
    }
    if( !deleted) throw new RuntimeException("deleteNode(): node not found");
  }

  // parent in this matching...
  // Does not yet set the .update flag at all; also hangon nondes need an updated
  // flag
  private MergeList makeMergeList( ElementNode parent ) {
    MergeList ml = new MergeList();
    HashSet partners = new HashSet();
    Node parentPartner = getFirstMapping( parent );
    int prevChildPos = -1;
    int parentPartnerChildCount = parentPartner instanceof ElementNode ?
      ((ElementNode) parentPartner).getChildCount() : -1000;
    ml.add( START, null, false );
    for( int i = 0;i<parent.getChildCount();i++) {
      Node current = parent.getChild(i);
      Node partner = getFirstMapping( current );
      int childPos = partnerChildPos( parentPartner, partner  );
      if( partner == null ) {
        // It's an insert node
        ml.addHangOn( current, true );
        ml.lockNeighborhood(0,1);
      } else if ( partners.contains( partner ) ) {
        // current is the n:th copy of a node (n>1)
        ml.addHangOn( current, !matches( partner, current) );
        ml.lockNeighborhood(0,1);
      } else if( childPos == -1 ) {
        // Copied from elsewhere
        ml.addHangOn( current, !matches( partner, current) );
        ml.lockNeighborhood(0,1);
      } else {
        ml.add( current, partner,  !matches( partner, current) );
        partners.add( partner );
        if( (prevChildPos + 1) != childPos ) // Out of sequence, lock previous and this
                                             // e.g. -1 0 1 3 4 5 => 1 & 3 locked
          ml.lockNeighborhood(1,0);
        prevChildPos = childPos;
      }
    }
    ml.add( END, null, false );
    if( (prevChildPos + 1 )!= parentPartnerChildCount )
      ml.lockNeighborhood(1,0); // Possible end shock, e.g. -1 0 1 2 4=e, and 4 children in parent
                                //                                        i.e. ix 3 was deleted
    return ml;
  }


  // Returns the pos of child below parent, or -1 of it's not a child of parent
  private int partnerChildPos( Node parentPartner, Node child ) {
    int pos = -1;
    if( parentPartner == null || !(parentPartner instanceof ElementNode ))
      return pos; // which is -1
    ElementNode parent = (ElementNode) parentPartner;
    for( int i = 0;i<parent.getChildCount() && pos == -1;i++)
      pos = parent.getChild(i) == child ? i : -1;
    return pos;
  }

  public NodeOp getStdOp( MapNode n ) {
    NodeOp op = null;
    for( int i=0;i<n.getOpCount();i++) {
      if( n.getOperation(i) instanceof DeleteOp )
        continue;
      if( op == null )
        op = n.getOperation(i);
      else
        throw new RuntimeException("getStdOp(): More than one non-delete ops");
    }
    return op;
  }

  // n1 MUST be in this matching, n2 in matchingB
  // n1 and n2 MUST be merge-partners, or null:
  // n1----base_node---n2
  // This is where the magic happens!
  public void mergeNode( MapNode mergeTree, MapNode n1, MapNode n2, TreeMatching matchingB ) {

/*    // Process deleted nodes
      mergeDeleted( n1, matchingB );
      mergeDeleted( n2, this );

    // Then inserts (which are STRICT ops)
      // from n1...
      for( int i=0;n1!=null && i<n1.getChildCount();i++) {
        MapNode inode = n1.getChild(i);
        NodeOp op = getStdOp( inode );
        if( op instanceof InsertOp ) {
          mergeTree.mergeChildAt( inode.copyOf(), i );
        }
      }
      // ...and n2
      for( int i=0;n2!=null && i<n2.getChildCount();i++) {
        MapNode inode = n2.getChild(i);
        NodeOp op = getStdOp( inode );
        if( op instanceof InsertOp ) {
          mergeTree.mergeChildAt( inode.copyOf(), i );
        }
      }
    // STRICT copy and update ops
      // from n1...
      for( int i=0;n1!=null && i<n1.getChildCount();i++) {
        MapNode inode = n1.getChild(i);
        NodeOp op = getStdOp( inode );
        if( (op instanceof UpdateOp) || (op instanceof CopyOp && op.state==NodeOp.STRICT ) ) {
          mergeTree.mergeChildAt( inode.copyOf(), i );
        }
      }
      // and n2...
      for( int i=0;n2!=null && i<n2.getChildCount();i++) {
        MapNode inode = n2.getChild(i);
        NodeOp op = getStdOp( inode );
        if( (op instanceof UpdateOp) || (op instanceof CopyOp && op.state==NodeOp.STRICT ) ) {
          mergeTree.mergeChildAt( inode.copyOf(), i );
        }
      }
    // LOOSE ops
      // from n1...
      for( int i=0;n1!=null && i<n1.getChildCount();i++) {
        MapNode inode = n1.getChild(i);
        NodeOp op = getStdOp( inode );
        if( op instanceof CopyOp && op.state==NodeOp.LOOSE && partnerIsLoose(inode, matchingB) ) {
          if( mergeTree.getChildCount() > i && mergeTree.getChild(i)==null ) {
            mergeTree.mergeChildAt( inode.copyOf(), i ); // Opportunistically grab this place
            op.setState(NodeOp.STRICT); // The op is henceforth strict (we've chosen this branch)
          } else
            // Couldn't put this node, mark other op STRICT
            markOtherStrict( inode, matchingB, n2, mergeTree );
        }
      }
      // from n2 ...
      for( int i=0;n2!=null && i<n2.getChildCount();i++) {
        MapNode inode = n2.getChild(i);
        NodeOp op = getStdOp( inode );
        if( op instanceof CopyOp && op.state==NodeOp.LOOSE && partnerIsLoose(inode, this) ) {
          if( mergeTree.getChildCount() > i && mergeTree.getChild(i)==null ) {
            mergeTree.mergeChildAt( inode.copyOf(), i ); // Opportunistically grab this place
            op.setState(NodeOp.STRICT); // The op is henceforth strict (we've chosen this branch)
          } else
            // Couldn't put this node, mark other op STRICT
            markOtherStrict( inode, this, n1, mergeTree );
        }
      }
    // Check assertion: there are no mergeTree null nodes
    for(int i=0;i<mergeTree.getChildCount();i++)
      if( mergeTree.getChild(i) == null ) throw new RuntimeException("mergeNode(): node has null children");
    // And recurse for each child
    for(int i=0;i<mergeTree.getChildCount();i++) {
      if( mergeTree.getChild(i).inThisMatching) {
        mergeNode( mergeTree.getChild(i),mergeTree.getChild(i).origNode,
          findPartner(mergeTree.getChild(i).origNode, matchingB), matchingB );
      } else {
        mergeNode( mergeTree.getChild(i),
          findPartner(mergeTree.getChild(i).origNode, this),mergeTree.getChild(i).origNode, matchingB );
      }
    }*//*
  }

  public boolean partnerIsLoose( MapNode n, TreeMatching otherMatching ) {
    MapNode partner = otherMatching.getFirstMapNode(n.partner); // partner is in the base tree
    // We don't need to check other partners under the assumption that copies are always strict
    if( partner != null ) {
      NodeOp op = getStdOp( partner );
      return op.getState() == NodeOp.LOOSE;
    } else
      return false;
  }

  public void markOtherStrict( MapNode n, TreeMatching otherMatching, MapNode thisNode, MapNode mergeTree ) {
    MapNode partner = otherMatching.getFirstMapNode(n.partner); // partner is in the base tree
    while( partner != null ) {
      NodeOp op = getStdOp(partner);
      op.setState(NodeOp.STRICT);
      // apply the ne strict node, if it's a child of thisNode
      for(int i=0;i<thisNode.getChildCount();i++) {
        NodeOp op2 = getStdOp( thisNode.getChild(i) );
        if( op==op2 ) {
          mergeTree.mergeChildAt( thisNode.getChild(i).copyOf(), i );
        }
      }
      partner = otherMatching.getNextMapNode();
    }
  }

  public MapNode findPartner( MapNode n, TreeMatching inMatching ) {
    MapNode partner = inMatching.getFirstMapNode(n.partner); // partner is in the base tree
    if( partner == null )
      return null;
    MapNode nextPartner = inMatching.getNextMapNode();
    while( nextPartner != null ) {
      if( partner.getChildCount() != nextPartner.getChildCount() )
        throw new MergeConflict("Multiple possible partners, different structures",partner,nextPartner);
      System.err.println("WARNING: Skipping possible partner child equality check");
        //      for( int i=0;i<partner.getChildCount();i++)
//        if( partner.getChild(i).partner != nextPartner.getChild(i).partner
      nextPartner = inMatching.getNextMapNode();
    }
    return partner;
  }

  // Checks that nodes deleted under n1 are also deleted under n2
  public void mergeDeleted( MapNode n1, TreeMatching otherTree ) {
    if( n1 == null )
      return; // Nothing to do
    for( int i = 0;i<n1.getOpCount();i++) {
      NodeOp no = n1.getOperation(i);
      if( no instanceof DeleteOp ) {
        DeleteOp delop = (DeleteOp) no;
        MapNode n2 = otherTree.getFirstMapNode(delop.node); // Delop.node is in the base tree
          // if n2 is null, there is no MapNode corresponding to delop.node => it's deleted in the
          // other tree as well, and all is well
        while( n2 != null ) {
          // There are NodeOp's corresponding to this, we need to make sure they are loose!
          for( int iOp=0;i< n2.getOpCount();iOp++) {
            if( n2.getOperation(iOp).getState() != NodeOp.LOOSE )
              throw new MergeConflict("Updates in " + otherTree.name +" conflict with deletions in " +
                name, n1, n2);
/*  The code below is BS... The mapNodes found here CANNOT be involved in any other op than delete (since GLU is not allowed)
              T1  BASE       T2
                a -\   del--parent_a
                a --a /
                a -/

            n2.getOperation(iOp).setState( NodeOp.DONE ); // Mark this nodeOp done (as it was deleted, nothing needs to be done)
            setPartnerOpState( n2.getOperation(iOp), NodeOp.STRICT ); // Actually, this shouldn't be necessary
                                                                    // Tohave any effect, the node must have been copied
                                                                    // somewhere else, and copies are planned to be STRICT
*//*
          }
          n2 = otherTree.getNextMapNode();
        } // End while
      } // end if deleteop
    }// end loop over all ops
  }


  public void initOpStates(MapNode root,boolean inThisMatching, TreeMatching otherMatching) {
    TreeMatching matching = inThisMatching ? this : otherMatching;
    for(int i=0;i<root.getChildCount();i++) {
      MapNode child = root.getChild(i);
      child.inThisMatching = inThisMatching;
      NodeOp op = getStdOp(child);
      if( op instanceof CopyOp ) {
        matching.getFirstMapNode(((CopyOp) op).srcNode);
        if( matching.getNextMapNode() == null &&
          root.partner instanceof ElementNode &&
          ((ElementNode) root.partner).children.size() > i &&
          ((CopyOp) op).srcNode == ((ElementNode) root.partner).children.elementAt(i) )
          op.setState(NodeOp.LOOSE);
        else
          op.setState(NodeOp.STRICT);
      } else
        op.setState(NodeOp.STRICT);
      initOpStates( child, inThisMatching, otherMatching );
    }
  }
*/
  // Matching looping routines
  /**************** Handling baseDocToMap hashtable
   *
   */
/*
  // Adds matching a->b; usually called both ways to add b->a as well
  private void addBaseDocToMap( Node a, MapNode b ) {
    if( a == null )
      return; // Don't add null mappings! (when the partner of a MapNode is null,
              // there is no node in the basedoc that maps to the MapNode
    if( !baseDocToMap.containsKey(a) )
      baseDocToMap.put(a,b);
    else {
      // Uh-uh, already a match; we need to add it to a set/make a new set
      Object match = nodeMap.get(a);
      if( match instanceof Set )
        ((Set) match).add(b);
      else {
        // Purge the node, and put a fresh set with the two nodes
        Set nodes = new HashSet();
        nodes.add(match);
        nodes.add(b);
        baseDocToMap.remove(a);
        baseDocToMap.put(a,nodes);
      }
    }
  }

  private Iterator baseDocToMapIter = null;

  public MapNode getFirstMapNode( Node src ) {
    Object mapsto = baseDocToMap.get(src);
    baseDocToMapIter = null;
    if ( mapsto == null )
      return null;
    else if ( mapsto instanceof MapNode ) {
      return (MapNode) mapsto;
    } else {
      // maps to several
      nodeIter = ((Set) mapsto).iterator();
      if( nodeIter.hasNext() )
        return (MapNode) nodeIter.next();
      else
        return null;
    }
  }

  public MapNode getNextMapNode() {
    if( baseDocToMapIter == null || !baseDocToMapIter.hasNext() )
      return null;
    else
      return (MapNode) baseDocToMapIter.next();
  }

  /****
   * Printing XML from a mapping
   *
   */
/*
  public void printXML( PrintWriter pw, MapNode root, int level ) {
    NodeOp op = getStdOp(root);
/*    if( op == null )
      return;
*//*
    if( op instanceof InsertOp ) {
      ((InsertOp) op).node.printXML1( pw, level,false );
    } else if( op instanceof UpdateOp ) {
      ((UpdateOp) op).newNode.printXML1( pw, level,false );
    } else if( op instanceof CopyOp ) {
      ((CopyOp) op).srcNode.printXML1( pw, level,false );
    }
    for( int i = 0; i< root.getChildCount(); i++ )
      printXML( pw, root.getChild(i),level + 1);

    if( op instanceof InsertOp ) {
      ((InsertOp) op).node.printXML2( pw, level );
    } else if( op instanceof UpdateOp ) {
      ((UpdateOp) op).newNode.printXML2( pw, level );
    } else if( op instanceof CopyOp ) {
      ((CopyOp) op).srcNode.printXML2( pw, level );
    }

  }
*/
//}