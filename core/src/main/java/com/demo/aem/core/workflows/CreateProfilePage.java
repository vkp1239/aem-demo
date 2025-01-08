package com.demo.aem.core.workflows;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component(service = WorkflowProcess.class, immediate = true, property = {"process.label=Profile Page Creation/Deletion Process Step"})
@Slf4j
public class CreateProfilePage implements WorkflowProcess {

    public static final String PROFILE_PAGE_ROOT_PATH = "/content/demo/us/en/profile";
    public static final String PROFILE_PAGE_TEMPLATE_PATH = "/conf/demo/settings/wcm/templates/page-content";
    public static final String PROFILE_COMPONENT_RESOURCE_TYPE = "demo/components/content/profile";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        log.debug("Profile Page Creation/Deletion Process Step Starts...");

        try (ResourceResolver resolver = Objects.requireNonNull(workflowSession.adaptTo(ResourceResolver.class), "ResourceResolver is null")) {
            String payload = Optional.ofNullable(workItem.getWorkflowData().getPayload()).map(Object::toString).orElse("");
            Resource payloadResource = resolver.resolve(payload);
            ContentFragment contentFragment = payloadResource.adaptTo(ContentFragment.class);

            if (contentFragment != null) {
                handleProfilePageCreation(resolver, contentFragment);
            } else {
                handleProfilePageDeletion(resolver, payload);
            }

        } catch (PersistenceException e) {
            log.error("Error occurred during profile page creation or deletion", e);
        }
    }

    private void handleProfilePageCreation(ResourceResolver resolver, ContentFragment contentFragment) throws PersistenceException {
        String pageName = contentFragment.getName();
        PageManager pageManager = resolver.adaptTo(PageManager.class);

        if (pageManager == null) {
            log.warn("PageManager is null, skipping profile page creation.");
            return;
        }

        Page page = Optional.ofNullable(pageManager.getContainingPage(PROFILE_PAGE_ROOT_PATH + "/" + pageName)).orElseGet(() -> createPage(pageManager, pageName, contentFragment.getTitle()));

        Optional.ofNullable(resolver.getResource(page.getPath() + "/jcr:content/root/container/container")).ifPresent(containerResource -> updateComponentProperties(contentFragment, resolver, containerResource));
    }

    private Page createPage(PageManager pageManager, String pageName, String title) {
        try {
            return pageManager.create(PROFILE_PAGE_ROOT_PATH, pageName, PROFILE_PAGE_TEMPLATE_PATH, title);
        } catch (WCMException e) {
            log.error("Failed to create page: {}", pageName, e);
            return null;
        }
    }

    private void handleProfilePageDeletion(ResourceResolver resolver, String payload) {
        String pageName = StringUtils.substringAfterLast(payload, "/");
        PageManager pageManager = resolver.adaptTo(PageManager.class);

        if (pageManager != null) {
            Optional.ofNullable(pageManager.getContainingPage(PROFILE_PAGE_ROOT_PATH + "/" + pageName)).ifPresent(page -> {
                try {
                    pageManager.delete(page, false);
                } catch (WCMException e) {
                    log.error("Failed to delete page: {}", page.getPath(), e);
                }
            });
        }
    }

    private void updateComponentProperties(ContentFragment contentFragment, ResourceResolver resolver, Resource containerResource) {
        if (!isFragmentAuthored(contentFragment)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("firstname", getElementValue(contentFragment, "firstName", String.class));
        properties.put("lastname", getElementValue(contentFragment, "lastName", String.class));
        properties.put("bio", getElementValue(contentFragment, "bio", String.class));
        properties.put("fileReference", getElementValue(contentFragment, "fileReference", String.class));
        properties.put("hobbies", getElementValue(contentFragment, "hobbies", String[].class));
        properties.put("sling:resourceType", PROFILE_COMPONENT_RESOURCE_TYPE);

        try {
            String profileResourcePath = containerResource.getPath() + "/profile";
            Resource profileResource = resolver.getResource(profileResourcePath);

            if (profileResource != null) {
                profileResource.adaptTo(ModifiableValueMap.class).putAll(properties);
            } else {
                ResourceUtil.getOrCreateResource(resolver, profileResourcePath, properties, "", true);
            }
            updateSkills(contentFragment, resolver, profileResourcePath);
            resolver.commit();
        } catch (PersistenceException e) {
            log.error("Error updating component properties for container: {}", containerResource.getPath(), e);
        }
    }

    private void updateSkills(ContentFragment contentFragment, ResourceResolver resolver, String profileResourcePath) throws PersistenceException {
        Resource skillResource = ResourceUtil.getOrCreateResource(resolver, profileResourcePath + "/skills", "", "", true);
        String[] skills = getElementValue(contentFragment, "skills", String[].class);
        // Step 1: Remove all existing child nodes under the "skills" resource
        for (Resource child : skillResource.getChildren()) {
            resolver.delete(child);
        }
        // Step 2: Recreate child nodes based on the skills array
        int index = 0;
        for (String skill : skills) {
            String childNodeName = "item" + index;
            Resource childResource = ResourceUtil.getOrCreateResource(skillResource.getResourceResolver(), skillResource.getPath() + "/" + childNodeName, "nt:unstructured", // Node type for the child
                    null, true);

            // Add the "skill" property to the child node
            ModifiableValueMap valueMap = childResource.adaptTo(ModifiableValueMap.class);
            if (valueMap != null) {
                valueMap.put("skill", skill);
            }
            index++;
        }
    }

    private <T> T getElementValue(ContentFragment contentFragment, String elementName, Class<T> clazz) {
        return Optional.ofNullable(contentFragment.getElement(elementName)).map(ContentElement::getValue).map(value -> value.getValue(clazz)).orElse(null);
    }

    private boolean isFragmentAuthored(ContentFragment contentFragment) {
        String[] requiredFields = {"firstName", "bio"};

        for (String field : requiredFields) {
            if (getElementValue(contentFragment, field, String.class) == null) {
                return false;
            }
        }
        return true;
    }
}
