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
package org.finra.herd.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import org.finra.herd.dao.HerdDao;
import org.finra.herd.dao.config.DaoSpringModuleConfig;
import org.finra.herd.model.AlreadyExistsException;
import org.finra.herd.model.api.xml.Attribute;
import org.finra.herd.model.api.xml.BusinessObjectDefinition;
import org.finra.herd.model.api.xml.BusinessObjectDefinitionCreateRequest;
import org.finra.herd.model.api.xml.BusinessObjectDefinitionKey;
import org.finra.herd.model.api.xml.BusinessObjectDefinitionKeys;
import org.finra.herd.model.api.xml.BusinessObjectDefinitionUpdateRequest;
import org.finra.herd.model.jpa.BusinessObjectDefinitionAttributeEntity;
import org.finra.herd.model.jpa.BusinessObjectDefinitionEntity;
import org.finra.herd.model.jpa.DataProviderEntity;
import org.finra.herd.model.jpa.NamespaceEntity;
import org.finra.herd.service.BusinessObjectDefinitionService;
import org.finra.herd.service.helper.HerdDaoHelper;
import org.finra.herd.service.helper.HerdHelper;

/**
 * The business object definition service implementation.
 */
@Service
@Transactional(value = DaoSpringModuleConfig.HERD_TRANSACTION_MANAGER_BEAN_NAME)
public class BusinessObjectDefinitionServiceImpl implements BusinessObjectDefinitionService
{
    @Autowired
    private HerdHelper herdHelper;

    @Autowired
    private HerdDao herdDao;

    @Autowired
    private HerdDaoHelper herdDaoHelper;

    /**
     * Creates a new business object definition.
     *
     * @param request the business object definition create request.
     *
     * @return the created business object definition.
     */
    @Override
    public BusinessObjectDefinition createBusinessObjectDefinition(BusinessObjectDefinitionCreateRequest request)
    {
        // Perform the validation.
        validateBusinessObjectDefinitionCreateRequest(request);

        // Get the namespace and ensure it exists.
        NamespaceEntity namespaceEntity = herdDaoHelper.getNamespaceEntity(request.getNamespace());

        // Get the data provider and ensure it exists.
        DataProviderEntity dataProviderEntity = herdDaoHelper.getDataProviderEntity(request.getDataProviderName());

        // Get business object definition key.
        BusinessObjectDefinitionKey businessObjectDefinitionKey =
            new BusinessObjectDefinitionKey(request.getNamespace(), request.getBusinessObjectDefinitionName());

        // Ensure a business object definition with the specified key doesn't already exist.
        BusinessObjectDefinitionEntity businessObjectDefinitionEntity = herdDao.getBusinessObjectDefinitionByKey(businessObjectDefinitionKey);
        if (businessObjectDefinitionEntity != null)
        {
            throw new AlreadyExistsException(String
                .format("Unable to create business object definition with name \"%s\" because it already exists for namespace \"%s\".",
                    businessObjectDefinitionKey.getBusinessObjectDefinitionName(), businessObjectDefinitionKey.getNamespace()));
        }

        // Create a business object definition entity from the request information.
        businessObjectDefinitionEntity = createBusinessObjectDefinitionEntity(request, namespaceEntity, dataProviderEntity);

        // Create and return the business object definition object from the persisted entity.
        return createBusinessObjectDefinitionFromEntity(businessObjectDefinitionEntity);
    }

    /**
     * Updates a business object definition.
     *
     * @param businessObjectDefinitionKey the business object definition key
     * @param request the business object definition update request
     *
     * @return the updated business object definition
     */
    @Override
    public BusinessObjectDefinition updateBusinessObjectDefinition(BusinessObjectDefinitionKey businessObjectDefinitionKey,
        BusinessObjectDefinitionUpdateRequest request)
    {
        // Perform validation and trim.
        herdHelper.validateBusinessObjectDefinitionKey(businessObjectDefinitionKey);
        validateBusinessObjectDefinitionUpdateRequest(request);

        // Retrieve and ensure that a business object definition already exists with the specified key.
        BusinessObjectDefinitionEntity businessObjectDefinitionEntity = herdDaoHelper.getBusinessObjectDefinitionEntity(businessObjectDefinitionKey);

        // Update and persist the entity.
        updateBusinessObjectDefinitionEntity(businessObjectDefinitionEntity, request);

        // Create and return the business object definition object from the persisted entity.
        return createBusinessObjectDefinitionFromEntity(businessObjectDefinitionEntity);
    }

