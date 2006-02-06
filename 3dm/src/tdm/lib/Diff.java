// $Id: Diff.java,v 1.18 2006/02/06 10:04:46 ctl Exp $ D
//
// Copyright (c) 2001, Tancred Lindholm <ctl@cs.hut.fi>
//
// This file is part of 3DM.
//
// 3DM is free software; you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// 3DM is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with 3DM; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package tdm.lib;

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;

/** Produces the diff between two naturally matched trees.
 *  Collapsing multiple copy-ops using the run attribute is not implemented in
 *  this version. <b>NOTE: Format of diff root tag changed in CVS rev
 *  1.16</b> Current code reads old diffs, but new diffs are not
 *  necesarily readable by old code!
 */

public class Diff {

  // Instance vars
  private Configuration cf = DEFAULT_CONFIG;
  private TreeAbstraction ta = null;
  //private SequenceEncoder se = null;
  private Object branchRoot;

  private static final Object NO_DST_REQUIRED = new Object(); // Must not be null!
    // Above must not be null; that -> nullptrex in sequence

  private NodeIndex index = null;
//  private Matching m = null;
  private static final Attributes EMPTY_ATTS = new AttributesImpl();

  /*private PRIVATIZE LATER*/ static final Configuration DEFAULT_CONFIG = new Configuration();

//  static final Set RESERVED;

// These need to be killed!
  static final String DIFF_NS ="diff:";
  public static final String DIFF_COPY_TAG = DIFF_NS+"copy";
  public static final String DIFF_INS_TAG = DIFF_NS+"insert";
  public static final String DIFF_ESC_TAG = DIFF_NS+"esc";
  public static final String DIFF_ROOT_TAG = "diff";

  public static final String DIFF_CPYSRC_ATTR = "src";
  public static final String DIFF_CPYDST_ATTR = "dst";
  public static final String DIFF_CPYRUN_ATTR = "run";
  public static final String DIFF_ROOTOP_ATTR = "op";

  public static final String DIFF_ROOTOP_INS = "insert";
//endkill

  /** Construct a diff operating on the matched trees passed to the constructor.
   *  Note that the matching contains pointers to the base and new trees.
   *  @param am Matching between trees to diff
   */
  public Diff(Matching am) {
    this( am, new BFSIndex(am.getBaseRoot()));
  }

  public Diff(Matching am, NodeIndex aIx) {
    index = aIx;
    ta = new TdmTreeAbstraction(am,index);
//    se = new TdmSequence(index);
    branchRoot = am.getBranchRoot();
  }

 public Diff( TreeAbstraction aTa, Configuration aCf, //SequenceEncoder aSe,
              Object aBranchRoot ) {
   cf = aCf;
   ta = aTa;
//   se = aSe;
   branchRoot = aBranchRoot;
 }

/*  public Diff(Matching am, NodeIndex aIndex) {
    m=am;
    index = aIndex;
  }*/

  /** Encode the diff between the trees passed to the constructor.
   *  @param ch Output encoder for the diff
   */
  public void diff( ContentHandler ch ) throws SAXException {
    ch.startDocument();
    boolean rootHasMatch = ta.lookupBase( branchRoot ) != null; //Q  m.getBranchRoot().hasBaseMatch();
    AttributesImpl rootAtts = new AttributesImpl();
    if( !rootHasMatch )
      addAttribute(rootAtts,cf.DIFF_ROOTOP_ATTR,cf.DIFF_ROOTOP_INS);
    if( cf.DIFF_NS != null && cf.DIFF_NS.length() > 0 )
      ch.startPrefixMapping("diff",cf.DIFF_NS);
    startElem(ch,cf.DIFF_ROOT_TAG,rootAtts);
    if( rootHasMatch ) {
      List stopNodes = ta.getStopNodes(branchRoot);
      copy(ch, stopNodes);
    } else {
      insert(branchRoot,ch);
    }
    endElem(ch,cf.DIFF_ROOT_TAG);
    if( cf.DIFF_NS != null && cf.DIFF_NS.length() > 0 )
      ch.endPrefixMapping("diff");
    ch.endDocument();
  }

