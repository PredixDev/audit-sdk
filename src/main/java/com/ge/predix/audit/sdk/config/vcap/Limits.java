package com.ge.predix.audit.sdk.config.vcap;

/**
 * Created by 212584872 on 1/8/2017.
 */
import lombok.*;

@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Limits{
    private int disk;
    private int fds;
    private int mem;
}