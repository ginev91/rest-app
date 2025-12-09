package org.example.main.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TableReservationEntityTest {

    @Test
    void prePersist_setsId_and_createdAt_ifMissing() throws Exception {
        TableReservationEntity tr = new TableReservationEntity();
        tr.setTableId(UUID.randomUUID());
        tr.setUserId(UUID.randomUUID());
        tr.setStartTime(OffsetDateTime.now().plusDays(1));
        tr.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));

        tr.setId(null);
        tr.setCreatedAt(null);

        Method m = TableReservationEntity.class.getDeclaredMethod("prePersist");
        m.setAccessible(true);
        m.invoke(tr);

        assertThat(tr.getId()).isNotNull();
        assertThat(tr.getCreatedAt()).isNotNull();
    }
}