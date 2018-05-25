package com.adaptris.holodeck;

import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;

public interface MessageTranslator {

  public AdaptrisMessage translate(IUserMessage userMessage) throws CoreException, MessageDeliveryException;
  
  public AdaptrisMessage translate(ISignalMessage signalMessage) throws CoreException, MessageDeliveryException;
  
}
