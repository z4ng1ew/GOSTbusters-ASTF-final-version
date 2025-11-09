package org.owasp.astf.testcases.bola.strategy;

import java.util.List;

public interface IdGenerationStrategy {
    List<String> generateIds(List<String> ownAccountIds);
}