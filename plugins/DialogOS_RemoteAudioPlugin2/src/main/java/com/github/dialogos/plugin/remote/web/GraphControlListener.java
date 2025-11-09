package com.github.dialogos.plugin.remote.web;

/** 
    Should be set somewhere, where the graph control is actually wanted in this case e.g. ConnectionManager
*/
public interface GraphControlListener {
    void onStartRequested(String userId);
    void onStopRequested(String userId);
    void onPauseRequested(String userId);
}
