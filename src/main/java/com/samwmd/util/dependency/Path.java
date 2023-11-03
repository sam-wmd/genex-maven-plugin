package com.samwmd.util.dependency;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Path extends Xpp3Dom {

    public Path(){
        super("path");
        addChild(new Xpp3Dom("groupId"));
        addChild(new Xpp3Dom("artifactId"));
        addChild(new Xpp3Dom("version"));

    }

    public Path setGroupId(String value) {
        getChild("groupId").setValue(value);

        return this;
    }

    public Path setArtifactId(String value) {
        getChild("artifactId").setValue(value);

        return this;
    }

    public Path setVersion(String value) {
        getChild("version").setValue(value);

        return this;
    }
}