// $Id: Merge.java,v 1.4 2001/03/21 19:15:13 ctl Exp $

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
    if( ((a.getBaseMatchType() | BranchNode.MATCH_CHILDREN) == 0 ) ||
        ((a.getBaseMatchType() | BranchNode.MATCH_CHILDREN) == 0 ) )
      throw new RuntimeException("mergeNode: match type should be match children, otherwise the node should be null!");
    MergeList mlistA = a != null ? makeMergeList( a ) : null;
    MergeList mlistB = b != null ? makeMergeList( b ) : null;
    System.out.println("Merge A list");
    mlistA.print();
    System.out.println("Merge B list");
    mlistB.print();

    if( mlistA != null && mlistB != null ) {
      mlistA = mergeLists( mlistA, mlistB ); // Merge lists
    } else if( mlistA == null ) {
      mlistA = mlistB;
      mlistB = null; // safety precaution
    }
    // Now, the merged list is in mlistA
    // Handle updates & Recurse
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
        BranchNode ca = (n.getBaseMatchType() & BranchNode.MATCH_CHILDREN ) != 0 ? n : null;
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
        System.out.println(a.toString());
        System.out.println(b.toString());
        return a.getContent();
      }
    } else if ( bUpdated )
      return b.getContent();
    else
      return a.getContent(); // A modified, or none modified, a is ok in both cases
  }

  private MergeList makeMergeList( BranchNode parent ) {
    MergeList ml = new MergeList(parent);
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
      MergeList merged = new MergeList();
      mergeDeletedOrMoved( mlistA, mlistB );

// LATER      mergeDeletedOrMovedOut( docA, docB, mlistA, mlistB, docBMatching );
/*
      System.out.println("DocA list:");
      mlistA.print();
      System.out.println("DocB list:");
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
            System.out.println("First list:");
            mlistA.print();
            System.out.println("Second list:");
            mlistB.print();
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
          // for now, follow sequencing of mlist A
          System.out.println("CONFLICT: Sequencing conflict, using only one list's sequencing");
          nextB = mlistB.findPartner(mlistA.getEntry(nextA)); ///getPartnerPos( mlistA.getEntry(nextA), mlistB, docB, docBMatching);
        }
        posA = nextA;
        posB = nextB;
      }
      System.out.println("Merged list:");
      merged.print();
      return merged;
    }

    private void mergeDeletedOrMoved( MergeList mlistA, MergeList mlistB ) {
      BaseNode baseParent = mlistA.getEntryParent().getBaseMatch();
      for( int i=0;i<baseParent.getChildCount();i++) {
        BaseNode bn = baseParent.getChild(i);
        boolean inListA = mlistA.matchInList(bn), inListB = mlistB.matchInList(bn);
        boolean deletedFromA = false, deletedFromB = false; // some code!!!!!! !inListA && bn.
        if( inListA && inListB )
          continue; // Present in both, everything OKi
        else if( deletedFromA && deletedFromB )
          continue; // Deleted in both, everything OK
        else if(
      }
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
    private BrancgNode entryParent = null; // Common parent of all entries

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
      ((MergeEntry) list.elementAt(tailPos)).inserts.add( new HangonEntry( n, updated ) );
    }


    int getEntryCount() {
      return tailPos + 1;
    }

    MergeEntry getEntry( int ix ) {
      return (MergeEntry) list.elementAt(ix);
    }

    void removeEntryAt( int ix ) {
      list.removeElementAt(ix);
      tailPos--;
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

    public boolean matchInList( BaseNode n ) {
      return index.get( n ) != null;
    }

    class ExpandingIterator implements Iterator {
      int mainPos = -1, // -1 here means no next pos
          hangonPos = -1;

      ExpandingIterator() {
        // !!! NOTE: Set up so as to start from node succeeding START
        mainPos = getEntryCount() > 2 ? 0 : -1; // 2 is the minimum entry count = START,END
        if( mainPos != -1 && getEntry(0).getHangonCount() == 0)
          mainPos = getEntryCount() > 1 ? 1 : -1;
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

