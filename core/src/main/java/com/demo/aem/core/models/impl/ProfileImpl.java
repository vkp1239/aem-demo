package com.demo.aem.core.models.impl;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.demo.aem.core.models.Profile;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Model(adaptables = {Resource.class,SlingHttpServletRequest.class}, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL, adapters = Profile.class)
@Slf4j
public class ProfileImpl implements Profile {
    private static final Logger log = LoggerFactory.getLogger(ProfileImpl.class);
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
    @Via("request")
    // Remove above two and use @SlingObject
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




    @PostConstruct
    void init() {
    log.info("Current Resource Path via Resource :- {}",resource.getPath());
    log.info("Current Resource Path via Request {}",slingHttpServletRequest.getRequestPathInfo().getResourcePath());
    log.info("Current Page Path via ScriptVariable {}",currentPage.getPath());
    PageManager pageManager = resolver.adaptTo(PageManager.class);
    log.info("Current Page Path via SlingObject's resolver {}",pageManager.getContainingPage(currentPage.getPath()).getPath());
    log.info("Home Page name :- {}",homePageResource.getName());

    }

    @Override
    public String getFirstName() {
        return firstname;
    }

    @Override
    public String getLastName() {
        return lastname;
    }

    @Override
    public String getEmailId() {
        return emailId;
    }

    @Override
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
}