    /**
     * Gets a business object definition for the specified key. This method starts a new transaction.
     *
     * @param businessObjectDefinitionKey the business object definition key
     *
     * @return the business object definition.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BusinessObjectDefinition getBusinessObjectDefinition(BusinessObjectDefinitionKey businessObjectDefinitionKey)
    {
        return getBusinessObjectDefinitionImpl(businessObjectDefinitionKey);
    }

    /**
     * Gets a business object definition for the specified key.
     *
     * @param businessObjectDefinitionKey the business object definition key
     *
     * @return the business object definition.
     */
    protected BusinessObjectDefinition getBusinessObjectDefinitionImpl(BusinessObjectDefinitionKey businessObjectDefinitionKey)
    {
        // Perform validation and trim.
        herdHelper.validateBusinessObjectDefinitionKey(businessObjectDefinitionKey);

        // Retrieve and ensure that a business object definition already exists with the specified key.
        BusinessObjectDefinitionEntity businessObjectDefinitionEntity = herdDaoHelper.getBusinessObjectDefinitionEntity(businessObjectDefinitionKey);

        // Create and return the business object definition object from the persisted entity.
        return createBusinessObjectDefinitionFromEntity(businessObjectDefinitionEntity);
    }

    /**
     * Deletes a business object definition for the specified name.
     *
     * @param businessObjectDefinitionKey the business object definition key
     *
     * @return the business object definition that was deleted.
     */
    @Override
    public BusinessObjectDefinition deleteBusinessObjectDefinition(BusinessObjectDefinitionKey businessObjectDefinitionKey)
    {
        // Perform validation and trim.
        herdHelper.validateBusinessObjectDefinitionKey(businessObjectDefinitionKey);

        // Retrieve and ensure that a business object definition already exists with the specified key.
        BusinessObjectDefinitionEntity businessObjectDefinitionEntity = herdDaoHelper.getBusinessObjectDefinitionEntity(businessObjectDefinitionKey);

        // Delete the business object definition.
        herdDao.delete(businessObjectDefinitionEntity);

        // Create and return the business object definition object from the deleted entity.
        return createBusinessObjectDefinitionFromEntity(businessObjectDefinitionEntity);
    }

    /**
     * Gets the list of all business object definitions defined in the system.
     *
     * @return the business object definition keys.
     */
    @Override
    public BusinessObjectDefinitionKeys getBusinessObjectDefinitions()
    {
        BusinessObjectDefinitionKeys businessObjectDefinitionKeys = new BusinessObjectDefinitionKeys();
        businessObjectDefinitionKeys.getBusinessObjectDefinitionKeys().addAll(herdDao.getBusinessObjectDefinitions());
        return businessObjectDefinitionKeys;
    }

    /**
     * Gets a list of all business object definitions defined in the system for a specified namespace.
     *
     * @param namespaceCode the namespace code
     *
     * @return the business object definition keys
     */
    @Override
    public BusinessObjectDefinitionKeys getBusinessObjectDefinitions(String namespaceCode)
    {
        // Validate and trim the namespace code.
        Assert.hasText(namespaceCode, "A namespace must be specified.");

        // Retrieve and return the list of business object definitions
        BusinessObjectDefinitionKeys businessObjectDefinitionKeys = new BusinessObjectDefinitionKeys();
        businessObjectDefinitionKeys.getBusinessObjectDefinitionKeys().addAll(herdDao.getBusinessObjectDefinitions(namespaceCode.trim()));
        return businessObjectDefinitionKeys;
    }

