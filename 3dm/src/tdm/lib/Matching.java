// $Id: Matching.java,v 1.5 2001/04/26 17:27:16 ctl Exp $

import java.util.Vector;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.Collections;

public class Matching {

  protected BaseNode baseRoot = null;
  private BranchNode branchRoot = null;

  protected Matching() {
    // Only called from TriMatching constructor
  }

  public Matching(BaseNode abase, BranchNode abranch ) {
    baseRoot = abase;
    branchRoot = abranch;
    buildMatching( baseRoot, branchRoot );
  }

  protected void buildMatching( BaseNode base, BranchNode branch ) {
    match( base, branch );
    matchSamePosUnmatched( base, branch );
    removeSmallCopies(branch);
    setMatchTypes(base);
  }

  static int fccount=0;
  private void match( BaseNode base, BranchNode branch ) {
//    System.out.println("Finding cand for " +branch.getContent().toString());
    Vector candidates = findCandidates( base, branch ); // Find candidates for node branch in base
    // Find best trees
    //System.out.println("Finding best match");
    int bestCount = 0;
    CandidateEntry best = null;
    Vector bestCandidates = new Vector();
    for( Iterator i = candidates.iterator(); i.hasNext(); ) {
      CandidateEntry candidate = (CandidateEntry) i.next();
      // Bounus count of 1 for all candidates whose parents match the parent of the current node
//     int initCount = candidate.candidate.parent != null && branch.parent != null
//          && ((BranchNode) branch.parent).getBaseMatch() == candidate.candidate.parent ? 1 : 0;
      int initCount = 0;
      int thisCount = dfsMatch( candidate.candidate, branch, initCount, null );
      if( thisCount == bestCount )
        bestCandidates.add( candidate );
      else if( thisCount > bestCount ) {
        bestCount = thisCount;
        bestCandidates.clear();
        bestCandidates.add( candidate );
      }
    }
    //System.out.println("Resolving amb");
    // Resolve ambiguities if any exist...
    if( bestCandidates.size() > 1 ) {
      // Ambiguities - we need to find out which one is the best...
      // First calc all missing left-right correlations
      for( Iterator i = bestCandidates.iterator(); i.hasNext(); ) {
        CandidateEntry candidate = (CandidateEntry) i.next();
        if( candidate.leftRightDown < 0.0 ) {
/*          System.out.println("left="+getDistanceOfLeft(candidate.candidate,branch));
          System.out.println("right="+getDistanceOfRight(candidate.candidate,branch));
          System.out.println("clist="+measure.childListDistance(candidate.candidate,branch));
*/
          candidate.leftRightDown = Math.min(
            measure.childListDistance(candidate.candidate,branch),
            Math.min( getDistanceOfRight(candidate.candidate,branch),
              getDistanceOfLeft(candidate.candidate,branch)));
        }
      }
      Collections.sort(bestCandidates,candlrdComp);
      /*System.out.println("Best cands for " + branch.getContent().toString());
      for( Iterator i = bestCandidates.iterator(); i.hasNext(); ) {
        CandidateEntry ce = (CandidateEntry) i.next();
        System.out.println( ce.distance + "," + ce.leftRightDown + ": " + ce.candidate.getContent().toString());
      }*/
    }
    best = bestCandidates.isEmpty() ? null : (CandidateEntry) bestCandidates.elementAt(0);
    if( best!=null && (bestCount == 1 &&
      (Math.min(best.leftRightDown,best.distance) > 0.1 ||
        best.candidate.content.getInfoSize() < COPY_THRESHOLD )))
      best=null; //System.out.println("No candidate was very good, leaving unmatched" );
    // Add matchings and find nodes below matching subtree
    Vector stopNodes = new Vector();
    if( best != null )
      dfsMatch( best.candidate, branch, 0, stopNodes );
    else {
      // Unmatched
      for( int i=0;i<branch.getChildCount();i++)
        stopNodes.add(branch.getChild(i));
    }
    // Recurse
    //System.out.println("---Recurse");
    for( Iterator i=stopNodes.iterator();i.hasNext();)
      match(base,(BranchNode) i.next());
  }

