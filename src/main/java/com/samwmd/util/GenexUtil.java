package com.samwmd.util;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
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
import java.util.ArrayList;
import java.util.List;

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
        String idType;
        if (entityId == null){
            idType = getIdType(attributeList);
        }else{
            idType = getIdType(attributeList,entityId);
        }
        setEntityName(entityName);
        setIdType(idType);
        setAttributeList(attributeList);
        setDtoAttributeList(dtoAttributeList);
        setOutputDir(outputDir);
        setUseLombok(useLombok);
        // Generate code based on the entityName and parsed attributes
        generateEntityClass();
        generateDto();

        if (isTrue(generateMapperArg))
            generateMapstructMapper();

        if (isTrue(generateRepositoryArg))
            generateRepository();
    }

    public boolean isTrue(String arg) throws IllegalArgumentException{
        if (arg == null) return false;
        if ( !List.of("true","false","f","t").contains(arg.toLowerCase()))
            throw new IllegalArgumentException("Illegal argument. You provided: "+arg+". Accepted values are ('true', 't', 'false', 'f')");

        return List.of("true","t").contains(arg.toLowerCase());
    }

    /**
     * returns the ID type of the entity based on the attributeList
     * if attributeList contains and attribute with the name "id": return the type of "id"
     * else return the first attribute's type
     * @param attributeList
     * @return String
     */
    private String getIdType(List<Attribute> attributeList) {
        Attribute attribute = attributeList.stream().filter(a->a.getName().toLowerCase().equals("id")).findFirst().orElse(null);
        return attribute == null ? attributeList.stream().findFirst().get().getType() : attribute.getType();
    }

    private String getIdType(List<Attribute> attributeList, String idName){
        Attribute attribute = attributeList.stream().filter(a->a.getName().toLowerCase() == idName.toLowerCase()).findFirst().orElse(null);
        if (attribute == null){
            throw new IllegalArgumentException("The provided idName ("+idName+") cannot be found in the entity's attributes.");
        }
        return attribute.getType();
    }
    private void generateDto(){
        String dir = outputDir+"/dto";
        velocityUtil.getContext().put("dtoAttributes", attributeList);
        generateSource(entityName+"Dto", dir, DTO_TEMPLATE_NAME);
    }

    private void generateRepository() {
        String dir = outputDir+"/repository";
        String templateName = REPOSITORY_TEMPLATE_NAME;
        generateSource(entityName+"Repository", dir, templateName);
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
        String templateName = MAPPER_TEMPLATE_NAME;
        generateSource(entityName+"Mapper",dir,templateName);
    }
    private void generateSource(String className, String outputDir, String templateName) {
        createDirectoryIfNotExists(outputDir);
        velocityUtil.getContext().put("package", outputDir.replace("/",".").split("java.")[1]);

        Template template = velocityEngine.getTemplate(templateName);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputDir +"/"+className+".java");
            template.merge(context, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Error while generating "+templateName.split(".")[0]+" class for : "+className);
        }
    }

    public List<Attribute> parseAttributes(String attributes) {
        String[] attributeArray = attributes.split(";");
        List<Attribute> attributeList = new ArrayList<>();

        for (String attributeString : attributeArray) {
            String[] attributeParts = attributeString.trim().split(":");
            if (attributeParts.length == 2) {
                String attributeName = attributeParts[0].trim();
                String attributeType = attributeParts[1].trim();
                attributeList.add(new Attribute(attributeName, attributeType));
            }
        }
        return attributeList;
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

