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

import brave.Tracer;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.megaease.easeagent.log4j2.Logger;
import com.megaease.easeagent.log4j2.LoggerFactory;
import com.megaease.easeagent.plugin.api.InitializeContext;
import com.megaease.easeagent.plugin.api.context.AsyncContext;
import com.megaease.easeagent.plugin.api.context.RequestContext;
import com.megaease.easeagent.plugin.api.trace.*;
import com.megaease.easeagent.plugin.bridge.NoOpContext;
import com.megaease.easeagent.plugin.bridge.NoOpTracer;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

public class TracingImpl implements ITracing {
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingImpl.class);
    private final Supplier<InitializeContext> supplier;
    private final brave.Tracing tracing;
    private final brave.Tracer tracer;
    private final TraceContext.Injector<Request> defaultInjector;
    private final TraceContext.Injector<Request> clientInjector;
    private final TraceContext.Injector<Request> consumerInjector;
    private final TraceContext.Injector<Request> producerInjector;
    private final TraceContext.Extractor<Request> defaultExtractor;
    private final TraceContext.Extractor<Request> producerExtractor;
    private final TraceContext.Extractor<Request> consumerExtractor;
    private final MessagingTracing messagingTracing;
    private final List<String> propagationKeys;

    private TracingImpl(@Nonnull Supplier<InitializeContext> supplier,
                        @Nonnull brave.Tracing tracing,
                        @Nonnull Tracer tracer,
                        @Nonnull TraceContext.Injector<Request> defaultInjector,
                        @Nonnull TraceContext.Injector<Request> clientInjector,
                        @Nonnull TraceContext.Injector<Request> producerInjector,
                        @Nonnull TraceContext.Injector<Request> consumerInjector,
                        @Nonnull TraceContext.Extractor<Request> defaultExtractor,
                        TraceContext.Extractor<Request> producerExtractor,
                        TraceContext.Extractor<Request> consumerExtractor,
                        @Nonnull MessagingTracing messagingTracing, List<String> propagationKeys) {
        this.supplier = supplier;
        this.tracing = tracing;
        this.tracer = tracer;
        this.defaultInjector = defaultInjector;
        this.clientInjector = clientInjector;
        this.consumerInjector = consumerInjector;
        this.producerInjector = producerInjector;
        this.defaultExtractor = defaultExtractor;
        this.consumerExtractor = consumerExtractor;
        this.producerExtractor = producerExtractor;
        this.messagingTracing = messagingTracing;
        this.propagationKeys = propagationKeys;
    }

    public static ITracing build(Supplier<InitializeContext> supplier, brave.Tracing tracing) {
        return tracing == null ? NoOpTracer.NO_OP_TRACING :
            new TracingImpl(supplier, tracing,
                tracing.tracer(),
                tracing.propagation().injector(Request::setHeader),
                tracing.propagation().injector(new RemoteSetterImpl<>(brave.Span.Kind.CLIENT)),
                tracing.propagation().injector(new RemoteSetterImpl<>(brave.Span.Kind.PRODUCER)),
                tracing.propagation().injector(new RemoteSetterImpl<>(brave.Span.Kind.CONSUMER)),
                tracing.propagation().extractor(Request::header),
                tracing.propagation().extractor(new RemoteGetterImpl<>(brave.Span.Kind.PRODUCER)),
                tracing.propagation().extractor(new RemoteGetterImpl<>(brave.Span.Kind.CONSUMER)),
                MessagingTracingImpl.build(tracing),
                tracing.propagation().keys());
    }

    @Override
    public boolean isNoop() {
        return false;
    }

    @Override
    public boolean hasCurrentSpan() {
        return tracing().currentTraceContext().get() != null;
    }


    private brave.Tracer tracer() {
        return this.tracer;
    }

    private brave.Tracing tracing() {
        return this.tracing;
    }

    @Override
    public Span currentSpan() {
        brave.Tracer tracer = tracer();
        Span span = NoOpTracer.NO_OP_SPAN;
        if (tracer != null) {
            span = build(tracer.currentSpan());
        }
        return NoOpTracer.noNullSpan(span);
    }

    private Span build(brave.Span bSpan) {
        return build(bSpan, false);
    }

    private Span build(brave.Span bSpan, boolean cacheScope) {
        Span span = SpanImpl.build(tracing(), bSpan, defaultInjector);
        if (cacheScope) {
            span.cacheScope();
        }
        return span;
    }

    private void setInfo(brave.Span span, Request request) {
        Span.Kind kind = request.kind();
        if (kind != null) {
            span.kind(SpanImpl.braveKind(kind));
        }
        span.name(request.name());
    }

    private TraceContext currentTraceContext() {
        Tracer tracer = tracer();
        if (tracer == null) {
            LOGGER.debug("tracer was null.");
            return null;
        }
        brave.Span span = tracer.currentSpan();
        if (span == null) {
            return null;
        }
        return span.context();
    }

    @Override
    public AsyncContext exportAsync() {
        TraceContext traceContext = currentTraceContext();
        if (traceContext == null) {
            return NoOpContext.NO_OP_ASYNC_CONTEXT;
        }
        return AsyncContextImpl.build(this, traceContext, supplier);
    }

    @Override
    public Scope importAsync(AsyncContext snapshot) {
        if (snapshot instanceof AsyncContextImpl) {
            TraceContext traceContext = ((AsyncContextImpl) snapshot).getTraceContext();
            CurrentTraceContext.Scope scope = tracing().currentTraceContext().maybeScope(traceContext);
            return new ScopeImpl(scope);
        }
        return NoOpTracer.NO_OP_SCOPE;
    }

    @Override
    public RequestContext nextServer(Request request) {
        brave.Span span = nextBraveSpan(defaultExtractor, request);
        AsyncRequest asyncRequest = new AsyncRequest(request);
        clientInjector.inject(span.context(), asyncRequest);
        Span newSpan = build(span, request.cacheScope());
        return new RequestContextImpl(this, span, newSpan, newSpan.maybeScope(), asyncRequest, supplier);
    }

    private brave.Span nextBraveSpan(TraceContext.Extractor<Request> extractor, Request request) {
        TraceContext maybeParent = tracing.currentTraceContext().get();
        // Unlike message consumers, we try current span before trying extraction. This is the proper
        // order because the span in scope should take precedence over a potentially stale header entry.
        //
        brave.Span span;
        if (maybeParent == null) {
            TraceContextOrSamplingFlags extracted = extractor.extract(request);
            span = tracer().nextSpan(extracted);
        } else { // If we have a span in scope assume headers were cleared before
            span = tracer.newChild(maybeParent);
        }
        if (span.isNoop()) {
            return span;
        }
        setInfo(span, request);
        return span;
    }

    @Override
    public RequestContext serverImport(Request request) {
        TraceContextOrSamplingFlags extracted = defaultExtractor.extract(request);
        brave.Span span = extracted.context() != null
            ? tracer().joinSpan(extracted.context())
            : tracer().nextSpan(extracted);
        if (span.isNoop()) {
            return NoOpContext.NO_OP_PROGRESS_CONTEXT;
        }
        setInfo(span, request);
        AsyncRequest asyncRequest = new AsyncRequest(request);
        defaultInjector.inject(span.context(), asyncRequest);
        Span newSpan = build(span, request.cacheScope());
        return new RequestContextImpl(this, span, newSpan, newSpan.maybeScope(), asyncRequest, supplier);
    }

    @Override
    public List<String> propagationKeys() {
        return propagationKeys;
    }

    @Override
    public Span nextSpan() {
        return nextSpan(null);
    }


    @Override
    public Span nextSpan(Message message) {
        Object msg = message == null ? null : message.get();
        Span span = null;
        if (msg == null) {
            span = build(tracer().nextSpan(), true);
        } else if (msg instanceof TraceContextOrSamplingFlags) {
            span = build(tracer().nextSpan((TraceContextOrSamplingFlags) msg), true);
        }
        return NoOpTracer.noNullSpan(span);
    }

    @Override
    public MessagingTracing messagingTracing() {
        return messagingTracing;
    }

    private void setMessageInfo(brave.Span span, MessagingRequest request) {
        if (request.operation() != null) {
            span.tag("messaging.operation", request.operation());
        }
        if (request.channelKind() != null) {
            span.tag("messaging.channel_kind", request.channelKind());
        }
        if (request.channelName() != null) {
            span.tag("messaging.channel_name", request.channelName());
        }
    }

    @Override
    public Span consumerSpan(MessagingRequest request) {
        brave.Span span = nextBraveSpan(consumerExtractor, request);
        if (span.isNoop()) {
            return NoOpTracer.NO_OP_SPAN;
        }
        setMessageInfo(span, request);
        return NoOpTracer.noNullSpan(build(span, request.cacheScope()));
    }

    @Override
    public Span producerSpan(MessagingRequest request) {
        brave.Span span = nextBraveSpan(producerExtractor, request);
        if (span.isNoop()) {
            return NoOpTracer.NO_OP_SPAN;
        }
        setMessageInfo(span, request);
        producerInjector.inject(span.context(), request);
        return NoOpTracer.noNullSpan(build(span, request.cacheScope()));
    }


}
