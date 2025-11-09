package org.owasp.astf.testcases.bola;

import java.util.ArrayList;
import java.util.List;

public class SimpleIdGenerationStrategy {
    public List<String> generateSuspiciousIds(List<String> ourIds) {
        List<String> suspicious = new ArrayList<>();
        
        // Генерируем ID других команд
        for (int teamId = 170; teamId <= 190; teamId++) {
            if (teamId != 179) { // Пропускаем свою команду
                for (int num = 1; num <= 5; num++) {
                    suspicious.add("acc-" + teamId + "-" + num);
                }
            }
        }
        
        // Добавляем общие ID
        suspicious.addAll(List.of(
            "acc-001", "acc-002", "acc-999", "acc-test", "acc-admin"
        ));
        
        return suspicious;
    }
}