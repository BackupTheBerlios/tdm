// $Id: Matching.java,v 1.18 2001/07/29 17:12:09 ctl Exp $

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
//    checkMa(branch);
    matchSubtrees( base, branch );
    removeSmallCopies(branch);
    matchSimilarUnmatched( base, branch );
    setMatchTypes(base);
    // Artificial roots always match (otherwise BranchNode.isLeft may fail!)
    branch.setBaseMatch(base,BranchNode.MATCH_FULL);
  }


  public BaseNode getBaseRoot() {
    return baseRoot;
  }

  public BranchNode getBranchRoot() {
    return branchRoot;
  }

  static int fccount=0;
  protected void matchSubtrees( BaseNode base, BranchNode branch ) {
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
      int thisCount = dfsMatch( candidate.candidate, branch, initCount );
      if( thisCount == bestCount )
        bestCandidates.add( candidate );
      else if( thisCount > bestCount ) {
        bestCount = thisCount;
        bestCandidates.clear();
        bestCandidates.add( candidate );
      }
    }
    // Add matchings and find nodes below matching subtree
    best = getBestCandidate(base,branch,bestCandidates,bestCount);
    Vector stopNodes = new Vector();
    if( best != null )
      dfsMatch( best.candidate, branch, 0, stopNodes, new MatchArea(branch) );
    else {
      // Unmatched
      for( int i=0;i<branch.getChildCount();i++)
        stopNodes.add(branch.getChild(i));
    }
    // Recurse
    //System.out.println("---Recurse");
    for( Iterator i=stopNodes.iterator();i.hasNext();)
      matchSubtrees(base,(BranchNode) i.next());
  }

  protected CandidateEntry getBestCandidate( BaseNode base, BranchNode branch, Vector bestCandidates,
    int bestCount ) {
    CandidateEntry best=null;
    //System.out.println("Resolving amb");
    // Resolve ambiguities if any exist...
    if( bestCandidates.size() > 1 ) {
      // Check if left neighbor of candidate is matched to left of base,
      // in that case we have a clear winner!
      for( Iterator i = bestCandidates.iterator(); i.hasNext(); ) {
        CandidateEntry candidate = (CandidateEntry) i.next();
        BranchNode left = (BranchNode) branch.getLeftSibling();
        if( left != null && left.hasBaseMatch() && left.getBaseMatch() ==
          candidate.candidate.getLeftSibling() ) {
          return candidate;
        }
      }
      // Didn't work..now we've try to make a judgement based on context
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
      (Math.min(best.leftRightDown,best.distance) > 0.1 /*||
        best.candidate.content.getInfoSize() < COPY_THRESHOLD */)))
      best=null; //System.out.println("No candidate was very good, leaving unmatched" );
    return best;
  }

  private void matchSimilarUnmatched( BaseNode base, BranchNode branch) {
    // Traverse in preorder -- to avoid building trees, just fixing levels where parents are matched
    for( int i=0;i<branch.getChildCount();i++)
      matchSimilarUnmatched(base,branch.getChild(i));

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
        if( i == 0 && !isMatched(baseMatch.getChild(0)) ){
          addMatchingIfSameType(n, baseMatch.getChild(0) );
          continue;
        } else if (i==branch.getChildCount()-1 && !isMatched(baseMatch.getChild(lastBaseChild))) {
          addMatchingIfSameType(n, baseMatch.getChild(lastBaseChild) );
          continue;
        }
        if( i > 0 ) {
          // See if node preceding n is matched, and its right neighbour unmatched
          // Base    xy     p=n's predecessor, x matches p and y unmatched
          // Branch  pn        => match y and n
          BaseNode x = branch.getChild(i-1).getBaseMatch();
          if( x != null && x.hasRightSibling() && !isMatched((BaseNode) x.getRightSibling())) {
            addMatchingIfSameType(n,(BaseNode) x.getRightSibling() );
            continue;
          }
        }
        if( i < branch.getChildCount()-1 ) {
          // See if node succeeding n is matched, and its right neighbour unmatched
          // Base    yx     p=n's succecessor, x matches p and y unmatched
          // Branch  np        => match y and n
          BaseNode x = branch.getChild(i+1).getBaseMatch();
          if( x != null && x.hasLeftSibling() && !isMatched((BaseNode) x.getLeftSibling())) {
            addMatchingIfSameType(n,(BaseNode) x.getLeftSibling() );
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
      // Has multiple matches, need to determine type of each copy
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
          minContentDist = measure.getDistance( base, master ); // May not have been calced already...
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
        boolean structMatch = exactChildListMatch(base,cand); // && false; //XXXXXXXXX
        boolean contMatch = cand.getContent().contentEquals(base.getContent()); // || true; //XXXXXXXXX
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

  public static int COPY_THRESHOLD = 128;
  static final int EDGE_BYTES = 4;

  private void removeSmallCopies( BranchNode root ) {
    BaseNode base = root.getBaseMatch();
    if( base != null && base.getLeft().getMatches().size() > 1 ) {
      // Iterate over the matches, and discard any that too small
      Set deletia = new HashSet();
      for( Iterator i = base.getLeft().getMatches().iterator();i.hasNext();) {
        BranchNode copy = (BranchNode) i.next();
        if( copy.getMatchArea().getInfoBytes() < COPY_THRESHOLD ) {
            deletia.add(copy);
        }
      }
      if( base.getLeft().getMatches().size() == deletia.size() ) {
        // We're deleting all matches... check if some match is the "original" instance
        // and if it's found, don't delete it!
        int maxcopybytes = 0, mincopybytes = Integer.MAX_VALUE;
        BranchNode origInstance = null;
        for( Iterator i = base.getLeft().getMatches().iterator();i.hasNext();) {
          BranchNode copy = (BranchNode) i.next();
          int copybytes = copy.getMatchArea().getInfoBytes();
          Node copyRoot = copy.getMatchArea().getRoot();
          Node copyBase = ((BranchNode) copyRoot).getBaseMatch();
          // Scan left
          while(  (copyRoot =  copyRoot.getLeftSibling()) != null &&
                  (copyBase =  copyBase.getLeftSibling()) != null &&
                  ((BranchNode) copyRoot).getBaseMatch() == copyBase &&
                  copybytes < COPY_THRESHOLD)
            copybytes += ((BranchNode) copyRoot).getMatchArea().getInfoBytes();
          // Scan right
          copyRoot = copy.getMatchArea().getRoot();
          copyBase = ((BranchNode) copyRoot).getBaseMatch();
          while(
                  (copyRoot = (BranchNode) copyRoot.getRightSibling()) != null &&
                  (copyBase = (BaseNode) copyBase.getRightSibling()) != null &&
                  ((BranchNode) copyRoot).getBaseMatch() == copyBase &&
                  copybytes < COPY_THRESHOLD)
            copybytes += copyRoot.getMatchArea().getInfoBytes();
          if( copybytes > maxcopybytes ) {
            origInstance = copy;
            maxcopybytes = copybytes;
          }
          if( copybytes < mincopybytes )
            mincopybytes = copybytes;
        }
        if( maxcopybytes > mincopybytes ) {
          // Mark if there is one copy that is "better" (more copybytes)
          deletia.remove(origInstance);
          origInstance.getMatchArea().addInfoBytes(COPY_THRESHOLD+1); // Now this is marked as the orig inst.
        }
      }
      if( !deletia.isEmpty() ) {
        for( Iterator i = deletia.iterator();i.hasNext();)
          delMatchArea(((BranchNode) i.next()).getMatchArea());
      }
    }
    for(int i=0;i<root.getChildCount();i++)
      removeSmallCopies(root.getChild(i));
  }


  protected Vector findCandidates( BaseNode tree, BranchNode key ) {
    // Find candidates for key
    //System.out.println("Fcand = " + (++fccount));
    Vector candidates = new Vector();
    findExactMatches( tree, key, candidates );
    if( candidates.isEmpty() )
      findFuzzyMatches( tree, key, candidates );
    return candidates;
  }

  final static double DFS_MATCH_THRESHOLD = 0.2;

  protected int dfsMatch( BaseNode a, BranchNode b, int count ) {
    return dfsMatch(a,b,count,null,null);
  }

  protected int dfsMatch( BaseNode a, BranchNode b, int count, Vector stopNodes, MatchArea ma ) {
    if( stopNodes != null ) {
      // Also means matchings should be added
      addMatching( b, a );
      ma.addInfoBytes( a.getContent().getInfoSize() );
/*      if( ma==null)
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXDIEDIEDIE!!!!");
*/
      b.setMatchArea( ma );
    }
    boolean childrenMatch = true;
    if( a.getChildCount() == b.getChildCount() ) {
      // Only match children, if there are equally many
      for( int i=0; childrenMatch && i<a.getChildCount(); i ++ ) {
        childrenMatch = a.getChild(i).getContent().contentEquals(b.getChild(i).getContent());
/*        if( !childrenMatch )
          // Try fuzzy matching
          childrenMatch = dfsTryFuzzyMatch( a.getChild(i), b.getChild(i) );*/
      }
    } else
      childrenMatch = false;

    if( !childrenMatch ) {
      // Mark all children as stopnodes
      for( int i=0; stopNodes!=null && i<b.getChildCount(); i ++ )
        stopNodes.add( b.getChild(i) );
    } else {
      // All children match
      if( ma != null )
        ma.addInfoBytes( a.getChildCount() * EDGE_BYTES );
      for( int i=0; i<a.getChildCount(); i ++ )
        count += dfsMatch( a.getChild(i), b.getChild(i), 0, stopNodes, ma );
    }
    return count + 1;
  }

  public void getAreaStopNodes( Vector stopNodes, BranchNode n ) {
    boolean childrenInSameArea = true;
    MatchArea parentArea = n.getMatchArea();
    if( parentArea == null )
      throw new RuntimeException("ASSERT FAILED");
    for( int i=0;i<n.getChildCount() && childrenInSameArea;i++)
      childrenInSameArea&= n.getChild(i).getMatchArea() == parentArea;
    if( !childrenInSameArea ) {
      stopNodes.add(n);
      return;
    } else {
      for( int i=0;i<n.getChildCount();i++)
        getAreaStopNodes(stopNodes,n.getChild(i));
    }
  }

  protected boolean dfsTryFuzzyMatch( Node a, Node b) {
    double distance = measure.getDistance(a,b );
    return  distance < DFS_MATCH_THRESHOLD;
  }

  protected void findExactMatches( BaseNode tree, BranchNode key, Vector candidates ) {
    for( Iterator i = new DFSTreeIterator(tree);i.hasNext();) {
      BaseNode cand = (BaseNode) i.next();
      //System.out.println("Cand=" + cand.getContent().toString());
      if( cand.getContent().contentEquals(key.getContent()) )
        candidates.add( new CandidateEntry( cand, 0.0, -1.0 )); // -1.0 => lr not present
    }
  }

  static final double MAX_FUZZY_MATCH = 0.2;
  static final double PARENT_DISCOUNT = 0.5;

  protected void findFuzzyMatches( BaseNode tree, BranchNode key, Vector candidates ) {
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

  // Only adds the matching if types match (i.e. both text or element)
  protected void addMatchingIfSameType( BranchNode a, BaseNode b ) {
    if( (a.getContent() instanceof XMLTextNode && b.getContent() instanceof XMLTextNode) ||
        (a.getContent() instanceof XMLElementNode && b.getContent() instanceof XMLElementNode)) {
      addMatching(a,b);
    }
  }

  protected void addMatching( BranchNode a, BaseNode b ) {
    a.setBaseMatch(b,BranchNode.MATCH_FULL);
    b.getLeft().addMatch(a);
  }

  protected void delMatching( BranchNode a, BaseNode b ) {
    a.delBaseMatch();
    b.getLeft().delMatch(a);
  }

  protected void delMatchArea(MatchArea m) {
//    if( 1==1) return;
    delMatchArea(m.getRoot(),m);
  }

  private void delMatchArea(BranchNode n,MatchArea m) {
    if( n.getMatchArea() == m ) {
      n.setMatchArea(null);
      delMatching(n,n.getBaseMatch());
      for( int i=0;i<n.getChildCount();i++)
        delMatchArea(n.getChild(i),m);
    }
  }

  private boolean isMatched( BaseNode n) {
    return n.getLeft().getMatchCount() > 0;
  }

/*
  private void checkMa( BranchNode n) {
    if( n.hasBaseMatch() || n.getMatchArea() != null )
      System.err.println("!!!!!!!!!!!!!!!!!!!!!MATCHED, but NULL MA!");
    for(int i=0;i<n.getChildCount();i++)
      checkMa(n.getChild(i));
  }
*/
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