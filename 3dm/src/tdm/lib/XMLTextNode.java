// $Id: XMLTextNode.java,v 1.5 2001/04/20 14:47:50 ctl Exp $

import java.security.MessageDigest;

public class XMLTextNode extends XMLNode {

  private char[] text=null;
  private byte[] cHash = null;

  XMLTextNode( String srctext ) {
    this( srctext.toCharArray() );
  }

  XMLTextNode( char[] srctext ) {
    this( srctext,0,srctext.length);
  }

  XMLTextNode( char[] srctext, int first, int length ) {
    text = new char[length];
    System.arraycopy(srctext,first,text,0,length);
//    System.out.println("NEW TN:"+new String(text));
    cHash = calculateHash(text);
    infoSize = text.length > Measure.TEXT_THRESHOLD ? text.length - Measure.TEXT_THRESHOLD : 1;
  }

  public boolean contentEquals( Object a ) {
    if( a instanceof XMLTextNode )
      return MessageDigest.isEqual(cHash,((XMLTextNode) a).cHash);
    else
      return false;
  }

  public char[] getText() {
    return text;
  }

  public String toString() {
    return new String(text);
  }
}