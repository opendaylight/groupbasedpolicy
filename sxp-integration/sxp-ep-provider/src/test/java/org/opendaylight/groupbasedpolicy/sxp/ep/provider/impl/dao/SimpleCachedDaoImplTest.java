/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Test for {@link SimpleCachedDaoImpl}.
 */
public class SimpleCachedDaoImplTest {

    private static final String KEY_1 = "dummyKey1";
    private static final String KEY_2 = "dummyKey2";
    private static final DummyDataObject DUMMY_DATA_1 = new DummyDataObject("dummyData1");
    private static final DummyDataObject DUMMY_DATA_2 = new DummyDataObject("dummyData2");

    private SimpleCachedDaoImpl<String, DummyDataObject> dao;

    @Before
    public void setUp() throws Exception {
        dao = new SimpleCachedDaoImpl<>();
        Assert.assertFalse(dao.find(KEY_1).isPresent());
    }

    @Test
    public void testUpdate() throws Exception {
        dao.update(KEY_1, DUMMY_DATA_1);
        final Optional<DummyDataObject> dataOpt = dao.find(KEY_1);
        Assert.assertTrue(dataOpt.isPresent());
        Assert.assertEquals(DUMMY_DATA_1.getDummyData(), dataOpt.get().getDummyData());
    }

    @Test
    public void testInvalidateCache() throws Exception {
        dao.update(KEY_1, DUMMY_DATA_1);
        Assert.assertTrue(dao.find(KEY_1).isPresent());
        dao.invalidateCache();
        Assert.assertFalse(dao.find(KEY_1).isPresent());
    }

    @Test
    public void testIsEmpty() throws Exception {
        Assert.assertTrue(dao.isEmpty());
        dao.update(KEY_1, DUMMY_DATA_1);
        Assert.assertFalse(dao.isEmpty());
    }

    @Test
    public void testValues() throws Exception {
        Assert.assertEquals(0, Iterables.size(dao.values()));
        dao.update(KEY_1, DUMMY_DATA_1);
        dao.update(KEY_1, DUMMY_DATA_2);
        Assert.assertEquals(1, Iterables.size(dao.values()));

        dao.update(KEY_2, DUMMY_DATA_2);
        Assert.assertEquals(2, Iterables.size(dao.values()));
    }

    private static final class DummyDataObject implements DataObject {
        private final String dummyData;

        public DummyDataObject(final String dummyData) {
            this.dummyData = dummyData;
        }

        public String getDummyData() {
            return dummyData;
        }

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return getClass();
        }
    }
}