// $Id: XMLNode.java,v 1.4 2001/04/20 14:47:50 ctl Exp $

import java.security.MessageDigest;

public abstract class XMLNode {

  protected int infoSize = 0;

  public XMLNode() {
  }

  public int getInfoSize() {
    return infoSize;
  }

  protected MessageDigest getMD() {
    try{
      return MessageDigest.getInstance("MD5");
    } catch ( java.security.NoSuchAlgorithmException e ) {
      System.err.println("MD5 hash generation not supported -- aborting");
      System.exit(-1);
    }
    return null;
  }

  public abstract boolean contentEquals( Object a );

  protected byte[] calculateHash(char[] data) {
    MessageDigest contentHash = getMD();
   contentHash.reset();
    for( int i=0;i<data.length;i++) {
      contentHash.update((byte) (data[i]&0xff));
      contentHash.update((byte) (data[i]>>8));
    }
    return contentHash.digest();
  }

  protected byte[] calculateHash(String data) {
    MessageDigest contentHash = getMD();
    contentHash.reset();
    for( int i=0;i<data.length();i++) {
      contentHash.update((byte) (data.charAt(i)&0xff));
      contentHash.update((byte) (data.charAt(i)>>8));
    }
    return contentHash.digest();
  }


}