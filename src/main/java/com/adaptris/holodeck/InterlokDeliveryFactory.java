package com.adaptris.holodeck;

import java.util.Map;

import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.IMessageDelivererFactory;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;

public class InterlokDeliveryFactory implements IMessageDelivererFactory {

  private Map<String, ?> properties;
  
  private IMessageDeliverer messageDeliverer;
  
  @Override
  public IMessageDeliverer createMessageDeliverer() throws MessageDeliveryException {
    if(this.getMessageDeliverer() == null) {
      this.setMessageDeliverer(new InterlokDeliverer(this.getProperties()));
    }
    
    return this.getMessageDeliverer();
  }

  @Override
  public void init(Map<String, ?> properties) throws MessageDeliveryException {
    this.setProperties(properties);
  }

  public Map<String, ?> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, ?> properties) {
    this.properties = properties;
  }

  public IMessageDeliverer getMessageDeliverer() {
    return messageDeliverer;
  }

  public void setMessageDeliverer(IMessageDeliverer messageDeliverer) {
    this.messageDeliverer = messageDeliverer;
  }

}
