/*
 * Copyright 2013 Proofpoint Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.kairosdb.core.datastore;

import org.junit.Test;
import org.kairosdb.core.DataPoint;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CachedSearchResultTest
{
	@Test
	public void test_createCachedSearchResult() throws IOException
	{

		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile";
		CachedSearchResult csResult =
				CachedSearchResult.createCachedSearchResult("metric1", tempFile, new ReentrantReadWriteLock());

		long now = System.currentTimeMillis();

		Map<String, String> tags = new HashMap();
		tags.put("host", "A");
		tags.put("client", "foo");
		csResult.startDataPointSet(tags);

		csResult.addDataPoint(now, 42);
		csResult.addDataPoint(now+1, 42.1);
		csResult.addDataPoint(now+2, 43);
		csResult.addDataPoint(now+3, 43.1);


		tags = new HashMap();
		tags.put("host", "B");
		tags.put("client", "foo");
		csResult.startDataPointSet(tags);

		csResult.addDataPoint(now, 1);
		csResult.addDataPoint(now+1, 1.1);
		csResult.addDataPoint(now+2, 2);
		csResult.addDataPoint(now+3, 2.1);

		tags = new HashMap();
		tags.put("host", "A");
		tags.put("client", "bar");
		csResult.startDataPointSet(tags);

		csResult.addDataPoint(now, 3);
		csResult.addDataPoint(now+1, 3.1);
		csResult.addDataPoint(now+2, 4);
		csResult.addDataPoint(now+3, 4.1);

		csResult.endDataPoints();

		List<DataPointRow> rows = csResult.getRows();

		assertEquals(3, rows.size());

		assertValues(rows.get(0), 42L, 42.1, 43L, 43.1);

		assertValues(rows.get(1), 1L, 1.1, 2L, 2.1);

		assertValues(rows.get(2), 3L, 3.1, 4L, 4.1);
	}

	@Test
	public void test_AddLongsBeyondBufferSize() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile";
		CachedSearchResult csResult = CachedSearchResult.createCachedSearchResult("metric2", tempFile, new ReentrantReadWriteLock());

		int numberOfDataPoints = CachedSearchResult.WRITE_BUFFER_SIZE * 2;
		csResult.startDataPointSet(Collections.<String, String>emptyMap());

		long now = System.currentTimeMillis();
		for (int i = 0; i < numberOfDataPoints; i++)
		{
			csResult.addDataPoint(now, 42);
		}

		csResult.endDataPoints();

		List<DataPointRow> rows = csResult.getRows();
		DataPointRow taggedDataPoints = rows.iterator().next();

		int count = 0;
		while(taggedDataPoints.hasNext())
		{
			DataPoint dataPoint = taggedDataPoints.next();
			assertThat(dataPoint.getLongValue(), equalTo(42L));
			count++;
		}

		assertThat(count, equalTo(numberOfDataPoints));

	}

	@Test
	public void test_AddDoublesBeyondBufferSize() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile";
		CachedSearchResult csResult = CachedSearchResult.createCachedSearchResult("metric3", tempFile, new ReentrantReadWriteLock());

		int numberOfDataPoints = CachedSearchResult.WRITE_BUFFER_SIZE * 2;
		csResult.startDataPointSet(Collections.<String, String>emptyMap());

		long now = System.currentTimeMillis();
		for (int i = 0; i < numberOfDataPoints; i++)
		{
			csResult.addDataPoint(now, 42.2);
		}

		csResult.endDataPoints();

		List<DataPointRow> rows = csResult.getRows();
		DataPointRow taggedDataPoints = rows.iterator().next();

		int count = 0;
		while(taggedDataPoints.hasNext())
		{
			DataPoint dataPoint = taggedDataPoints.next();
			assertThat(dataPoint.getDoubleValue(), equalTo(42.2));
			count++;
		}

		assertThat(count, equalTo(numberOfDataPoints));

	}

	private void assertValues(DataPointRow dataPoints, Number... numbers)
	{
		int count = 0;
		while (dataPoints.hasNext())
		{
			DataPoint dp = dataPoints.next();

			if (dp.isInteger())
			{
				Long value = (Long)numbers[count];
				assertEquals(value.longValue(), dp.getLongValue());
			}
			else
			{
				Double value = (Double)numbers[count];
				assertEquals(value.doubleValue(), dp.getDoubleValue());
			}

			count ++;
		}
	}


}
