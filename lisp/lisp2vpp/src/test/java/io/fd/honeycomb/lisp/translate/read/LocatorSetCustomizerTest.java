/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.LocatorSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetails;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetailsReplyDump;


public class LocatorSetCustomizerTest
        extends ListReaderCustomizerTest<LocatorSet, LocatorSetKey, LocatorSetBuilder> {

    private InstanceIdentifier<LocatorSet> emptyId;
    private InstanceIdentifier<LocatorSet> validId;

    public LocatorSetCustomizerTest() {
        super(LocatorSet.class, LocatorSetsBuilder.class);
    }

    @Before
    public void init() {
        emptyId = InstanceIdentifier.create(LocatorSet.class);
        validId = InstanceIdentifier.create(LocatorSets.class).child(LocatorSet.class, new LocatorSetKey("loc-set"));

        defineDumpData();
        defineMapping(mappingContext, "loc-set", 1, "locator-set-context");
    }

    private void defineDumpData() {
        LispLocatorSetDetailsReplyDump dump = new LispLocatorSetDetailsReplyDump();
        LispLocatorSetDetails detail = new LispLocatorSetDetails();
        detail.context = 4;
        detail.lsName = "loc-set".getBytes(StandardCharsets.UTF_8);
        detail.lsIndex = 1;

        dump.lispLocatorSetDetails = ImmutableList.of(detail);

        when(api.lispLocatorSetDump(any())).thenReturn(future(dump));
    }


    @Test
    public void readCurrentAttributes() throws Exception {
        LocatorSetBuilder builder = new LocatorSetBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        assertNotNull(builder);
        assertEquals("loc-set", builder.getName());
        assertEquals("loc-set", builder.getKey().getName());
    }

    @Test
    public void getAllIds() throws Exception {
        final List<LocatorSetKey> keys = getCustomizer().getAllIds(emptyId, ctx);

        assertEquals(1, keys.size());
        assertEquals("loc-set", keys.get(0).getName());
    }

    @Override
    protected ReaderCustomizer<LocatorSet, LocatorSetBuilder> initCustomizer() {
        return new LocatorSetCustomizer(api, new NamingContext("loc", "locator-set-context"));
    }
}