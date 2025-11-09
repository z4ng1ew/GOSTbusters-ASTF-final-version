package org.owasp.astf.shared.result;

/**
 * Represents a security finding from a test case execution.
 */
public class Finding {
    private final String id;
    private final String title;
    private final String description;
    private final Severity severity;
    private final String testCaseId;
    private final String endpoint;
    private final String remediation;

    /**
     * Constructs a new Finding with the specified details.
     *
     * @param id the unique identifier for the finding
     * @param title the title of the finding
     * @param description the detailed description
     * @param severity the severity level
     * @param testCaseId the test case identifier
     * @param endpoint the affected endpoint
     * @param remediation the remediation advice
     */
    public Finding(String id, String title, String description, Severity severity, String testCaseId, String endpoint, String remediation) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.testCaseId = testCaseId;
        this.endpoint = endpoint;
        this.remediation = remediation;
    }

    /** @return the unique identifier */
    public String getId() { return id; }
    
    /** @return the finding title */
    public String getTitle() { return title; }
    
    /** @return the detailed description */
    public String getDescription() { return description; }
    
    /** @return the severity level */
    public Severity getSeverity() { return severity; }
    
    /** @return the test case identifier */
    public String getTestCaseId() { return testCaseId; }
    
    /** @return the affected endpoint */
    public String getEndpoint() { return endpoint; }
    
    /** @return the remediation advice */
    public String getRemediation() { return remediation; }
}

/**
 * Severity levels for security findings.
 */
enum Severity {
    /** Informational findings */
    INFO, 
    /** Low severity issues */
    LOW, 
    /** Medium severity issues */
    MEDIUM, 
    /** High severity issues */
    HIGH, 
    /** Critical severity issues */
    CRITICAL
}