  protected void copy( ContentHandler ch, List stopNodes ) throws
      SAXException {
    // Find stopnodes
    Sequence s = new Sequence();
    for (Iterator i = stopNodes.iterator(); i.hasNext(); ) {
      Object stopNode = i.next();
      Object dst = ta.lookupBase(stopNode); //  index.getId(stopNode.getBaseMatch());
      // BUGFIX 030115
      if( !emitChildList(ch, s, stopNode, dst, false) ) {
        AttributesImpl copyAtts = new AttributesImpl();
        addAttribute(copyAtts, cf.DIFF_CPYDST_ATTR, ta.identify(dst));
        startElem(ch,  cf.DIFF_INS_TAG, copyAtts);
        endElem(ch,  cf.DIFF_INS_TAG);
      }
      // ENDBUGFIX
    }
  }


  protected void insert(Object branch, ContentHandler ch) throws
      SAXException {
    // Element node
    Sequence s = new Sequence();
    boolean escape = ta.needsEscape(branch);
    if (escape)
      startElem(ch, cf.DIFF_ESC_TAG, EMPTY_ATTS);
    ta.content(ch,branch,true);
    if (escape)
      endElem(ch, cf.DIFF_ESC_TAG);
    emitChildList(ch, s, branch, NO_DST_REQUIRED, true);
    if (escape)
      startElem(ch, cf.DIFF_ESC_TAG, EMPTY_ATTS);
    ta.content(ch,branch,false);
    if (escape)
      endElem(ch, cf.DIFF_ESC_TAG);
  }


  private boolean emitChildList(ContentHandler ch, Sequence s, Object parent,
                             Object dst, boolean insMode ) throws SAXException {

    Vector children = new Vector();
    for( Iterator i=ta.getChildIterator(parent);i.hasNext();) {
      children.add(i.next());
    }
    for (int ic = 0; ic < children.size() ; ic++) {
      boolean lastStopNode = ic == children.size() - 1;
      Object child = children.elementAt(ic);
      Object baseMatch = ta.lookupBase(child);
      if (  baseMatch != null ) {
        List childStopNodes = ta.getStopNodes(child);
        Object src =  ta.lookupBase(child);
        if (childStopNodes.size() == 0 && !lastStopNode) {
          if (s.isEmpty()) {
            s.init(src, dst);
            continue;
          }
          else if (s.appends(ta,src, dst)) {
            s.append(src);
            continue;
          }
        }
        // Did not append to sequence (or @ last stopnode) => emit sequence
        if (!s.appends(ta,src, dst)) {
          // Current does not append to prev seq -> output prev seq + new
          // in separate tags
          if (!s.isEmpty()) {
            openCopy(s.src, s.dst, s.run, ch);
            closeCopy(ch);
          }
          if (childStopNodes.size() > 0 || lastStopNode) {
            openCopy(src, dst, 1, ch);
            copy( ch, childStopNodes);
            closeCopy(ch);
            s.setEmpty(); // Reset sequence
          }
          else
            s.init(src, dst);
        }
        else { // appends to open sequence (other reason for seq. break)
          s.append(src);
          openCopy(s.src,s.dst, s.run, ch);
          copy( ch, childStopNodes);
          closeCopy(ch);
          s.setEmpty(); // Reset sequence
        }

      } // endif has base match
      else {
        if (!s.isEmpty()) {
          openCopy(s.src, s.dst , s.run, ch);
          closeCopy(ch);
          s.setEmpty();
        }
        if( !insMode ) {
          // Insert tree...
          AttributesImpl copyAtts = new AttributesImpl();
          addAttribute(copyAtts, cf.DIFF_CPYDST_ATTR, ta.identify(dst));
          // SHORTINS = Concatenate several <ins> tags to a single one
          if (ic == 0 || (ta.lookupBase(children.elementAt(ic - 1)) != null)) // SHORTINS
            startElem(ch, cf.DIFF_INS_TAG, copyAtts);
        }
        insert(child, ch);
        if( !insMode ) {
          if (lastStopNode || (ta.lookupBase(children.elementAt(ic + 1)) != null)) // SHORTINS
            endElem( ch, cf.DIFF_INS_TAG);
        }
      }
    } // endfor children
    return children.size() > 0;
  }

  // BFS Enumeration of nodes. Useful beacuse adjacent nodes have subsequent ids
  // => diff can use the "run" attribute more often

  static class BFSIndex implements NodeIndex, IdIndex {

    protected Map nodeToNumber = new HashMap();
    protected Map numberToNode = new HashMap();
    private Object rootId = null;

