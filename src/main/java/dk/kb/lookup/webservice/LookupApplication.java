package dk.kb.lookup.webservice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import dk.kb.lookup.config.ServiceConfig;
import dk.kb.webservice.ServiceExceptionMapper;

public class LookupApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(
                JacksonJsonProvider.class,
                ServiceConfig.getImplementationClass(),
                ServiceExceptionMapper.class
        ));
    }
}
