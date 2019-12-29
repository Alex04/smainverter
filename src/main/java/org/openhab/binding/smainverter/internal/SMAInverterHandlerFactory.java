/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.smainverter.internal;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SMAInverterHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Alexander Mack - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.smainverter", service = ThingHandlerFactory.class)
public class SMAInverterHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(SMAInverterHandlerFactory.class);

    private static final Collection<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(SMAInverterBindingConstants.THING_TYPE_SMAINVERTER);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        logger.debug("SMA Inverter Factory called for supported types");
        logger.debug(SUPPORTED_THING_TYPES_UIDS.toString());
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        logger.debug("SMA Inverter Factory called for creating handler: " + thing);

        if (thingTypeUID.getId().equals(SMAInverterBindingConstants.THING_TYPE_SMAINVERTER.getId())) {
            logger.debug("Creating SMA Handler for thing: " + thing);
            SMAInverterHandler handler = new SMAInverterHandler(thing);
            return handler;
        }
        logger.debug("SMA Inverter Factory no handler found");

        return null;
    }
}