  private void matchSamePosUnmatched( BaseNode base, BranchNode branch) {
    // Traverse in preorder -- to avoid building trees, just fixing levels where parents are matched
    for( int i=0;i<branch.getChildCount();i++)
      matchSamePosUnmatched(base,branch.getChild(i));

    BaseNode baseMatch = branch.getBaseMatch(); // old  baseparent
    if( baseMatch != null && baseMatch.getChildCount()>0 ) {
      // Scan for unmapped nodes
      for( int i=0;i<branch.getChildCount();i++) {
        BranchNode n = branch.getChild(i);
        BaseNode leftcand=null,rightcand=null;
        int lastBaseChild = baseMatch.getChildCount()-1;
        if( n.getBaseMatch() != null)
          continue; // Mapped, all is well
        // end points
        if( i == 0 && !baseMatch.getChild(0).isMatched() ){
          addMatching(n, baseMatch.getChild(0) );
          continue;
        } else if (i==branch.getChildCount()-1 && !baseMatch.getChild(lastBaseChild).isMatched()) {
          addMatching(n, baseMatch.getChild(lastBaseChild) );
          continue;
        }
        if( i > 0 ) {
          // See if node preceding n is matched, and its right neighbour unmatched
          // Base    xy     p=n's predecessor, x matches p and y unmatched
          // Branch  pn        => match y and n
          BaseNode x = branch.getChild(i-1).getBaseMatch();
          if( x != null && x.hasRightSibling() && !((BaseNode) x.getRightSibling()).isMatched()) {
            addMatching(n,(BaseNode) x.getRightSibling() );
            continue;
          }
        }
        if( i < branch.getChildCount()-1 ) {
          // See if node succeeding n is matched, and its right neighbour unmatched
          // Base    yx     p=n's succecessor, x matches p and y unmatched
          // Branch  np        => match y and n
          BaseNode x = branch.getChild(i+1).getBaseMatch();
          if( x != null && x.hasLeftSibling() && !((BaseNode) x.getLeftSibling()).isMatched()) {
            addMatching(n,(BaseNode) x.getLeftSibling() );
            continue;
          }
        }
      } // endfor
    } // endif
  }

  private Set removedMatchings = new HashSet();

  private void setMatchTypes( BaseNode base ) {
    // Postorder traversal, because we use the child mappings to orient us
    // therefore, this better change child mappings first!
    for(int i=0;i<base.getChildCount();i++)
      setMatchTypes(base.getChild(i));
    if( base.getLeft().getMatches().size() > 1 ) {
      // Has multiple matches, need to deterime type of each copy
      // Scan for primary copy...
      int minDist = Integer.MAX_VALUE;
      double minContentDist = Double.MAX_VALUE;
      BranchNode master = null;
      for( Iterator i=base.getLeft().getMatches().iterator();i.hasNext();) {
        BranchNode cand = (BranchNode) i.next();
        int dist = exactChildListMatch(base,cand) ? 0 : measure.matchedChildListDistance( base, cand );
        if( dist < minDist ) {
          minDist = dist;
          master = cand;
        } else if( dist == minDist ) {
          double cDist = measure.getDistance( base, cand );
          if( cDist < minContentDist ) {
            minContentDist = cDist;
            master = cand;
          }
        }
      }
      removedMatchings.clear();
      // Master is now the best match, which will be assigned as MATCH_FULL
      for( Iterator i=base.getLeft().getMatches().iterator();i.hasNext();) {
      BranchNode cand = (BranchNode) i.next();
      if( cand == master )
        continue;
      boolean structMatch = exactChildListMatch(base,cand);
      boolean contMatch = cand.getContent().contentEquals(base.getContent());
      if( !structMatch && !contMatch )
        removedMatchings.add( cand );
      else
        cand.setMatchType( (contMatch ? BranchNode.MATCH_CONTENT : 0) +
                          (structMatch ? BranchNode.MATCH_CHILDREN : 0) );
      }
      // Delete any removed matchings
      for( Iterator i = removedMatchings.iterator();i.hasNext();) {
        BranchNode cand = (BranchNode) i.next();
        delMatching( cand, cand.getBaseMatch() );
      }
    } // If node copied

  }

