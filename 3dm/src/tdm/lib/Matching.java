// $Id: Matching.java,v 1.3 2001/04/21 18:00:26 ctl Exp $

import java.util.Vector;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
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

  private void buildMatching( BaseNode base, BranchNode branch ) {
    match( base, branch );
    matchSamePosUnmatched( base, branch );
    removeSmallCopies(branch);
  }

  private void match( BaseNode base, BranchNode branch ) {
    Vector candidates = findCandidates( base, branch ); // Find candidates for node branch in base
    // Find best trees
    int bestCount = 0;
    CandidateEntry best = null;
    Vector bestCandidates = new Vector();
    for( Iterator i = candidates.iterator(); i.hasNext(); ) {
      CandidateEntry candidate = (CandidateEntry) i.next();
      int thisCount = dfsMatch( candidate.candidate, branch, 0, null );
      if( thisCount == bestCount )
        bestCandidates.add( candidate );
      else if( thisCount > bestCount ) {
        bestCount = thisCount;
        bestCandidates.clear();
        bestCandidates.add( candidate );
      }
    }
    // Resolve ambiguities if any exist...
    if( bestCandidates.size() > 1 ) {
      // Ambiguities - we need to find out which one is the best...
      // First calc all missing left-right correlations
      for( Iterator i = bestCandidates.iterator(); i.hasNext(); ) {
        CandidateEntry candidate = (CandidateEntry) i.next();
        if( candidate.leftRightDist < 0.0 )
          candidate.leftRightDist = Math.min( getDistanceOfLeft(candidate.candidate,branch),
                                      getDistanceOfLeft(candidate.candidate,branch));
      }
      Collections.sort(bestCandidates,candComp);
    }
    best = bestCandidates.isEmpty() ? null : (CandidateEntry) bestCandidates.elementAt(0);
    if( best!=null && (bestCount == 1 && best.distance > 0.1 ))
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
          addMatching(n, baseMatch.getChild(0) );
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

  static final int COPY_THRESHOLD = 18;
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
       info +=a.getContent().getInfoSize();  // a is a n:th copy of base
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
        childrenMatch = a.getContent().contentEquals(b.getContent());
        if( !childrenMatch ) {
          // Try fuzzy mathcing
          double distance = measure.getDistance( a, b );
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

  private void findFuzzyMatches( BaseNode tree, BranchNode key, Vector candidates ) {
    SortedSet cset = new TreeSet(candComp);
    double cutoff = 2*MAX_FUZZY_MATCH;
    for( Iterator i = new DFSTreeIterator(tree);i.hasNext();) {
      BaseNode cand = (BaseNode) i.next();
      double lrDist = Math.min(getDistanceOfLeft(key,cand),getDistanceOfRight(key,cand));
      double minDist = Math.min(Math.min(lrDist,measure.childListDistance(key,cand)),
          measure.getDistance(cand,key));
      if( minDist < 2*MAX_FUZZY_MATCH ) {
        cset.add(new CandidateEntry(cand,minDist,lrDist));
        cutoff = cutoff < 2*minDist ? cutoff : 2*minDist;
      }
    }
    for( Iterator i = cset.iterator();i.hasNext();) {
      CandidateEntry en = (CandidateEntry) i.next();
      if( en.distance > cutoff )
        break;
      else
        candidates.add(en);
    }
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
    double leftRightDist = 0.0;
    double distance=0.0;
    CandidateEntry( BaseNode n, double d, double lr ) {
      candidate = n;
      distance = d;
      leftRightDist = lr;
    }
  }

  class CandidateComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      double diff = ((CandidateEntry) o1).distance-((CandidateEntry) o2).distance;
      return diff < 0 ? -1 : (diff> 0 ? 1 : 0 );
    }
  }

  private CandidateComparator candComp = new CandidateComparator();

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