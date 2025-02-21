package org.openmrs.module.appointments.service.impl;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.dao.UserLocationDao;
import org.openmrs.module.appointments.service.UserLocationService;

import java.util.List;

public class UserLocationServiceImpl implements UserLocationService {

    private UserLocationDao userLocationDao;

    public void setUserLocationDao(UserLocationDao userLocationDao) {
        this.userLocationDao = userLocationDao;
    }

    public List<String> getUserLocationIds() {
        User user = Context.getAuthenticatedUser();
        return userLocationDao.getUserLocationIds(user.getId());
    }
}
