package com.homefellas.metrics;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Resource;

import junit.framework.Assert;

import org.junit.Test;

import com.homefellas.dao.core.AbstractCoreTestDao;

public class MetricServiceIntTest extends AbstractCoreTestDao {

	@Resource(name="metricService")
	private MetricService metricService; 
	
	@Test
	public void testInsertMetric()
	{
		Collection<Metric> metrics = new ArrayList<Metric>();
		Metric metric1 = new Metric(getClass().getName(), 23423);
		metrics.add(metric1);

		Metric metric2 = new Metric(getClass().getName(), 5421);
		metrics.add(metric2);
		
		Metric metric3 = new Metric(getClass().getName(), 12387);
		metrics.add(metric3);
		
		Metric metric4 = new Metric(getClass().getName(), 978);
		metrics.add(metric4);
		
		Metric metric5 = new Metric(getClass().getName(), 8876);
		metrics.add(metric5);
		
		metricService.saveAllMetrics(metrics);
	}
	
//	@CollectTimeMetrics
//	public void annoationedMethod()
//	{
//		try
//		{
//			Thread.sleep(1000);
//		}
//		catch (InterruptedException e)
//		{
//			e.printStackTrace();
//		}
//		
//	}
	
//	@CollectTimeMetrics(MetricCollectionPriority.DEBUG)
//	public void annoitationDebugMethod()
//	{
//		try
//		{
//			Thread.sleep(1000);
//		}
//		catch (InterruptedException e)
//		{
//			e.printStackTrace();
//		}
//	}
	
	@Test
	public void testAlwaysPointcut()
	{
		Assert.assertEquals(0, metricCache.getSize());
//		annoationedMethod();
//		Assert.assertEquals(1, metricCache.getSize());
//		annoitationDebugMethod();
//		Assert.assertEquals(1, metricCache.getSize());
	}
	
	
//	@Test
//	public void testSaveClientMetric()
//	{
//		ClientMetric clientMetric = TestModelBuilder.buildClientMetric(true);
//		
//		metricService.saveClientMetric(clientMetric);
//		
//		Assert.assertEquals(1,countRowsInTable("metric_client"));
//	}
//	
//	@Test
//	public void testSaveBulkClientMetrics()
//	{
//		ClientMetric clientMetric1 = TestModelBuilder.buildClientMetric(true);
//		ClientMetric clientMetric2 = TestModelBuilder.buildClientMetric(true);
//		ClientMetric clientMetric3 = TestModelBuilder.buildClientMetric(true);
//		ClientMetric clientMetric4 = TestModelBuilder.buildClientMetric(true);
//		ClientMetric clientMetric5 = TestModelBuilder.buildClientMetric(true);
//		List<ClientMetric> clientMetrics = new ArrayList<ClientMetric>();
//		clientMetrics.add(clientMetric1);
//		clientMetrics.add(clientMetric2);
//		clientMetrics.add(clientMetric3);
//		clientMetrics.add(clientMetric4);
//		clientMetrics.add(clientMetric5);
//		
//		metricService.saveBulkClientMetrics(clientMetrics);
//		
//		Assert.assertEquals(5,countRowsInTable("metric_client"));
//	}

}
