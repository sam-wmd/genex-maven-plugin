package com.samwmd.util;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Named
@Singleton
@Slf4j
@Setter
public class GenexUtil {

    private static final String ENTITY_TEMPLATE_NAME = "Entity.vm";
    private static final String LOMBOK_ENTITY_TEMPLATE_NAME = "LombokEntity.vm";
    private static final String REPOSITORY_TEMPLATE_NAME = "JpaRepository.vm";
    private static final String DTO_TEMPLATE_NAME = "Dto.vm";
    private static final String MAPPER_TEMPLATE_NAME = "MapStructMapper.vm";

    private String entityName;
    private String idType;
    private List<Attribute> attributeList;
    private List<Attribute> dtoAttributeList;
    private String outputDir;
    private boolean useLombok;

    @Inject
    private VelocityUtil velocityUtil;
    private VelocityEngine velocityEngine;
    private VelocityContext context;

    @PostConstruct
    void initVelocity(){
        velocityEngine = velocityUtil.getVelocityEngine();
        context = velocityUtil.getContext();
    }

    public void generateCode(
            String entityName,
            String entityId,
            String attributes,
            String dtoAttributes,
            String outputDir,
            boolean useLombok,
            String generateRepositoryArg,
            String generateMapperArg) throws IOException {

        entityName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1);
        // Parse the attributes provided by the user
        List<Attribute> attributeList = parseAttributes(attributes);
        List<Attribute> dtoAttributeList = dtoAttributes == null ? attributeList : parseAttributes(dtoAttributes);

        String idType = entityId == null ? getIdTypeOrElseFirstAttributesType(attributeList) : getIdType(attributeList,entityId);

        setEntityName(entityName);
        setIdType(idType);
        setAttributeList(attributeList);
        setDtoAttributeList(dtoAttributeList);
        setOutputDir(outputDir);
        setUseLombok(useLombok);
        // Generate code based on the entityName and parsed attributes
        generateEntityClass();
        generateDto();

        if (parseBoolean(generateMapperArg)) {
            generateMapstructMapper();
        } else {
            log.info("Generating Mapstruct mapper disabled," +
                    " in order to enable pass \"t\" or \"true\" as generateMapper argument" +
                    "config value passed: " + generateMapperArg);
        }
        if (parseBoolean(generateRepositoryArg)) {
            generateRepository();
        } else {
            log.info("Generating repository disabled," +
                    " in order to enable pass \"t\" or \"true\" as generateRepository argument" +
                    "config value passed: " + generateRepositoryArg);
        }
    }

    private boolean parseBoolean(String arg) {
        if (arg == null) {
            return false;
        }

        return List.of("true","t").contains(arg.toLowerCase());
    }

    /**
     * returns the ID type of the entity based on the attributeList
     * if attributeList contains and attribute with the name "id": return the type of "id"
     * else return the first attribute's type
     * @param attributeList
     * @return String
     */
    private String getIdTypeOrElseFirstAttributesType(List<Attribute> attributeList) {
        return attributeList.stream()
                .filter(a-> a.getName().equalsIgnoreCase("id"))
                .findFirst().orElse(attributeList.stream().findFirst().get()).getType();
    }

    private String getIdType(List<Attribute> attributeList, String idName){
       return attributeList.stream()
                .filter(a-> a.getName().equalsIgnoreCase(idName))
                .findFirst()
                .orElseThrow( () -> new IllegalArgumentException("The provided idName ("+idName+") cannot be found in the entity's attributes."))
                .getType();
    }
    private void generateDto(){
        String dir = outputDir+"/dto";
        velocityUtil.getContext().put("dtoAttributes", dtoAttributeList);
        generateSource(entityName+"Dto", dir, DTO_TEMPLATE_NAME);
    }

    private void generateRepository() {
        String dir = outputDir+"/repository";
        generateSource(entityName+"Repository", dir, REPOSITORY_TEMPLATE_NAME);
    }

    private void generateEntityClass() {
        String dir = outputDir + "/model";

        velocityUtil.getContext().put("entityName", entityName);
        velocityUtil.getContext().put("idType", idType);
        velocityUtil.getContext().put("attributes", attributeList);

        String templateName = useLombok ? LOMBOK_ENTITY_TEMPLATE_NAME : ENTITY_TEMPLATE_NAME;
        createDirectoryIfNotExists(dir);
        generateSource(entityName,dir,templateName);
    }

    private void generateMapstructMapper(){
        String dir = outputDir+"/mapper";
        generateSource(entityName+"Mapper",dir,MAPPER_TEMPLATE_NAME);
    }
    private void generateSource(String className, String outputDir, String templateName) {

        createDirectoryIfNotExists(outputDir);
        velocityUtil.getContext().put("package", outputDir.replace("/",".").split("java.")[1]);

        try(FileWriter fileWriter = new FileWriter(outputDir +"/"+className+".java")) {
            velocityEngine.getTemplate(templateName).merge(context, fileWriter);
        } catch (IOException e) {
            System.err.println("Error while generating "+templateName.split(".")[0]+" class for : "+className);
        }
    }

    public List<Attribute> parseAttributes(String attributes) {

        return Arrays.stream(attributes.split(";"))
                .map(String::trim)
                .map(atributeString -> atributeString.split(":"))
                .filter(attributeParts -> attributeParts.length == 2)
                .map(attributeParts -> new Attribute(attributeParts[0].trim(), attributeParts[1].trim()))
                .collect(Collectors.toList());
    }

    private void createDirectoryIfNotExists(String pathName){
        Path path = Path.of(pathName);
        if (!Files.exists(path)){
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

