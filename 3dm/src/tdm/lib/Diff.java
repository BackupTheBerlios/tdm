// $Id: Diff.java,v 1.14 2006/02/06 09:01:39 ctl Exp $ D
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

/** Produces the diff between two naturally matched trees.
 *  Collapsing multiple copy-ops using the run attribute is not implemented in
 *  this version.
 */

public class Diff {

  private static final Object NO_DST_REQUIRED = new Object();
    // Above must not be null; that -> nullptrex in sequence

  private NodeIndex index = null;

  private Matching m = null;
  private static final Attributes EMPTY_ATTS = new AttributesImpl();
  static final Set RESERVED;

  static final String DIFF_NS ="diff:";
  static {
      RESERVED = new HashSet();
      RESERVED.add(DIFF_NS+"copy");
      RESERVED.add(DIFF_NS+"insert");
      RESERVED.add(DIFF_NS+"esc");
  }

  /** Construct a diff operating on the matched trees passed to the constructor.
   *  Note that the matching contains pointers to the base and new trees.
   *  @param am Matching between trees to diff
   */
  public Diff(Matching am) {
    m=am;
    index = new BFSIndex(m.getBaseRoot());
  }

  public Diff(Matching am, NodeIndex aIndex) {
    m=am;
    index = aIndex;
  }

  /** Encode the diff between the trees passed to the constructor.
   *  @param ch Output encoder for the diff
   */
  public void diff( ContentHandler ch ) throws SAXException {
    ch.startDocument();
    ch.startElement("","","diff",EMPTY_ATTS);
    Vector stopNodes = new Vector();
    m.getAreaStopNodes(stopNodes, m.getBranchRoot());
    copy(m.getBaseRoot(), m.getBranchRoot(), ch, stopNodes);
    ch.endElement("","","diff");
    ch.endDocument();
  }

  protected void copy(BaseNode b, BranchNode branch, ContentHandler ch,
                      Vector stopNodes) throws
      SAXException {
    // Find stopnodes
    Sequence s = new Sequence();
    for (Iterator i = stopNodes.iterator(); i.hasNext(); ) {
      BranchNode stopNode = (BranchNode) i.next();
      Object dst = index.getId(stopNode.getBaseMatch());
      // BUGFIX 030115
      if (stopNode.getChildCount() == 0) {
        AttributesImpl copyAtts = new AttributesImpl();
        copyAtts.addAttribute("", "", "dst", "CDATA", String.valueOf(dst));
        ch.startElement("", "", DIFF_NS + "insert", copyAtts);
        ch.endElement("", "", DIFF_NS + "insert");
      }
      // ENDBUGFIX
      emitChildList(ch, s, stopNode, dst, false);
    }
  }


  protected void insert(BranchNode branch, ContentHandler ch) throws
      SAXException {
    XMLNode content = branch.getContent();
    if (content instanceof XMLTextNode) {
      XMLTextNode ct = (XMLTextNode) content;
      ch.characters(ct.getText(), 0, ct.getText().length);
    }
    else {
      // Element node
      Sequence s = new Sequence();
      XMLElementNode ce = (XMLElementNode) content;
      boolean escape = RESERVED.contains(ce.getQName());
      if (escape)
        ch.startElement("", "", DIFF_NS + "esc", EMPTY_ATTS);
      ch.startElement(ce.getNamespaceURI(), ce.getLocalName(), ce.getQName(),
                      ce.getAttributes());
      if (escape)
        ch.endElement("", "", DIFF_NS + "esc");
      emitChildList(ch, s, branch, NO_DST_REQUIRED , true);
      if (escape)
        ch.startElement("", "", DIFF_NS + "esc", EMPTY_ATTS);
      ch.endElement(ce.getNamespaceURI(), ce.getLocalName(), ce.getQName());
      if (escape)
        ch.endElement("", "", DIFF_NS + "esc");
    }
  }


