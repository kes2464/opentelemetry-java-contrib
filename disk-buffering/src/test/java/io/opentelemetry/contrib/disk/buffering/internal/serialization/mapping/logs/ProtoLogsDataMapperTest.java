/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.LogsData;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtoLogsDataMapperTest {

  private static final LogRecordData LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBody(Body.string("Log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .build();

  private static final LogRecordData OTHER_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBody(Body.string("Other log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .build();

  private static final LogRecordData LOG_RECORD_WITH_DIFFERENT_SCOPE_SAME_RESOURCE =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION)
          .setAttributes(TestData.ATTRIBUTES)
          .setBody(Body.string("Same resource other scope log"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .build();

  private static final LogRecordData LOG_RECORD_WITH_DIFFERENT_RESOURCE =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_WITHOUT_SCHEMA_URL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION)
          .setAttributes(TestData.ATTRIBUTES)
          .setBody(Body.string("Different resource log"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .build();

  @Test
  void verifyConversionDataStructure() {
    List<LogRecordData> signals = Collections.singletonList(LOG_RECORD);

    LogsData result = mapToProto(signals);

    List<ResourceLogs> resourceLogsList = result.getResourceLogsList();
    assertEquals(1, resourceLogsList.size());
    assertEquals(1, resourceLogsList.get(0).getScopeLogsList().size());
    assertEquals(1, resourceLogsList.get(0).getScopeLogsList().get(0).getLogRecordsList().size());

    assertThat(mapFromProto(result)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleLogsWithSameResourceAndScope() {
    List<LogRecordData> signals = Arrays.asList(LOG_RECORD, OTHER_LOG_RECORD);

    LogsData proto = mapToProto(signals);

    List<ResourceLogs> resourceLogsList = proto.getResourceLogsList();
    assertEquals(1, resourceLogsList.size());
    List<ScopeLogs> scopeLogsList = resourceLogsList.get(0).getScopeLogsList();
    assertEquals(1, scopeLogsList.size());
    List<LogRecord> logRecords = scopeLogsList.get(0).getLogRecordsList();
    assertEquals(2, logRecords.size());
    assertEquals("Log body", logRecords.get(0).getBody().getStringValue());
    assertEquals("Other log body", logRecords.get(1).getBody().getStringValue());

    assertEquals(2, mapFromProto(proto).size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleLogsWithSameResourceDifferentScope() {
    List<LogRecordData> signals =
        Arrays.asList(LOG_RECORD, LOG_RECORD_WITH_DIFFERENT_SCOPE_SAME_RESOURCE);

    LogsData proto = mapToProto(signals);

    List<ResourceLogs> resourceLogsList = proto.getResourceLogsList();
    assertEquals(1, resourceLogsList.size());
    List<ScopeLogs> scopeLogsList = resourceLogsList.get(0).getScopeLogsList();
    assertEquals(2, scopeLogsList.size());
    ScopeLogs firstScope = scopeLogsList.get(0);
    ScopeLogs secondScope = scopeLogsList.get(1);
    List<LogRecord> firstScopeLogs = firstScope.getLogRecordsList();
    List<LogRecord> secondScopeLogs = secondScope.getLogRecordsList();
    assertEquals(1, firstScopeLogs.size());
    assertEquals(1, secondScopeLogs.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleLogsWithDifferentResource() {
    List<LogRecordData> signals = Arrays.asList(LOG_RECORD, LOG_RECORD_WITH_DIFFERENT_RESOURCE);

    LogsData proto = mapToProto(signals);

    List<ResourceLogs> resourceLogsList = proto.getResourceLogsList();
    assertEquals(2, resourceLogsList.size());
    ResourceLogs firstResourceLogs = resourceLogsList.get(0);
    ResourceLogs secondResourceLogs = resourceLogsList.get(1);
    List<ScopeLogs> firstScopeLogsList = firstResourceLogs.getScopeLogsList();
    List<ScopeLogs> secondScopeLogsList = secondResourceLogs.getScopeLogsList();
    assertEquals(1, firstScopeLogsList.size());
    assertEquals(1, secondScopeLogsList.size());
    ScopeLogs firstScope = firstScopeLogsList.get(0);
    ScopeLogs secondScope = secondScopeLogsList.get(0);
    List<LogRecord> firstScopeLogs = firstScope.getLogRecordsList();
    List<LogRecord> secondScopeLogs = secondScope.getLogRecordsList();
    assertEquals(1, firstScopeLogs.size());
    assertEquals(1, secondScopeLogs.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  private static LogsData mapToProto(Collection<LogRecordData> signals) {
    return ProtoLogsDataMapper.getInstance().toProto(signals);
  }

  private static List<LogRecordData> mapFromProto(LogsData protoData) {
    return ProtoLogsDataMapper.getInstance().fromProto(protoData);
  }
}
