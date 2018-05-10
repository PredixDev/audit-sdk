package com.ge.predix.audit.sdk.message.tracing;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by Martin Saad on 4/27/2017.
 */
public class CheckpointTest {

    @Test
    public void CheckPointBuilderTest(){
        Checkpoint cp = Checkpoint.builder().build();
        assertThat(cp.getCheckpoint(),is(Checkpoint.AUDIT_SDK));
    }

    @Test
    public void CheckPointAllArgsConstructorTest(){
        Checkpoint cp = new Checkpoint("","",LifeCycleEnum.CHECK,"");
        assertThat(cp.getCheckpoint(),is(Checkpoint.AUDIT_SDK));
    }
}