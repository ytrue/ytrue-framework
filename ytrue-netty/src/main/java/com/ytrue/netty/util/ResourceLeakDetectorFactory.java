package com.ytrue.netty.util;

import com.ytrue.netty.util.internal.ObjectUtil;
import com.ytrue.netty.util.internal.PlatformDependent;
import com.ytrue.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author ytrue
 * @date 2023-08-10 9:23
 * @description 直接从源码中赋值过来的类，是用来创建内存泄漏探测器的工厂。该类可以为用户返回一个内存泄漏探测器
 */
public abstract class ResourceLeakDetectorFactory {
    private static final Logger logger = LoggerFactory.getLogger(ResourceLeakDetectorFactory.class);

    private static volatile ResourceLeakDetectorFactory factoryInstance = new DefaultResourceLeakDetectorFactory();


    public static ResourceLeakDetectorFactory instance() {
        return factoryInstance;
    }


    public static void setResourceLeakDetectorFactory(ResourceLeakDetectorFactory factory) {
        factoryInstance = ObjectUtil.checkNotNull(factory, "factory");
    }


    public final <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource) {
        return newResourceLeakDetector(resource, ResourceLeakDetector.SAMPLING_INTERVAL);
    }


    @Deprecated
    public abstract <T> ResourceLeakDetector<T> newResourceLeakDetector(
            Class<T> resource, int samplingInterval, long maxActive);


    @SuppressWarnings("deprecation")
    public <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource, int samplingInterval) {
        return newResourceLeakDetector(resource, ResourceLeakDetector.SAMPLING_INTERVAL, Long.MAX_VALUE);
    }


    private static final class DefaultResourceLeakDetectorFactory extends ResourceLeakDetectorFactory {
        private final Constructor<?> obsoleteCustomClassConstructor;
        private final Constructor<?> customClassConstructor;

        DefaultResourceLeakDetectorFactory() {
            String customLeakDetector;
            try {
                customLeakDetector = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return SystemPropertyUtil.get("io.netty.customResourceLeakDetector");
                    }
                });
            } catch (Throwable cause) {
                logger.error("Could not access System property: io.netty.customResourceLeakDetector", cause);
                customLeakDetector = null;
            }
            if (customLeakDetector == null) {
                obsoleteCustomClassConstructor = customClassConstructor = null;
            } else {
                obsoleteCustomClassConstructor = obsoleteCustomClassConstructor(customLeakDetector);
                customClassConstructor = customClassConstructor(customLeakDetector);
            }
        }

        private static Constructor<?> obsoleteCustomClassConstructor(String customLeakDetector) {
            try {
                final Class<?> detectorClass = Class.forName(customLeakDetector, true,
                        PlatformDependent.getSystemClassLoader());

                if (ResourceLeakDetector.class.isAssignableFrom(detectorClass)) {
                    return detectorClass.getConstructor(Class.class, int.class, long.class);
                } else {
                    logger.error("Class {} does not inherit from ResourceLeakDetector.", customLeakDetector);
                }
            } catch (Throwable t) {
                logger.error("Could not load custom resource leak detector class provided: {}",
                        customLeakDetector, t);
            }
            return null;
        }

        private static Constructor<?> customClassConstructor(String customLeakDetector) {
            try {
                final Class<?> detectorClass = Class.forName(customLeakDetector, true,
                        PlatformDependent.getSystemClassLoader());

                if (ResourceLeakDetector.class.isAssignableFrom(detectorClass)) {
                    return detectorClass.getConstructor(Class.class, int.class);
                } else {
                    logger.error("Class {} does not inherit from ResourceLeakDetector.", customLeakDetector);
                }
            } catch (Throwable t) {
                logger.error("Could not load custom resource leak detector class provided: {}",
                        customLeakDetector, t);
            }
            return null;
        }

        @SuppressWarnings("deprecation")
        @Override
        public <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource, int samplingInterval,
                                                                   long maxActive) {
            if (obsoleteCustomClassConstructor != null) {
                try {
                    @SuppressWarnings("unchecked")
                    ResourceLeakDetector<T> leakDetector =
                            (ResourceLeakDetector<T>) obsoleteCustomClassConstructor.newInstance(
                                    resource, samplingInterval, maxActive);
                    logger.debug("Loaded custom ResourceLeakDetector: {}",
                            obsoleteCustomClassConstructor.getDeclaringClass().getName());
                    return leakDetector;
                } catch (Throwable t) {
                    logger.error(
                            "Could not load custom resource leak detector provided: {} with the given resource: {}",
                            obsoleteCustomClassConstructor.getDeclaringClass().getName(), resource, t);
                }
            }

            ResourceLeakDetector<T> resourceLeakDetector = new ResourceLeakDetector<T>(resource, samplingInterval,
                    maxActive);
            logger.debug("Loaded default ResourceLeakDetector: {}", resourceLeakDetector);
            return resourceLeakDetector;
        }

        @Override
        public <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource, int samplingInterval) {
            if (customClassConstructor != null) {
                try {
                    @SuppressWarnings("unchecked")
                    ResourceLeakDetector<T> leakDetector =
                            (ResourceLeakDetector<T>) customClassConstructor.newInstance(resource, samplingInterval);
                    logger.debug("Loaded custom ResourceLeakDetector: {}",
                            customClassConstructor.getDeclaringClass().getName());
                    return leakDetector;
                } catch (Throwable t) {
                    logger.error(
                            "Could not load custom resource leak detector provided: {} with the given resource: {}",
                            customClassConstructor.getDeclaringClass().getName(), resource, t);
                }
            }

            ResourceLeakDetector<T> resourceLeakDetector = new ResourceLeakDetector<T>(resource, samplingInterval);
            logger.debug("Loaded default ResourceLeakDetector: {}", resourceLeakDetector);
            return resourceLeakDetector;
        }
    }
}
