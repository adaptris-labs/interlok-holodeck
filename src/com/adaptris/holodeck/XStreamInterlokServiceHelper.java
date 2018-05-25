package com.adaptris.holodeck;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.Service;
import com.adaptris.core.XStreamMarshaller;
import com.adaptris.core.util.LifecycleHelper;

public class XStreamInterlokServiceHelper implements InterlokServiceHelper {

  private String userMessageServicePath;
  
  private String signalMessageServicePath;
  
  private Service userMessageService;
  
  private Service signalMessageService;
  
  public XStreamInterlokServiceHelper(String userMessageServicePath, String signalMessageServicePath) {
    this.setSignalMessageServicePath(signalMessageServicePath);
    this.setUserMessageServicePath(userMessageServicePath);
  }
  
  @Override
  public void runUserMessageService(AdaptrisMessage message) throws CoreException {
    if(this.getUserMessageService() == null) {
      this.setUserMessageService(this.createUserMessageService());
      LifecycleHelper.init(this.getUserMessageService());
      LifecycleHelper.start(this.getUserMessageService());
    }
    
    this.getUserMessageService().doService(message);
  }

  @Override
  public void runSignalMessageService(AdaptrisMessage message) throws CoreException {
    if(this.getSignalMessageService() == null) {
      this.setSignalMessageService(this.createSignalMessageService());
      LifecycleHelper.init(this.getSignalMessageService());
      LifecycleHelper.start(this.getSignalMessageService());
    }
    
    this.getSignalMessageService().doService(message);
  }

  private Service createUserMessageService() throws CoreException{
    return createService(this.getUserMessageServicePath());
  }
  
  private Service createSignalMessageService() throws CoreException {
    return createService(this.getSignalMessageServicePath());
  }
  
  private Service createService(String marshalledServicePath) throws CoreException {
    if(StringUtils.isEmpty(marshalledServicePath))
      throw new CoreException("User message service path not set.");
    
    File marshalledServiceFile = new File(marshalledServicePath);
    if((!marshalledServiceFile.exists()) || (marshalledServiceFile.isDirectory()))
      throw new CoreException("Marshalled service file doies not exist; " + marshalledServiceFile.getAbsolutePath());
    
    try {
      return (Service) new XStreamMarshaller().unmarshal(marshalledServiceFile);
    } catch (Exception ex) {
      throw new CoreException(ex);
    }
  }

  public String getUserMessageServicePath() {
    return userMessageServicePath;
  }

  public void setUserMessageServicePath(String userMessageServicePath) {
    this.userMessageServicePath = userMessageServicePath;
  }

  public String getSignalMessageServicePath() {
    return signalMessageServicePath;
  }

  public void setSignalMessageServicePath(String signalMessageServicePath) {
    this.signalMessageServicePath = signalMessageServicePath;
  }

  public Service getUserMessageService() {
    return userMessageService;
  }

  public void setUserMessageService(Service userMessageService) {
    this.userMessageService = userMessageService;
  }

  public Service getSignalMessageService() {
    return signalMessageService;
  }

  public void setSignalMessageService(Service signalMessageService) {
    this.signalMessageService = signalMessageService;
  }

}
