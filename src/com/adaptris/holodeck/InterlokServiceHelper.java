package com.adaptris.holodeck;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;

public interface InterlokServiceHelper {

  public void runUserMessageService(AdaptrisMessage message) throws CoreException;
  
  public void runSignalMessageService(AdaptrisMessage message) throws CoreException;
  
}