    /**
     * Validates the business object definition create request. This method also trims request parameters.
     *
     * @param request the request.
     *
     * @throws IllegalArgumentException if any validation errors were found.
     */
    private void validateBusinessObjectDefinitionCreateRequest(BusinessObjectDefinitionCreateRequest request)
    {
        // Validate.
        Assert.hasText(request.getNamespace(), "A namespace must be specified.");
        Assert.hasText(request.getBusinessObjectDefinitionName(), "A business object definition name must be specified.");
        Assert.hasText(request.getDataProviderName(), "A data provider name must be specified.");

        // Remove leading and trailing spaces.
        request.setNamespace(request.getNamespace().trim());
        request.setBusinessObjectDefinitionName(request.getBusinessObjectDefinitionName().trim());
        request.setDataProviderName(request.getDataProviderName().trim());

        // Validate attributes.
        herdHelper.validateAttributes(request.getAttributes());
    }

    /**
     * Validates the business object definition update request. This method also trims request parameters.
     *
     * @param request the request.
     *
     * @throws IllegalArgumentException if any validation errors were found.
     */
    private void validateBusinessObjectDefinitionUpdateRequest(BusinessObjectDefinitionUpdateRequest request)
    {
        // Validate attributes.
        herdHelper.validateAttributes(request.getAttributes());
    }

    /**
     * Creates and persists a new business object definition entity from the request information.
     *
     * @param request the request.
     * @param namespaceEntity the namespace.
     * @param dataProviderEntity the data provider.
     *
     * @return the newly created business object definition entity.
     */
    private BusinessObjectDefinitionEntity createBusinessObjectDefinitionEntity(BusinessObjectDefinitionCreateRequest request, NamespaceEntity namespaceEntity,
        DataProviderEntity dataProviderEntity)
    {
        // Create a new entity.
        BusinessObjectDefinitionEntity businessObjectDefinitionEntity = new BusinessObjectDefinitionEntity();
        businessObjectDefinitionEntity.setNamespace(namespaceEntity);
        businessObjectDefinitionEntity.setName(request.getBusinessObjectDefinitionName());
        businessObjectDefinitionEntity.setDescription(request.getDescription());
        businessObjectDefinitionEntity.setDataProvider(dataProviderEntity);

        // Create the attributes if they are specified.
        if (!CollectionUtils.isEmpty(request.getAttributes()))
        {
            List<BusinessObjectDefinitionAttributeEntity> attributeEntities = new ArrayList<>();
            businessObjectDefinitionEntity.setAttributes(attributeEntities);
            for (Attribute attribute : request.getAttributes())
            {
                BusinessObjectDefinitionAttributeEntity attributeEntity = new BusinessObjectDefinitionAttributeEntity();
                attributeEntities.add(attributeEntity);
                attributeEntity.setBusinessObjectDefinition(businessObjectDefinitionEntity);
                attributeEntity.setName(attribute.getName());
                attributeEntity.setValue(attribute.getValue());
            }
        }

        // Persist and return the new entity.
        return herdDao.saveAndRefresh(businessObjectDefinitionEntity);
    }

