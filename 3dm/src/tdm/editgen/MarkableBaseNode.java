// $Id: MarkableBaseNode.java,v 1.3 2002/10/30 15:13:35 ctl Exp $
package editgen;

import BaseNode;
import XMLNode;

public class MarkableBaseNode extends BaseNode {

  public static final int MARK_NONE = 0;
  public static final int MARK_CONTENT = 1;
  public static final int MARK_STRUCTURE = 2;

  protected int markCount = 0;
  protected int subtreeSize = 0;

  MarkableBaseNode(XMLNode aContent) {
    super( aContent );
  }

  public void mark(int mark) {
    markCount|=mark;
  }

  public boolean isMarked() {
    return markCount > 0;
  }

  public boolean isMarkedContent() {
    return (markCount & MARK_CONTENT) != 0;
  }

  public boolean isMarkedStructure() {
    return (markCount & MARK_STRUCTURE) != 0;
  }

  public int getMark() {
    return markCount;
  }

/*
  public void unmark() {
    if(markCount==0)
      throw new IllegalStateException("Too many unmarks");
    markCount--;
  }
*/
/*  public boolean isLocked() {
    MarkableBaseNode leftSib = (MarkableBaseNode) getLeftSibling(),
      rightSib = (MarkableBaseNode) getRightSibling();
    return isMarked &&
           (leftSib != null && leftSib.isMarked
           ((childPos > 0 ) & ((MarkableBaseNode) parent.getChildAsNode(childPos-1)).isMarked) &&
           ((childPos < parent.getChildCount()-1 ) & ((MarkableBaseNode) parent.getChildAsNode(childPos+1)).isMarked);
  }
*/

  public void lock(int type) {
    lock( true, true, type );
  }

  public void lockSubtree(int type) {
    for( int i=0;i<getChildCount();i++) {
      MarkableBaseNode n = (MarkableBaseNode) getChild(i);
      n.lock(type);
      n.lockSubtree(type);
    }
  }

  public void lockLeft(int type) {
    lock( true, false, type );
  }

  public void lockRight(int type) {
    lock( false, true, type );
  }

  public void lock(boolean left, boolean right, int type) {
    MarkableBaseNode leftSib = left ? (MarkableBaseNode) getLeftSibling() : null,
      rightSib = right ? (MarkableBaseNode) getRightSibling() : null;
    mark(type);
    if( rightSib != null ) rightSib.mark(type);
    if( leftSib != null ) leftSib.mark(type);
  }

  public int getSubteeSize() {
    return subtreeSize;
  }

  public void setSubtreeSize( int aSize) {
    subtreeSize = aSize;
  }
}