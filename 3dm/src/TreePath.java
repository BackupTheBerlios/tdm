// $Id: TreePath.java,v 1.1 2001/03/14 08:23:55 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE

import java.util.Vector;
import java.util.Stack;

public class TreePath {
  public Vector path = new Vector();
  public boolean isRelative;

  // If src is null, an absolute path is built

  public TreePath(ONode src, ONode dest) {
    if( src == null ) {
      isRelative = false;
      Vector dstPath = buildNodePath( dest );
      int dstSize=dstPath.size();
      for( int i=0;i<dstSize;i++ )
        path.add( new  Integer( ((ONode) dstPath.elementAt(i)).childNo ) ); // -1 means go to parent
    } else {
    isRelative = true;
    Vector srcPath = buildNodePath( src );
    Vector dstPath = buildNodePath( dest );
    int srcSize=srcPath.size(),dstSize=dstPath.size();
    int commonLength = 0, maxLength =  srcSize > dstSize ? dstSize : srcSize;
    while( commonLength < maxLength &&
      ( srcPath.elementAt(commonLength) == dstPath.elementAt(commonLength) ) )
      commonLength++;
    if( commonLength == 0)
      throw new RuntimeException("TreePath(): No common root!");
    // Example
    // r/a/b/c/d/e/dst
    // r/a/b/x/y/src
    // commonLength = 3 =>
    // Go size(srcPath) - commonLength steps back, and then take steps from dstPath[commonLength] to  dstPath[size(dstPath)-1]
    for( int i=0;i<(srcSize-commonLength);i++ )
      path.add( new  Integer( -1 ) ); // -1 means go to parent
    for( int i=commonLength;i<dstSize;i++ )
      path.add( new  Integer( ((ONode) dstPath.elementAt(i)).childNo ) ); // -1 means go to parent
    }
  }

  public static Vector buildNodePath( ONode n ) {
    Stack s = new Stack();
    Vector v = new Vector();
    while( n != null ) {
      s.push(n);
      n = n.parent;
    }
    while( !s.empty() )
      v.add(s.pop());
    return v;
  }

  public String toString() {
    StringBuffer ps = new StringBuffer();
    int plen = path.size();
    if( !isRelative )
      ps.append('/');
    for(int i=0;i<plen;i++) {
      int pos = ((Integer) path.elementAt(i)).intValue();
      if( pos == -1)
        ps.append("..");
      else
        ps.append(pos);
      if( i < plen-1 )
        ps.append('/');
    }
    return ps.toString();
  }
}