    /**
     * Update and persist the business object definition per specified update request.
     *
     * @param businessObjectDefinitionEntity the business object definition entity
     * @param request the business object definition update request
     */
    private void updateBusinessObjectDefinitionEntity(BusinessObjectDefinitionEntity businessObjectDefinitionEntity,
        BusinessObjectDefinitionUpdateRequest request)
    {
        // Update the entity with the new description value.
        businessObjectDefinitionEntity.setDescription(request.getDescription());

        // Update the attributes.
        // Load all existing attribute entities in a map with a "lowercase" attribute name as the key for case insensitivity.
        Map<String, BusinessObjectDefinitionAttributeEntity> existingAttributeEntities = new HashMap<>();
        for (BusinessObjectDefinitionAttributeEntity attributeEntity : businessObjectDefinitionEntity.getAttributes())
        {
            String mapKey = attributeEntity.getName().toLowerCase();
            if (existingAttributeEntities.containsKey(mapKey))
            {
                throw new IllegalStateException(String.format(
                    "Found duplicate attribute with name \"%s\" for business object definition {namespace: \"%s\", businessObjectDefinitionName: \"%s\"}.",
                    mapKey, businessObjectDefinitionEntity.getNamespace().getCode(), businessObjectDefinitionEntity.getName()));
            }
            existingAttributeEntities.put(mapKey, attributeEntity);
        }

        // Process the list of attributes to determine that business object definition attribute entities should be created, updated, or deleted.
        List<BusinessObjectDefinitionAttributeEntity> createdAttributeEntities = new ArrayList<>();
        List<BusinessObjectDefinitionAttributeEntity> retainedAttributeEntities = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getAttributes()))
        {
            for (Attribute attribute : request.getAttributes())
            {
                // Use a "lowercase" attribute name for case insensitivity.
                String lowercaseAttributeName = attribute.getName().toLowerCase();
                if (existingAttributeEntities.containsKey(lowercaseAttributeName))
                {
                    // Check if the attribute value needs to be updated.
                    BusinessObjectDefinitionAttributeEntity attributeEntity = existingAttributeEntities.get(lowercaseAttributeName);
                    if (!StringUtils.equals(attribute.getValue(), attributeEntity.getValue()))
                    {
                        // Update the business object attribute entity.
                        attributeEntity.setValue(attribute.getValue());
                    }

                    // Add this entity to the list of business object definition attribute entities to be retained.
                    retainedAttributeEntities.add(attributeEntity);
                }
                else
                {
                    // Create a new business object attribute entity.
                    BusinessObjectDefinitionAttributeEntity attributeEntity = new BusinessObjectDefinitionAttributeEntity();
                    businessObjectDefinitionEntity.getAttributes().add(attributeEntity);
                    attributeEntity.setBusinessObjectDefinition(businessObjectDefinitionEntity);
                    attributeEntity.setName(attribute.getName());
                    attributeEntity.setValue(attribute.getValue());

                    // Add this entity to the list of the newly created business object definition attribute entities.
                    retainedAttributeEntities.add(attributeEntity);
                }
            }
        }

        // Remove any of the currently existing attribute entities that did not get onto the retained entities list.
        businessObjectDefinitionEntity.getAttributes().retainAll(retainedAttributeEntities);

        // Add all of the newly created business object definition attribute entities.
        businessObjectDefinitionEntity.getAttributes().addAll(createdAttributeEntities);

        // Persist the entity.
        herdDao.saveAndRefresh(businessObjectDefinitionEntity);
    }

    /**
     * Creates the business object definition from the persisted entity.
     *
     * @param businessObjectDefinitionEntity the newly persisted business object definition entity.
     *
     * @return the business object definition.
     */
    private BusinessObjectDefinition createBusinessObjectDefinitionFromEntity(BusinessObjectDefinitionEntity businessObjectDefinitionEntity)
    {
        // Create the business object definition information.
        BusinessObjectDefinition businessObjectDefinition = new BusinessObjectDefinition();
        businessObjectDefinition.setId(businessObjectDefinitionEntity.getId());
        businessObjectDefinition.setNamespace(businessObjectDefinitionEntity.getNamespace().getCode());
        businessObjectDefinition.setBusinessObjectDefinitionName(businessObjectDefinitionEntity.getName());
        businessObjectDefinition.setDescription(businessObjectDefinitionEntity.getDescription());
        businessObjectDefinition.setDataProviderName(businessObjectDefinitionEntity.getDataProvider().getName());

        // Add in the attributes.
        List<Attribute> attributes = new ArrayList<>();
        businessObjectDefinition.setAttributes(attributes);
        for (BusinessObjectDefinitionAttributeEntity attributeEntity : businessObjectDefinitionEntity.getAttributes())
        {
            Attribute attribute = new Attribute();
            attributes.add(attribute);
            attribute.setName(attributeEntity.getName());
            attribute.setValue(attributeEntity.getValue());
        }

        return businessObjectDefinition;
    }
}
