// $Id: Merge.java,v 1.22 2001/06/06 21:44:18 ctl Exp $

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Merge {

  TriMatching m = null;
  ConflictLog clog = new ConflictLog();

  public Merge(TriMatching am) {
    m = am;
  }

  public ConflictLog getConflictLog() {
    return clog;
  }

  public void merge( ContentHandler ch ) throws SAXException {
    ch.startDocument();
    mergeNode( m.getLeftRoot(), m.getRightRoot(), ch );
    ch.endDocument();
  }

  int debug = 0; // Debug variable

  public void mergeNode( BranchNode a, BranchNode b, ContentHandler ch ) throws SAXException {
    if( (a != null && ((a.getBaseMatchType() | BranchNode.MATCH_CHILDREN) == 0 )) ||
        (b != null && ((b.getBaseMatchType() | BranchNode.MATCH_CHILDREN) == 0 ) ) )
      throw new RuntimeException("mergeNode: match type should be match children, otherwise the node should be null!");
    MergeList mlistA = a != null ? makeMergeList( a ) : null;
    MergeList mlistB = b != null ? makeMergeList( b ) : null;
    MergePairList merged = null;
    clog.enterSubtree();
//    System.out.println("A = " + a ==null ? "-" : a.getContent().toString());
//    System.out.println("B = " + b ==null ? "-" : b.getContent().toString());
//    System.out.println("--------------------------");


/*
  if( mlistA.getEntryCount() > 30 )
      debug=1;
*/
    if(debug>0 || mlistA.getEntryCount()>40) {
    System.out.println("Merge A list");
    if( mlistA != null )
      mlistA.print();
    else
      System.out.println("--none--");
    System.out.println("Merge B list");
    if( mlistB != null )
      mlistB.print();
    else
      System.out.println("--none--");
    }
    if( mlistA != null && mlistB != null )
      merged = mergeLists( mlistA, mlistB ); // Merge lists
    else
      merged = mergeListToPairList( mlistA == null ? mlistB : mlistA, null );

    // Now, the merged list is in merged
    // Handle updates & Recurse
    for( int i=0;i<merged.getPairCount();i++) {
      MergePair mergePair = merged.getPair(i);
      XMLNode mergedNode = cmerge( mergePair );
      if( mergedNode instanceof XMLTextNode ) {
        XMLTextNode text = (XMLTextNode) mergedNode;
        ch.characters(text.getText(),0,text.getText().length);
      } else {
        // It's an element node
        XMLElementNode mergedElement = (XMLElementNode) mergedNode;
        ch.startElement(mergedElement.getNamespaceURI(),mergedElement.getLocalName(),mergedElement.getQName(),mergedElement.getAttributes());
        // Figure out partners for recurse
        MergePair recursionPartners = getRecursionPartners( mergePair );
        // Recurse!
        mergeNode(recursionPartners.getFirstNode(),recursionPartners.getSecondNode(),ch);
        ch.endElement(mergedElement.getNamespaceURI(),mergedElement.getLocalName(),mergedElement.getQName());
      }
      clog.nextChild();
    }
    clog.exitSubtree();
  }

  private XMLNode cmerge( MergePair mp ) {
    // Merge contents of node and partner (but only if there's a partner)
    //-------------------
    // Table
    // n1    n2     Merge
    // any   null   n1
    // cont  cont   merge(n1,n2)
    // cont  str    n2
    // cont  full   merge(n1,n2)
    // str   str    FORCED content merge
    // str   full   n1 cont
    // full  full   merge(n1,n2)

    BranchNode n1 = mp.getFirstNode(), n2 = mp.getSecondNode();
    if( n1 == null || n2==null )
      return (n1==null ? n2 : n1).getContent();
    else if( n1.isMatch(BranchNode.MATCH_CONTENT) ) {
      if( !n2.isMatch(BranchNode.MATCH_CONTENT) )
        return n2.getContent();
      else
        return mergeNodeContent( n1, n2 );
    } else {
       // n doesn't match content
      if( n2.isMatch(BranchNode.MATCH_CONTENT) )
        return n1.getContent();
      else // Neither matches content => forced merge
        return mergeNodeContent( n1, n2 );
    }
  }

  private MergePair getRecursionPartners(MergePair mp) {
    BranchNode n1 = mp.getFirstNode(), n2 = mp.getSecondNode();
    if( n1 == null || n2 == null ) {
      // No pair, so just go on!
      return mp;
    } else {
      // We have a pair, do as the table in the thesis says:
      // n1    n2     Merge
      // any   -      n1,-
      // -     any    -,n2
      // str   str    n1,n2
      // str   cont   -,n2
      // cont  str    n1,-
      // cont  cont   n1,n2 (FORCED)
      if( n1.isMatch(BranchNode.MATCH_CHILDREN) && n2.isMatch(BranchNode.MATCH_CHILDREN) )
        return mp;
      else if( n1.isMatch(BranchNode.MATCH_CHILDREN) && n2.isMatch(BranchNode.MATCH_CONTENT) )
        return new MergePair(n2,null);
      else if( n1.isMatch(BranchNode.MATCH_CONTENT) && n2.isMatch(BranchNode.MATCH_CHILDREN) )
        return new MergePair(n1,null);
      else // Both content matches --> forced merge
        return mp;

    }
  }

  private XMLNode mergeNodeContent( BranchNode a, BranchNode b ) {
    boolean aUpdated = !matches( a, a.getBaseMatch() ),
            bUpdated = !matches( b, b.getBaseMatch() );
    if( aUpdated && bUpdated ) {
        System.out.println(a.isLeftTree() + ": " + a.getContent().toString() );
        System.out.println(b.isLeftTree() + ": " + b.getContent().toString() );

      if( matches( a, b ) ) {
        clog.addNodeWarning(ConflictLog.UPDATE,"Node updated in both branches, but updates are equal",
          a.getBaseMatch(),a,b);
        return a.getContent();
      } else {

        //
        // CONFLICTCODE here
        // if XMLElementNode try merging attributes if XMLTextnode give up
        clog.addNodeConflict(ConflictLog.UPDATE,"Node updated in both branches, using branch 1",
          a.getBaseMatch(),a,b);
/*
        System.out.println("CONFLICT; Node updated in both branches, picking first one:");
        System.out.println(a.getContent().toString());
        System.out.println(b.getContent().toString());
*/
        return a.isLeftTree() ? a.getContent() : b.getContent();
      }
    } else if ( bUpdated )
      return b.getContent();
    else
      return a.getContent(); // A modified, or none modified, a is ok in both cases
  }

  private MergeList makeMergeList( BranchNode parent ) {
    MergeList ml = new MergeList(parent);
    if( parent.getBaseMatch() == null ) {
      // The parent is unmatched, treat all nodes as inserts/n:th copies
      ml.add( START, false );
      for( int i = 0;i<parent.getChildCount();i++)
        ml.addHangOn( parent.getChild(i), true );
      ml.lockNeighborhood(0,1);
      ml.add( END, false );
      return ml;
    }
    Map baseMatches = new HashMap();
    int prevChildPos = -1; // Next is always prevChildPos + 1, so the first should be 0 =>
                           // init to -1, -2 means not found in base
    int childPos = -1;
    ml.add( START, false );
    for( int i = 0;i<parent.getChildCount();i++) {
      BranchNode current = parent.getChild(i);
      BaseNode match = current.getBaseMatch();
      if( match == null ) {
        // It's an insert node
        ml.addHangOn( current, true );
        ml.lockNeighborhood(0,1);
      } else if( match.getParent() != parent.getBaseMatch() ) {///          childPos == -1 ) {
        // Copied from elsewhere
        ml.addHangOn( current, !matches( match, current) );
        ml.lockNeighborhood(0,1);
      } else if ( baseMatches.containsKey( match ) ) {
        // current is the n:th copy of a node (n>1)
        ml.addHangOn( current, !matches( match, current) );
        Integer firstPos = (Integer) baseMatches.get(match);
        if( firstPos != null ) {
          // Lock the first occurenece as well
//TEMP UNCOMMNETED FOR review/p2          ml.lockNeighborhood(firstPos.intValue(),1,1);
          baseMatches.put(match,null);  // Put null into hashtable, so we won't lock more than once
                                        // (it wouldn't hurt, but just to be nice)
        }
        ml.lockNeighborhood(0,1);
      } else {
        // Found in base, check for moves
        ml.add( current,  !matches( match, current) );
        baseMatches.put( match, new Integer( ml.tailPos ) );
        childPos = match.getChildPos();
        childPos = childPos == -1 ? -2 : childPos; // Remember to set not found to -2
        if( (prevChildPos + 1) != childPos ) { // Out of sequence, lock previous and this
                                                          // e.g. -1 0 1 3 4 5 => 1 & 3 locked


          boolean moved = false;
          // Possibly out of sequence.. check of nodes between prev
          if( prevChildPos != -2 && childPos != -2 && prevChildPos < childPos ) {
            // Not moved if every node between prevChildPos+1 and childPos-1 (ends included) is
            // deleted
            // SLOWCODE, should be optimized
            for(int j=0;!moved && j<parent.getChildCount();j++) {
              BaseNode aBase = parent.getChild(j).getBaseMatch();
              int basePos = aBase == null ? -1 : aBase.getChildPos();
              if( basePos != -1 && basePos > prevChildPos && basePos < childPos )
                moved = true;
            }
          } else
            moved = true;
          if( moved ) {
            ml.lockNeighborhood(1,0);
            ml.setMoved(true);
          } else
            ml.setMoved(false);
        }
        prevChildPos = childPos;
      } // end if found in base
    }
    ml.add( END, false );
    if( (prevChildPos + 1 )!= parent.getBaseMatch().getChildCount() )  ///parentPartnerChildCount )
      ml.lockNeighborhood(1,0); // Possible end shock, e.g. -1 0 1 2 4=e, and 4 children in parent
                                //                                        i.e. ix 3 was deleted
    return ml;
  }

  class MergePair {
    BranchNode first,second;
    MergePair( BranchNode aFirst, BranchNode aSecond ) {
      first = aFirst;
      second = aSecond;
    }

    public BranchNode getFirstNode() {
      return first;
    }

    public BranchNode getSecondNode() {
      return second;
    }

  }

  class MergePairList {
    Vector list = new Vector();
    public void append( BranchNode a, BranchNode b ) {
      list.add(new MergePair(a,b));
    }

    public int getPairCount() {
      return list.size();
    }

    public MergePair getPair(int ix){
      return (MergePair) list.elementAt(ix);
    }
  }

  private MergePairList mergeListToPairList( MergeList mlistA, MergeList mlistB ) {
    MergePairList merged = new MergePairList();
    for( int i=0;i<mlistA.getEntryCount()-1;i++) { // -1 due to end symbol
      MergeEntry me = mlistA.getEntry(i);
      if( i > 0) { // Don't append __START__
        merged.append(me.getNode(),me.getNode().getFirstPartner(BranchNode.MATCH_FULL));
      }
      for( int ih=0;ih<me.getHangonCount();ih++) {
        BranchNode hangon=me.getHangon(ih).getNode();
        merged.append(hangon,hangon.getFirstPartner(BranchNode.MATCH_FULL));
      }
      if( mlistB != null ) {
        MergeEntry pair = mlistB.getEntry(mlistB.findPartner(me));
        if( pair != null && !checkOtherHangons(me,pair,mlistA,mlistB) ) {
          for(int ih=0;i<pair.getHangonCount();ih++) {
            BranchNode hangon = pair.getHangon(ih).getNode();
            merged.append(hangon,hangon.getFirstPartner(BranchNode.MATCH_FULL));
          }
        }
      }
    }
    return merged;
  }

  public MergePairList mergeLists( MergeList mlistA, MergeList mlistB ) {
    MergePairList merged = new MergePairList();
    mergeDeletedOrMoved( mlistA, mlistB );
/*
    System.out.println("A list (after delormove):");
    mlistA.print();
    System.out.println("B list (after delormove):");
    mlistB.print();
*/
    // Now we should have exactly the same entries in mlistA and mlistB
    // quick check
    if( mlistA.getEntryCount() != mlistB.getEntryCount() )
        throw new RuntimeException("ASSERTION FAILED: MergeList.merge(): lists different lengths!");

    int posA = 0, posB = 0, logchildpos=-1;
    MergeEntry ea = mlistA.getEntry(posA), eb= mlistB.getEntry(posB);
    while(true) {
      // Dump hangons from ea... (we do this first as __START__ may have hangons)
      for(int i=0;i<ea.getHangonCount();i++) {
        BranchNode na = ea.getHangon(i).getNode();
        merged.append(na,na.getFirstPartner(BranchNode.MATCH_FULL));
      }
      // And then from eb..
      if( eb.getHangonCount() > 0 ) {
        // Append b hangons (unless they were equal to the hangons of ea)
        if( !checkOtherHangons(ea,eb,mlistA,mlistB) ) {
          for(int i=0;i<eb.getHangonCount();i++) {
            BranchNode nb = eb.getHangon(i).getNode();
            merged.append(nb,nb.getFirstPartner(BranchNode.MATCH_FULL));
          }
        }
      }
      // --end hangon dump
      int nextA=-1,nextB=-1;
      // figure out the next one
      nextA = ea.locked && mlistA.getEntry(posA+1).locked ? posA + 1 : -1; // -1 means free
      nextB = eb.locked && mlistB.getEntry(posB+1).locked ? posB + 1 : -1;
      if( nextA == -1 && nextB == -1 ) { // No locking, just let both go forward
        nextA = posA + 1;
        nextB = posB + 1;
      }
      // Handle free positions
      if( nextB == -1 )
        nextB = mlistB.findPartner(mlistA.getEntry(nextA));
      else if (nextA == -1 )
        nextA = mlistA.findPartner(mlistB.getEntry(nextB));
      else if (nextB != mlistB.findPartner(mlistA.getEntry(nextA))) {
        // add CONFLICTCODE here
        clog.addListConflict( ConflictLog.MOVE,"Conflicting moves inside child list, using the sequencing of branch 1",
          ea.getNode().getBaseMatch(),ea.getNode(),eb.getNode()/*, mlistA, mlistB*/ );
        return mergeListToPairList(ea.getNode().isLeftTree() ? mlistA : mlistB,
          ea.getNode().isLeftTree() ? mlistB : mlistA);
      }
      posA = nextA;
      posB = nextB;
      ea = mlistA.getEntry(posA);
      eb = mlistB.getEntry(posB);
      // See if we're done
      if( ea.node == END || eb.node == END ) {
        if( ea.node != eb.node )
          throw new RuntimeException("ASSERTION FAILED: Merge.mergeLists(). Both cursors not at end");
        break;
      }
      // pos is set up so that ea and eb are merge-partners
      merged.append(ea.getNode(),eb.getNode());
      logchildpos = merged.getPairCount()-1;
    }
    return merged;
  }

  private boolean checkOtherHangons(MergeEntry ea, MergeEntry eb, MergeList mla, MergeList mlb ) {
    boolean hangonsAreEqual = false;
    if( ea.getHangonCount() > 0 ) {
      // Check if the hangons match _exactly_ (no inserts, and exactly same sequence of copies)
      // Then we can include the hangons just once. This resembles the case when content of
      // two nodes has been updated the same way... not a conflict, but maybe suspicious
      if( eb.getHangonCount() == ea.getHangonCount() ) {
        hangonsAreEqual = true;
        for(int i=0;hangonsAreEqual && i<ea.getHangonCount();i++)
          hangonsAreEqual = matches( eb.getHangon(i).getNode(),  ea.getHangon(i).getNode() );
      }
      // Both have hangons,
      // add CONFLICTCODE here
      // for now, chain A and B hangons
      if( hangonsAreEqual )
        // Need: add with explicit childpos
        // How should we encode the inserts, i.e. tell which nodes were inserted
        clog.addListWarning(ConflictLog.INSERT,"Equal insertions/copies in both branches after the context nodes.",
          ea.getNode().getBaseMatch() != null ? ea.getNode().getBaseMatch() : eb.getNode().getBaseMatch(),
          ea.getNode(), eb.getNode() /*, mla, mlb*/ );
        //System.out.println(); // as updated(or A if no update)-Other");
      else
        clog.addListWarning(ConflictLog.INSERT,"Insertions/copies in both branches after the context nodes. Sequencing the insertions.",
          ea.getNode().getBaseMatch() != null ? ea.getNode().getBaseMatch() : eb.getNode().getBaseMatch(),
          ea.getNode(), eb.getNode() /*, mla, mlb*/ );

//            System.out.println("CONFLICTW; both nodes have hangons; sequencing them"); // as updated(or A if no update)-Other");
/*        System.out.println("First list:");
      mlistA.print();
      System.out.println("Second list:");
      mlistB.print();*/
    }
    return hangonsAreEqual;
  }

  private static final int NOP = 1;
  private static final int MOVE_I = 2;
  private static final int MOVE_F = 3;
  private static final int DELETE = 4;

  // Tells what happens to bn in ml

  private int getOperation( BaseNode bn, MergeList ml ) {
    int mlPos = ml.matchInList(bn);
    if( mlPos == -1 ) {
      // Movef or delete
      MatchedNodes copiesInThisTree = null;
      if( ml.getEntryParent().isLeftTree() )
        copiesInThisTree = bn.getLeft();
      else
        copiesInThisTree = bn.getRight();
      if( copiesInThisTree.getMatches().isEmpty() )
        return DELETE;
      else
        return MOVE_F;
    } else {
      if( ml.getEntry(mlPos).isMoved() )
        return MOVE_I;
      else
        return NOP;
    }
  }


  private void mergeDeletedOrMoved( MergeList mlistA, MergeList mlistB ) {
    BaseNode baseParent = mlistA.getEntryParent().getBaseMatch();
    for( int i=0;i<baseParent.getChildCount();i++) {
      BaseNode bn = baseParent.getChild(i);
      int op1 = getOperation( bn, mlistA ),
          op2 = getOperation( bn, mlistB );
      // Swap ops, so that op1 is always the smaller (to simplify the if clauses)
      if( op1 > op2 ) {
        int t=op1; op1=op2; op2=t;
        MergeList tl = mlistA; mlistA = mlistB; mlistB = tl;
      }
//      System.out.println( op1 + " " + op2 + ": " + bn.getContent().toString() );
      /*************************************************************
      * Table to implement; mlistA is for op1 and mlistB for op2
      * Op1     Op2
      * NOP     NOP     OK
      * NOP     MOVE_I  OK, internal moves are handled by the merging of the lists
      * NOP     MOVE_F  Delete the node from mlistA
      * NOP     DELETE  Delete the node from mlistA
      * MOVE_I  MOVE_I  OK, internal moves are handled by the merging of the lists
      * MOVE_I  MOVE_F  Conflicting moves
      * MOVE_I  DELETE  Conflict - node is deleted and moved
      * MOVE_F  MOVE_F  Possibly conflict, see the code below
      * MOVE_F  DELETE  Conflict - node is deleted and moved
      * DELETE  DELETE  OK
      ***************************************************************/
      if( (op1==NOP && op2==NOP ) ||
          (op1==NOP && op2==MOVE_I ) ||
          (op1==MOVE_I && op2==MOVE_I ) ||
          (op1==DELETE && op2==DELETE ) )
        continue; // All OK cases
      if( op1 == NOP && ( op2 == MOVE_F || op2 == DELETE ) ) {
        // Delete the node from mlistA
        int ix = mlistA.matchInList(bn);
        if( isDeletiaModified(mlistA.getEntry(ix).getNode(),mlistA) )
          // CONFLICTCODE here
          clog.addListWarning( ConflictLog.UPDATE, "Modifications in deleted subtree.",
          bn,mlistA.getEntry(ix).getNode(),null);

//          System.out.println("CONFLICTW: Modifications in deleted subtree.");

        if( mlistA.getEntry(ix).getHangonCount() > 0 ) {
          // we need to move the hangons to the predecessor
          for( int ih = 0; ih < mlistA.getEntry(ix).getHangonCount(); ih++)
            mlistA.getEntry(ix-1).addHangon(mlistA.getEntry(ix).getHangon(ih));
        }
        mlistA.removeEntryAt(mlistA.matchInList(bn));
      } else if( op1 == MOVE_I && op2== MOVE_F ) {
        // CONFLICTCODE here
        BranchNode op1node = mlistA.getEntry( mlistA.matchInList(bn) ).getNode();
        clog.addListConflict( ConflictLog.MOVE,
        "Node moved to different locations - trying to recover by ignoring move inside childlist (copies and inserts immediately following the node may have been deleted)",
        bn,op1node,op1node.getFirstPartner(BranchNode.MATCH_FULL));
//        System.out.println("CONFLICT: Node moved to different locations - moving on by ignoring MOVE_I + hangons!");
        mlistA.removeEntryAt(mlistA.matchInList(bn));
      } else if( op1 == MOVE_I && op2== DELETE ) {
        // CONFLICTCODE here
        clog.addListConflict( ConflictLog.MOVE,
        "Node moved and deleted - trying to recover by deleting the node (copies and inserts immediately following the node may also have been deleted)",
        bn,mlistA.getEntry( mlistA.matchInList(bn) ).getNode(), null );
//        System.out.println("CONFLICT: Node moved and deleted - moving on by deleting the node + hangons!. ");
        mlistA.removeEntryAt(mlistA.matchInList(bn));
      } else if( op1 == MOVE_F && op2 == MOVE_F ) {
        if( isMovefMovefConflict( bn ) ) {
          // CONFLICTCODE here
          clog.addListConflict( ConflictLog.MOVE,"The node was moved to different locations. It will appear at each location.",
          bn,bn.getLeft().getFullMatch(),bn.getRight().getFullMatch() );
//          System.out.println("CONFLICT: Node is far-moved to two different locations. " +
//                "Implicit copies will occur in the output.");
        }
      } else if (op1 == MOVE_F && op2 == DELETE ) {
          // CONFLICTCODE here
          clog.addListConflict( ConflictLog.MOVE,"The node was moved and deleted. Ignoring the deletion.",
          bn,bn.getLeft().getFullMatch(),bn.getRight().getFullMatch() );
//          System.out.println("CONFLICT: Node is far-moved and deleted. The far-moved copy will persist.");
      }
    }
  }

  // Check if the deletia rooted at n is modified w.r.t. base

  private boolean isDeletiaModified(BranchNode n, MergeList ml) {
    BaseNode m = n.getBaseMatch();
    if( m == null )
      return true; // the node was inserted => modified
    if( getOperation(m,ml) != NOP ) // Notice that we check for move instantly, dut updates only when
                                    // we know the node was deleted. This is because moves are
                                    // visible on the previous level w.r.t. the updates
      return true; // the node has been moved
    if( n.getBaseMatchType() != BranchNode.MATCH_FULL )
      return true; // either structural or content modification (otherwise match would be full!)
    boolean deletedInOther = n.getPartners().getMatches().isEmpty(); // NOTE: By definition of the merge
                                                                     // matching, at least one match is full!
    if( deletedInOther ) {
      if( !matches( n, m ) )
        return true; // The node is updated
      // Check children
      MergeList mlistN = makeMergeList(n);
      for( int i=1;i<n.getChildCount();i++) { // Strange ixes because we ignore start & end symbols
        if( isDeletiaModified( n.getChild(i), mlistN ) )
          return true;
      }
      return false; // got trough the children (recursively), no modifications
    } else
      return false; // No modification here, and no recurse needed (the node has a structmatch in the other tree)
  }

  private boolean isMovefMovefConflict( BaseNode n ) {
    return _isMovefMovefConflict( n, n.getRight().getMatches(), n.getLeft().getMatches() ) ||
          _isMovefMovefConflict( n, n.getLeft().getMatches(), n.getRight().getMatches() );
  }

  // Check if base node n is moved under matching parents. If so, that is good, because
  // the list merge will handle possible conflicts. If they are not moved under
  // matching parents we have a conflict. (which unresolved results in two copies)

  // Specifically the condition is that the parent of each BranchNode bn is moved to
  // (structurally or content) must structurally match another BranchNode that has bn
  // as a child.

  // Although called rarely, this code is really horribly slow --- O(n^3)?!
  // Should maybe be trimmed to avoid having a really lousy boundary on the algorithm
  private boolean _isMovefMovefConflict( BaseNode n, Set matchesA, Set matchesB ) {
    for( Iterator i = matchesB.iterator(); i.hasNext(); ) {
      BranchNode bnA = (BranchNode) i.next();
      BranchNode bnAparent = bnA.getParent();
      if( (bnAparent.getBaseMatchType() & BranchNode.MATCH_CHILDREN) == 0)
        return true; // here's a copy with no structural match on the other side => conflict
      for( Iterator ip = bnAparent.getPartners().getMatches().iterator(); ip.hasNext(); ) {
        BranchNode bnBparent = (BranchNode) ip.next();
        boolean hasNasChild = false;
        for( int ic = 0; ic < bnBparent.getChildCount() && !hasNasChild;ic ++ )
          hasNasChild = matchesA.contains( bnBparent.getChild(ic) );
        if( hasNasChild && ( bnBparent.getBaseMatchType() & BranchNode.MATCH_CHILDREN ) == 0 )
          return true; // here's a copy with no structural match on the other side => conflict
      }
    }
    return false;
  }

//----------------------


  class HangonEntry {
    BranchNode node = null;
    boolean updated = false; // XXX will probably be obsolete
    HangonEntry( BranchNode an, boolean u ) {
      node = an;
      updated = u;
    }

    HangonEntry() {
    }

    public BranchNode getNode() {
      return node;
    }

    public String toString() {
      return (updated ? '*' : ' ')+node.getContent().toString();
    }
  }

  class MergeEntry extends HangonEntry {
//    BranchNode node = null;
//    Node basePartner = null;
    Vector inserts = new Vector();
    boolean locked = false;
    private BranchNode mergePartner = null;
    private boolean moved = false;

///    boolean updated = false; // XXX will probably be obsolete

    MergeEntry( BranchNode n, boolean upd ) {
      super(n,upd);
    }
/*      node = n;
      updated = upd;
    }
*/
    MergeEntry() {
    }


    public boolean isMoved() {
      return moved;
    }

    public void setMoved(boolean amoved ) {
      moved=amoved;
    }

    public void setMergePartner(BranchNode n) {
      mergePartner = n;
    }

    public BranchNode getMergePartner() {
      return mergePartner;
    }

    void print(int pos) {
      System.out.print(pos+": ");
      System.out.print(isMoved() ? 'm' : '-');
      System.out.print(locked ? '*' : '-');
      System.out.print(mergePartner!=null ? 'p' : '-');
      System.out.print(' ' + node.getContent().toString() + ' ');
      if( node.getChildCount() > 0 )
        System.out.print(' ' + node.getChild(0).getContent().toString() + ' ');
      System.out.println( inserts.toString() );
    }

    int getHangonCount() {
      return inserts.size();
    }

    HangonEntry getHangon( int ix ) {
      return (HangonEntry) inserts.elementAt(ix);
    }

    void addHangon( BranchNode n, boolean updated ) {
      addHangon( new HangonEntry( n,updated ) );
    }

    void addHangon( HangonEntry e ) {
      inserts.add( e );
    }

  }

  // Merge list start and end markers. Cleaner if they were in MergeList, but
  // Java doesn't allow statics in nested classes
  static BranchNode START = new BranchNode(/*null,-1,*/new XMLTextNode("__START__"));
  static BranchNode END = new BranchNode(/*null,-1,*/new XMLTextNode("__END__"));

  // TODO: START end END markers should be completely hidden if possible
  class MergeList {
    private Vector list = new Vector();
    private Map index = new HashMap(); // looks up Entry index based on base partner
    int tailPos = -1; // current tail pos
    private BranchNode entryParent = null; // Common parent of all entries
    private MergeEntry currentEntry = null;

    public MergeList( BranchNode anEntryParent ) {
      entryParent = anEntryParent;
    }

    public BranchNode getEntryParent() {
      return entryParent;
    }

    void add( MergeEntry n ) {
      tailPos++;
      ensureCapacity( tailPos + 1, false);
      if( list.elementAt(tailPos) != null )
        n.locked = ((MergeEntry) list.elementAt(tailPos)).locked;
      list.setElementAt(n,tailPos);
      index.put( n.node.getBaseMatch(), new Integer(tailPos));
      currentEntry = n;
    }

    void add( BranchNode n, boolean updated ) {
      add( new MergeEntry(n, updated ) );
/*      tailPos++;
      ensureCapacity( tailPos + 1);
      ((MergeEntry) list.elementAt(tailPos)).node = n;
//      ((MergeEntry) list.elementAt(currentPos)).basePartner = basePartner;
      ((MergeEntry) list.elementAt(tailPos)).updated = updated;
*/
    }

    void addHangOn( BranchNode n, boolean updated ) {
      getEntry(tailPos).addHangon(n, updated);
      currentEntry = null;
    }


    public void setMoved( boolean moved ) {
      currentEntry.setMoved( moved );
    }

    int getEntryCount() {
      return tailPos + 1;
    }

    MergeEntry getEntry( int ix ) {
      return (MergeEntry) list.elementAt(ix);
    }

    // FIX: Needs to rebuild the index-- this needs to be fixed with lazy deletions
    // and an aggregated index rebuild. or maybe we could make the index use pointers
    void removeEntryAt( int ix ) {
      list.removeElementAt(ix);
      tailPos--;
      index.clear();
      for( int i=0;i<getEntryCount();i++)
        index.put( getEntry(i).node.getBaseMatch(), new Integer(i));
    }

    void lockNeighborhood(  int left, int right ) {
      lockNeighborhood( tailPos, left, right );
    }

    void lockNeighborhood( int acurrentPos, int left, int right ) {
      ensureCapacity( acurrentPos + right + 1, true);
      for( int i = acurrentPos - left; i<=acurrentPos + right; i++)
        ((MergeEntry) list.elementAt(i)).locked = true;
    }

    public int findPartner( MergeEntry b ) {
      if( b.node == START )
        return 0;
      else if( b.node == END )
        return getEntryCount() - 1; // Assuming the other list is equally long
      return ((Integer) index.get( b.node.getBaseMatch() )).intValue();
    }

    public int matchInList( BaseNode n ) {
      Integer i = (Integer) index.get( n );
      if( i == null )
        return -1;
      else
        return i.intValue();
    }

/*
    class ExpandingIterator implements Iterator {
      int mainPos = -1, // -1 here means no next pos
          hangonPos = -1;

      ExpandingIterator() {
        // !!! NOTE: Set up so as to start from node succeeding START
        mainPos = getEntryCount() > 1 ? 0 : -1; // 2 is the minimum entry count = START,END
        if( mainPos != -1 && getEntry(0).getHangonCount() == 0)
          mainPos = getEntryCount() > 2 ? 1 : -1; // Works assuming the END node cannot have hangons
                                                  // and it shouldnt
        else
          hangonPos = 0;
      }

      public boolean hasNext() {
        return mainPos != -1;
      }

      public Object next() throws NoSuchElementException {
        if( mainPos == -1 )
          throw new NoSuchElementException();
        Object item = null;
        if( hangonPos == -1 )
          item = getEntry(mainPos); //.node;
        else
          item = getEntry(mainPos).getHangon(hangonPos); //.node;
        calcNextPos();
        return item;
      }

      public void remove()  {
        throw new java.lang.UnsupportedOperationException();
      }

      private void calcNextPos() {
        hangonPos++;
        if( hangonPos >= getEntry(mainPos).getHangonCount() ) {
          hangonPos = -1;
          mainPos++;
        }
        if( mainPos >= getEntryCount() -1 ) // -1 to stop when we get to END marker
          mainPos=-1;
      }
    }

    public Iterator getExpandingIterator() {
      return new ExpandingIterator();
    }

*/
    private void ensureCapacity( int size, boolean fill ) {
      for( int i = list.size(); i < size; i ++ )
        list.add( fill ? new MergeEntry() : null);
    }


    void print() {
      int pos = 0;
      MergeEntry me = null;
      do {
        me = (MergeEntry) list.elementAt(pos);
        me.print(pos);
        pos++;
      } while( me.node != END );
    }
  }

  //
  //
  // Utility functions
  //
  protected boolean matches( Node a, Node b ) {
    if( a== null || b==null)
      return false;
/*    if( a.getContent() == null )
      throw new RuntimeException("NULL content?");*/
    return a.getContent().contentEquals(b.getContent());
  }

}

