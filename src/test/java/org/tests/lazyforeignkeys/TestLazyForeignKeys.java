package org.tests.lazyforeignkeys;

import io.ebean.BaseTestCase;
import io.ebean.DB;
import io.ebean.Ebean;
import io.ebean.Query;
import io.ebean.text.PathProperties;
import org.ebeantest.LoggedSqlCollector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestLazyForeignKeys extends BaseTestCase {

  @Before
  public void prepare() {
    MainEntity ent1 = new MainEntity();
    ent1.setId("ent1");
    ent1.setAttr1("attr1");
    DB.save(ent1);

    MainEntityRelation rel1 = new MainEntityRelation();
    MainEntity e1 = new MainEntity();
    e1.setId("ent1");
    MainEntity e2 = new MainEntity();
    e2.setId("ent2");

    rel1.setEntity1(e1);
    rel1.setEntity2(e2);
    DB.save(rel1);
  }

  @After
  public void cleanup() {
    DB.find(MainEntity.class).delete();
    DB.find(MainEntityRelation.class).delete();
  }

  @Test
  public void testFindOne() throws Exception {
    // use findOne without select, so lazy loading will occur
    LoggedSqlCollector.start();

    MainEntityRelation rel1 = DB.find(MainEntityRelation.class).findOne();

    assertEquals("ent1", rel1.getEntity1().getId());
    assertEquals("ent2", rel1.getEntity2().getId());

    assertEquals("attr1", rel1.getEntity1().getAttr1());
    assertFalse(rel1.getEntity1().isDeleted());
    assertTrue(rel1.getEntity2().isDeleted());

    List<String> loggedSql = LoggedSqlCollector.stop();
    assertThat(loggedSql).hasSize(3);
    assertThat(loggedSql.get(0)).contains("select t0.id, t0.attr1, t0.id1, t0.id2 from main_entity_relation");
    if (isSqlServer()) {
      assertThat(loggedSql.get(1)).contains("select t0.id, t0.attr1, t0.attr2, CASE WHEN t0.id is null THEN 1 ELSE 0 END from main_entity t0");
    } else {
      assertThat(loggedSql.get(1)).contains("select t0.id, t0.attr1, t0.attr2, t0.id is null from main_entity t0");
      assertThat(loggedSql.get(2)).contains("select t0.id, t0.attr1, t0.attr2, t0.id is null from main_entity t0");
    }
  }

  @Test
  public void testFindListWithSelect() {
    PathProperties pathProp = new PathProperties();
    pathProp.addToPath(null, "attr1");
    pathProp.addToPath("entity1", "id");
    pathProp.addToPath("entity2", "id");

    Query<MainEntityRelation> query = Ebean.find(MainEntityRelation.class).apply(pathProp);
    List<MainEntityRelation> list = query.findList();
    assertEquals(1, list.size());

    assertSql(query).contains("t0.id, t0.attr1, t0.id1, t0.id2, t1.id, t2.id");

    MainEntityRelation rel1 = list.get(0);
    assertEquals("ent1", rel1.getEntity1().getId());
    assertEquals("ent2", rel1.getEntity2().getId());

    assertEquals("attr1", rel1.getEntity1().getAttr1());
    assertFalse(rel1.getEntity1().isDeleted());
    assertTrue(rel1.getEntity2().isDeleted());
  }
}
