package com.auditcli.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record McpCapabilities() {
}
