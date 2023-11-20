package com.samwmd.mojo;

import com.samwmd.util.GenexUtil;
import com.samwmd.util.VelocityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;


/**
 * Mojo to generate source files based on command line arguments
 * Example usage:
 * <pre>
 *     mvn genex:generate -DentityName=Person -Dattributes='id:Long;username:String' -DdtoAttributes='username:String'
 * </pre>
 */
@Slf4j
@Mojo(name="generate", defaultPhase = LifecyclePhase.INITIALIZE)
public class GenexMojo extends AbstractMojo {

    /**
     * Utility class for performing Genex-related operations.
     */
    @Inject
    private GenexUtil genexUtil;

    /**
     * Utility class for working with Velocity templates.
     */
    @Inject
    private VelocityUtil velocityUtil;

    /**
     * The name of the entity for which code will be generated.
     */
    @Parameter(property="entityName", required = true)
    private String entityName;

    /**
     * A semi-colon-separated list of attributes for the entity.
     * Example:
     * -Dattributes='id:Long;username:String'
     */
    @Parameter(property="attributes", required = true)
    private String attributes;

    /**
     * Optional: A comma-separated list of attributes for the Data Transfer Object (DTO).
     */
    @Parameter(property = "dtoAttributes", required = false)
    private String dtoAttributes;

    /**
     * Optional: The data type for the entity's identifier (e.g., 'Long' or 'Integer').
     */
    @Parameter(property = "entityId", required = false)
    private String entityId;

    /**
     * Optional: Specify whether to generate a repository for the entity.
     */
    @Parameter(property = "generateRepository", required = false, defaultValue = "true")
    private String generateRepository;

    /**
     * Optional: Specify whether to generate a Mapper class for the entity.
     */
    @Parameter(property = "generateMapper", required=false, defaultValue = "true")
    private String generateMapper;
    /**
     * The base directory where code will be generated.
     */
    private final String baseDir = "src/main/java/";

    /**
     * Optional: The output directory path for generated code. If not specified, the base directory will be used.
     */
    @Parameter(property = "outputDir", required=false, defaultValue = "")
    private String outputDirectoryPath;

    /**
     * The group ID of the Maven project.
     */
    @Parameter(defaultValue = "${project.groupId}", readonly = true, required = true)
    private String groupId;

    /**
     * The Maven project object for accessing project information.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject mavenProject;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Generating code for entityName: " + entityName);
        getLog().info("Using attributes: " + attributes);
        getLog().info("groupId: "+groupId);

        velocityUtil.getContext().put("groupId", groupId);
        if (outputDirectoryPath == null){
            outputDirectoryPath = baseDir+groupId.replace(".","/");
        }
        // generate classes
        getLog().info("genMapper: "+generateMapper);
        try {
            this.genexUtil.generateCode(
                    entityName,
                    entityId,
                    attributes,
                    dtoAttributes,
                    outputDirectoryPath,
                    projectHasLombokDependency(),
                    parseBoolean(generateRepository),
                    parseBoolean(generateMapper));

        } catch (IllegalArgumentException e) {
            log.error("Generating code failed: " + e + ". Process terminated.");
            System.exit(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean projectHasLombokDependency(){
        List<Dependency> dependencies = mavenProject.getDependencies();
        return dependencies.stream().anyMatch( dependency -> "org.projectlombok".equals(dependency.getGroupId()) &&
                "lombok".equals(dependency.getArtifactId()));
    }

    private boolean parseBoolean(String arg) {
        if (arg == null) {
            return false;
        }

        if(!List.of("true", "t", "false", "f").contains(arg.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Illegal argument. You've provided: "+arg+". Accepted values are ('true', 't', 'false', 'f')"
            );
        }

        return List.of("true","t").contains(arg.toLowerCase());
    }
}
