package com.homefellas.service.location;

import static com.homefellas.model.core.TestModelBuilder.buildCountry;
import static com.homefellas.model.core.TestModelBuilder.buildCounty;
import static com.homefellas.model.core.TestModelBuilder.buildLocationAlias;
import static com.homefellas.model.core.TestModelBuilder.buildState;
import static com.homefellas.model.core.TestModelBuilder.buildZip;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import javax.annotation.Resource;

import net.sf.ehcache.Ehcache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.transaction.annotation.Transactional;

import com.homefellas.dao.core.AbstractCoreTestDao;
import com.homefellas.model.location.Country;
import com.homefellas.model.location.County;
import com.homefellas.model.location.LocationAlias;
import com.homefellas.model.location.LocationAlias.LocationAliasEnum;
import com.homefellas.model.location.LocationSearchResult;
import com.homefellas.model.location.State;
import com.homefellas.model.location.Zip;

public class LocationServiceIntTest  extends AbstractCoreTestDao
{
	@Autowired
	private LocationService locationService;
	
	@Autowired
	private ILocationDao locationDao;
	
	public static final String DELIMETER = ", ";
	
	protected Zip denvilleZip;
	protected Zip wilmingtonZip1;
	protected Zip wilmingtonZip2;
	protected Zip parkRidgeZip;
	protected Zip whippanyZip;
	protected Zip newarkNJZip;
	protected Zip newarkDEZip;
	protected Zip nycZip;
	
	protected State delware;
	protected State newJersey;
	protected State newYork;
	
	protected County newCastle;
	protected County burgen;
	protected County morris;
	protected County essex;
	protected County bronx;
	
	protected Country untitedStates;
	
	private LocationAlias cityAlias;
	private LocationAlias countyAlias;
	private LocationAlias stateAlias;
	
	@Resource( name="locationCache")
	protected Ehcache locationCache; 
	
	@Autowired
	@Qualifier(value="locationCache")
	public void setRoleCache(final EhCacheFactoryBean factoryBean) {
	    locationCache = factoryBean.getObject();
	  }
	
	
	@Before
	public void setupIntegrationTest()
	{
		untitedStates = buildCountry(true, "US", "United States");
		createCountry(untitedStates);
		
		delware = buildState(true, "DE", "Delaware", untitedStates, "19805", true);
		createState(delware);
		
		newJersey = buildState(true, "NJ", "New Jersey", untitedStates, "07834", true);
		createState(newJersey);
		
		newYork = buildState(true, "NY", "New York", untitedStates, "10453", true);
		createState(newYork);
		
		newCastle = buildCounty(true, "19805", delware, "New Castle");
		createCounty(newCastle);
		
		essex = buildCounty(true, "07777", newJersey, "Essex");
		createCounty(essex);
		
		burgen = buildCounty(true, "07656", newJersey, "Burgen");
		createCounty(burgen);
		
		bronx = buildCounty(true, "10453", newYork, "Bronx");
		createCounty(bronx);
		
		morris = buildCounty(true, "07834", newJersey, "Morris");
		createCounty(morris);
		
		wilmingtonZip1 = buildZip(true, "Wilmington", "19805", delware, newCastle);
		createZip(wilmingtonZip1);
		
		wilmingtonZip2 = buildZip(true, "Wlimington", "19803", delware, newCastle);
		createZip(wilmingtonZip2);
		
		newarkDEZip = buildZip(true, "Newark", "19711", delware, newCastle);
		createZip(newarkDEZip);
		
		denvilleZip = buildZip(true, "Denville", "07834", newJersey, morris);
		createZip(denvilleZip);
		
		newarkNJZip = buildZip(true, "Newark", "07777", newJersey, essex);
		createZip(newarkNJZip);
		
		nycZip = buildZip(true, "New York City", "10453", newYork, bronx);
		createZip(nycZip);
		
		parkRidgeZip = buildZip(true, "Park Ridge", "07656", newJersey, burgen);
		createZip(parkRidgeZip);
		
		whippanyZip = buildZip(true, "Whippany", "07981", newJersey, morris);
		createZip(whippanyZip);
		
		cityAlias = buildLocationAlias(true, LocationAliasEnum.CITY, "University of Delaware", "19711");
		createLocationAlias(cityAlias);
		
		countyAlias = buildLocationAlias(true, LocationAliasEnum.COUNTY, "Home County", "07834");
		createLocationAlias(countyAlias);
		
		stateAlias = buildLocationAlias(true, LocationAliasEnum.STATE, "Home State", "19805");
		createLocationAlias(stateAlias);
		
	}
	
	@Transactional
	private void createCountry(Country country)
	{
		locationDao.createCountry(country);
	}
	
	@Transactional 
	private void createZip(Zip zip)
	{
		locationDao.createZip(zip);
	}
	
	@Transactional 
	private void createState(State state)
	{
		locationDao.createState(state);
	}
	
	@Transactional
	private void createLocationAlias(LocationAlias alias)
	{
		locationDao.createLocationAlias(alias);
		
	}
	
	@Transactional 
	private void createCounty(County county)
	{ 
		locationDao.createCounty(county);
	}
	
	@Test
	public void testGetZips() {
		
		List<Zip> zips = locationService.getZips();
		Assert.assertTrue(zips.contains(denvilleZip));
		Assert.assertTrue(zips.contains(whippanyZip));
		Assert.assertTrue(zips.contains(parkRidgeZip));
		Assert.assertTrue(zips.contains(wilmingtonZip1));
		Assert.assertTrue(zips.contains(wilmingtonZip2));
	}

	@Test
	public void testGetStates() {
		List<State> states = locationService.getStates();
		assertTrue(states.contains(newJersey));
		assertTrue(states.contains(delware));
	}

