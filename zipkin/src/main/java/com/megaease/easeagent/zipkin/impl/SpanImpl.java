/*
 * Copyright (c) 2017, MegaEase
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.megaease.easeagent.zipkin.impl;

import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import com.megaease.easeagent.plugin.api.trace.Request;
import com.megaease.easeagent.plugin.api.trace.Scope;
import com.megaease.easeagent.plugin.api.trace.Span;
import com.megaease.easeagent.plugin.bridge.NoOpTracer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SpanImpl implements Span {
    private static final Map<Kind, brave.Span.Kind> KINDS;

    static {
        Map<Kind, brave.Span.Kind> kinds = new HashMap<>();
        kinds.put(Kind.CLIENT, brave.Span.Kind.CLIENT);
        kinds.put(Kind.SERVER, brave.Span.Kind.SERVER);
        kinds.put(Kind.PRODUCER, brave.Span.Kind.PRODUCER);
        kinds.put(Kind.CONSUMER, brave.Span.Kind.CONSUMER);
        KINDS = Collections.unmodifiableMap(kinds);
    }

    private final Tracing tracing;
    private final brave.Span span;
    private CurrentTraceContext.Scope scope;
    private final TraceContext.Injector<Request> injector;

    private SpanImpl(@Nonnull Tracing tracing, @Nonnull brave.Span span, @Nonnull TraceContext.Injector<Request> injector) {
        this.tracing = tracing;
        this.span = span;
        this.injector = injector;
    }

    public static Span build(Tracing tracing, brave.Span span, TraceContext.Injector<Request> injector) {
        if (span == null) {
            return NoOpTracer.NO_OP_SPAN;
        }
        return new SpanImpl(tracing, span, injector);
    }

    public static brave.Span.Kind braveKind(Kind kind) {
        return KINDS.get(kind);
    }

    protected brave.Span getSpan() {
        return span;
    }

    @Override
    public Span name(String name) {
        span.name(name);
        return this;
    }

    @Override
    public Span tag(String key, String value) {
        if (key == null || value == null) {
            return this;
        }
        span.tag(key, value);
        return this;
    }

    @Override
    public Span annotate(String value) {
        span.annotate(value);
        return this;
    }

    @Override
    public boolean isNoop() {
        return span.isNoop();
    }

    @Override
    public Span start() {
        span.start();
        return this;
    }

    @Override
    public Span start(long timestamp) {
        span.start(timestamp);
        return this;
    }

    @Override
    public Span kind(@Nullable Kind kind) {
        span.kind(KINDS.get(kind));
        return this;
    }

    @Override
    public Span annotate(long timestamp, String value) {
        span.annotate(timestamp, value);
        return this;
    }

    @Override
    public Span error(Throwable throwable) {
        span.error(throwable);
        return this;
    }

    @Override
    public Span remoteServiceName(String remoteServiceName) {
        span.remoteServiceName(remoteServiceName);
        return this;
    }

    @Override
    public boolean remoteIpAndPort(@Nullable String remoteIp, int remotePort) {
        span.remoteIpAndPort(remoteIp, remotePort);
        return false;
    }

    @Override
    public void abandon() {
        span.abandon();
    }

    @Override
    public void finish() {
        closeScope();
        span.finish();
    }

    @Override
    public void finish(long timestamp) {
        closeScope();
        span.finish(timestamp);
    }

    private void closeScope() {
        if (scope != null) {
            scope.close();
        }
    }

    @Override
    public void flush() {
        span.flush();
    }

    @Override
    public void inject(Request request) {
        injector.inject(span.context(), request);
    }

    @Override
    public Scope maybeScope() {
        return new ScopeImpl(tracing.currentTraceContext().maybeScope(span.context()));
    }

    @Override
    public Span cacheScope() {
        if (scope != null) {
            return this;
        }
        scope = tracing.currentTraceContext().maybeScope(span.context());
        return this;
    }

    @Override
    public String traceIdString() {
        return span.context().traceIdString();
    }

    @Override
    public String spanIdString() {
        return span.context().spanIdString();
    }

    @Override
    public String parentIdString() {
        return span.context().parentIdString();
    }

    @Override
    public Long traceId() {
        return span.context().traceId();
    }

    @Override
    public Long spanId() {
        return span.context().spanId();
    }

    @Override
    public Long parentId() {
        return span.context().parentId();
    }
}
