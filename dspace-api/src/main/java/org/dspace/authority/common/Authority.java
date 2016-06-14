/**
 * This package contains classes for general authority module.
 *
 * @since 2015-12-03
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version $Revision$
 */
package org.dspace.authority.common;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;

/**
 * This class represents authority for authority person module.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version $Revision$
 * @since 2015-12-03
 */
public final class Authority {

    /** Name of table for this entity. **/
    private static final String TABLE_NAME = "AUTHORITY";

    /** Log4j logger instance. */
    private static final Logger LOGGER = Logger.getLogger(Authority.class);

    /** The table row corresponding to this item. */
    private final TableRow tableRow;

    /** Our context. */
    private final Context ourContext;

    /**
     * Construct an authority with the given table row.
     *
     * @param context the context this object exists in
     * @param row the corresponding row in the table
     *
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    Authority(final Context context, final TableRow row) throws SQLException {
        ourContext = context;

        if (row.getTable() == null) {
            row.setTable(TABLE_NAME);
        }
        tableRow = row;

        context.cache(this, row.getIntColumn("authority_id"));
    }

    /**
     * Create new authority in general authority module.
     *
     * @param context DSpace context object
     * @return the newly created authority
     * @throws SQLException Is thrown when was problem with witting to database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public static Authority create(final Context context) throws SQLException, AuthorizeException {
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("Only administrators has permission to create authority.");
        }

        final TableRow row = DatabaseManager.create(context, TABLE_NAME);
        final Authority authority = new Authority(context, row);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Authority with id(" + authority.getId() + ") was created.");
            LOGGER.info(LogManager.getHeader(context, "create_authority", "authority_name=" + authority.getId()));
        }
        return authority;
    }

    /**
     * Get an authority from the database.
     *
     * @param context DSpace context object
     * @param authorityId The id of authority person.
     * @return Authority person, or null if the uid is invalid.
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public static Authority find(final Context context, final int authorityId) throws SQLException {
        Authority authority = (Authority) context.fromCache(Authority.class, authorityId);

        // Not in cache
        if (authority == null) {
            final TableRow row = DatabaseManager.find(context, TABLE_NAME, authorityId);

            if (row == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(LogManager.getHeader(context,
                            "find_authority",
                            "not_found,authority_id=" + authorityId));
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(LogManager.getHeader(context,
                            "find_authority",
                            "authority_id=" + authorityId));
                }
                authority = new Authority(context, row);
            }
        }

        return authority;
    }

    /**
     * Get all the authorities from database.
     *
     * @param context DSpace context object
     * @return an iterator over the authorities in the database.
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public static AuthorityIterator findAll(final Context context) throws SQLException {
        final String query = "SELECT * FROM " + TABLE_NAME;
        final TableRowIterator rows = DatabaseManager.queryTable(context, TABLE_NAME, query);
        return new AuthorityIterator(context, rows);
    }

    /**
     * Update authority person in database.
     *
     * @throws SQLException Is thrown when was problem with writing to database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public void update() throws SQLException, AuthorizeException {
        if (!AuthorizeManager.isAdmin(ourContext)) {
            throw new AuthorizeException("Only administrators has permission to update authority.");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LogManager.getHeader(ourContext, "update_authority", "authority_id=" + getId()));
        }
        DatabaseManager.update(ourContext, tableRow);
    }

    /**
     * Delete authority person from database.
     *
     * @throws SQLException Is thrown when was problem with writing to database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public void delete() throws SQLException, AuthorizeException {
        if (!AuthorizeManager.isAdmin(ourContext)) {
            throw new AuthorizeException("Only administrators has permission to delete authority.");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LogManager.getHeader(ourContext, "delete_authority", "authority_id=" + getId()));
        }
        ourContext.removeCached(this, getId());
        DatabaseManager.delete(ourContext, tableRow);
    }

    /**
     * Returns id of authority person in database.
     *
     * @return Id of authority person.
     */
    public int getId() {
        return tableRow.getIntColumn("authority_id");
    }

    /**
     * Returns name of authority.
     *
     * @return String filled with name.
     */
    public String getName() {
        return tableRow.getStringColumn("authority_key");
    }

    /**
     * Set name of authority.
     *
     * @param name Name of authority.
     */
    public void setName(final String name) {
        tableRow.setColumn("authority_key", name);
    }

    /**
     * Returns key of person in authority.
     *
     * @return key
     */
    public String getKey() {
        return tableRow.getStringColumn("authority_value");
    }

    /**
     * Set key of authority.
     *
     * @param key Key of person in authority.
     */
    public void setKey(final String key) {
        tableRow.setColumn("authority_value", key);
    }

    /**
     * Returns id of authority person, to which belongs this key.
     *
     * @return id
     */
    public int getAuthorityPersonId() {
        return tableRow.getIntColumn("authority_person_id");
    }

    /**
     * Set id of authority person to this key.
     *
     * @param authorityPersonId Id of authority person to which will this belongs.
     */
    public void setAuthorityPersonId(final long authorityPersonId) {
        tableRow.setColumn("authority_person_id", authorityPersonId);
    }
}
