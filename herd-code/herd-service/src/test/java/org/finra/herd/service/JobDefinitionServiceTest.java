/*
* Copyright 2015 herd contributors
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
package org.finra.herd.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import org.finra.herd.model.AlreadyExistsException;
import org.finra.herd.model.ObjectNotFoundException;
import org.finra.herd.model.jpa.JobDefinitionEntity;
import org.finra.herd.model.api.xml.JobDefinition;
import org.finra.herd.model.api.xml.JobDefinitionCreateRequest;
import org.finra.herd.model.api.xml.JobDefinitionUpdateRequest;
import org.finra.herd.model.api.xml.Parameter;
import org.finra.herd.model.api.xml.S3PropertiesLocation;

/**
 * This class tests functionality within the JobDefinitionService.
 */
public class JobDefinitionServiceTest extends AbstractServiceTest
{
    private static final String INVALID_NAME = "Herd_Invalid_Name_" + UUID.randomUUID().toString().substring(0, 3);

    /**
     * This method tests the happy path scenario by providing all the parameters except attributes
     */
    @Test
    public void testCreateJobDefinition() throws Exception
    {
        createJobDefinition(null);
    }

    /**
     * This method tests the scenario in which the jobName is invalid IllegalArgumentException is expected to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateJobDefinitionNoJobName() throws Exception
    {
        String invalidJobName = TEST_ACTIVITI_NAMESPACE_CD;

        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);
        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();

        // Enter the invalid job name.
        request.setJobName(invalidJobName);

        // The following method is expected to throw an IllegalArgumentException.
        jobDefinitionService.createJobDefinition(request);
    }

    /**
     * This method tests the scenario in which the namespace is invalid ObjectNotFoundException is expected to be thrown
     */
    @Test(expected = ObjectNotFoundException.class)
    public void testCreateJobDefinitionInvalidNamespace() throws Exception
    {
        // Create the job request without registering the namespace entity - ${TEST_ACTIVITI_NAMESPACE_CD}
        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();

        // Following method must throw ObjectNotFoundException, as the namespace entity ${TEST_ACTIVITI_NAMESPACE_CD} does not exist.
        jobDefinitionService.createJobDefinition(request);
    }

    /**
     * This method tests the scenario in which the user tries to register the same job flow twice AlreadyExistsException is expected to be thrown.
     */
    @Test(expected = AlreadyExistsException.class)
    public void testCreateJobDefinitionAlreadyExists() throws Exception
    {
        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Create and persist a valid job definition.
        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();
        jobDefinitionService.createJobDefinition(request);

        // Create the same request again which is an invalid operation, as the workflow exists.
        // Following must throw AlreadyExistsException
        jobDefinitionService.createJobDefinition(request);
    }

    /**
     * This method tests the scenario in which the user passes an ill-formatted xml file XMLException is expected to be thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateJobDefinitionInvalidActivitiXml() throws Exception
    {
        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Create and persist a valid job definition.
        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();

        // Get the XML file for the test workflow.
        InputStream xmlStream = resourceLoader.getResource(ACTIVITI_XML_HERD_WORKFLOW_WITH_CLASSPATH).getInputStream();

        // Just remove "startEvent" text from the XML file which makes the following line in the XML file as INVALID
        // <startEvent id="startevent1" name="Start"></startEvent>
        request.setActivitiJobXml(IOUtils.toString(xmlStream).replaceAll("startEvent", ""));

        // Try creating the job definition and this must throw XMLException.
        jobDefinitionService.createJobDefinition(request);
    }

    /**
     * This method tests the happy path scenario in which all the parameters including the attributes are given.
     */
    @Test
    public void testCreateJobDefinitionWithParams() throws Exception
    {
        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Create and persist a valid job definition.
        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();

        // Add parameters
        List<Parameter> parameterEntities = new ArrayList<>();
        Parameter parameterEntity = new Parameter();
        parameterEntity.setName(ATTRIBUTE_NAME_1_MIXED_CASE);
        parameterEntity.setValue(ATTRIBUTE_VALUE_1);
        parameterEntities.add(parameterEntity);
        request.setParameters(parameterEntities);

        // Try creating the job definition.
        jobDefinitionService.createJobDefinition(request);
    }

    /**
     * This method tests the happy path scenario in which no parameters are given.
     */
    @Test
    public void testCreateJobDefinitionWithNoParams() throws Exception
    {
        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Create and persist a valid job definition.
        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();

        request.setParameters(null);

        // Try creating the job definition.
        jobDefinitionService.createJobDefinition(request);
    }

