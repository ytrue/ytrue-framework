package com.ytrue.middleware.orm.spring;

import org.springframework.asm.ClassReader;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.AnnotationMetadataReadingVisitor;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ytrue
 * @date 2022/9/10 12:14
 * @description SimpleMetadataReader
 */
public class SimpleMetadataReader  implements MetadataReader {

    private final Resource resource;

    private final ClassMetadata classMetadata;

    private final AnnotationMetadata annotationMetadata;

    public SimpleMetadataReader(Resource resource, ClassLoader classLoader) throws IOException {
        // resource == 扫描的dao class文件

        ClassReader classReader;
        try (InputStream is = new BufferedInputStream(resource.getInputStream())) {
            classReader = new ClassReader(is);
        } catch (IllegalArgumentException ex) {
            throw new NestedIOException("ASM ClassReader failed to parse class file - " +
                    "probably due to a new Java class file version that isn't supported yet: " + resource, ex);
        }

        AnnotationMetadataReadingVisitor visitor = new AnnotationMetadataReadingVisitor(classLoader);
        classReader.accept(visitor, ClassReader.SKIP_DEBUG);

        this.annotationMetadata = visitor;
        // (since AnnotationMetadataReadingVisitor extends ClassMetadataReadingVisitor)
        this.classMetadata = visitor;
        this.resource = resource;
    }


    @Override
    public Resource getResource() {
        return this.resource;
    }

    @Override
    public ClassMetadata getClassMetadata() {
        return this.classMetadata;
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return this.annotationMetadata;
    }
}
