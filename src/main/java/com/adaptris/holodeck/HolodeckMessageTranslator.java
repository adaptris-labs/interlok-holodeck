package com.adaptris.holodeck;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.util.base64.Base64EncodingWriterOutputStream;
import org.holodeckb2b.common.messagemodel.Receipt;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.mmd.xml.Property;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.packaging.ErrorSignalElement;
import org.holodeckb2b.ebms3.packaging.PayloadInfoElement;
import org.holodeckb2b.ebms3.packaging.ReceiptElement;
import org.holodeckb2b.ebms3.packaging.UserMessageElement;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultMessageFactory;

public class HolodeckMessageTranslator implements MessageTranslator {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  
  /**
   * The namespace URI of the XML schema that defines the structure of the delivery file
   */
  private static final String DELIVERY_NS_URI = "http://holodeck-b2b.org/schemas/2018/01/delivery/single_xml";
  /**
   * The QName of the container element that contains the message info
   */
  private static final String XML_ROOT_NAME = "ebmsMessage";
  
  /**
   * The QName of the container element that contains the message info, i.e. the <code>eb:Messaging</code> element
   */
  protected static final QName ROOT_QNAME = new QName(EbMSConstants.EBMS3_NS_URI, "Messaging",
                                                         EbMSConstants.EBMS3_NS_PREFIX);

  protected static final String RECEIPT_CHILD_NS_URI =
                                              "http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild";
  protected static final String RECEIPT_CHILD_ELEM_NAME = "ReceiptChild";

  @Override
  public AdaptrisMessage translate(IUserMessage userMessage) throws CoreException, MessageDeliveryException {
    AdaptrisMessage message = DefaultMessageFactory.getDefaultInstance().newMessage();
    doXmlTranslation(message, userMessage);
    
    return message;
  }

  @Override
  public AdaptrisMessage translate(ISignalMessage signalMessage) throws CoreException, MessageDeliveryException {
    AdaptrisMessage message = DefaultMessageFactory.getDefaultInstance().newMessage();
    
    doXmlTranslation(message, signalMessage);

    return message;
  }

  
  private void doXmlTranslation(AdaptrisMessage message, ISignalMessage sigMsgUnit) throws MessageDeliveryException {
    final OMElement   container = createContainerElementName();

    if (sigMsgUnit instanceof IReceipt) {
        log.debug("Create a new Receipt to prevent content from inclusion in XML");
        IReceipt deliveryReceipt = createDeliveryReceipt((IReceipt) sigMsgUnit);
        log.debug("Add receipt meta data to XML");
        ReceiptElement.createElement(container, deliveryReceipt);
    } else if (sigMsgUnit instanceof IErrorMessage) {
        log.debug("Add error meta data to XML");
        ErrorSignalElement.createElement(container, (IErrorMessage) sigMsgUnit);
    }

    log.debug("Added signal meta data to XML, write to disk");
    try {
        writeXMLDocument(message.getWriter(), container, sigMsgUnit.getMessageId());
        log.info("Signal message with msgID=" + sigMsgUnit.getMessageId() + " successfully delivered");
    } catch (final Exception ex) {
        log.error("An error occurred while delivering the signal message [" + sigMsgUnit.getMessageId()
                                                                + "]\n\tError details: " + ex.getMessage());
        // And signal failure
        throw new MessageDeliveryException("Unable to deliver signal message [" + sigMsgUnit.getMessageId()
                                                + "]. Error details: " + ex.getMessage());
    }
    
  }