    /**
     * This method tests the scenario in which an invalid Java class name is given. This must throw IllegalArgumentException from the Activiti layer.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateJobDefinitionInvalidActivitiElement() throws Exception
    {
        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Create and persist a valid job definition.
        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();

        // Read the Activiti XML file so that an error can be injected.
        InputStream xmlStream = resourceLoader.getResource(ACTIVITI_XML_HERD_WORKFLOW_WITH_CLASSPATH).getInputStream();

        // Inject an error by having an invalid Activiti element name in the XML file.
        // Note that XML file structure is correct as per the XML schema. However, there is an invalid Activiti element in the XML file.
        // The line below will be affected in the XML file as per this error injection.
        // <serviceTask id="servicetask1" name="Test Service Step" activiti:class="org.activiti.engine.impl.test.NoOpServiceTask">
        request.setActivitiJobXml(IOUtils.toString(xmlStream).replaceAll("serviceTask", "invalidActivitiTask"));

        // Try creating the job definition and the Activiti layer mush throw an exception.
        jobDefinitionService.createJobDefinition(request);
    }

    /**
     * Asserts that when a job definition is created using {@link S3PropertiesLocation}, the S3 location information is persisted.
     * 
     * @throws Exception
     */
    @Test
    public void testCreateJobDefinitionWithS3PropertiesLocationPersistsEntity() throws Exception
    {
        S3PropertiesLocation s3PropertiesLocation = getS3PropertiesLocation();

        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();
        request.setS3PropertiesLocation(s3PropertiesLocation);

        JobDefinition jobDefinition = jobDefinitionService.createJobDefinition(request);

        Assert.assertEquals("jobDefinition s3PropertiesLocation", request.getS3PropertiesLocation(), jobDefinition.getS3PropertiesLocation());

        JobDefinitionEntity jobDefinitionEntity = herdDao.findById(JobDefinitionEntity.class, jobDefinition.getId());

        Assert.assertNotNull("jobDefinitionEntity is null", jobDefinitionEntity);
        Assert.assertEquals("jobDefinitionEntity s3BucketName", s3PropertiesLocation.getBucketName(), jobDefinitionEntity.getS3BucketName());
        Assert.assertEquals("jobDefinitionEntity s3ObjectKey", s3PropertiesLocation.getKey(), jobDefinitionEntity.getS3ObjectKey());
    }

    /**
     * Asserts that if {@link S3PropertiesLocation} is given, bucket name is required.
     * 
     * @throws Exception
     */
    @Test
    public void testCreateJobDefinitionWithS3PropertiesLocationValidateBucketNameRequired() throws Exception
    {
        S3PropertiesLocation s3PropertiesLocation = getS3PropertiesLocation();
        s3PropertiesLocation.setBucketName(null);
        testCreateJobDefinitionWithS3PropertiesLocationValidate(s3PropertiesLocation, IllegalArgumentException.class,
            "S3 properties location bucket name must be specified.");
    }

    /**
     * Asserts that if {@link S3PropertiesLocation} is given, key is required.
     * 
     * @throws Exception
     */
    @Test
    public void testCreateJobDefinitionWithS3PropertiesLocationValidateObjectKeyRequired() throws Exception
    {
        S3PropertiesLocation s3PropertiesLocation = getS3PropertiesLocation();
        s3PropertiesLocation.setKey(null);
        testCreateJobDefinitionWithS3PropertiesLocationValidate(s3PropertiesLocation, IllegalArgumentException.class,
            "S3 properties location object key must be specified.");
    }

