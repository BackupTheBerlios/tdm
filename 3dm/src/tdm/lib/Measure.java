//$Id: Measure.java,v 1.6 2001/05/16 10:31:41 ctl Exp $

import org.xml.sax.Attributes;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

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
    String b = "return stzingDist( a, b, a.length()+b.length() );";
    System.out.println("a="+a);
    System.out.println("b="+b);
    System.out.println("Dist = " + (new Measure()).qDist(a,b));
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
          int amismatch = stringDist( v1,v2,1.0 );
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
        amismatch = stringDist( ((XMLTextNode) ca).getText(), ((XMLTextNode) cb).getText(),1.0 ) / 2;
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

  public int stringDist( String a, String b,double limit ) {
    return qDist(a,b);
    //return stringDist( a, b,(int) a.length()+b.length(), stringComp);
  }

  public int stringDist( char[] a, char[] b,double limit ) {
    int d = qDist(a,b);
/*    System.out.println("A="+new String(a,0,a.length));
    System.out.println("B="+new String(b,0,b.length));
    System.out.println("dist="+d);
    System.out.println("sdist="+stringDist( a, b,(int) a.length+b.length, charArrayComp ));*/
    return d;
    //return qDist(a,b);
    //return stringDist( a, b,(int) a.length+b.length, charArrayComp );
  }

  public double childListDistance( Node a, Node b ) {
    if( a.getChildCount()== 0 && b.getChildCount() == 0)
      return ZERO_CHILDREN_MATCH; // Zero children is also a match!
    else {
      char[] ac = new char[a.getChildCount()];
      char[] bc = new char[b.getChildCount()];
      for( int i=0;i<a.getChildCount();i++)
        ac[i]=(char) (a.getChildAsNode(i).getContent().getContentHash()&0xffff);
      for( int i=0;i<b.getChildCount();i++)
        bc[i]=(char) (b.getChildAsNode(i).getContent().getContentHash()&0xffff);
      return ((double) stringDist(ac,bc,1.0))
                       / ((double) a.getChildCount() + b.getChildCount());
/*      return ((double) stringDist(a,b,a.getChildCount()+b.getChildCount(),nodeChildComp))
                       / ((double) a.getChildCount() + b.getChildCount());

*/
    }
  }

  public int matchedChildListDistance( BaseNode a, BranchNode b ) {
    char[] ac = new char[a.getChildCount()];
    char[] bc = new char[b.getChildCount()];
    for( int i=0;i<a.getChildCount();i++)
      ac[i]=(char) i;
    for( int i=0;i<b.getChildCount();i++) {
      BaseNode m = b.getBaseMatch();
      if( m!= null && m.getParent() == a )
        bc[i] = (char) m.getChildPos();
      else
        bc[i] = (char) -i;
    }
    return stringDist(ac,bc,1.0);

//    return stringDist(a,b,a.getChildCount()+b.getChildCount(),matchedNodeChildComp);
  }

  // Directly adapted from [Myers86]

  private int stringDist( Object a, Object b, int max, TokenComparator tc ) {
//DBG    if( 1==1 ) return max/2;
//    if( 1==1) return (int) Math.round( Math.random()*tc.getLength(a)*.4 );
    /*if( max > 50 )
      System.out.println("max="+max);*/
    //max = max > 10 ? 10 : max;
    int arraySize = 2*max+1;
    int v[] = null;
    if( true ) throw new RuntimeException("DISABLED---!");
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

  /// q-Gram Distance [Ukkonen92]

  class Counter {
    public int count = 1;
  }

  private static final int INIT_CAPACITY=2048;
  private static /*final*/ int Q=4; // Which gram to use

  private Map aGrams = new HashMap(INIT_CAPACITY);
  private Map bGrams = new HashMap(INIT_CAPACITY);

  public int qDist( String a , String b ) {
    decideQ(a.length()+b.length());
    buildQGrams(a,aGrams);
    buildQGrams(b,bGrams);
    return calcQDistance();
  }

  public int qDist( char[] a , char[] b ) {
    decideQ(a.length+b.length);
    buildQGrams(a,aGrams);
    buildQGrams(b,bGrams);
    return calcQDistance();
  }

  protected void buildQGrams(String a, Map grams) {
    grams.clear();
/*    if( a.length() < Q )
        grams.put(a,new Counter());
    else {*/
      for( int i=0;i<a.length();i++) {
      String gram = a.substring(i,i+Q > a.length() ? a.length() : i+Q );
      if( grams.containsKey(gram) )
        ((Counter) grams.get(gram)).count++;
      else
        grams.put(gram,new Counter());
      }
    //}
  }

  protected void buildQGrams(char[] a, Map grams) {
    grams.clear();
    for( int i=0;i<a.length;i++) {
      int count = i + Q > a.length ? a.length -i : Q;
      String gram = new String(a,i,count);
      if( grams.containsKey(gram) )
        ((Counter) grams.get(gram)).count++;
      else
        grams.put(gram,new Counter());
    }
  }

  protected int decideQ( int total ) {
   int q = 1;
   if( total > 150 )
      q = 4;
    else if( total > 50 )
      q = 2;
    return q;
  }

  protected int calcQDistance() {
    int dist = 0;
    // first, loop over agrams
    for( Iterator i = aGrams.keySet().iterator();i.hasNext();) {
      Object gramA = i.next();
      int countA = ((Counter) aGrams.get(gramA)).count;
      int countB = 0;
      if( bGrams.containsKey(gramA) )
        countB = ((Counter) bGrams.get(gramA)).count;
      else
//        System.out.println("Not in B: " +gramA.toString());
      dist += Math.abs(countA-countB);
    }
    // And add any grams present in b but not in a
    for( Iterator i = bGrams.keySet().iterator();i.hasNext();) {
      Object gramB = i.next();
      if( !aGrams.containsKey(gramB) ) {
        dist += ((Counter) bGrams.get(gramB)).count;
//        System.out.println("Not in A: " +gramB.toString());
      }
    }
    return dist;
  }

}