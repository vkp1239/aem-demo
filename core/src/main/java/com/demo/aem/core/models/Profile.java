package com.demo.aem.core.models;

import java.util.List;
import java.util.Map;

public interface Profile {
    String getFirstName();
    String getLastName();
    String getEmailId();
    String getFileReference();
    String getBio();
    String [] getHobbies();
    List<Map<String,String>> getSkills();

}
