package org.openmrs.module.appointments.dao;

import java.util.List;

public interface UserLocationDao {
    List<String> getUserLocationIds(Integer userId);
}
