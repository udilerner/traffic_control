/*
 * Copyright 2016 Comcast Cable Communications Management, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comcast.cdn.traffic_control.traffic_router.core.loc;

import com.comcast.cdn.traffic_control.traffic_router.core.cache.Cache;
import com.comcast.cdn.traffic_control.traffic_router.core.cache.CacheLocation;
import com.comcast.cdn.traffic_control.traffic_router.core.cache.CacheRegister;
import com.comcast.cdn.traffic_control.traffic_router.core.ds.DeliveryService;
import com.comcast.cdn.traffic_control.traffic_router.core.router.TrafficRouter;
import com.comcast.cdn.traffic_control.traffic_router.geolocation.Geolocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TrafficRouter.class)
public class CoverageZoneTest {

	private TrafficRouter trafficRouter;

	@Before
	public void before() throws Exception {
		DeliveryService deliveryService = mock(DeliveryService.class);
		when(deliveryService.getId()).thenReturn("delivery-service-1");

		Cache.DeliveryServiceReference deliveryServiceReference = new Cache.DeliveryServiceReference("delivery-service-1", "some.example.com");

		List<Cache.DeliveryServiceReference> deliveryServices = new ArrayList<Cache.DeliveryServiceReference>();
		deliveryServices.add(deliveryServiceReference);

		Geolocation testLocation = new Geolocation(40.0, -101);
		Geolocation eastLocation = new Geolocation(40.0, -100);
		Geolocation westLocation = new Geolocation(40.0, -105);

		Cache eastCache1 = new Cache("east-cache-1", "hashid", 1);
		eastCache1.setIsAvailable(true);
		CacheLocation eastCacheGroup = new CacheLocation("east-cache-group", eastLocation);
		eastCacheGroup.addCache(eastCache1);

		Cache westCache1 = new Cache("west-cache-1", "hashid", 1);
		westCache1.setIsAvailable(true);
		westCache1.setDeliveryServices(deliveryServices);

		CacheLocation westCacheGroup = new CacheLocation("west-cache-group", westLocation);
		westCacheGroup.addCache(westCache1);

		List<CacheLocation> cacheGroups = new ArrayList<CacheLocation>();
		cacheGroups.add(eastCacheGroup);
		cacheGroups.add(westCacheGroup);

		NetworkNode eastNetworkNode = new NetworkNode("12.23.34.0/24", "east-cache-group", testLocation);

		CacheRegister cacheRegister = mock(CacheRegister.class);

		when(cacheRegister.getCacheLocationById("east-cache-group")).thenReturn(eastCacheGroup);

		when(cacheRegister.filterAvailableLocations("delivery-service-1")).thenReturn(cacheGroups);
		when(cacheRegister.getDeliveryService("delivery-service-1")).thenReturn(deliveryService);

		trafficRouter = PowerMockito.mock(TrafficRouter.class);
		Whitebox.setInternalState(trafficRouter, "cacheRegister", cacheRegister);
		when(trafficRouter.getCoverageZoneCacheLocation("12.23.34.45", "delivery-service-1")).thenCallRealMethod();
		when(trafficRouter.getCacheRegister()).thenReturn(cacheRegister);
		when(trafficRouter.orderCacheLocations(cacheGroups,testLocation)).thenCallRealMethod();
		when(trafficRouter.getSupportingCaches(anyListOf(Cache.class), eq(deliveryService))).thenCallRealMethod();
		PowerMockito.when(trafficRouter, "getNetworkNode", "12.23.34.45").thenReturn(eastNetworkNode);
		PowerMockito.when(trafficRouter, "getClosestCacheLocation", cacheGroups, testLocation, deliveryService).thenCallRealMethod();
	}

	@Test
	public void trafficRouterReturnsNearestCacheGroupForDeliveryService() throws Exception {
		CacheLocation cacheLocation = trafficRouter.getCoverageZoneCacheLocation("12.23.34.45", "delivery-service-1");
		assertThat(cacheLocation.getId(), equalTo("west-cache-group"));
	}
}
