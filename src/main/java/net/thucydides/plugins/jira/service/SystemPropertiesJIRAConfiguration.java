package net.thucydides.plugins.jira.service;

import com.google.inject.Inject;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.LocalPreferences;
import net.thucydides.core.util.PropertiesFileLocalPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Obtain the JIRA configuration details from system properties.
 */
public class SystemPropertiesJIRAConfiguration implements JIRAConfiguration {

    private final EnvironmentVariables environmentVariables;

    public static final String JIRA_URL = "jira.url";
    public static final String JIRA_PROJECT = "jira.project";
    public static final String JIRA_USERNAME = "jira.username";
    public static final String JIRA_PASSWORD = "jira.password";
    public static final String JIRA_WIKI_RENDERER = "jira.wiki.renderer";


    private final Logger logger = LoggerFactory.getLogger(SystemPropertiesJIRAConfiguration.class);

    @Inject
    public SystemPropertiesJIRAConfiguration(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
        updateEnvironmentVariablesFromPropertiesFiles(environmentVariables);
    }

    private void updateEnvironmentVariablesFromPropertiesFiles(EnvironmentVariables environmentVariables) {
        LocalPreferences localPreferences = new PropertiesFileLocalPreferences(environmentVariables);
        try {
            localPreferences.loadPreferences();
        } catch (IOException e) {
            logger.warn("Failed to load JIRA preferences from thucydides.properties file", e);
        }
    }

    public String getJiraUser() {
        return getEnvironmentOrSystemProperty(JIRA_USERNAME);
    }

    private String getEnvironmentOrSystemProperty(String field) {
        return getEnvironmentOrSystemProperty(field, null);
    }

    private String getEnvironmentOrSystemProperty(String field, String defaultValue) {
        if (environmentVariables.getProperty(field) != null) {
            return environmentVariables.getProperty(field);
        }
        if (environmentVariables.getValue(field) != null) {
            return environmentVariables.getValue(field);
        }
        return environmentVariables.getValue(field);
    }

    public String getJiraPassword() {
        return getEnvironmentOrSystemProperty(JIRA_PASSWORD);
    }

    public boolean isWikiRenderedActive() {
        return Boolean.valueOf(getEnvironmentOrSystemProperty(JIRA_WIKI_RENDERER, "true"));
    }

    public String getProject() {
        return getEnvironmentOrSystemProperty(JIRA_PROJECT);
    }

    public String getJiraWebserviceUrl() {
        String baseUrl = environmentVariables.getProperty(JIRA_URL);
        return (baseUrl != null) ? baseUrl + "/rpc/soap/jirasoapservice-v2" : null;
    }

    public String getJiraUrl() {
        return environmentVariables.getProperty(JIRA_URL);
    }
}
