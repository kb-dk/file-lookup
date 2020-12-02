package dk.kb.lookup.webservice;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import dk.kb.lookup.config.LookupServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener to handle the various setups and configuration sanity checks that can be carried out at when the
 * context is deployed/initalized.
 */

public class LookupContextListener implements ServletContextListener {
    private final Logger log = LoggerFactory.getLogger(getClass());


    /**
     * On context initialisation this
     * i) Initialises the logging framework (logback).
     * ii) Initialises the configuration class.
     * @param sce context provided by the web server upon initialization.
     * @throws java.lang.RuntimeException if anything at all goes wrong.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            log.info("Initializing Template service v{}", getClass().getPackage().getImplementationVersion());
            InitialContext ctx = new InitialContext();
            String configFile = (String) ctx.lookup("java:/comp/env/application-config");
            LookupServiceConfig.initialize(configFile);
        } catch (NamingException e) {
            throw new RuntimeException("Failed to lookup settings", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load settings", e);        } 
        log.info("Template service initialized.");
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.debug("Template service destroyed");
    }

}