    public BFSIndex( Node root ) {
      int id = 0;
      LinkedList queue = new LinkedList();
      queue.add(root);
      while( !queue.isEmpty() ) {
        Node n = (Node) queue.removeFirst();
        nodeToNumber.put(n,new Long(id));
        numberToNode.put(String.valueOf(id),n);
        for( int i=0;i<n.getChildCount();i++)
          queue.add(n.getChildAsNode(i));
        id++;
      }
      rootId=getId(root).toString();
    }

    public Object getId(Object n) {
      return nodeToNumber.get(n);
    }

    public Node lookup(Object id) {
      return (Node) numberToNode.get(id.toString());
    }

    public Object getRootId() {
      return rootId;
    }

  }

  protected void openCopy( Object src, Object dst, long run, ContentHandler ch )
      throws SAXException {
    AttributesImpl copyAtts = new AttributesImpl();
    addAttribute( copyAtts, cf.DIFF_CPYSRC_ATTR,  ta.identify(src));
    if( dst != NO_DST_REQUIRED )
      addAttribute(copyAtts, cf.DIFF_CPYDST_ATTR, ta.identify(dst));
    if( run > 1)
      addAttribute(copyAtts, cf.DIFF_CPYRUN_ATTR, String.valueOf(run));
    startElem(ch, cf.DIFF_COPY_TAG, copyAtts);
  }

  protected void closeCopy( ContentHandler ch )
    throws SAXException {
    endElem(ch, cf.DIFF_COPY_TAG);
  }

  // Workaround QName hell

  protected void addAttribute( AttributesImpl a, String name, String value ) {
    if( cf.useQName )
      a.addAttribute("","",name,"CDATA",value);
    else
      a.addAttribute("" /*cf.DIFF_NS*/,name,"","CDATA",value);

  }

  protected void startElem( ContentHandler c , String name, Attributes atts) throws
      SAXException {
    if (cf.useQName)
      c.startElement("","",name,atts);
    else
      c.startElement(cf.DIFF_NS,name,"",atts);
  }


  protected void endElem( ContentHandler c , String name ) throws
      SAXException {
    if (cf.useQName)
      c.endElement("","",name);
    else
      c.endElement(cf.DIFF_NS,name,"");
  }

  class Sequence {
   Object src = null;
   Object dst = null;
   Object tail=null;
   long run = -1;

    void setEmpty() {
      run = -1;
    }

    boolean isEmpty() {
      return run == -1;
    }

    void init(Object asrc, Object adst) {
      src = asrc;
      tail = asrc;
      dst = adst;
      run = 1;
    }

    void append(Object aSrc) {
      run++;
      tail=aSrc;
    }

    boolean appends(TreeAbstraction se,Object asrc, Object adst) {
      return !isEmpty()  && adst.equals(dst) &&
          se.appends(tail,asrc);
    }

/*    Object src = null;
    long run = -1;
    Object dst = null;
    long nsrc;
    boolean srcIsNum=false;

    void setEmpty() {
      run = -1;
    }

    boolean isEmpty() {
      return run == -1;
    }

    void init(Object asrc, Object adst) {
      src = asrc;
      if( src instanceof Number ) {
        srcIsNum = true;
        nsrc = ((Number) src).longValue();
      }
      dst = adst;
      run = 1;
    }

    void append() {
      run++;
    }

    boolean appends(Object asrc, Object adst) {
      return !isEmpty()  && adst.equals(dst) && srcIsNum &&
          asrc instanceof Number && ((Number) asrc).longValue() == (nsrc + run);
    }*/
  }

  public static class Configuration {

    protected Set RESERVED=null;

    protected String DIFF_NS =""; //diff:"; //XDiFf-"; // should be solved using proper namespaces!
    protected String DIFF_COPY_TAG = DIFF_NS+"copy";
    protected String DIFF_INS_TAG = DIFF_NS+"insert";
    protected String DIFF_ESC_TAG = DIFF_NS+"esc";
    protected String DIFF_ROOT_TAG = "diff";

    protected String DIFF_CPYSRC_ATTR = "src";
    protected String DIFF_CPYDST_ATTR = "dst";
    protected String DIFF_CPYRUN_ATTR = "run";
    protected String DIFF_ROOTOP_ATTR = "op";

