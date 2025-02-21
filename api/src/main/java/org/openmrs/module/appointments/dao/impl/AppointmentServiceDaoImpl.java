package org.openmrs.module.appointments.dao.impl;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.appointments.dao.AppointmentServiceDao;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;

public class AppointmentServiceDaoImpl implements AppointmentServiceDao{

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



    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<AppointmentServiceDefinition> getAllAppointmentServices(boolean includeVoided, Integer userId) {

        // Get User Locations
        List<String> locationIds = new LinkedList<>();
        if (userId != null){
        Session session = sessionFactory.getCurrentSession();
        locationIds = session.createSQLQuery(LOCATION_QUERY)
                .setParameter("user_id", userId)
                .list();
        }

        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(AppointmentServiceDefinition.class, "appointmentService");
        if (!locationIds.isEmpty()) {
            criteria.add(
                    Restrictions.or(
                            Restrictions.in("location.id", locationIds),
                            Restrictions.isNull("location")
                    )
            );
        }

        if(!includeVoided) {
            criteria.add(Restrictions.eq("voided", includeVoided));
        }
        return criteria.list();
    }

    @Transactional
    @Override
    public AppointmentServiceDefinition save(AppointmentServiceDefinition appointmentServiceDefinition) {
        Session currentSession = sessionFactory.getCurrentSession();
        currentSession.saveOrUpdate(appointmentServiceDefinition);
        return appointmentServiceDefinition;
    }

    @Override
    public AppointmentServiceDefinition getAppointmentServiceByUuid(String uuid) {
        Session currentSession = sessionFactory.getCurrentSession();
        Criteria criteria = currentSession.createCriteria(AppointmentServiceDefinition.class, "appointmentServiceDefinition");
        criteria.add(Restrictions.eq("uuid", uuid));
        AppointmentServiceDefinition appointmentServiceDefinition = (AppointmentServiceDefinition) criteria.uniqueResult();
        evictObjectFromSession(currentSession, appointmentServiceDefinition);
        return appointmentServiceDefinition;
    }

    @Override
    public AppointmentServiceDefinition getNonVoidedAppointmentServiceByName(String serviceName) {
        Session currentSession = sessionFactory.getCurrentSession();
        Criteria criteria = currentSession.createCriteria(AppointmentServiceDefinition.class, "appointmentServiceDefinition");
        criteria.add(Restrictions.eq("name", serviceName));
        criteria.add(Restrictions.eq("voided", false));
        AppointmentServiceDefinition appointmentServiceDefinition = (AppointmentServiceDefinition) criteria.uniqueResult();
        evictObjectFromSession(currentSession, appointmentServiceDefinition);
        return appointmentServiceDefinition;
    }

    @Override
    public AppointmentServiceType getAppointmentServiceTypeByUuid(String uuid) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(AppointmentServiceType.class, "appointmentServiceType");
        criteria.add(Restrictions.eq("uuid", uuid));
        return (AppointmentServiceType) criteria.uniqueResult();
    }

    private void evictObjectFromSession(Session currentSession, AppointmentServiceDefinition appointmentServiceDefinition) {
        if (appointmentServiceDefinition != null) {
            currentSession.evict(appointmentServiceDefinition);
        }
    }
}
