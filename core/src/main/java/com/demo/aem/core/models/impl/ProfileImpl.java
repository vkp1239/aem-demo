package com.demo.aem.core.models.impl;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.demo.aem.core.models.Profile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(adaptables = {SlingHttpServletRequest.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL, adapters = Profile.class,
        resourceType = {ProfileImpl.RESOURCE_TYPE}
)
@Exporter(name = "jackson", extensions = "json", selector = "model",
        options = {@ExporterOption(name = "MapperFeature.SORT_PROPERTIES_ALPHABETICALLY", value = "false")})
@Slf4j
public class ProfileImpl implements Profile {

    public static final String RESOURCE_TYPE = "demo/components/content/profile";

    @ValueMapValue
    String firstname;

    @ValueMapValue
    String lastname;

    @ValueMapValue
    String emailId;

    @ValueMapValue
    String fileReference;

    @ValueMapValue
    String bio;

    @Self
    @Via("resource")
    Resource resource;

    @Self
    SlingHttpServletRequest slingHttpServletRequest;

    @ScriptVariable
    Page currentPage;

    @SlingObject
    ResourceResolver resolver;

    @ResourcePath(path = "/content/demo/us/en")
    @Via("resource")
    Resource homePageResource;

    @ValueMapValue
    String[] hobbies;

    @ChildResource
    Resource skills;

// Path for exporter
// http://localhost:4502/content/demo/us/en/demo/jcr:content/root/container/container/profile.model.json

    @PostConstruct
    void init() {
        log.info("Current Resource Path via Resource :- {}", resource.getPath());
        log.info("Current Resource Path via Request {}", slingHttpServletRequest.getRequestPathInfo().getResourcePath());
        log.info("Current Page Path via ScriptVariable {}", currentPage.getPath());
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        log.info("Current Page Path via SlingObject's resolver {}", pageManager.getContainingPage(currentPage.getPath()).getPath());
        log.info("Home Page name :- {}", homePageResource.getName());

    }

    @Override
    @JsonIgnore
    public String getFirstName() {
        return firstname;
    }

    @Override
    @JsonIgnore
    public String getLastName() {
        return lastname;
    }

    @Override
    public String getEmailId() {
        return emailId;
    }

    @Override
    @JsonProperty(value = "profile-image")
    public String getFileReference() {
        return fileReference;
    }

    @Override
    public String getBio() {
        return bio;
    }

    @Override
    public String[] getHobbies() {
        return hobbies;
    }

    @Override
    public List<Map<String, String>> getSkills() {
        List<Map<String, String>> skillList = new ArrayList<>();
        for (Resource item : skills.getChildren()) {
            Map<String, String> values = new HashMap<>();
            values.put("skill", item.getValueMap().get("skill").toString());
            values.put("level", item.getValueMap().get("level").toString());
            skillList.add(values);
        }
        return skillList;
    }

    public String getName() {
        return firstname + " " + lastname;
    }

}
