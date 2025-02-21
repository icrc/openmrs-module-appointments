package org.openmrs.module.appointments.dao.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.module.appointments.dao.UserLocationDao;

import java.util.LinkedList;
import java.util.List;

public class UserLocationDaoImpl implements UserLocationDao {

    private SessionFactory sessionFactory;

    // SQL query to get location IDs associated with the user
    private static final String LOCATION_QUERY = "SELECT l.location_id\n" +
                                                 "FROM location l\n" +
                                                 "WHERE l.location_id IN (\n" +
                                                 "    SELECT debm.basis_identifier\n" +
                                                 "    FROM datafilter_entity_basis_map debm\n" +
                                                 "    WHERE debm.basis_type = 'org.openmrs.Location'\n" +
                                                 "      AND debm.entity_type = 'org.openmrs.User'\n" +
                                                 "      AND debm.entity_identifier = :user_id)";

    public List<String> getUserLocationIds(Integer userId) {
        List<String> locationIds = new LinkedList<>();
        if (userId != null){
            Session session = sessionFactory.getCurrentSession();
            locationIds = session.createSQLQuery(LOCATION_QUERY)
                    .setParameter("user_id", userId)
                    .list();
        }
        return locationIds;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
}