  private void doXmlTranslation(AdaptrisMessage message, IUserMessage usrMsgUnit) throws MessageDeliveryException {
    log.debug("Delivering user message with msgId=" + usrMsgUnit.getMessageId());

    // We first convert the user message into a MMD document
    final MessageMetaData mmd = new MessageMetaData((IUserMessage) usrMsgUnit);

    final OMElement    container = createContainerElementName();

    log.debug("Add general message info to XML container");
    // Add the information on the user message to the container
    final OMElement  usrMsgElement = UserMessageElement.createElement(container, mmd);

    if (!Utils.isNullOrEmpty(mmd.getPayloads())) {
        log.debug("Add payload meta info to XML container");
        // Generate a element id and set this a reference in payload property
        int i = 1;
        for (final IPayload p : mmd.getPayloads()) {
            final Property refProp = new Property();
            refProp.setName("org:holodeckb2b:ref");
            refProp.setValue("pl-" + i++);
            p.getProperties().add(refProp);
        }
        PayloadInfoElement.createElement(usrMsgElement, mmd.getPayloads());
    }

    Writer fw = null;
    try {
        log.debug("Message meta data complete, start writing this to AdaptrisMessage");
        fw = message.getWriter();
        final XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(fw);
        log.debug("Write the meta data to file");
        xmlWriter.writeStartElement(XML_ROOT_NAME);
        usrMsgElement.serialize(xmlWriter);
        xmlWriter.flush();
        xmlWriter.close();
        log.debug("Meta data writen to file");
        if (!Utils.isNullOrEmpty(mmd.getPayloads())) {
            log.debug("Write payload contents");
            fw.write("<Payloads>");
            int i = 1;
            for(final IPayload p : mmd.getPayloads()) {
                log.debug("Create <Payload> element");
                fw.write("<Payload xml:id=\"pl-" + i++ + "\">");
                writeEncodedPayload(p.getContentLocation(), fw);
                fw.write("</Payload>\n");
            }
            log.debug("Close the <Payloads> element");
            fw.write("</Payloads>\n");
        }
        fw.write("</" + XML_ROOT_NAME + ">");
        fw.close();
        log.info("User message with msgID=" + mmd.getMessageId() + " successfully delivered");
    } catch (IOException | XMLStreamException ex) {
        log.error("An error occurred while delivering the user message [" + mmd.getMessageId()
                                                                + "]\n\tError details: " + ex.getMessage());
        // And signal failure
        throw new MessageDeliveryException("Unable to deliver user message [" + mmd.getMessageId()
                                                + "]. Error details: " + ex.getMessage());
    }
    
  }
  
  protected OMElement createContainerElementName() {
    final OMFactory   f = OMAbstractFactory.getOMFactory();
    final OMElement rootElement = f.createOMElement(XML_ROOT_NAME, DELIVERY_NS_URI, XMLConstants.DEFAULT_NS_PREFIX);
    // Declare the namespaces
    rootElement.declareDefaultNamespace(DELIVERY_NS_URI);
    rootElement.declareNamespace(EbMSConstants.EBMS3_NS_URI, EbMSConstants.EBMS3_NS_PREFIX);

    return rootElement;
  }
  
  /**
   * Helper method to write the payload content base64 encoded to an output stream.
   *
   * @param sourceFile        The file to add to the output
   * @param output            The output writer
   * @throws IOException      When reading from the source or writing to the output fails
   */
  @SuppressWarnings("resource")
  private void writeEncodedPayload(final String sourceFile, final Writer output) throws IOException {
    final Base64EncodingWriterOutputStream b64os;
    try (FileInputStream fis = new FileInputStream(sourceFile)) {
      b64os = new Base64EncodingWriterOutputStream(output);
      final byte[] buffer = new byte[4096];
      int r = fis.read(buffer);
      while (r > 0) {
        b64os.write(buffer, 0, r);
        r = fis.read(buffer);
      }
    }
    b64os.complete();
    b64os.flush();
  }
  
  private void writeXMLDocument(Writer amWriter, final OMElement xml, final String msgId) throws Exception {
    final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(amWriter);
    xml.serialize(writer);
    writer.flush();
    writer.close();
    amWriter.flush();
    amWriter.close();
  }
  
  /**
   * Helper class to create a new Receipt that includes only the name of the first element of the original Receipt's
   * content.
   *
   * @param originalReceipt   The original Receipt
   * @return  A new Receipt with only the name of the first element of the original as content.
   */
  private IReceipt createDeliveryReceipt(IReceipt originalReceipt) {
    Receipt deliveryReceipt = new Receipt(originalReceipt);

    final OMElement rcptChild = OMAbstractFactory.getOMFactory().createOMElement(RECEIPT_CHILD_ELEM_NAME, RECEIPT_CHILD_NS_URI, "");
    rcptChild.declareDefaultNamespace(RECEIPT_CHILD_NS_URI);

    // If there was actual content (and we could get access to it) use the name of
    // first element
    final String firstReceiptChildName = originalReceipt.getContent().get(0).getLocalName();
    String mmdRcptName;
    switch (firstReceiptChildName) {
    case "NonRepudiationInformation":
      mmdRcptName = "ebbp:NonRepudiationInformation";
      break;
    case "UserMessage":
      mmdRcptName = "eb:UserMessage";
      break;
    default:
      mmdRcptName = "unspecified";
    }
    rcptChild.setText(mmdRcptName);

    deliveryReceipt.setContent(Arrays.asList(new OMElement[] { rcptChild }));

    return deliveryReceipt;
  }
}