	@Test
	public void testGetCounties() {
		List<County> counties = locationService.getCounties();
		assertTrue(counties.contains(burgen));
		assertTrue(counties.contains(morris));
		assertTrue(counties.contains(newCastle));
	}

	@Test
	public void testGetCityAliases() {
		List<LocationAlias> list = locationService.getCityAliases();
		
		Assert.assertTrue(list.contains(cityAlias));
		Assert.assertFalse(list.contains(countyAlias));
		Assert.assertFalse(list.contains(stateAlias));
		
		
	}

	@Test
	public void testGetStateAliases() {
		List<LocationAlias> list = locationService.getStateAliases();
		
		Assert.assertFalse(list.contains(cityAlias));
		Assert.assertFalse(list.contains(countyAlias));
		Assert.assertTrue(list.contains(stateAlias));
	}

//	@Test
//	public void testGetCountyAliases() {
//		List<LocationAlias> list = locationService.getCountyAliases();
//		
//		Assert.assertFalse(list.contains(cityAlias));
//		Assert.assertTrue(list.contains(countyAlias));
//		Assert.assertFalse(list.contains(stateAlias));
//	}
//
	private String buildStateKey(State state)
	{
		return state.getCode() + DELIMETER + state.getCountry().getName();
	}
	
	private String buildCountyKey(County county)
	{
		return (county.getName() + DELIMETER + county.getState().getName()).toLowerCase();
	}
	
	private String buildZipKey(Zip zip)
	{
		return (zip.getCity() + DELIMETER + zip.getState().getName()).toLowerCase();
	}
	
//	@Test
//	public void testLoadLocationCache() {
////		locationService.loadLocationCache();
//	
//		List<CacheContent> cachContents = (List<CacheContent>)locationCache.get(LocationCacheKeyEnum.ALL_LOCATION).getValue();
//		CacheContent cacheContent;
//		
////		CacheContent cacheContent = new CacheContent(buildStateKey(newJersey), newJersey);
////		assertTrue(cachContents.contains(cacheContent));
////		cacheContent = new CacheContent(buildStateKey(delware), delware);
////		assertTrue(cachContents.contains(cacheContent));
////		
//		
//		cacheContent = new CacheContent(buildZipKey(wilmingtonZip1), wilmingtonZip1);
//		assertTrue(cachContents.contains(cacheContent));		
//		cacheContent = new CacheContent(buildZipKey(wilmingtonZip2), wilmingtonZip2);
//		assertTrue(cachContents.contains(cacheContent));
//		cacheContent = new CacheContent(buildZipKey(whippanyZip), whippanyZip);
//		assertTrue(cachContents.contains(cacheContent));
//		cacheContent = new CacheContent(buildZipKey(parkRidgeZip), parkRidgeZip);
//		assertTrue(cachContents.contains(cacheContent));
//		cacheContent = new CacheContent(buildZipKey(denvilleZip), denvilleZip);
//		assertTrue(cachContents.contains(cacheContent));
//		
////		cacheContent = new CacheContent(buildCountyKey(newCastle), newCastle);
////		assertTrue(cachContents.contains(cacheContent));
////		cacheContent = new CacheContent(buildCountyKey(morris), morris);
////		assertTrue(cachContents.contains(cacheContent));
////		cacheContent = new CacheContent(buildCountyKey(burgen), burgen);
////		assertTrue(cachContents.contains(cacheContent));
//		
//		cacheContent = new CacheContent(cityAlias.getAlias().toLowerCase(), cityAlias);
//		assertTrue(cachContents.contains(cacheContent));
//		cacheContent = new CacheContent(stateAlias.getAlias().toLowerCase(), stateAlias);
//		assertTrue(cachContents.contains(cacheContent));
////		cacheContent = new CacheContent(countyAlias.getAlias().toLowerCase(), countyAlias);
////		assertTrue(cachContents.contains(cacheContent));
//		
//	}

//	@Test
//	public void testFindLocation()
//	{
//		String lueceneLocation = locationService.getLocationLueceneIndexHome()+"_unittest";
//		locationService.setLocationLueceneIndexHome(lueceneLocation);
//		
////		locationService.loadLocationCache();
//		
//		List<LocationSearchResult> locations = locationService.findLocation("den");
//		assertTrue(locations.get(0).getDefaultZip().equals(denvilleZip.getDefaultZip()));
//		
////		locations = locationService.findLocation("Denv");
////		assertTrue(locations.contains(denvilleZip));
//		
//		locations = locationService.findLocation("wilming");
//		assertTrue(locations.get(0).getDefaultZip().equals(wilmingtonZip1.getDefaultZip()));
//		
//		locations = locationService.findLocation("sdf dsaf");
//		assertTrue(locations.isEmpty());
//		
//		locations = locationService.findLocation("new");
//		assertTrue(locations.get(0).getDefaultZip().equals(newarkDEZip.getDefaultZip()));
////		assertTrue(locations.get(1).getDefaultZip().equals(newarkNJZip.getDefaultZip()));
////		assertTrue(locations.contains(newCastle));
//		
//		locations = locationService.findLocation("new", 2);
//		assertEquals(2, locations.size());
//		
//		locations = locationService.findLocation("home");
////		assertTrue(locations.contains(countyAlias));
////		assertTrue(locations.contains(stateAlias));
//		
//		locations = locationService.findLocation("university");
//		assertTrue(locations.get(0).getDefaultZip().equals(cityAlias.getDefaultZip()));
//		
////		locations = locationService.findLocation("new");
////		Assert.assertFalse(locations.contains(newYork));
////		assertTrue(locations.get(0).getDefaultZip().equals(nycZip.getDefaultZip()));
//		
//		
//	}
}
