/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.context.module;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.navercorp.pinpoint.profiler.context.compress.SpanProcessor;
import com.navercorp.pinpoint.profiler.context.id.TransactionIdEncoder;
import com.navercorp.pinpoint.profiler.context.provider.AgentInfoFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.CommandDispatcherProvider;
import com.navercorp.pinpoint.profiler.context.provider.ConnectionFactoryProviderProvider;
import com.navercorp.pinpoint.profiler.context.provider.HeaderTBaseSerializerProvider;
import com.navercorp.pinpoint.profiler.context.provider.MetadataMessageConverterProvider;
import com.navercorp.pinpoint.profiler.context.provider.PinpointClientFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.SpanDataSenderProvider;
import com.navercorp.pinpoint.profiler.context.provider.SpanProcessorProvider;
import com.navercorp.pinpoint.profiler.context.provider.SpanStatClientFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.StatDataSenderProvider;
import com.navercorp.pinpoint.profiler.context.provider.TcpDataSenderProvider;
import com.navercorp.pinpoint.profiler.context.thrift.DefaultTransactionIdEncoder;
import com.navercorp.pinpoint.profiler.context.thrift.MessageConverter;
import com.navercorp.pinpoint.profiler.context.thrift.SpanThriftMessageConverterProvider;
import com.navercorp.pinpoint.profiler.context.thrift.StatThriftMessageConverterProvider;
import com.navercorp.pinpoint.profiler.context.thrift.ThriftMessageToResultConverterProvider;
import com.navercorp.pinpoint.profiler.receiver.CommandDispatcher;
import com.navercorp.pinpoint.profiler.sender.DataSender;
import com.navercorp.pinpoint.profiler.sender.EnhancedDataSender;
import com.navercorp.pinpoint.profiler.sender.ResultResponse;
import com.navercorp.pinpoint.profiler.util.AgentInfoFactory;
import com.navercorp.pinpoint.rpc.client.ConnectionFactoryProvider;
import com.navercorp.pinpoint.rpc.client.PinpointClientFactory;
import com.navercorp.pinpoint.thrift.dto.TSpan;
import com.navercorp.pinpoint.thrift.dto.TSpanChunk;
import com.navercorp.pinpoint.thrift.io.HeaderTBaseSerializer;
import org.apache.thrift.TBase;

/**
 * @author Woonduk Kang(emeroad)
 */
public class ThriftModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(TransactionIdEncoder.class).to(DefaultTransactionIdEncoder.class).in(Scopes.SINGLETON);

        Key<CommandDispatcher> commandDispatcher = Key.get(CommandDispatcher.class);
        bind(commandDispatcher).toProvider(CommandDispatcherProvider.class).in(Scopes.SINGLETON);
//        expose(commandDispatcher);

        TypeLiteral<SpanProcessor<TSpan, TSpanChunk>> spanPostProcessorType = new TypeLiteral<SpanProcessor<TSpan, TSpanChunk>>() {};
        bind(spanPostProcessorType).toProvider(SpanProcessorProvider.class).in(Scopes.SINGLETON);

        bind(ConnectionFactoryProvider.class).toProvider(ConnectionFactoryProviderProvider.class).in(Scopes.SINGLETON);

        Key<PinpointClientFactory> pinpointClientFactory = Key.get(PinpointClientFactory.class, DefaultClientFactory.class);
        bind(pinpointClientFactory).toProvider(PinpointClientFactoryProvider.class).in(Scopes.SINGLETON);
//        expose(pinpointClientFactory);

        bind(HeaderTBaseSerializer.class).toProvider(HeaderTBaseSerializerProvider.class).in(Scopes.SINGLETON);

        TypeLiteral<EnhancedDataSender<Object>> dataSenderTypeLiteral = new TypeLiteral<EnhancedDataSender<Object>>() {};
        bind(dataSenderTypeLiteral).toProvider(TcpDataSenderProvider.class).in(Scopes.SINGLETON);
        expose(dataSenderTypeLiteral);

        Key<PinpointClientFactory> pinpointStatClientFactory = Key.get(PinpointClientFactory.class, SpanStatClientFactory.class);
        bind(pinpointStatClientFactory).toProvider(SpanStatClientFactoryProvider.class).in(Scopes.SINGLETON);

        TypeLiteral<MessageConverter<TBase<?, ?>>> thriftMessageConverter = new TypeLiteral<MessageConverter<TBase<?, ?>>>() {};
        Key<MessageConverter<TBase<?, ?>>> spanMessageConverterKey = Key.get(thriftMessageConverter, SpanConverter.class);
        bind(spanMessageConverterKey).toProvider(SpanThriftMessageConverterProvider.class ).in(Scopes.SINGLETON);
//        expose(spanMessageConverterKey);

        Key<MessageConverter<TBase<?, ?>>> metadataMessageConverterKey = Key.get(thriftMessageConverter, MetadataConverter.class);
        bind(metadataMessageConverterKey).toProvider(MetadataMessageConverterProvider.class ).in(Scopes.SINGLETON);
//        expose(metadataMessageConverterKey);


        // Stat Thrift Converter
        TypeLiteral<MessageConverter<TBase<?, ?>>> statMessageConverter = new TypeLiteral<MessageConverter<TBase<?, ?>>>() {};
        Key<MessageConverter<TBase<?, ?>>> statMessageConverterKey = Key.get(statMessageConverter, StatConverter.class);
        bind(statMessageConverterKey).toProvider(StatThriftMessageConverterProvider.class ).in(Scopes.SINGLETON);

        Key<DataSender> spanDataSender = Key.get(DataSender.class, SpanDataSender.class);
        bind(spanDataSender).toProvider(SpanDataSenderProvider.class).in(Scopes.SINGLETON);
        expose(spanDataSender);

        Key<DataSender> statDataSender = Key.get(DataSender.class, StatDataSender.class);
        bind(DataSender.class).annotatedWith(StatDataSender.class)
                .toProvider(StatDataSenderProvider.class).in(Scopes.SINGLETON);
        expose(statDataSender);

        // For AgentInfoSender
//        TypeLiteral<AgentInfoFactory> agentInfoFactoryTypeLiteral = new TypeLiteral<AgentInfoFactory>() {};
//        bind(agentInfoFactoryTypeLiteral).toProvider(AgentInfoFactoryProvider.class).in(Scopes.SINGLETON);
//        expose(agentInfoFactoryTypeLiteral);

        TypeLiteral<MessageConverter<ResultResponse>> resultMessageConverter = new TypeLiteral<MessageConverter<ResultResponse>>() {};
        Key<MessageConverter<ResultResponse>> resultMessageConverterKey = Key.get(resultMessageConverter, ResultConverter.class);
        bind(resultMessageConverterKey).toProvider(ThriftMessageToResultConverterProvider.class ).in(Scopes.SINGLETON);
        expose(resultMessageConverterKey);

        Key<ModuleLifeCycle> rpcModuleLifeCycleKey = Key.get(ModuleLifeCycle.class, Names.named("RPC-MODULE"));
        bind(rpcModuleLifeCycleKey).to(ThriftModuleLifeCycle.class).in(Scopes.SINGLETON);
        expose(rpcModuleLifeCycleKey);
    }

}
