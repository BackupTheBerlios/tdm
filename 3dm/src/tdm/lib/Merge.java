// $Id: Merge.java,v 1.7 2001/03/26 14:44:44 ctl Exp $

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

  public Merge(TriMatching am) {
    m = am;
  }

  public void merge( ContentHandler ch ) throws SAXException {
    ch.startDocument();
    mergeNode( m.leftRoot, m.rightRoot, ch );
    ch.endDocument();
  }

  public void mergeNode( BranchNode a, BranchNode b, ContentHandler ch ) throws SAXException {
    if( (a != null && ((a.getBaseMatchType() | BranchNode.MATCH_CHILDREN) == 0 )) ||
        (b != null && ((b.getBaseMatchType() | BranchNode.MATCH_CHILDREN) == 0 ) ) )
      throw new RuntimeException("mergeNode: match type should be match children, otherwise the node should be null!");
    MergeList mlistA = a != null ? makeMergeList( a ) : null;
    MergeList mlistB = b != null ? makeMergeList( b ) : null;
/*
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
*/
    if( mlistA != null && mlistB != null ) {
      mlistA = mergeLists( mlistA, mlistB ); // Merge lists
      //System.out.println("Merged list:");
      //mlistA.print();
    } else if( mlistA == null ) {
      mlistA = mlistB;
      mlistB = null; // safety precaution
    }
    // Now, the merged list is in mlistA
    // Handle updates & Recurse
/*DEBUG    if( mlistA == null )
      System.out.flush();*/
    for( Iterator i = mlistA.getExpandingIterator();i.hasNext();) {
      BranchNode n = (BranchNode) i.next();
      BranchNode nPartner = n.getFirstPartner( BranchNode.MATCH_CONTENT );
      // Merge contents of node and partner (but only if there's partner)
      XMLNode mergedNode = nPartner != null ? mergeNodeContent( n, nPartner ) : n.getContent();
      if( mergedNode instanceof XMLTextNode ) {
        String text = ((XMLTextNode) mergedNode).getText();
        ch.characters(text.toCharArray(),0,text.length());
      } else {
        // It's an element node
        XMLElementNode mergedElement = (XMLElementNode) mergedNode;
        ch.startElement(mergedElement.getNamespaceURI(),mergedElement.getLocalName(),mergedElement.getQName(),mergedElement.getAttributes());
        // Figure out partners for recurse
        BranchNode ca = (n.getBaseMatch() == null)  || // if insert, or match children
                        (n.getBaseMatchType() & BranchNode.MATCH_CHILDREN ) != 0 ? n : null;
        BranchNode cb = n.getFirstPartner( BranchNode.MATCH_CHILDREN );
        // Recurse!
        mergeNode(ca,cb,ch);
        ch.endElement(mergedElement.getNamespaceURI(),mergedElement.getLocalName(),mergedElement.getQName());
      }

    }

  }


  private XMLNode mergeNodeContent( BranchNode a, BranchNode b ) {
/*    // First check if either is null
    if( a == null )
      return b.getContent();
    else if ( b == null )
      return a.getContent();
*/
    boolean aUpdated = !matches( a, a.getBaseMatch() ),
            bUpdated = !matches( b, b.getBaseMatch() );
    if( aUpdated && bUpdated ) {
      if( matches( a, b ) ) {
        System.out.println("CONFLICTW; Node updated in both branches, but updates are equal");
        return a.getContent();
      } else {
        //
        // CONFLICTCODE here
        // if XMLElementNode try merging attributes if XMLTextnode give up
        System.out.println("CONFLICT; Node updated in both branches, picking first one:");
        System.out.println(a.getContent().toString());
        System.out.println(b.getContent().toString());
        return a.getContent();
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
//--
    Set baseMatches = new HashSet();
/**    Node parentPartner = getFirstMapping( parent );
    int parentPartnerChildCount = parentPartner instanceof ElementNode ?
      ((ElementNode) parentPartner).getChildCount() : -1000;
*/
    int prevChildPos = -1; // Next is always prevChildPos + 1, so the first should be 0 =>
                           // init to -1
    ml.add( START, false );
    for( int i = 0;i<parent.getChildCount();i++) {
      BranchNode current = parent.getChild(i);
      BaseNode match = current.getBaseMatch();
/// xlat: partners = match
///      int childPos = partnerChildPos( parentPartner, partner  );
      if( match == null ) {
        // It's an insert node
        ml.addHangOn( current, true );
        ml.lockNeighborhood(0,1);
      } else if( match.getParent() != parent.getBaseMatch() ) {///          childPos == -1 ) {
        // Copied from elsewhere
        ml.addHangOn( current, !matches( match, current) );
        ml.lockNeighborhood(0,1);
      } else if ( baseMatches.contains( match ) ) {
        // current is the n:th copy of a node (n>1)
        ml.addHangOn( current, !matches( match, current) );
        ml.lockNeighborhood(0,1);
      } else {
        ml.add( current,  !matches( match, current) );
        baseMatches.add( match );
        if( (prevChildPos + 1) != match.getChildPos() )  ///childPos ) // Out of sequence, lock previous and this
                                             // e.g. -1 0 1 3 4 5 => 1 & 3 locked
          ml.lockNeighborhood(1,0);
        prevChildPos = match.getChildPos(); ///childPos;
      }
    }
    ml.add( END, false );
    if( (prevChildPos + 1 )!= parent.getBaseMatch().getChildCount() )  ///parentPartnerChildCount )
      ml.lockNeighborhood(1,0); // Possible end shock, e.g. -1 0 1 2 4=e, and 4 children in parent
                                //                                        i.e. ix 3 was deleted
    return ml;
  }


//-------- merging mergelists: this & other
// May modify mlistA by adding hangons from mlistB
  public MergeList mergeLists( MergeList mlistA, MergeList mlistB ) {
    MergeList merged = new MergeList(mlistA.getEntryParent());
    mergeDeletedOrMoved( mlistA, mlistB );

// LATER      mergeDeletedOrMovedOut( docA, docB, mlistA, mlistB, docBMatching );
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

    // So let's do the merge
    // Some things to prove:
    // No node can get "skipped" (deleted)
    // No node gets extra copies
    // Sequencing isn't violated if merge is successful
    int posA = 0, posB = 0, nextA=-1,nextB=-1;
    while(true) {
      // pos is set up so that ea and eb are merge-partners
      MergeEntry ea = mlistA.getEntry(posA), eb= mlistB.getEntry(posB);
//        MergeEntry chosenEn=ea,otherEn=eb;
      merged.add( ea );
      // Hangon nodes
      if( eb.inserts.size() > 0 ) {
        if( ea.inserts.size() > 0 ) {
          // Both have hangons,
          // add CONFLICTCODE here
          // for now, chain A and B hangons
          System.out.println("CONFLICTW; both nodes have hangons; sequencing them"); // as updated(or A if no update)-Other");
/*          System.out.println("First list:");
          mlistA.print();
          System.out.println("Second list:");
          mlistB.print();*/
        }
        for( int i=0;i<eb.inserts.size();i++)
          ea.inserts.add( eb.inserts.elementAt(i) );
      }
      // See if we're done
      if( ea.node == END || eb.node == END ) {
        if( ea.node != eb.node )
          throw new RuntimeException("ASSERTION FAILED: Merge.mergeLists(). Both cursors not at end");
        break;
      }
      // figure out the next one
      nextA = ea.locked && mlistA.getEntry(posA+1).locked ? posA + 1 : -1; // -1 means free
      nextB = eb.locked && mlistB.getEntry(posB+1).locked ? posB + 1 : -1;
      if( nextA == -1 && nextB == -1 ) { // No locking, just let both go forward
        nextA = posA + 1;
        nextB = posB + 1;
      }
      if( nextB == -1 )
        nextB = mlistB.findPartner(mlistA.getEntry(nextA));   /// getPartnerPos( mlistA.getEntry(nextA), mlistB, docB, docBMatching );
      else if (nextA == -1 )
        nextA = mlistA.findPartner(mlistB.getEntry(nextB)); ///getPartnerPos( mlistB.getEntry(nextB), mlistA, docA, this );
      else if (nextB != mlistB.findPartner(mlistA.getEntry(nextA))) { //           getPartnerPos( mlistA.getEntry(nextA), mlistB, docB, docBMatching ) ) {
        // add CONFLICTCODE here
        // This part is especially troublesome, as just using the sequencing of A may get us into
        // and infinite loop! (m5 rev 1.1!)
        // for now, follow sequencing of mlist A
        System.out.println("CONFLICT: Sequencing conflict, using only one list's sequencing");
        //nextB = mlistB.findPartner(mlistA.getEntry(nextA)); ///getPartnerPos( mlistA.getEntry(nextA), mlistB, docB, docBMatching);
        return mlistA;
      }
      posA = nextA;
      posB = nextB;
    }
/*    System.out.println("Merged list:");
    merged.print();*/
    return merged;
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
      // Swap ops, so that op1 is always the smaller (to simplyfy the if clauses)
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
          System.out.println("CONFLICTW: Modifications in deleted subtree.");

        if( mlistA.getEntry(ix).getHangonCount() > 0 ) {
          // we need to move the hangons to the predecessor
          for( int ih = 0; ih < mlistA.getEntry(ix).getHangonCount(); ih++)
            mlistA.getEntry(ix-1).addHangon(mlistA.getEntry(ix).getHangon(ih));
        }
        mlistA.removeEntryAt(mlistA.matchInList(bn));
      } else if( op1 == MOVE_I && op2== MOVE_F ) {
        // CONFLICTCODE here
        System.out.println("CONFLICT: Node moved to different locations - moving on by ignoring MOVE_I + hangons!");
        mlistA.removeEntryAt(mlistA.matchInList(bn));
      } else if( op1 == MOVE_I && op2== DELETE ) {
        // CONFLICTCODE here
        System.out.println("CONFLICT: Node moved and deleted - moving on by deleting the node + hangons!. ");
        mlistA.removeEntryAt(mlistA.matchInList(bn));
      } else if( op1 == MOVE_F && op2 == MOVE_F ) {
        if( isMovefMovefConflict( bn ) ) {
          // CONFLICTCODE here
          System.out.println("CONFLICT: Node is far-moved to two different locations. " +
                "Implicit copies will occur in the output.");
        }
      } else if (op1 == MOVE_F && op2 == DELETE ) {
          // CONFLICTCODE here
          System.out.println("CONFLICT: Node is far-moved and deleted. The far-moved copy will persist.");
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

    // FIXFIX: now a node is considered moved just beacuse it's locked.
    public boolean isMoved() {
      return locked;
    }

    void print() {
      System.out.print(updated ? 'x' : ' ');
      System.out.print(locked ? '*' : ' ');
      System.out.print(' ' + node.getContent().toString() + ' ');
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
  static BranchNode START = new BranchNode(null,-1,new XMLTextNode("__START__"));
  static BranchNode END = new BranchNode(null,-1,new XMLTextNode("__END__"));

  // TODO: START end END markers should be completely hidden if possible
  class MergeList {
    private Vector list = new Vector();
    private Map index = new HashMap(); // looks up Entry index based on base partner
    int tailPos = -1; // current tail pos
    private BranchNode entryParent = null; // Common parent of all entries

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
          item = getEntry(mainPos).node;
        else
          item = getEntry(mainPos).getHangon(hangonPos).node;
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


    private void ensureCapacity( int size, boolean fill ) {
      for( int i = list.size(); i < size; i ++ )
        list.add( fill ? new MergeEntry() : null);
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

