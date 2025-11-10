package com.thp.sqlsaas.entitlement;

import com.thp.sqlsaas.entitlement.model.*;
import com.thp.sqlsaas.persistence.entity.EntitlementPolicy;
import com.thp.sqlsaas.persistence.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {
    
    @Mock
    private PolicyService policyService;
    
    @Mock
    private PolicyLoader policyLoader;
    
    @InjectMocks
    private EntitlementService entitlementService;
    
    private EntitlementContext testContext;
    
    @BeforeEach
    void setUp() {
        testContext = EntitlementContext.builder()
                .userId("john_doe")
                .tenantId("1")
                .roles(new HashSet<>(Set.of("USER")))
                .build();
        
        // Add source permissions
        SourcePermissions sourcePerms = SourcePermissions.builder()
                .readableTables(Set.of("issues", "pulls"))
                .build();
        
        Map<String, Set<String>> columns = new HashMap<>();
        columns.put("issues", Set.of("id", "title", "state", "assignee"));
        sourcePerms.setTableColumns(columns);
        
        testContext.addSourcePermissions("github", sourcePerms);
    }
    
    @Test
    void testAuthorizeQuery_NoAuthentication_Denied() {
        // Given
        EntitlementContext noAuthContext = EntitlementContext.builder().build();
        
        // When
        EntitlementDecision decision = entitlementService.authorizeQuery(
                noAuthContext, "github", "issues", Set.of("id", "title")
        );
        
        // Then
        assertFalse(decision.isAllowed());
        assertEquals("User not authenticated", decision.getDenialReason());
    }
    
    @Test
    void testAuthorizeQuery_NoPolicies_Allowed() {
        // Given
        when(policyService.getApplicablePolicies(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        
        // When
        EntitlementDecision decision = entitlementService.authorizeQuery(
                testContext, "github", "issues", Set.of("id", "title")
        );
        
        // Then
        assertTrue(decision.isAllowed());
        assertNull(decision.getDenialReason());
    }
    
    @Test
    void testAuthorizeQuery_WithRLSPolicy_FiltersApplied() {
        // Given
        EntitlementPolicy policyEntity = createRLSPolicyEntity();
        Policy policy = createRLSPolicy();
        
        when(policyService.getApplicablePolicies("1", "github", "issues"))
                .thenReturn(List.of(policyEntity));
        when(policyLoader.convertToPolicy(policyEntity))
                .thenReturn(policy);
        
        // When
        EntitlementDecision decision = entitlementService.authorizeQuery(
                testContext, "github", "issues", Set.of("id", "title", "assignee")
        );
        
        // Then
        assertTrue(decision.isAllowed());
        assertEquals(1, decision.getRowFilters().size());
        
        RowFilter filter = decision.getRowFilters().get(0);
        assertEquals("assignee", filter.getColumnName());
        assertEquals(RowFilter.FilterOperator.EQUALS, filter.getOperator());
        assertEquals("john_doe", filter.getValue());
    }
    
    @Test
    void testAuthorizeQuery_WithCLSPolicy_ColumnsRemoved() {
        // Given
        EntitlementPolicy policyEntity = createCLSPolicyEntity();
        Policy policy = createCLSPolicy();
        
        when(policyService.getApplicablePolicies("1", "github", "issues"))
                .thenReturn(List.of(policyEntity));
        when(policyLoader.convertToPolicy(policyEntity))
                .thenReturn(policy);
        
        // When
        EntitlementDecision decision = entitlementService.authorizeQuery(
                testContext, 
                "github", 
                "issues", 
                Set.of("id", "title", "email") // email should be removed
        );
        
        // Then
        assertTrue(decision.isAllowed());
        assertFalse(decision.getAllowedColumns().contains("email"));
        assertTrue(decision.getAllowedColumns().contains("id"));
        assertTrue(decision.getAllowedColumns().contains("title"));
    }
    
    @Test
    void testAuthorizeQuery_WithDenyPolicy_AccessDenied() {
        // Given
        EntitlementPolicy policyEntity = createDenyPolicyEntity();
        Policy policy = createDenyPolicy();
        
        when(policyService.getApplicablePolicies("1", "github", "audit_logs"))
                .thenReturn(List.of(policyEntity));
        when(policyLoader.convertToPolicy(policyEntity))
                .thenReturn(policy);
        
        // When
        EntitlementDecision decision = entitlementService.authorizeQuery(
                testContext, "github", "audit_logs", Set.of("id", "action")
        );
        
        // Then
        assertFalse(decision.isAllowed());
        assertNotNull(decision.getDenialReason());
        assertTrue(decision.getDenialReason().contains("deny-audit-logs"));
    }
    
    @Test
    void testAuthorizeQuery_AdminRole_BypassesRestrictions() {
        // Given
        testContext.getRoles().add("ADMIN");
        
        EntitlementPolicy allowPolicy = createAdminAllowPolicyEntity();
        Policy policy = createAdminAllowPolicy();
        
        when(policyService.getApplicablePolicies("1", "github", "issues"))
                .thenReturn(List.of(allowPolicy));
        when(policyLoader.convertToPolicy(allowPolicy))
                .thenReturn(policy);
        
        // When
        EntitlementDecision decision = entitlementService.authorizeQuery(
                testContext, "github", "issues", Set.of("id", "title")
        );
        
        // Then
        assertTrue(decision.isAllowed());
        assertTrue(decision.getRowFilters().isEmpty()); // No filters for admin
    }
    
    @Test
    void testApplyRowFilters() {
        // Given
        List<Map<String, Object>> rows = List.of(
                Map.of("id", 1, "assignee", "john_doe"),
                Map.of("id", 2, "assignee", "jane_smith"),
                Map.of("id", 3, "assignee", "john_doe")
        );
        
        RowFilter filter = RowFilter.builder()
                .columnName("assignee")
                .operator(RowFilter.FilterOperator.EQUALS)
                .value("john_doe")
                .build();
        
        // When
        List<Map<String, Object>> filtered = entitlementService.applyRowFilters(
                rows, List.of(filter)
        );
        
        // Then
        assertEquals(2, filtered.size());
        assertTrue(filtered.stream()
                .allMatch(row -> "john_doe".equals(row.get("assignee"))));
    }
    
    @Test
    void testApplyColumnMasks() {
        // Given
        List<Map<String, Object>> rows = List.of(
                Map.of("id", 1, "name", "user/personal-project"),
                Map.of("id", 2, "name", "org/main-repo")
        );
        
        ColumnMask mask = ColumnMask.builder()
                .maskType(ColumnMask.MaskType.PARTIAL)
                .build();
        
        Map<String, ColumnMask> masks = Map.of("name", mask);
        
        // When
        List<Map<String, Object>> masked = entitlementService.applyColumnMasks(
                rows, masks
        );
        
        // Then
        assertEquals(2, masked.size());
        masked.forEach(row -> {
            String name = (String) row.get("name");
            assertTrue(name.startsWith("***"));
        });
    }
    
    // Helper methods to create test data
    
    private EntitlementPolicy createRLSPolicyEntity() {
        EntitlementPolicy policy = new EntitlementPolicy();
        policy.setId(1L);
        policy.setTenantId("1");
        policy.setPolicyId("rls-github-own-issues");
        policy.setPolicyType("RLS");
        policy.setSourcePattern("github");
        policy.setTablePattern("issues");
        policy.setCondition("user.role != 'ADMIN'");
        policy.setAction("FILTER");
        policy.setPolicyConfig(Map.of(
                "column", "assignee",
                "operator", "=",
                "value", "${user.id}"
        ));
        policy.setPriority(10);
        policy.setEnabled(true);
        return policy;
    }
    
    private Policy createRLSPolicy() {
        return Policy.builder()
                .policyId("rls-github-own-issues")
                .type(Policy.PolicyType.RLS)
                .action(Policy.PolicyAction.FILTER)
                .sourcePattern("github")
                .tablePattern("issues")
                .condition("user.role != 'ADMIN'")
                .rowFilter(RowFilter.builder()
                        .columnName("assignee")
                        .operator(RowFilter.FilterOperator.EQUALS)
                        .value("${user.id}")
                        .build())
                .build();
    }
    
    private EntitlementPolicy createCLSPolicyEntity() {
        EntitlementPolicy policy = new EntitlementPolicy();
        policy.setId(2L);
        policy.setTenantId("1");
        policy.setPolicyId("cls-hide-email");
        policy.setPolicyType("CLS");
        policy.setSourcePattern("github");
        policy.setTablePattern("issues");
        policy.setCondition("user.role != 'HR_ADMIN'");
        policy.setAction("DENY");
        policy.setPolicyConfig(Map.of("denied_columns", List.of("email")));
        policy.setPriority(10);
        return policy;
    }
    
    private Policy createCLSPolicy() {
        return Policy.builder()
                .policyId("cls-hide-email")
                .type(Policy.PolicyType.CLS)
                .action(Policy.PolicyAction.DENY)
                .sourcePattern("github")
                .tablePattern("issues")
                .condition("user.role != 'HR_ADMIN'")
                .allowedColumns(Set.of("email"))
                .build();
    }
    
    private EntitlementPolicy createDenyPolicyEntity() {
        EntitlementPolicy policy = new EntitlementPolicy();
        policy.setId(3L);
        policy.setTenantId("1");
        policy.setPolicyId("deny-audit-logs");
        policy.setPolicyType("TABLE_ACCESS");
        policy.setSourcePattern("github");
        policy.setTablePattern("audit_logs");
        policy.setCondition("user.role != 'SECURITY_ADMIN'");
        policy.setAction("DENY");
        policy.setPolicyConfig(Map.of());
        policy.setPriority(100);
        return policy;
    }
    
    private Policy createDenyPolicy() {
        return Policy.builder()
                .policyId("deny-audit-logs")
                .type(Policy.PolicyType.TABLE_ACCESS)
                .action(Policy.PolicyAction.DENY)
                .sourcePattern("github")
                .tablePattern("audit_logs")
                .condition("user.role != 'SECURITY_ADMIN'")
                .build();
    }
    
    private EntitlementPolicy createAdminAllowPolicyEntity() {
        EntitlementPolicy policy = new EntitlementPolicy();
        policy.setId(4L);
        policy.setTenantId("1");
        policy.setPolicyId("admin-all-access");
        policy.setPolicyType("TABLE_ACCESS");
        policy.setSourcePattern("*");
        policy.setTablePattern("*");
        policy.setCondition("user.role = 'ADMIN'");
        policy.setAction("ALLOW");
        policy.setPolicyConfig(Map.of());
        policy.setPriority(1000);
        return policy;
    }
    
    private Policy createAdminAllowPolicy() {
        return Policy.builder()
                .policyId("admin-all-access")
                .type(Policy.PolicyType.TABLE_ACCESS)
                .action(Policy.PolicyAction.ALLOW)
                .sourcePattern("*")
                .tablePattern("*")
                .condition("user.role = 'ADMIN'")
                .build();
    }
}
