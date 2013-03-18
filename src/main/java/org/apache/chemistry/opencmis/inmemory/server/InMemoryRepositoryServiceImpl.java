/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.inmemory.server;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.FolderTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.ItemTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PolicyTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyBooleanDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyHtmlDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIdDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyUriDefinition;
import org.apache.chemistry.opencmis.commons.definitions.RelationshipTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.SecondaryTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.ContentStreamAllowed;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractTypeDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ItemTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RelationshipTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.SecondaryTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeMutabilityImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.inmemory.NameValidator;
import org.apache.chemistry.opencmis.inmemory.TypeValidator;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.ObjectStore;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.StoreManager;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.TypeManagerCreatable;
import org.apache.chemistry.opencmis.server.support.TypeManager;

public class InMemoryRepositoryServiceImpl extends InMemoryAbstractServiceImpl {

    public InMemoryRepositoryServiceImpl(StoreManager storeManager) {
        super(storeManager);
    }

    public RepositoryInfo getRepositoryInfo(CallContext context, String repositoryId, ExtensionsData extension) {

        validator.getRepositoryInfo(context, repositoryId, extension);

        RepositoryInfo repoInfo = getRepositoryInfoFromStoreManager(repositoryId);

        return repoInfo;
    }

    public List<RepositoryInfo> getRepositoryInfos(CallContext context, ExtensionsData extension) {

        validator.getRepositoryInfos(context, extension);
        List<RepositoryInfo> res = new ArrayList<RepositoryInfo>();
        List<String> repIds = fStoreManager.getAllRepositoryIds();
        for (String repId : repIds) {
            res.add(fStoreManager.getRepositoryInfo(repId));
        }
        return res;
    }

    public TypeDefinitionList getTypeChildren(CallContext context, String repositoryId, String typeId,
            Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

        validator.getTypeChildren(context, repositoryId, typeId, extension);

        boolean inclPropDefs = includePropertyDefinitions == null ? false : includePropertyDefinitions;
        getRepositoryInfoFromStoreManager(repositoryId); // just to check if
        // repository exists

        int skip = skipCount == null ? 0 : skipCount.intValue();
        int max = maxItems == null ? -1 : maxItems.intValue();

        TypeDefinitionListImpl result = new TypeDefinitionListImpl();
        List<TypeDefinitionContainer> children;
        if (typeId == null) {
            // spec says that base types must be returned in this case
            children = fStoreManager.getRootTypes(repositoryId, inclPropDefs);
        } else {
            children = getTypeDescendants(context, repositoryId, typeId, BigInteger.valueOf(1),
                    inclPropDefs, null);
        }
        result.setNumItems(BigInteger.valueOf(children.size()));
        result.setHasMoreItems(children.size() > max - skip);
        List<TypeDefinition> childrenTypes = new ArrayList<TypeDefinition>();
        ListIterator<TypeDefinitionContainer> it = children.listIterator(skip);
        if (max < 0) {
            max = children.size();
        }
        for (int i = skip; i < max + skip && it.hasNext(); i++) {
            childrenTypes.add(it.next().getTypeDefinition());
        }

        result.setList(childrenTypes);
        return result;
    }

    public TypeDefinition getTypeDefinition(CallContext context, String repositoryId, String typeId,
            ExtensionsData extension) {

        validator.getTypeDefinition(context, repositoryId, typeId, extension);

        TypeDefinitionContainer tc = fStoreManager.getTypeById(repositoryId, typeId);
        if (tc != null) {
            return tc.getTypeDefinition();
        } else {
            throw new CmisObjectNotFoundException("unknown type id: " + typeId);
        }
    }

    public List<TypeDefinitionContainer> getTypeDescendants(CallContext context, String repositoryId, String typeId,
            BigInteger depth, Boolean includePropertyDefinitions, ExtensionsData extension) {

        validator.getTypeDescendants(context, repositoryId, typeId, extension);

        boolean inclPropDefs = includePropertyDefinitions == null ? false : includePropertyDefinitions;

        if (depth != null && depth.intValue() == 0) {
            throw new CmisInvalidArgumentException("depth == 0 is illegal in getTypeDescendants");
        }

        List<TypeDefinitionContainer> result = null;
        if (typeId == null) {
            // spec says that depth must be ignored in this case
            Collection<TypeDefinitionContainer> tmp = fStoreManager.getTypeDefinitionList(repositoryId,
                    inclPropDefs);
            result = new ArrayList<TypeDefinitionContainer>(tmp);
        } else {
            TypeDefinitionContainer tc = fStoreManager.getTypeById(repositoryId, typeId, inclPropDefs,
                    depth == null ? -1 : depth.intValue());
            if (tc == null) {
                throw new CmisInvalidArgumentException("unknown type id: " + typeId);
            } else {
                result = tc.getChildren();
            }
        }

        return result;
    }

    public TypeDefinition createType(String repositoryId, TypeDefinition type, ExtensionsData extension) {

        if (null == repositoryId)
            throw new CmisInvalidArgumentException("Repository id may not be null");

        TypeManagerCreatable typeManager = fStoreManager.getTypeManager(repositoryId);
        if (null == typeManager)
            throw new CmisInvalidArgumentException("Unknown repository " + repositoryId);
        
        AbstractTypeDefinition newType = TypeValidator.completeType(type);
        TypeValidator.adjustTypeNamesAndId(newType);
        TypeValidator.checkType(typeManager, newType);
        typeManager.addTypeDefinition(newType);
        return newType;
    }

    public TypeDefinition updateType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
        String typeId = type.getId();
        TypeManagerCreatable typeManager = fStoreManager.getTypeManager(repositoryId);
        if (null == typeManager)
            throw new CmisInvalidArgumentException("Unknown repository " + repositoryId);
        
        TypeDefinitionContainer typeDefC = typeManager.getTypeById(typeId);
        if (null == typeDefC)
            throw new CmisInvalidArgumentException("Cannot update type unknown type id: " + typeId);

        typeManager.updateTypeDefinition(type);
        return type;
    }

    public void deleteType(String repositoryId, String typeId, ExtensionsData extension) {
        
        TypeManagerCreatable typeManager = fStoreManager.getTypeManager(repositoryId);
        if (null == typeManager)
            throw new CmisInvalidArgumentException("Unknown repository " + repositoryId);
        
        TypeDefinitionContainer typeDefC = typeManager.getTypeById(typeId);
        if (null == typeDefC)
            throw new CmisInvalidArgumentException("Cannot delete type unknown type id: " + typeId);

        ObjectStore objectStore = fStoreManager.getObjectStore(repositoryId);
        if (objectStore.isTypeInUse(typeId)) {
            throw new CmisInvalidArgumentException("type definition " + typeId + " cannot be deleted, type is in use.");            
        }
        
        typeManager.deleteTypeDefinition(typeId);        
    }
    
    private RepositoryInfo getRepositoryInfoFromStoreManager(String repositoryId) {
        RepositoryInfo repoInfo = fStoreManager.getRepositoryInfo(repositoryId);
        if (null == repoInfo || !repoInfo.getId().equals(repositoryId)) {
            throw new CmisInvalidArgumentException("Unknown repository: " + repositoryId);
        }
        return repoInfo;
    }

}