    @Test
    public void testGetJobDefinition() throws Exception
    {
        // Create a new job definition.
        JobDefinition jobDefinition = createJobDefinition();

        // Retrieve the job definition.
        jobDefinition = jobDefinitionService.getJobDefinition(jobDefinition.getNamespace(), jobDefinition.getJobName());

        // Validate that the retrieved job definition matches what we created.
        validateJobDefinition(jobDefinition);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void testGetJobDefinitionNoExist() throws Exception
    {
        // Retrieve a job definition that doesn't exist.
        jobDefinitionService.getJobDefinition(INVALID_NAME, INVALID_NAME);
    }

    @Test
    public void testUpdateJobDefinition() throws Exception
    {
        // Create job definition create request using hard coded test values.
        JobDefinitionCreateRequest createRequest = createJobDefinitionCreateRequest();

        // Set 2 distinct parameters.
        List<Parameter> parameters = new ArrayList<>();
        createRequest.setParameters(parameters);

        Parameter parameter = new Parameter(ATTRIBUTE_NAME_1_MIXED_CASE, ATTRIBUTE_VALUE_1);
        parameters.add(parameter);
        parameter = new Parameter(ATTRIBUTE_NAME_2_MIXED_CASE, ATTRIBUTE_VALUE_2);
        parameters.add(parameter);

        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Create the job definition in the database.
        jobDefinitionService.createJobDefinition(createRequest);

        // Create an update request with a varied set of data that is based on the same data used in the create request.
        JobDefinitionUpdateRequest updateRequest = createUpdateRequest(createRequest);

        // Update the job definition in the database.
        JobDefinition jobDefinition = jobDefinitionService.updateJobDefinition(createRequest.getNamespace(), createRequest.getJobName(), updateRequest);

        // Validate the updated job definition.
        assertNotNull(jobDefinition);
        assertEquals(createRequest.getNamespace(), jobDefinition.getNamespace());
        assertEquals(createRequest.getJobName(), jobDefinition.getJobName());
        assertEquals(updateRequest.getDescription(), jobDefinition.getDescription());
        assertEquals(updateRequest.getParameters(), jobDefinition.getParameters());
        assertTrue(jobDefinition.getActivitiJobXml().contains("Unit Test 2"));
    }

    @Test(expected = ObjectNotFoundException.class)
    public void testUpdateJobDefinitionNamespaceNoExist() throws Exception
    {
        // Create an update request.
        JobDefinitionUpdateRequest updateRequest = createUpdateRequest(createJobDefinitionCreateRequest());

        // Update the process Id to match an invalid namespace and invalid job name to pass validation.
        updateRequest.setActivitiJobXml(
            updateRequest.getActivitiJobXml().replace(TEST_ACTIVITI_NAMESPACE_CD + "." + TEST_ACTIVITI_JOB_NAME, INVALID_NAME + "." + INVALID_NAME));

        // Try to update a job definition that has a namespace that doesn't exist.
        jobDefinitionService.updateJobDefinition(INVALID_NAME, INVALID_NAME, updateRequest);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void testUpdateJobDefinitionJobNameNoExist() throws Exception
    {
        // Create an update request.
        JobDefinitionUpdateRequest updateRequest = createUpdateRequest(createJobDefinitionCreateRequest());

        // Update the process Id to match a valid namespace and invalid job name to pass validation.
        updateRequest.setActivitiJobXml(updateRequest.getActivitiJobXml()
            .replace(TEST_ACTIVITI_NAMESPACE_CD + "." + TEST_ACTIVITI_JOB_NAME, TEST_ACTIVITI_NAMESPACE_CD + "." + INVALID_NAME));

        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Try to update a job definition that has a namespace that exists, but a job name that doesn't exist.
        jobDefinitionService.updateJobDefinition(TEST_ACTIVITI_NAMESPACE_CD, INVALID_NAME, updateRequest);
    }

    @Test
    public void testUpdateJobDefinitionWithS3Properties() throws Exception
    {
        S3PropertiesLocation s3PropertiesLocation = getS3PropertiesLocation();

        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Create job definition create request using hard coded test values.
        JobDefinitionCreateRequest createRequest = createJobDefinitionCreateRequest();

        // Create the job definition in the database.
        jobDefinitionService.createJobDefinition(createRequest);

        // Create an update request with a varied set of data that is based on the same data used in the create request.
        JobDefinitionUpdateRequest updateRequest = createUpdateRequest(createRequest);
        updateRequest.setS3PropertiesLocation(s3PropertiesLocation);

        // Update the job definition in the database.
        JobDefinition updatedJobDefinition = jobDefinitionService.updateJobDefinition(createRequest.getNamespace(), createRequest.getJobName(), updateRequest);
        JobDefinitionEntity updatedJobDefinitionEntity = herdDao.findById(JobDefinitionEntity.class, updatedJobDefinition.getId());

        Assert.assertEquals("updatedJobDefinition s3PropertiesLocation", s3PropertiesLocation, updatedJobDefinition.getS3PropertiesLocation());
        Assert.assertEquals("updatedJobDefinitionEntity s3BucketName", s3PropertiesLocation.getBucketName(), updatedJobDefinitionEntity.getS3BucketName());
        Assert.assertEquals("updatedJobDefinitionEntity s3ObjectKey", s3PropertiesLocation.getKey(), updatedJobDefinitionEntity.getS3ObjectKey());
    }

    @Test
    public void testUpdateJobDefinitionWithS3PropertiesClear() throws Exception
    {
        S3PropertiesLocation s3PropertiesLocation = getS3PropertiesLocation();

        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Create job definition create request using hard coded test values.
        JobDefinitionCreateRequest createRequest = createJobDefinitionCreateRequest();
        createRequest.setS3PropertiesLocation(s3PropertiesLocation);

        // Create the job definition in the database.
        jobDefinitionService.createJobDefinition(createRequest);

        // Create an update request with a varied set of data that is based on the same data used in the create request.
        JobDefinitionUpdateRequest updateRequest = createUpdateRequest(createRequest);

        // Update the job definition in the database.
        JobDefinition updatedJobDefinition = jobDefinitionService.updateJobDefinition(createRequest.getNamespace(), createRequest.getJobName(), updateRequest);
        JobDefinitionEntity updatedJobDefinitionEntity = herdDao.findById(JobDefinitionEntity.class, updatedJobDefinition.getId());

        Assert.assertNull("updatedJobDefinition s3PropertiesLocation", updatedJobDefinition.getS3PropertiesLocation());
        Assert.assertNull("updatedJobDefinitionEntity s3BucketName", updatedJobDefinitionEntity.getS3BucketName());
        Assert.assertNull("updatedJobDefinitionEntity s3ObjectKey", updatedJobDefinitionEntity.getS3ObjectKey());
    }

    /**
     * Create an update request with a varied set of data that is based on the same data used in the create request.
     *
     * @param createRequest the create request.
     *
     * @return the update request.
     */
    private JobDefinitionUpdateRequest createUpdateRequest(JobDefinitionCreateRequest createRequest)
    {
        // Create an update request that modifies all data from the create request.
        JobDefinitionUpdateRequest updateRequest = new JobDefinitionUpdateRequest();
        updateRequest.setDescription(createRequest.getDescription() + "2");
        updateRequest.setActivitiJobXml(createRequest.getActivitiJobXml().replace("Unit Test", "Unit Test 2"));

        List<Parameter> parameters = new ArrayList<>();
        updateRequest.setParameters(parameters);

        // Delete the first parameter, update the second parameter, and add a new third parameter.
        Parameter parameter = new Parameter(ATTRIBUTE_NAME_2_MIXED_CASE, ATTRIBUTE_VALUE_2 + "2");
        parameters.add(parameter);
        parameter = new Parameter(ATTRIBUTE_NAME_3_MIXED_CASE, ATTRIBUTE_VALUE_3);
        parameters.add(parameter);
        return updateRequest;
    }

    /**
     * Creates a new standard job definition.
     *
     * @return the created job definition.
     * @throws Exception if any problems were encountered.
     */
    private JobDefinition createJobDefinition() throws Exception
    {
        // Create the namespace entity.
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        // Create job definition create request using hard coded test values.
        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();

        // Create the job definition in the database.
        JobDefinition jobDefinition = jobDefinitionService.createJobDefinition(request);

        // Validate the created job definition.
        validateJobDefinition(jobDefinition);

        return jobDefinition;
    }

    /**
     * Validates a standard job definition.
     *
     * @param jobDefinition the job definition to validate.
     */
    private void validateJobDefinition(JobDefinition jobDefinition)
    {
        // Validate the basic job definition fields.
        assertNotNull(jobDefinition);
        assertEquals(TEST_ACTIVITI_NAMESPACE_CD, jobDefinition.getNamespace());
        assertEquals(TEST_ACTIVITI_JOB_NAME, jobDefinition.getJobName());
        assertEquals(JOB_DESCRIPTION, jobDefinition.getDescription());
        assertTrue(jobDefinition.getParameters().size() == 1);

        Parameter parameter = jobDefinition.getParameters().get(0);
        assertEquals(ATTRIBUTE_NAME_1_MIXED_CASE, parameter.getName());
        assertEquals(ATTRIBUTE_VALUE_1, parameter.getValue());
    }

    private S3PropertiesLocation getS3PropertiesLocation()
    {
        S3PropertiesLocation s3PropertiesLocation = new S3PropertiesLocation();
        s3PropertiesLocation.setBucketName("testBucketName");
        s3PropertiesLocation.setKey("testKey");
        return s3PropertiesLocation;
    }

    /**
     * Asserts that when a job definition is created with the given {@link S3PropertiesLocation}, then an exception of the given type and message is thrown.
     * 
     * @param s3PropertiesLocation {@link S3PropertiesLocation}
     * @param exceptionType expected exception type
     * @param exceptionMessage expected exception message
     */
    private void testCreateJobDefinitionWithS3PropertiesLocationValidate(S3PropertiesLocation s3PropertiesLocation, Class<? extends Exception> exceptionType,
        String exceptionMessage)
    {
        createNamespaceEntity(TEST_ACTIVITI_NAMESPACE_CD);

        JobDefinitionCreateRequest request = createJobDefinitionCreateRequest();
        request.setS3PropertiesLocation(s3PropertiesLocation);

        try
        {
            jobDefinitionService.createJobDefinition(request);
            Assert.fail("expected " + exceptionType + ", but no exception was thrown");
        }
        catch (Exception e)
        {
            Assert.assertEquals("thrown exception type", exceptionType, e.getClass());
            Assert.assertEquals("thrown exception message", exceptionMessage, e.getMessage());
        }
    }
}
