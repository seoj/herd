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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.finra.herd.dao.HerdDao;
import org.finra.herd.dao.config.DaoSpringModuleConfig;
import org.finra.herd.model.api.xml.FileTypeKeys;
import org.finra.herd.service.FileTypeService;

/**
 * The file type service implementation.
 */
@Service
@Transactional(value = DaoSpringModuleConfig.HERD_TRANSACTION_MANAGER_BEAN_NAME)
public class FileTypeServiceImpl implements FileTypeService
{
    @Autowired
    private HerdDao herdDao;

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTypeKeys getFileTypes()
    {
        FileTypeKeys fileTypeKeys = new FileTypeKeys();
        fileTypeKeys.getFileTypeKeys().addAll(herdDao.getFileTypes());
        return fileTypeKeys;
    }
}
