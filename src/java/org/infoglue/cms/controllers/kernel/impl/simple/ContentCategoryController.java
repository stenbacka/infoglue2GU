/* ===============================================================================
 *
 * Part of the InfoGlue Content Management Platform (www.infoglue.org)
 *
 * ===============================================================================
 *
 *  Copyright (C)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2, as published by the
 * Free Software Foundation. See the file LICENSE.html for more information.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, including the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc. / 59 Temple
 * Place, Suite 330 / Boston, MA 02111-1307 / USA.
 *
 * ===============================================================================
 *
 * $Id: ContentCategoryController.java,v 1.3 2005/03/14 21:08:12 jed Exp $
 */
package org.infoglue.cms.controllers.kernel.impl.simple;

import java.util.*;

import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.Category;
import org.infoglue.cms.entities.management.impl.simple.CategoryImpl;
import org.infoglue.cms.entities.content.ContentCategoryVO;
import org.infoglue.cms.entities.content.ContentCategory;
import org.infoglue.cms.entities.content.impl.simple.ContentCategoryImpl;
import org.infoglue.cms.exception.SystemException;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.PersistenceException;

/**
 * The ContentCategoryController manages all actions related to persistence
 * and querying for ContentCategory relationships.
 *
 * TODO: When we convert have Hibernate manage all of these relationships, it will pull it
 * TODO: all back with one query and be a helluva lot faster than this basic implementation
 *
 * @author Frank Febbraro (frank@phase2technology.com)
 */
public class ContentCategoryController extends BaseController
{
	private static final ContentCategoryController instance = new ContentCategoryController();

	private static final String findByContentVersion = new StringBuffer("SELECT c ")
			.append("FROM org.infoglue.cms.entities.content.impl.simple.ContentCategoryImpl c ")
			.append("WHERE c.contentVersionId = $1").toString();

	private static final String findByContentVersionAttribute = new StringBuffer("SELECT c ")
			.append("FROM org.infoglue.cms.entities.content.impl.simple.ContentCategoryImpl c ")
			.append("WHERE c.attributeName = $1 ")
			.append("AND c.contentVersionId = $2")
			.append("ORDER BY c.category.name").toString();

	private static final String findByCategory = new StringBuffer("SELECT c ")
			.append("FROM org.infoglue.cms.entities.content.impl.simple.ContentCategoryImpl c ")
			.append("WHERE c.category.categoryId = $1 ").toString();

	public static ContentCategoryController getController()
	{
		return instance;
	}

	private ContentCategoryController() {}

	/**
	 * Find a ContentCategory by it's identifier.
	 * @param	id The id of the Category to find
	 * @return	The CategoryVO identified by the provided id
	 * @throws	SystemException If an error happens
	 */
	public ContentCategoryVO findById(Integer id) throws SystemException
	{
		return (ContentCategoryVO)getVOWithId(ContentCategoryImpl.class, id);
	}

	/**
	 * Find a List of ContentCategories for the specific attribute and Content Version.
	 * @param	attribute The attribute name of the ContentCategory to find
	 * @param	versionId The Content Version id of the ContentCategory to find
	 * @return	A list of ContentCategoryVO that have the provided content version and attribute
	 * @throws	SystemException If an error happens
	 */
	public List findByContentVersionAttribute(String attribute, Integer versionId) throws SystemException
	{
		List params = new ArrayList();
		params.add(attribute);
		params.add(versionId);
		return executeQuery(findByContentVersionAttribute, params);
	}

	/**
	 * Find a List of ContentCategories for a Content Version.
	 * @param	versionId The Content Version id of the ContentCategory to find
	 * @return	A list of ContentCategoryVO that have the provided content version and attribute
	 * @throws	SystemException If an error happens
	 */
	public List findByContentVersion(Integer versionId) throws SystemException
	{
		List params = new ArrayList();
		params.add(versionId);
		return executeQuery(findByContentVersion, params);
	}

	/**
	 * Find a List of ContentCategories for the specific attribute and Content Version.
	 * @param	categoryId The Category id of the ContentCategory to find
	 * @return	A list of ContentCategoryVO that have the provided category id
	 * @throws	SystemException If an error happens
	 */
	public List findByCategory(Integer categoryId) throws SystemException
	{
		List params = new ArrayList();
		params.add(categoryId);
		return executeQuery(findByCategory, params);
	}

