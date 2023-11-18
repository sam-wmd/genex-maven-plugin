package com.samwmd.mojo;

import com.samwmd.util.dependency.Configuration;
import com.samwmd.util.dependency.Path;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatically adds lombok and mapstruct dependencies to project pom.xml
 */
@Mojo(name="add-lombok-mapstruct", defaultPhase = LifecyclePhase.INITIALIZE)
public class DependencyMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject mavenProject;

    private Dependency createMapstructDependency() {

        Dependency mapstructDep = new Dependency();
        mapstructDep.setGroupId("org.mapstruct");
        mapstructDep.setArtifactId("mapstruct");
        mapstructDep.setVersion("${org.mapstruct.version}");

        return mapstructDep;
    }

    private Dependency createMapstructProcessorDependency() {

        Dependency mapstructProcessorDep = new Dependency();
        mapstructProcessorDep.setGroupId("org.mapstruct");
        mapstructProcessorDep.setArtifactId("mapstruct-processor");
        mapstructProcessorDep.setVersion("${org.mapstruct.version}");

        return mapstructProcessorDep;
    }

    private Dependency createLombokDependency() {

        Dependency lombokDep = new Dependency();
        lombokDep.setGroupId("org.projectlombok");
        lombokDep.setArtifactId("lombok");
        lombokDep.setVersion("${lombok.version}");
        lombokDep.setScope("provided");

        return lombokDep;
    }

    private Plugin createLombokMapstructPlugin(Configuration config) {
        Plugin lombokMapstructPlugin = new Plugin();

        lombokMapstructPlugin.setGroupId("org.apache.maven.plugins");
        lombokMapstructPlugin.setArtifactId("maven-compiler-plugin");
        lombokMapstructPlugin.setVersion("3.11.0");
        lombokMapstructPlugin.setConfiguration(config);

        return lombokMapstructPlugin;
    }

    private List<Path> getAnnotationProcessorPathList() {
        return List.of(
                new Path().setGroupId("org.projectlombok").setArtifactId("lombok").setVersion("${lombok.version}"),
                new Path().setGroupId("org.mapstruct").setArtifactId("mapstruct-processor").setVersion("${org.mapstruct.version}"),
                new Path().setGroupId("org.projectlombok").setArtifactId("lombok-mapstruct-binding").setVersion("0.2.0")
        );
    }

    private Model initializeMavenProjectModel(Map<String, String> properties, List<Dependency> dependencies) {

        Model model = mavenProject.getModel();

        File pomFile = model.getPomFile();

        try (FileReader reader = new FileReader(pomFile)){

            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            model = xpp3Reader.read(reader);

        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException(e);
        }

        for( var entry : properties.entrySet()) {
            model.addProperty(entry.getKey(), entry.getValue());
        }

        for( var d : dependencies){
            model.addDependency(d);
        }

        return model;
    }

    private void registerCompilerPlugin(Model model, Configuration config) {

        Plugin mavenCompilerPlugin =
                model.getBuild()
                        .getPlugins()
                        .stream()
                        .filter(p->p.getArtifactId().equals("maven-compiler-plugin"))
                        .findFirst()
                        .orElse(null);

        if (mavenCompilerPlugin == null){
            model.getBuild().addPlugin(createLombokMapstructPlugin(config));
        }else {
            model.getBuild().removePlugin(mavenCompilerPlugin);
            mavenCompilerPlugin.setConfiguration(config);
            model.getBuild().addPlugin(mavenCompilerPlugin);
        }
    }
    private void updatePomFile(Model model) {

        try (FileWriter writer = new FileWriter(model.getPomFile())){
            MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
            xpp3Writer.write(writer, model);

            getLog().info("pom.xml updated !");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Model model = initializeMavenProjectModel(
                Map.of("org.mapstruct.version","1.5.5.Final","lombok.version","1.18.30"),
                List.of(
                        createMapstructDependency(),
                        createMapstructProcessorDependency(),
                        createLombokDependency()
                )
        );

        Configuration config = new Configuration();
        config.setAnnotationProcessorPaths(getAnnotationProcessorPathList());

        registerCompilerPlugin(model, config);

        updatePomFile(model);
    }
}
