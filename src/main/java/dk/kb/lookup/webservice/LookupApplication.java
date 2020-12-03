package dk.kb.lookup.webservice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import dk.kb.lookup.api.impl.MemoryImpl;
import dk.kb.lookup.webservice.exception.ServiceExceptionMapper;

public class LookupApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(
                JacksonJsonProvider.class,
                MemoryImpl.class,
                ServiceExceptionMapper.class
        ));
    }
}
