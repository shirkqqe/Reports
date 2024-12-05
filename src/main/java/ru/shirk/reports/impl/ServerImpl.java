package ru.shirk.reports.impl;

import ru.shirk.reports.Reports;

public class ServerImpl implements Server {
    @Override
    public String getName() {
        return Reports.getConfigurationManager().getConfig("settings.yml").c("properties.serverName");
    }

    @Override
    public String getProxyName() {
        return Reports.getConfigurationManager().getConfig("settings.yml").c("properties.server");
    }
}
