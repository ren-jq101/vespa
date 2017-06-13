// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.jdisc.core;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.yahoo.jdisc.application.ContainerActivator;
import com.yahoo.jdisc.application.ContainerBuilder;
import com.yahoo.jdisc.application.ContainerThread;
import com.yahoo.jdisc.application.OsgiFramework;
import com.yahoo.jdisc.service.CurrentContainer;
import com.yahoo.jdisc.statistics.ActiveContainerMetrics;

import java.util.concurrent.ThreadFactory;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
class ApplicationEnvironmentModule extends AbstractModule {

    private final ApplicationLoader loader;

    public ApplicationEnvironmentModule(ApplicationLoader loader) {
        this.loader = loader;
    }

    @Override
    protected void configure() {
        bind(ContainerActivator.class).toInstance(loader);
        bind(CurrentContainer.class).toInstance(loader);
        bind(OsgiFramework.class).toInstance(loader.osgiFramework());
        bind(ThreadFactory.class).to(ContainerThread.Factory.class);
        bind(ActiveContainerMetrics.class).toInstance(loader.getActiveContainerMetrics());
    }

    @Provides
    public ContainerBuilder containerBuilder() {
        return loader.newContainerBuilder();
    }
}
