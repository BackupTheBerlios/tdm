// $Id: Matching.java,v 1.2 2001/04/20 14:47:49 ctl Exp $

import java.util.Vector;
import java.util.Iterator;


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
  }

  private void match( BaseNode base, BranchNode branch ) {
    Vector candidates = findCandidates( base, branch ); // Find candidates for node branch in base
  }

  private Vector findCandidates( BaseNode tree, BranchNode key ) {
    Vector candidates = new Vector();
    findExactMatches( tree, key, candidates );
    if( candidates.isEmpty() )
      findFuzzyMatches( tree, key, candidates );
    return candidates;
  }


  private void findExactMatches( BaseNode tree, BranchNode key, Vector candidates ) {
    for( Iterator i = new DFSTreeIterator(tree);i.hasNext();) {
      BaseNode cand = (BaseNode) i.next();
      //System.out.println("Cand=" + cand.getContent().toString());
      if( cand.getContent().contentEquals(key.getContent()) )
        candidates.add(cand);
    }
  }

  private void findFuzzyMatches( BaseNode tree, BranchNode key, Vector candidates ) {
    for( Iterator i = new DFSTreeIterator(tree);i.hasNext();) {
      BaseNode cand = (BaseNode) i.next();

      if( cand.getContent().contentEquals(key.getContent()) )
        candidates.add(cand);
    }
  }

  private void measureMatching( BaseNode base, BranchNode br) {

  }

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