  private boolean exactChildListMatch( BaseNode base, BranchNode a) {
    if( a.getChildCount() != base.getChildCount() )
      return false;
    for(int i=0;i<a.getChildCount();i++) {
      if( base.getChild(i) != a.getChild(i).getBaseMatch() )
        return false;
    }
    return true;
  }

  static final int COPY_THRESHOLD = 32;
  static final int EDGE_BYTES = 8;

  private void removeSmallCopies( BranchNode root ) {
    BaseNode base = root.getBaseMatch();
    if( base != null && base.getLeft().getMatches().size() > 1 ) {
      int info = findCopyTree(root,base,false);
      if(  info < COPY_THRESHOLD ) {
        findCopyTree(root,base,true); // Too small a copy, remove matchings for it
//        System.out.println("XXX Removed small copy rooted at " + root.toString() );
      }
    }
    for(int i=0;i<root.getChildCount();i++)
      removeSmallCopies(root.getChild(i));
  }

  // return = 0 => a not copied
  private int findCopyTree( BranchNode a, BaseNode base, boolean remove ) {
    int info = 0;
    if( a.getBaseMatch() == base && base.getLeft().getMatches().size() > 1 ) {
      if( remove )
        delMatching(a,base);
      else
       info +=base.getContent().getInfoSize();  // a is a n:th copy of base
      if( a.getChildCount() == base.getChildCount() ) {
        for( int i=0;i<a.getChildCount();i++) {
          info += findCopyTree(a.getChild(i),base.getChild(i),remove);
          info += EDGE_BYTES; // 8 bytes for each edge
        }
      }
    }
    return info;
  }


  private Vector findCandidates( BaseNode tree, BranchNode key ) {
    // Find candidates for key
    //System.out.println("Fcand = " + (++fccount));
    Vector candidates = new Vector();
    findExactMatches( tree, key, candidates );
    if( candidates.isEmpty() )
      findFuzzyMatches( tree, key, candidates );
    return candidates;
  }

  final static double DFS_MATCH_THRESHOLD = 0.2;

  private int dfsMatch( BaseNode a, BranchNode b, int count, Vector stopNodes ) {
    if( stopNodes != null ) {
      // Also means matchings should be added
      addMatching( b, a );
    }
    boolean childrenMatch = true;
    if( a.getChildCount() == b.getChildCount() ) {
      // Only match children, if there are equally many
      for( int i=0; childrenMatch && i<a.getChildCount(); i ++ ) {
        childrenMatch = a.getChild(i).getContent().contentEquals(b.getChild(i).getContent());
        if( !childrenMatch ) {
          // Try fuzzy mathcing
          double distance = measure.getDistance( a.getChild(i), b.getChild(i) );
          childrenMatch =  distance < DFS_MATCH_THRESHOLD;
        }
      }
    } else
      childrenMatch = false;

    if( !childrenMatch ) {
      // Mark all children as stopnodes
      for( int i=0; stopNodes!=null && i<b.getChildCount(); i ++ )
        stopNodes.add( b.getChild(i) );
    } else {
      // All children match
      for( int i=0; i<a.getChildCount(); i ++ )
        count += dfsMatch( a.getChild(i), b.getChild(i), 0, stopNodes );
    }
    return count + 1;
  }

  private void findExactMatches( BaseNode tree, BranchNode key, Vector candidates ) {
    for( Iterator i = new DFSTreeIterator(tree);i.hasNext();) {
      BaseNode cand = (BaseNode) i.next();
      //System.out.println("Cand=" + cand.getContent().toString());
      if( cand.getContent().contentEquals(key.getContent()) )
        candidates.add( new CandidateEntry( cand, 0.0, -1.0 )); // -1.0 => lr not present
    }
  }

  static final double MAX_FUZZY_MATCH = 0.2;
  static final double PARENT_DISCOUNT = 0.5;

