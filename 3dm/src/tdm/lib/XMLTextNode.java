// $Id: XMLTextNode.java,v 1.8 2001/09/05 21:22:30 ctl Exp $

import java.security.MessageDigest;

/** Stores XML element nodes. */
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
///    System.out.println("NEW TN:"+new String(text));
    cHash = calculateHash(text);
    infoSize = text.length > Measure.TEXT_THRESHOLD ? text.length -
                Measure.TEXT_THRESHOLD : 1;
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

  public void setText(char[] aText) {
    text = aText;
  }

  public String toString() {
    return new String(text);
  }

  public int getContentHash() {
    return cHash[0]+cHash[1]<<8+cHash[2]<<16+cHash[3]<<24;
  }

}