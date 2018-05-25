package com.adaptris.holodeck;

import java.util.Map;

import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;

public class InterlokDeliverer implements IMessageDeliverer {
  
  private static final String USER_MESSAGE_SERVICE_PROPERTY = "userMessageServicePath";
  
  private static final String SIGNAL_MESSAGE_SERVICE_PROPERTY = "signalMessageServicePath";
  
  private MessageTranslator messageTranslator;

  private InterlokServiceHelper serviceHelper;
  
  private Map<String, ?> properties;
  
  public InterlokDeliverer(Map<String, ?> properties) {
    this.setProperties(properties);
    this.setMessageTranslator(new HolodeckMessageTranslator());
    this.setServiceHelper(new XStreamInterlokServiceHelper((String) properties.get(USER_MESSAGE_SERVICE_PROPERTY), (String) properties.get(SIGNAL_MESSAGE_SERVICE_PROPERTY)));
  }
  
  @Override
  public void deliver(IMessageUnit rcvdMsgUnit) throws MessageDeliveryException {
    try {
      if (rcvdMsgUnit instanceof IUserMessage)
        deliverUserMessage((IUserMessage) rcvdMsgUnit);
      else // message unit is a signal
        deliverSignalMessage((ISignalMessage) rcvdMsgUnit);
    } catch (CoreException ex) {
      throw new MessageDeliveryException(ex.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private void deliverUserMessage(IUserMessage rcvdMsgUnit) throws CoreException, MessageDeliveryException {
    AdaptrisMessage adaptrisMessage = this.getMessageTranslator().translate(rcvdMsgUnit);
    this.getServiceHelper().runUserMessageService(adaptrisMessage);
  }

  private void deliverSignalMessage(ISignalMessage rcvdMsgUnit) throws CoreException, MessageDeliveryException {
    AdaptrisMessage adaptrisMessage = this.getMessageTranslator().translate(rcvdMsgUnit);
    this.getServiceHelper().runSignalMessageService(adaptrisMessage);
  }

  public Map<String, ?> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, ?> properties) {
    this.properties = properties;
  }

  public MessageTranslator getMessageTranslator() {
    return messageTranslator;
  }

  public void setMessageTranslator(MessageTranslator messageTranslator) {
    this.messageTranslator = messageTranslator;
  }

  public InterlokServiceHelper getServiceHelper() {
    return serviceHelper;
  }

  public void setServiceHelper(InterlokServiceHelper serviceHelper) {
    this.serviceHelper = serviceHelper;
  }


}