  private void emitChildList(ContentHandler ch, Sequence s, BranchNode parent,
                             Object dst, boolean insMode ) throws SAXException {
    for (int ic = 0; ic < parent.getChildCount(); ic++) {
      boolean lastStopNode = ic == parent.getChildCount() - 1;
      BranchNode child = parent.getChild(ic);
      if (child.hasBaseMatch()) {
        Vector childStopNodes = new Vector();
        m.getAreaStopNodes(childStopNodes, child);
        Object src = index.getId(child.getBaseMatch());
        if (childStopNodes.size() == 0 && !lastStopNode) {
          if (s.isEmpty()) {
            s.init(src, dst);
            continue;
          }
          else if (s.appends(src, dst)) {
            s.append();
            continue;
          }
        }
        // Did not append to sequence (or @ last stopnode) => emit sequence
        if (!s.appends(src, dst)) {
          // Current does not append to prev seq -> output prev seq + new
          // in separate tags
          if (!s.isEmpty()) {
            openCopy(s.src, s.dst, s.run, ch);
            closeCopy(ch);
          }
          if (childStopNodes.size() > 0 || lastStopNode) {
            openCopy(src, dst, 1, ch);
            copy(child.getBaseMatch(), child, ch, childStopNodes);
            closeCopy(ch);
            s.setEmpty(); // Reset sequence
          }
          else
            s.init(src, dst);
        }
        else { // appends to open sequence (other reason for seq. break)
          s.append();
          openCopy(s.src, s.dst, s.run, ch);
          copy(child.getBaseMatch(), child, ch, childStopNodes);
          closeCopy(ch);
          s.setEmpty(); // Reset sequence
        }

      } // endif has base match
      else {
        if (!s.isEmpty()) {
          openCopy(s.src, s.dst, s.run, ch);
          closeCopy(ch);
          s.setEmpty();
        }
        if( !insMode ) {
          // Insert tree...
          AttributesImpl copyAtts = new AttributesImpl();
          copyAtts.addAttribute("", "", "dst", "CDATA", String.valueOf(dst));
          // SHORTINS = Concatenate several <ins> tags to a single one
          if (ic == 0 || parent.getChild(ic - 1).hasBaseMatch()) // SHORTINS
            ch.startElement("", "", DIFF_NS + "insert", copyAtts);
        }
        insert(child, ch);
        if( !insMode ) {
          if (lastStopNode || parent.getChild(ic + 1).hasBaseMatch()) // SHORTINS
            ch.endElement("", "", DIFF_NS + "insert");
        }
      }
    } // endfor children
  }

  // BFS Enumeration of nodes. Useful beacuse adjacent nodes have subsequent ids
  // => diff can use the "run" attribute more often

  static class BFSIndex implements NodeIndex, IdIndex {

    protected Map nodeToNumber = new HashMap();
    protected Map numberToNode = new HashMap();
    private String rootId = null;

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

    public Object getId(Node n) {
      return nodeToNumber.get(n);
    }

    public Node lookup(Object id) {
      return (Node) numberToNode.get(id);
    }

    public String getRootId() {
      return rootId;
    }

  }

  protected void openCopy( Object src, Object dst, long run, ContentHandler ch )
      throws SAXException {
    AttributesImpl copyAtts = new AttributesImpl();
    copyAtts.addAttribute("", "", "src", "CDATA", String.valueOf(src));
    if( dst != NO_DST_REQUIRED )
      copyAtts.addAttribute("", "", "dst", "CDATA", String.valueOf(dst));
    if( run > 1)
      copyAtts.addAttribute("", "", "run", "CDATA", String.valueOf(run));
    ch.startElement("", "", DIFF_NS + "copy", copyAtts);
  }

  protected void closeCopy( ContentHandler ch )
    throws SAXException {
    ch.endElement("", "", DIFF_NS + "copy");
  }

  protected void openInsert( Object src, Object dst ) {

  }

  protected void closeInsert() {

  }

  class Sequence {
    Object src = null;
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
      return !isEmpty() && adst.equals(dst) && srcIsNum &&
          asrc instanceof Number && ((Number) asrc).longValue() == (nsrc + run);
    }
  }
}
