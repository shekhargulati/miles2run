package org.miles2run.business.producers;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by shekhargulati on 04/03/14.
 */
public class EntityManagerProducer {

    @Produces
    @PersistenceContext
    private EntityManager entityManager;
}
