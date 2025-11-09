package org.owasp.astf.testcases.bola;

import org.owasp.astf.testcases.bola.strategy.IdGenerationStrategy;
import org.owasp.astf.testcases.bola.service.ConsentService;

public class BolaTestContext {
    private final IdGenerationStrategy idStrategy;
    private final ConsentService consentService;

    public BolaTestContext(IdGenerationStrategy idStrategy, ConsentService consentService) {
        this.idStrategy = idStrategy;
        this.consentService = consentService;
    }

    public IdGenerationStrategy getIdStrategy() {
        return idStrategy;
    }

    public ConsentService getConsentService() {
        return consentService;
    }
}