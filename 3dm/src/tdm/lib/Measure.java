//$Id: Measure.java,v 1.4 2001/04/24 10:00:42 ctl Exp $

import org.xml.sax.Attributes;

public class Measure {

  public static final double MAX_DIST = 1.0;
  public static final double ZERO_CHILDREN_MATCH = 1.0;
  public static final int ELEMENT_NAME_INFO = 1;
  public static final int ATTR_INFO = 2;
  public static final int ATTR_VALUE_THRESHOLD = 5;
  public static final int TEXT_THRESHOLD = 5;

  public static final int DISTBUF_SIZE = 8192;
  public final int[] DISTBUF = new int[DISTBUF_SIZE];
  // Tester
  public static void main(String[] args) {
    String a = "return stringDist( a, b, a.length()+b.length() );";
    String b = "rezurn stringDist( a,b, a.length()+b.length() );";
    System.out.println("a="+a);
    System.out.println("b="+b);
    //System.out.println("Dist = " + stringDist(a,b));
  }

  public Measure() {
  }


  // Distance measure between nodes

  private int mismatched = 0;
  private int total = 0;
  private boolean totalMismatch = false;

  static final int C= 20; // Correlation damping value (default no of mismatched bytes)
                          // A perfect match of C bytes gets correlation 0.5
                          // as matches ->perfect, miscorrelation -> 0

  public void resetDistance() {
    mismatched = 0;
    total = 0;
    totalMismatch = false;
  }

  public double getDistance() {
    return getDistance(null,null);
  }

  public double getDistance(Node a, Node b ) {
    if( a!=null && b!= null )
      includeNodes( a, b );
    double penalty = Math.max(0.0,1.0-((double) total)/((double) C));
    double distance= penalty+(1.0-penalty)*((double) (mismatched))/((double) total);
    resetDistance();
    return totalMismatch ? 1.0 : distance;
  }

  private void includeNodes( Node a, Node b ) {
    if( a== null || b== null || totalMismatch)
      return;
    XMLNode ca = a.getContent(), cb = b.getContent();
    if( ca instanceof XMLElementNode && cb instanceof XMLElementNode ) {
      XMLElementNode ea = (XMLElementNode) ca,eb=(XMLElementNode) cb;
      total+=ELEMENT_NAME_INFO;
      mismatched += ea.getQName().equals(eb.getQName()) ? 0 : ELEMENT_NAME_INFO;
      Attributes aa =  ea.getAttributes(), ab = eb.getAttributes();
      for( int i=0;i<aa.getLength();i++)  {
        int index = ab.getIndex(aa.getQName(i));
        if( index != -1 ) {
          String v1 =  aa.getValue(i), v2 = ab.getValue(index);
          int amismatch = stringDist( v1,v2 );
          int info = (v1.length() > ATTR_VALUE_THRESHOLD ? v1.length() : 1 ) +
                     (v2.length() > ATTR_VALUE_THRESHOLD ? v2.length() : 1 );
          mismatched += amismatch > info ? info : amismatch;
          total+=info;
        } else {
          mismatched += ATTR_INFO;
          total += ATTR_INFO;
        }
      }
      // Scan for deleted from b
      for( int i=0;i<ab.getLength();i++) {
        if( aa.getIndex(ab.getQName(i)) == -1 ) {
          mismatched += ATTR_INFO;
          total += ATTR_INFO;
        }
      }
    } else if ( ca instanceof XMLTextNode && cb instanceof XMLTextNode ) {
      int info = ca.getInfoSize() + cb.getInfoSize() / 2,
        amismatch = stringDist( ((XMLTextNode) ca).getText(), ((XMLTextNode) cb).getText() ) / 2;
      mismatched += amismatch > info  ? info : amismatch;
      total+=info;
    } else
      totalMismatch = true;
  }



  private  TokenComparator stringComp = new TokenComparator() {
      int getLength( Object o ) {
        return ((String) o).length();
      }
      boolean equals( Object a, int ia, Object b, int ib ) {
        return ((String) a).charAt(ia)==((String) b).charAt(ib);
      }
    };

  private TokenComparator charArrayComp = new TokenComparator() {
      int getLength( Object o ) {
        return ((char[] ) o).length;
      }
      boolean equals( Object a, int ia, Object b, int ib ) {
        return ((char[]) a)[ia] ==((char[]) b)[ib];
      }
    };

  private TokenComparator nodeChildComp = new TokenComparator() {
      int getLength( Object o ) {
        return ((Node ) o).getChildCount();
      }
      boolean equals( Object a, int ia, Object b, int ib ) {
        return ((Node) a).getChildAsNode(ia).getContent().contentEquals(
          ((Node) b).getChildAsNode(ib).getContent());
      }
    };

  private TokenComparator matchedNodeChildComp = new TokenComparator() {
      int getLength( Object o ) {
        return ((Node ) o).getChildCount();
      }
      boolean equals( Object a, int ia, Object b, int ib ) {
        return ((BaseNode) a).getChild(ia) == ((BranchNode) b).getBaseMatch();
      }
    };

  public int stringDist( String a, String b ) {
    return stringDist( a, b, a.length()+b.length(), stringComp);
  }

  public int stringDist( char[] a, char[] b ) {
    return stringDist( a, b, a.length+b.length, charArrayComp );
  }

  public double childListDistance( Node a, Node b ) {
    if( a.getChildCount()== 0 && b.getChildCount() == 0)
      return ZERO_CHILDREN_MATCH; // Zero children is also a match!
    else
      return ((double) stringDist(a,b,a.getChildCount()+b.getChildCount(),nodeChildComp))
                       / ((double) a.getChildCount() + b.getChildCount());
  }

  public int matchedChildListDistance( BaseNode a, BranchNode b ) {
    return stringDist(a,b,a.getChildCount()+b.getChildCount(),matchedNodeChildComp);
  }

  // Directly adapted from [Myers86]

  private int stringDist( Object a, Object b, int max, TokenComparator tc ) {
//DBG    if( 1==1 ) return max/2;
    int arraySize = 2*max+1;
    int v[] = null;
    if( arraySize <= DISTBUF_SIZE )
      v = DISTBUF; // Use preallocated buffer (speedup!)
    else
      v = new int[2*max+1];
    int x=0,y=0;
    final int VBIAS = max, N = tc.getLength(a), M=tc.getLength(b);
    v[VBIAS+1]=0;
    for(int d=0;d<=max;d++) {
      for( int k=-d;k<=d;k+=2 ) {
        if( k==-d || ( k!=d && v[k-1+VBIAS] < v[k+1+VBIAS] ) )
          x = v[k+1+VBIAS];
        else
          x = v[k-1+VBIAS]+1;
        y=x-k;
        while( x < N && y < M && tc.equals(a,x,b,y)  ) {
          x++;
          y++;
        }
        v[k+VBIAS]=x;
        if( x >= N && y>= M )
          return d;
      }
    }
  return Integer.MAX_VALUE; // D > max
  }


  abstract class TokenComparator {
    abstract int getLength( Object o );
    abstract boolean equals( Object a, int ia, Object b, int ib );
  }
}