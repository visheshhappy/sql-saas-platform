package com.thp.sqlsaas.server;

import com.thp.sqlsaas.connector.ConnectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerController {
    
    @Autowired
    private ConnectorService connectorService;
    
    @GetMapping("/health")
    public String health() {
        return "Server is running! " + connectorService.getMessage();
    }
}