	/**
	 * Saves a ContentCategoryVO whether it is new or not.
	 * @param	c The ContentCategoryVO to save
	 * @return	The saved ContentCategoryVO
	 * @throws	SystemException If an error happens
	 */
	public ContentCategoryVO save(ContentCategoryVO c) throws SystemException
	{
		return c.isUnsaved() ? create(c) : (ContentCategoryVO)updateEntity(ContentCategoryImpl.class, c);
	}

	/**
	 * Creates a ContentCategory from a ContentCategoryVO
	 */
	private ContentCategoryVO create(ContentCategoryVO c) throws SystemException
	{
		Database db = beginTransaction();

		try
		{
			ContentCategory contentCategory = createWithDatabase(c, db);
			commitTransaction(db);
			return contentCategory.getValueObject();
		}
		catch(Exception e)
		{
			rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}
	}

	public ContentCategory createWithDatabase(ContentCategoryVO c, Database db) throws SystemException, PersistenceException
	{
		// Need this crappy hack to forge the relationship (castor completely sucks like this)
		// TODO: When hibernate comes, just save the VOs and if it has a child VO with an id set
		// TODO: it is used to make the relationship...ask me for clarification -frank
		Category category = (Category)getObjectWithId(CategoryImpl.class, c.getCategory().getId(), db);

		ContentCategory contentCategory = new ContentCategoryImpl();
		contentCategory.setValueObject(c);
		contentCategory.setCategory((CategoryImpl)category);
		db.create(contentCategory);
		return contentCategory;
	}

	/**
	 * Deletes a ContentCategory
	 * @param	id The id of the ContentCategory to delete
	 * @throws	SystemException If an error happens
	 */
	public void delete(Integer id) throws SystemException
	{
		deleteEntity(ContentCategoryImpl.class, id);
	}

	/**
	 * Deletes all ContentCategories for a particular ContentVersion
	 * @param	versionId The id of the ContentCategory to delete
	 * @throws	SystemException If an error happens
	 */
	public void deleteByContentVersion(Integer versionId) throws SystemException
	{
		delete(findByContentVersion(versionId));
	}

	/**
	 * Deletes all ContentCategories for a particular ContentVersion using the provided Database
	 * @param	versionId The id of the ContentCategory to delete
	 * @param	db The Database instance to use
	 * @throws	SystemException If an error happens
	 */
	public void deleteByContentVersion(Integer versionId, Database db) throws SystemException
	{
		delete(findByContentVersion(versionId), db);
	}

	/**
	 * Deletes all ContentCategories for a particular Category
	 * @param	categoryId The id of the ContentCategory to delete
	 * @throws	SystemException If an error happens
	 */
	public void deleteByCategory(Integer categoryId) throws SystemException
	{
		delete(findByCategory(categoryId));
	}

	/**
	 * Deletes all ContentCategories for a particular Category using the provided Database
	 * @param	categoryId The id of the Category to delete
	 * @param	db The Database instance to use
	 * @throws	SystemException If an error happens
	 */
	public void deleteByCategory(Integer categoryId, Database db) throws SystemException
	{
		delete(findByCategory(categoryId), db);
	}

	private static void delete(Collection contentCategories) throws SystemException
	{
		Database db = beginTransaction();

		try
		{
			delete(contentCategories, db);
			commitTransaction(db);
		}
		catch (Exception e)
		{
			rollbackTransaction(db);
			throw new SystemException(e);
		}
	}

	/**
	 * Deletes a collection of content categories using the given database
	 * @param contentCategories a collection of ContentCategoryVOs to delete
	 * @param db the database to be used for the delete
	 * @throws SystemException if a database error occurs
	 */
	private static void delete(Collection contentCategories, Database db) throws SystemException
	{
		for (Iterator i = contentCategories.iterator(); i.hasNext();)
			deleteEntity(ContentCategoryImpl.class, ((ContentCategoryVO)i.next()).getId(), db);
	}

	/**
	 * Implemented for BaseController
	 */
	public BaseEntityVO getNewVO()
	{
		return new ContentCategoryVO();
	}
}
