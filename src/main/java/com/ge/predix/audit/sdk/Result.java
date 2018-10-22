package com.ge.predix.audit.sdk;

import com.ge.predix.audit.sdk.message.AuditEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by 212554562 on 10/2/2018.
 */
@Data
@Builder
@AllArgsConstructor
public class Result<T extends AuditEvent> {

    private List<AuditEventFailReport<T>> failReports;

    @Override
    public String toString() {

        Map<FailCode, Set<FailType>> resultSummery = getSummery();

        return "Result{" +
                "resultSummery=" + resultSummery +
                '}';
    }



    Map<FailCode, Set<FailType>> getSummery() {

        if(failReports == null){
            return null;
        }

        Map<FailCode, List<AuditEventFailReport<T>>> failCodesMap = failReports.stream()
                .collect(Collectors.groupingBy(AuditEventFailReport::getFailureReason));

        return failCodesMap.entrySet().stream().collect(
                Collectors.toMap(
                        l -> l.getKey(),
                        l -> listToFailTypeCountMap(l.getValue())
                )
        );
    }


    private Set<FailType> listToFailTypeCountMap(List<AuditEventFailReport<T>> list){
        Map<FailType , List<String>> map = new HashMap<>();

        for(AuditEventFailReport a : list){
            FailType type = new FailType(a.getDescription(),a.getThrowable());
            List<String> messageIds = map.get(type);
            if(messageIds == null){
                messageIds = new ArrayList<>();
            }
            messageIds.add(a.getAuditEvent().getMessageId());
            map.put(type, messageIds);
        }

        for(Map.Entry<FailType , List<String>> e : map.entrySet()){
            e.getKey().setMessageIds(e.getValue());
        }

        return map.keySet();
    }


    @Data
    static class FailType{
        String Description;
        Throwable throwable;
        List<String> messageIds = new ArrayList<>();

        public FailType(String description, Throwable throwable) {
            Description = description;
            this.throwable = throwable;
        }

        public void setMessageIds(List<String> messageIds) {
            this.messageIds = messageIds;
        }

        @Override
        public String toString() {
            return "FailType{" +
                    "Description='" + Description + '\'' +
                    ", throwable=" + throwable +
                    ", messageIds=" + messageIds +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FailType failType = (FailType) o;

            if (Description != null ? !Description.equals(failType.Description) : failType.Description != null)
                return false;
            return throwable != null ? throwable.equals(failType.throwable) : failType.throwable == null;

        }

        @Override
        public int hashCode() {
            int result = Description != null ? Description.hashCode() : 0;
            result = 31 * result + (throwable != null ? throwable.hashCode() : 0);
            return result;
        }
    }
}
