package com.samwmd.util.dependency;

import lombok.experimental.Accessors;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.List;

@Accessors(fluent = true)
public class Configuration extends Xpp3Dom {

    public Configuration setSource(String value){
        getChild("source").setValue(value);
        return this;
    }

    public Configuration setTarget(String value){
        getChild("target").setValue(value);
        return this;
    }


    public Configuration setAnnotationProcessorPaths(List<Path> annotationProcessorPaths) {
        for (Path path: annotationProcessorPaths){
            this.getChild("annotationProcessorPaths").addChild(path);
        }

        return this;
    }

    public Configuration() {
        super("configuration");
        addChild(new Xpp3Dom("annotationProcessorPaths"));
    }
}
