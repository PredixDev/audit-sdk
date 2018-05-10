package com.ge.predix.audit.sdk.config.vcap;


import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class VcapServices {
    private List<AuditService> auditService;
}