    protected final String DIFF_ROOTOP_INS = "insert";

    protected boolean useQName = true;

    public Configuration() {
      init();
    }

    public Configuration( String aNameSpace,
                          String aCopyTag,
                          String aInsTag,
                          String aEscTag,
                          String aRootTag,
                          String aCopySrcAttr,
                          String aCopyDstAttr,
                          String aCopyRunAttr,
                          String aRootOpAttr,
                          boolean aUseQNames ) {
      DIFF_NS = aNameSpace;
      DIFF_COPY_TAG = aCopyTag;
      DIFF_INS_TAG = aInsTag;
      DIFF_ESC_TAG = aEscTag;
      DIFF_ROOT_TAG = aRootTag;
      DIFF_CPYSRC_ATTR = aCopySrcAttr;
      DIFF_CPYDST_ATTR = aCopyDstAttr;
      DIFF_CPYRUN_ATTR = aCopyRunAttr;
      DIFF_ROOTOP_ATTR = aRootOpAttr;
      useQName = aUseQNames;
      init();
    }

    protected void init() {
      RESERVED = new HashSet();
      RESERVED.add(DIFF_COPY_TAG);
      RESERVED.add(DIFF_INS_TAG);
      RESERVED.add(DIFF_ESC_TAG);
    }
  }

  public interface TreeAbstraction {
    public List getStopNodes( Object changeNode );
    public Object lookupBase( Object changeNode );
    public void content( ContentHandler ch, Object changeNode, boolean start) throws SAXException; // return open tagname
    public boolean needsEscape( Object changeNode );
    public Iterator getChildIterator(Object changeNode);
    public String identify(Object changeNode);
    //public String getRun(Object start, Object end);
    public boolean appends( Object baseTail, Object baseNext );

  }

/*
  public interface SequenceEncoder {
    public String identify(Object node);
    //public String getRun(Object start, Object end);
    public boolean appends( Object current, Object next );
  }*/

  protected static class TdmTreeAbstraction implements TreeAbstraction {

    Matching m;
    NodeIndex ix;

    public TdmTreeAbstraction( Matching am, NodeIndex aIx ) {
      m=am;
      ix = aIx;
    }

    public List getStopNodes(Object changeNode) {
      Vector v = new Vector();
      m.getAreaStopNodes(v,(BranchNode) changeNode);
      return v;
    }

    public Object lookupBase(Object changeNode) {
      return ((BranchNode) changeNode).getBaseMatch();
    }

    public Object getChangeRoot() {
      return m.getBranchRoot();
    }

    public void content(ContentHandler ch, Object branch, boolean open)
        throws SAXException {
      XMLNode content = ((BranchNode) branch).getContent();
      if (content instanceof XMLTextNode ) {
        if( !open )
          return;
        XMLTextNode ct = (XMLTextNode) content;
        ch.characters(ct.getText(), 0, ct.getText().length);
      }  else {
        // Element node
        XMLElementNode ce = (XMLElementNode) content;
        if( open)
          ch.startElement(ce.getNamespaceURI(), ce.getLocalName(), ce.getQName(),
                        ce.getAttributes());
        else
          ch.endElement(ce.getNamespaceURI(), ce.getLocalName(), ce.getQName());
      }

    }
    public boolean needsEscape(Object changeNode) {
      XMLNode content = ((BranchNode) changeNode).getContent();
      if( content instanceof XMLElementNode &&
        DEFAULT_CONFIG.RESERVED.contains(((XMLElementNode) content).getQName()) )
        return true;
      return false;
    }

    public Iterator getChildIterator(Object changeNode) {
      return ((BranchNode) changeNode).children.listIterator();
    }

    public boolean appends(Object tail, Object next) {
      return ((Number) ix.getId(next)).longValue() ==
          ((Number) ix.getId(tail)).longValue() +1 ;
    }

    public String identify(Object node) {
      return ix.getId(node).toString();
    }

  }

/*
  class TdmSequence implements SequenceEncoder {

    NodeIndex ix;

    public TdmSequence(NodeIndex aIx) {
      ix = aIx;
    }

    public boolean appends(Object tail, Object next) {
      return ((Number) ix.getId(next)).longValue() ==
          ((Number) ix.getId(tail)).longValue() +1 ;
    }

    public String identify(Object node) {
      return ix.getId(node).toString();
    }
  }*/
}
