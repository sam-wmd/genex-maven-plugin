package com.samwmd.util;

import lombok.Getter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Properties;

@Named
@Singleton
@Getter
public class VelocityUtil {

    private final VelocityContext context = new VelocityContext();
    private final VelocityEngine velocityEngine;

    public VelocityUtil(){
        Properties p = new Properties();
        p.setProperty("file.resource.loader.path", "src/main/template");
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loader", "class");
        velocityEngine.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init(p);
    }

}
