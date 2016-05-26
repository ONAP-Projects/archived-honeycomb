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

package io.fd.honeycomb.v3po.translate.v3po.vpp;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.BridgeDomainAddDel;
import org.openvpp.jvpp.dto.BridgeDomainAddDelReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainCustomizer
        extends FutureJVppCustomizer
        implements ListWriterCustomizer<BridgeDomain, BridgeDomainKey> {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainCustomizer.class);

    private static final byte ADD_OR_UPDATE_BD = (byte) 1;
    private final NamingContext bdContext;

    public BridgeDomainCustomizer(@Nonnull final FutureJVpp futureJvpp, @Nonnull final NamingContext bdContext) {
        super(futureJvpp);
        this.bdContext = Preconditions.checkNotNull(bdContext, "bdContext should not be null");
    }

    @Nonnull
    @Override
    public List<BridgeDomain> extract(@Nonnull final InstanceIdentifier<BridgeDomain> currentId,
                                      @Nonnull final DataObject parentData) {
        return ((BridgeDomains) parentData).getBridgeDomain();
    }

    private BridgeDomainAddDelReply addOrUpdateBridgeDomain(final int bdId, @Nonnull final BridgeDomain bd)
            throws VppApiInvocationException {
        final BridgeDomainAddDel request = new BridgeDomainAddDel();
        request.bdId = bdId;
        request.flood = booleanToByte(bd.isFlood());
        request.forward = booleanToByte(bd.isForward());
        request.learn = booleanToByte(bd.isLearn());
        request.uuFlood = booleanToByte(bd.isUnknownUnicastFlood());
        request.arpTerm = booleanToByte(bd.isArpTermination());
        request.isAdd = ADD_OR_UPDATE_BD;

        final BridgeDomainAddDelReply reply =
                TranslateUtils.getReply(getFutureJVpp().bridgeDomainAddDel(request).toCompletableFuture());
        if (reply.retval < 0) {
            LOG.warn("Bridge domain {} (id={}) add/update failed", bd.getName(), bdId);
            throw new VppApiInvocationException("bridgeDomainAddDel", reply.context, reply.retval);
        } else {
            LOG.debug("Bridge domain {} (id={}) add/update successful", bd.getName(), bdId);
        }

        return reply;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                       @Nonnull final BridgeDomain dataBefore,
                                       @Nonnull final WriteContext ctx) throws WriteFailedException.CreateFailedException {
        LOG.debug("writeCurrentAttributes: id={}, current={}, ctx={}", id, dataBefore, ctx);
        final String bdName = dataBefore.getName();

        try {
            // FIXME we need the bd index to be returned by VPP or we should have a counter field (maybe in context similar to artificial name)
            // Here we assign the next available ID from bdContext's perspective
            int index = 1;
            while(bdContext.containsName(index, ctx.getMappingContext())) {
                index++;
            }
            addOrUpdateBridgeDomain(index, dataBefore);
            bdContext.addName(index, bdName, ctx.getMappingContext());
        } catch (VppApiInvocationException e) {
            LOG.warn("Failed to create bridge domain", e);
            throw new WriteFailedException.CreateFailedException(id, dataBefore, e);
        }
    }

    private byte booleanToByte(@Nullable final Boolean aBoolean) {
        return aBoolean != null && aBoolean
                ? (byte) 1
                : (byte) 0;
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                        @Nonnull final BridgeDomain dataBefore,
                                        @Nonnull final WriteContext ctx) throws WriteFailedException.DeleteFailedException {
        LOG.debug("deleteCurrentAttributes: id={}, dataBefore={}, ctx={}", id, dataBefore, ctx);

        final String bdName = id.firstKeyOf(BridgeDomain.class).getName();
        int bdId = bdContext.getIndex(bdName, ctx.getMappingContext());
        final BridgeDomainAddDel request = new BridgeDomainAddDel();
        request.bdId = bdId;

        final BridgeDomainAddDelReply reply =
                TranslateUtils.getReply(getFutureJVpp().bridgeDomainAddDel(request).toCompletableFuture());
        if (reply.retval < 0) {
            LOG.warn("Bridge domain {} (id={}) delete failed", bdName, bdId);
            throw new WriteFailedException.DeleteFailedException(id,
                    new VppApiInvocationException("bridgeDomainAddDel", reply.context, reply.retval));
        } else {
            LOG.debug("Bridge domain {} (id={}) deleted successfully", bdName, bdId);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                        @Nonnull final BridgeDomain dataBefore, @Nonnull final BridgeDomain dataAfter,
                                        @Nonnull final WriteContext ctx) throws WriteFailedException.UpdateFailedException {
        LOG.debug("updateCurrentAttributes: id={}, dataBefore={}, dataAfter={}, ctx={}", id, dataBefore, dataAfter,
                ctx);

        final String bdName = checkNotNull(dataAfter.getName());
        checkArgument(bdName.equals(dataBefore.getName()),
                "BridgeDomain name changed. It should be deleted and then created.");

        try {
            addOrUpdateBridgeDomain(bdContext.getIndex(bdName, ctx.getMappingContext()), dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Failed to create bridge domain", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

}
