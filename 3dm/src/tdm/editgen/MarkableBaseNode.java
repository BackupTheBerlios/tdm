// $Id: MarkableBaseNode.java,v 1.1 2002/10/25 11:44:47 ctl Exp $
package editgen;

import BaseNode;
import XMLNode;

public class MarkableBaseNode extends BaseNode {

  protected boolean isMarked = false;
  protected int subtreeSize = 0;

  MarkableBaseNode(XMLNode aContent) {
    super( aContent );
  }

  public void mark() {
    isMarked = true;
  }

  public boolean isMarked() {
    return isMarked;
  }

/*  public boolean isLocked() {
    MarkableBaseNode leftSib = (MarkableBaseNode) getLeftSibling(),
      rightSib = (MarkableBaseNode) getRightSibling();
    return isMarked &&
           (leftSib != null && leftSib.isMarked
           ((childPos > 0 ) & ((MarkableBaseNode) parent.getChildAsNode(childPos-1)).isMarked) &&
           ((childPos < parent.getChildCount()-1 ) & ((MarkableBaseNode) parent.getChildAsNode(childPos+1)).isMarked);
  }
*/

  public void lock() {
    lock( true, true );
  }

  public void lockSubtree() {
    for( int i=0;i<getChildCount();i++) {
      MarkableBaseNode n = (MarkableBaseNode) getChild(i);
      n.lock();
      n.lockSubtree();
    }
  }

  public void lockLeft() {
    lock( true, false );
  }

  public void lockRight() {
    lock( false, true );
  }

  public void lock(boolean left, boolean right) {
    MarkableBaseNode leftSib = left ? (MarkableBaseNode) getLeftSibling() : null,
      rightSib = right ? (MarkableBaseNode) getRightSibling() : null;
    mark();
    if( rightSib != null ) rightSib.mark();
    if( leftSib != null ) leftSib.mark();
  }

  public int getSubteeSize() {
    return subtreeSize;
  }

  public void setSubtreeSize( int aSize) {
    subtreeSize = aSize;
  }
}