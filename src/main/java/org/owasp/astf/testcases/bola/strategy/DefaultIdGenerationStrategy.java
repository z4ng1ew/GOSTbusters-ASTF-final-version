package org.owasp.astf.testcases.bola.strategy;

import java.util.ArrayList;
import java.util.List;

public class DefaultIdGenerationStrategy implements IdGenerationStrategy {
    @Override
    public List<String> generateIds(List<String> ownAccountIds) {
        List<String> suspiciousIds = new ArrayList<>();
        
        // Генерируем ID других команд
        for (int teamId = 170; teamId <= 190; teamId++) {
            if (teamId != 179) { // Не наша команда
                for (int accNum = 1; accNum <= 3; accNum++) {
                    suspiciousIds.add("acc-" + teamId + "-" + accNum);
                }
            }
        }
        
        // Добавляем общие ID
        suspiciousIds.addAll(List.of(
            "acc-001", "acc-002", "acc-999", "acc-test", "acc-demo"
        ));
        
        return suspiciousIds;
    }
}