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

package io.fd.honeycomb.v3po.translate.spi.write;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * CompositeListWriter SPI to customize its behavior.
 *
 * @param <C> Specific DataObject derived type (Identifiable), that is handled by this customizer
 * @param <K> Specific Identifier for handled type (C)
 */
@Beta
public interface ListWriterCustomizer<C extends DataObject & Identifiable<K>, K extends Identifier<C>> extends
    WriterCustomizer<C> {

}