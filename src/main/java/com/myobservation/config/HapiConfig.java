package com.myobservation.config;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración de Spring para proveer un bean de HapiContext.
 * Esto permite que HapiContext sea inyectado en otros servicios como HL7ParserService.
 */
@Configuration // Indica a Spring que esta clase contiene definiciones de beans
public class HapiConfig {

    /**
     * Define un bean de tipo HapiContext.
     * Spring llamará a este método una vez al iniciar la aplicación para crear la instancia.
     * Luego, esta instancia podrá ser inyectada automáticamente donde se necesite.
     * @return Una instancia configurada de HapiContext.
     */
    @Bean // Marca este método como un productor de un bean de Spring
    public HapiContext hapiContext() {
        DefaultHapiContext context = new DefaultHapiContext();
        context.getParserConfiguration().setValidating(false);
        return context;
    }
}