  private void findFuzzyMatches( BaseNode tree, BranchNode key, Vector candidates ) {
    SortedSet cset = new TreeSet(candComp);
    double cutoff = 2*MAX_FUZZY_MATCH;
    for( Iterator i = new DFSTreeIterator(tree);i.hasNext();) {
      BaseNode cand = (BaseNode) i.next();
      /* XPERboolean parentMatch = cand.parent != null && key.parent != null &&
        ((BranchNode) key.parent).getBaseMatch() == cand.parent;
      double discount = parentMatch ? PARENT_DISCOUNT : 1.0;*/
      double discount = 1.0;
      double lrdDist = discount*Math.min(
        Math.min(getDistanceOfLeft(key,cand),getDistanceOfRight(key,cand)),
        measure.childListDistance(key,cand));
      double minDist = discount*Math.min(lrdDist,measure.getDistance(cand,key));
      if( minDist < 2*MAX_FUZZY_MATCH ) {
        cset.add(new CandidateEntry(cand,minDist,lrdDist));
        cutoff = cutoff < 2*minDist ? cutoff : 2*minDist;
      }
    }
//    System.out.println("Fuzz set size=" + cset.size());
    for( Iterator i = cset.iterator();i.hasNext();) {
      CandidateEntry en = (CandidateEntry) i.next();
      if( en.distance > cutoff )
        break;
      else
        candidates.add(en);
    }
    //System.out.println("Fuzz set size=" + candidates.size());

  }

  private void measureMatching( BaseNode base, BranchNode br) {

  }

  static final double END_MATCH = Measure.MAX_DIST;
  static Measure measure = new Measure();

  protected double getDistanceOfLeft( Node a, Node b ) {
    if( a.parent == null || b.parent == null )
      return Measure.MAX_DIST;
    if( a.getChildPos() > 0 && b.getChildPos() > 0 ) {
      return measure.getDistance(a.getLeftSibling(), b.getLeftSibling() );
    } else
      return END_MATCH;
  }

  protected double getDistanceOfRight( Node a, Node b ) {
    if( a.parent == null || b.parent == null )
      return Measure.MAX_DIST;
    if( a.getChildPos() < a.parent.getChildCount() -1 && b.getChildPos() < b.parent.getChildCount() -1 ) {
      //DEBUG!
/*      double x = measure.getDistance(a.getRightSibling(), b.getRightSibling() );
      if( Double.isNaN(x) )
        measure.getDistance(a.getRightSibling(), b.getRightSibling() );*/
      //ENDDEBUG
      return measure.getDistance(a.getRightSibling(), b.getRightSibling() );
    } else
      return END_MATCH;
  }

  protected void addMatching( BranchNode a, BaseNode b ) {
    a.setBaseMatch(b,BranchNode.MATCH_FULL);
    b.getLeft().addMatch(a);
  }

  protected void delMatching( BranchNode a, BaseNode b ) {
    a.delBaseMatch();
    b.getLeft().delMatch(a);
  }

  class CandidateEntry {
    BaseNode candidate=null;
    double leftRightDown = 0.0;
    double distance=0.0;
    CandidateEntry( BaseNode n, double d, double lrd ) {
      candidate = n;
      distance = d;
      leftRightDown = lrd;
    }
  }

  private Comparator candComp = new Comparator() {
    public int compare(Object o1, Object o2) {
      double diff = ((CandidateEntry) o1).distance-((CandidateEntry) o2).distance;
      return diff < 0 ? -1 : (diff> 0 ? 1 : 0 );
    }
  };

  private Comparator candlrdComp = new Comparator() {
    public int compare(Object o1, Object o2) {
      double diff = ((CandidateEntry) o1).leftRightDown-((CandidateEntry) o2).leftRightDown;
      return diff < 0 ? -1 : (diff> 0 ? 1 : 0 );
    }
  };


  class DFSTreeIterator implements Iterator {
    Node currentNode = null;
    int currentChild = 0;

    public DFSTreeIterator( Node root ) {
      currentNode = root;
    }

    public boolean hasNext() {
      return currentNode != null;
    }

    public Object next() {
      Node result = currentNode;
      if( currentNode.getChildCount() > 0 )
        currentNode = currentNode.getChildAsNode(0);
      else  {
      // back up until unvisited child found
        while( currentNode != null &&
          ( currentNode.parent == null || currentNode.childPos == currentNode.parent.getChildCount()-1 ) )
          currentNode = currentNode.parent;
        if ( currentNode != null )
          currentNode = currentNode.parent.getChildAsNode( currentNode.childPos + 1 );
        }
      return result;
    }

    public void remove()  {
      throw new java.lang.UnsupportedOperationException();
    }
  }
}