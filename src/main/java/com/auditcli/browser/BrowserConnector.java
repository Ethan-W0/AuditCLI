package com.auditcli.browser;

public interface BrowserConnector {
    String status();

    String connectDefault();

    String disconnect();
}
