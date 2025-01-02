package lx.mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaPredicate;

import jakarta.persistence.criteria.Root;
import lx.model.Zdm;
import lx.utils.HibernateUtil;

public class ZdmMapper {
    public static void saveOrUpdateBatch(Collection<Zdm> entitys) {
        try (Session session = HibernateUtil.getSessionFactory()) {
            Transaction transaction = session.beginTransaction();
            try {
                entitys.forEach(session::saveOrUpdate);
                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            }
        }
    }

    public static List<Zdm> unPush() {
        try (Session session = HibernateUtil.getSessionFactory()) {
            HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
            JpaCriteriaQuery<Zdm> query = builder.createQuery(Zdm.class);
            Root<Zdm> root = query.from(Zdm.class);
            JpaPredicate predicate = builder.equal(root.get("pushed"), false);
            return session.createQuery(query.where(predicate)).list();
        }
    }

    public static Collection<String> pushedIds() {
        try (Session session = HibernateUtil.getSessionFactory()) {
            HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
            JpaCriteriaQuery<Zdm> query = builder.createQuery(Zdm.class);
            Root<Zdm> root = query.from(Zdm.class);
            JpaPredicate predicate = builder.and(
                    builder.equal(root.get("pushed"), true),
                    builder.greaterThan(root.get("article_time"), LocalDateTime.now().minusMonths(1).toString()));
            List<Zdm> pushed = session.createQuery(query.where(predicate)).list();
            return pushed.stream().map(Zdm::getArticleId).collect(Collectors.toSet());
        }
    }

}
