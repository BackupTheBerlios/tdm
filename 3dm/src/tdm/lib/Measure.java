//$Id: Measure.java,v 1.1 2001/04/18 09:30:05 ctl Exp $

public class Measure {

  // Tester
  public static void main(String[] args) {
    String a = "return stringDist( a, b, a.length()+b.length() );";
    String b = "rezurn stringDist( a,b, a.length()+b.length() );";
    System.out.println("a="+a);
    System.out.println("b="+b);
    System.out.println("Dist = " + stringDist(a,b));
  }

  public Measure() {
  }


  static int stringDist( String a, String b ) {
    return stringDist( a, b, a.length()+b.length() );
  }

  // Directly adapted from [Myers86]

  static int stringDist( String a, String b, int max ) {
    int v[] = new int[2*max+1];
    int x=0,y=0;
    final int VBIAS = max, N = a.length(), M=b.length();
    for(int d=0;d<=max;d++) {
      for( int k=-d;k<=d;k+=2 ) {
        if( k==-d || ( k!=d && v[k-1+VBIAS] < v[k+1+VBIAS] ) )
          x = v[k+1+VBIAS];
        else
          x = v[k-1+VBIAS]+1;
        y=x-k;
        while( x < N && y < M && a.charAt(x)==b.charAt(y) ) {
          x++;
          y++;
        }
        v[k+VBIAS]=x;
        if( x >= N && y>= M )
          return d;
      }
    }
  return -1; // D > max
  }
}