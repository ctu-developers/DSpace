/**
 * This package contains classes for general authority module.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version $Revision$
 * @since 2015-12-03
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.UUID;

/**
 * This class represents person in authority module.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version $Revision$
 * @since 2015-12-03
 */
public class AuthorityPerson {

    /** Name of table for this entity. **/
    private static final String TABLE_NAME = "AUTHORITY_PERSON";

    /** Log4j logger instance. */
    private static final Logger LOGGER = Logger.getLogger(AuthorityPerson.class);

    /** The table row corresponding to this item. */
    private final TableRow tableRow;

    /** List of authority keys for this person. */
    private List<Authority> authorities;

    /** Our context. */
    private final Context ourContext;

    /** Format for date saving/loading. */
    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd";

    /**
     * Construct an authority person with the given table row.
     *
     * @param context the context this object exists in
     * @param row the corresponding row in the table
     *
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    AuthorityPerson(final Context context, final TableRow row) throws SQLException {
        ourContext = context;

        if (row.getTable() == null) {
            row.setTable(TABLE_NAME);
        }
        tableRow = row;

        context.cache(this, row.getIntColumn("authority_person_id"));
    }

    /**
     * Create new authority person in general authority module.
     *
     * @param context DSpace context object
     * @return the newly created authority person
     * @throws SQLException Is thrown when was problem with witting to database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public static AuthorityPerson create(final Context context) throws SQLException, AuthorizeException {
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("Only administrators has permission to create authority person.");
        }

        final TableRow row = DatabaseManager.create(context, TABLE_NAME);

        final Date date = Calendar.getInstance().getTime();
        final DateFormat formatter = new SimpleDateFormat(DATE_FORMAT_STRING, Locale.ENGLISH);
        row.setColumn("created", formatter.format(date));

        final AuthorityPerson person = new AuthorityPerson(context, row);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Authority person with id(" + person.getId() + ") was created.");
            LOGGER.info(LogManager.getHeader(context, "create_authority_person", "authority_person_id=" + person.getId()));
        }
        return person;
    }

    /**
     * Get an authority person from the database.
     *
     * @param context DSpace context object
     * @param authorityPersonId The id of authority person.
     * @return Authority person, or null if the uid is invalid.
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public static AuthorityPerson find(final Context context, final int authorityPersonId) throws SQLException {
        AuthorityPerson authorityPerson = (AuthorityPerson) context.fromCache(AuthorityPerson.class, authorityPersonId);

        // Not in cache
        if (authorityPerson == null) {
            final TableRow row = DatabaseManager.find(context, TABLE_NAME, authorityPersonId);

            if (row == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(LogManager.getHeader(context,
                            "find_authority_person",
                            "not_found,authority_person_id=" + authorityPersonId));
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(LogManager.getHeader(context,
                            "find_authority_person",
                            "authority_person_id=" + authorityPersonId));
                }
                authorityPerson = new AuthorityPerson(context, row);
            }
        }

        return authorityPerson;
    }

    /**
     * Get an authority person from the database by his uid.
     *
     * @param context DSpace context object
     * @param uid The uid of authority person.
     * @return Authority person, or null if the uid is invalid.
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public static AuthorityPerson findByUID(final Context context, final String uid) throws SQLException {
        final TableRow row = DatabaseManager.findByUnique(context, TABLE_NAME, "authority_person_uid", uid);
        AuthorityPerson authorityPerson = null;

        if (row == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(LogManager.getHeader(context, "find_authority_person_by_uid", "not_found,authority_person_uid=" + uid));
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(LogManager.getHeader(context, "find_authority_person_by_uid", "authority_person_uid=" + uid));
            }
            authorityPerson = new AuthorityPerson(context, row);
        }

        return authorityPerson;
    }

    /**
     * Get an authority person from the database by key of authority.
     *
     * @param context DSpace context object
     * @param authorityName Name of authority.
     * @param authorityKey Key of authority, which belongs name.
     * @return Authority person, or null if the uid is invalid.
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public static AuthorityPerson findByKey(final Context context, final String authorityName, final String authorityKey) throws SQLException {
        TableRow row = DatabaseManager.querySingleTable(context, "AUTHORITY",
                "SELECT * FROM AUTHORITY WHERE authority_key=? AND authority_value=?", authorityName, authorityKey);

        if (row == null) {
            return null;
        }

        row = DatabaseManager.find(context, TABLE_NAME, row.getIntColumn("authority_person_id"));
        AuthorityPerson authorityPerson = null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(LogManager.getHeader(context, "find_authority_person_by_key", "name/key=" + authorityName + "/" + authorityKey));
        }

        if (row != null) {
            authorityPerson = new AuthorityPerson(context, row);
        }

        return authorityPerson;
    }

    /**
     * Get authority persons from the database by name.
     *
     * @param context DSpace context object
     * @param firstName First name of person.
     * @param lastName Last name of person.
     * @return Authority persons, or empty list.
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public static AuthorityPersonIterator findByName(final Context context, final String firstName, final String lastName) throws SQLException {
        final String query = "SELECT * FROM " + TABLE_NAME + " WHERE firstname=? and lastname=?";
        final TableRowIterator rows = DatabaseManager.queryTable(context, TABLE_NAME, query, firstName, lastName);
        return new AuthorityPersonIterator(context, rows);
    }

    /**
     * Get authority persons from the database like that name.
     *
     * @param context DSpace context object
     * @param firstName Firstname of person
     * @param lastName Lastname of person.
     * @return Authority persons, or empty list.
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public static AuthorityPersonIterator findLikeName(final Context context, String firstName, String lastName) throws SQLException {
        final String query = "SELECT * FROM " + TABLE_NAME + " WHERE LOWER(LASTNAME) like '%" + lastName.toLowerCase() + "%' AND LOWER(FIRSTNAME) like '%" + firstName.toLowerCase() + "%'";
        final TableRowIterator rows = DatabaseManager.queryTable(context, TABLE_NAME, query);
        return new AuthorityPersonIterator(context, rows);
    }

    /**
     * Get authority persons from the database like that name.
     *
     * @param context DSpace context object
     * @param name Name of the person.
     * @return Authority persons, or empty list.
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public static AuthorityPersonIterator findLikeName(final Context context, String name) throws SQLException {
        final String query = "SELECT * FROM " + TABLE_NAME + " WHERE LOWER(CONCAT(LASTNAME, CONCAT(' ', FIRSTNAME))) like '%" + name.toLowerCase() + "%' OR LOWER(CONCAT(FIRSTNAME, CONCAT(' ', LASTNAME))) like '%" + name.toLowerCase() + "'";
        final TableRowIterator rows = DatabaseManager.queryTable(context, TABLE_NAME, query);
        return new AuthorityPersonIterator(context, rows);
    }

    /**
     * Get all the authority persons in database.
     *
     * @param context DSpace context object
     * @return an iterator over the authority persons in the database.
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public static AuthorityPersonIterator findAll(final Context context) throws SQLException {
        final String query = "SELECT * FROM " + TABLE_NAME;
        final TableRowIterator rows = DatabaseManager.queryTable(context, TABLE_NAME, query);
        return new AuthorityPersonIterator(context, rows);
    }

    /**
     * Update authority person in database.
     *
     * @throws SQLException Is thrown when was problem with writing to database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public final void update() throws SQLException, AuthorizeException {
        if (!AuthorizeManager.isAdmin(ourContext)) {
            throw new AuthorizeException("Only administrators has permission to update authority person.");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LogManager.getHeader(ourContext, "update_authority_person", "authority_person_id=" + getId()));
        }
        DatabaseManager.update(ourContext, tableRow);
    }

    /**
     * Delete authority person from database.
     *
     * @throws SQLException Is thrown when was problem with writing to database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public final void delete() throws SQLException, AuthorizeException {
        if (!AuthorizeManager.isAdmin(ourContext)) {
            throw new AuthorizeException("Only administrators has permission to delete authority person.");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LogManager.getHeader(ourContext, "delete_authority_person", "authority_person_id=" + getId()));
        }

        final List<Authority> newestAuthorities = getAuthorities();
        for (final Authority authority : newestAuthorities) {
            authority.delete();
        }

        ourContext.removeCached(this, getId());
        DatabaseManager.delete(ourContext, tableRow);
    }

    /**
     * Returns id of authority person in database.
     *
     * @return Id of authority person.
     */
    public final int getId() {
        return tableRow.getIntColumn("authority_person_id");
    }

    /**
     * Returns uid for authority person.
     *
     * @return String with uid.
     */
    public final String getUid() {
        return tableRow.getStringColumn("authority_person_uid");
    }

    /**
     * Sets uid for authority person.
     *
     * @param uid uid which will be user for person.
     */
    public final void setUid(final String uid) {
        tableRow.setColumn("authority_person_uid", uid);
    }

    /**
     * Returns last name of authority person.
     *
     * @return Last name of this person.
     */
    public final String getLastName() {
        return tableRow.getStringColumn("lastname");
    }

    /**
     * Sets last name of authority person.
     *
     * @param lastName Last name which will be set.
     */
    public final void setLastName(final String lastName) {
        tableRow.setColumn("lastname", lastName);
    }

    /**
     * Returns first name of authority person.
     *
     * @return Returns first name.
     */
    public final String getFirstName() {
        return tableRow.getStringColumn("firstname");
    }

    /**
     * Sets firstname of authority person.
     *
     * @param firstName First name for this person.
     */
    public final void setFirstName(final String firstName) {
        tableRow.setColumn("firstname", firstName);
    }

    /**
     * Returns name of authority person.
     *
     * @return Name for this person.
     */
    public final String getName() {
        return tableRow.getStringColumn("lastname") + ", " + tableRow.getStringColumn("firstname");
    }

    /**
     * Returns list of authorities for authority person.
     *
     * @return List of authorities.
     *
     * @throws SQLException Is thrown when was problem with reading from database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public final List<Authority> getAuthorities() throws SQLException, AuthorizeException {
        if (authorities == null) {
            authorities = new ArrayList<>();
            final String query = "SELECT * FROM AUTHORITY WHERE authority_person_id = ?";
            final TableRowIterator rows = DatabaseManager.queryTable(ourContext, "AUTHORITY", query, getId());

            try {
                Authority authority;
                TableRow row;
                while (rows.hasNext()) {
                    row = rows.next();

                    authority = (Authority) ourContext.fromCache(Authority.class, row.getIntColumn("authority_id"));
                    if (authority == null) {
                        authorities.add(new Authority(ourContext, row));
                    } else {
                        authorities.add(authority);
                    }
                }
            } finally {
                // close the TableRowIterator to free up resources
                if (rows != null) {
                    rows.close();
                }
            }
        }

        return authorities;
    }

    /**
     * Get key of authority.
     *
     * @param authorityName Name of authority.
     * @return Key if exists, otherwise null.
     * @throws SQLException Is thrown when was problem with reading from database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public final String getAuthorityKey(final String authorityName) throws SQLException, AuthorizeException {
        if (!AuthorizeManager.isAdmin(ourContext)) {
            throw new AuthorizeException("Only administrators has permission to read authority key.");
        }

        final List<Authority> newestAuthorities = getAuthorities();
        String key = null;

        for (final Authority authority : newestAuthorities) {
            if (authority.getName().equals(authorityName)) {
                key = authority.getKey();
                break;
            }
        }

        return key;
    }

    /**
     * Create an authority in this authority person, with immediate effect.
     *
     * @return the newly created bundle
     *
     * @throws SQLException Is thrown when was problem with writing to database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public final Authority createAuthority() throws SQLException, AuthorizeException {
        final Authority authority = Authority.create(ourContext);
        addAuthority(authority);
        return authority;
    }

    /**
     * Add an existing authority to this authority person. This has immediate effect.
     *
     * @param authority authority to add
     *
     * @throws SQLException Is thrown when was problem with writing to database.
     * @throws AuthorizeException Is throw when logged user in context is not admin.
     */
    public final void addAuthority(final Authority authority) throws SQLException, AuthorizeException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LogManager.getHeader(ourContext, "add_authority", "authority_person_id="
                    + getId() + ",authority_id=" + authority.getId()));
        }

        // Check it's not already there
        final List<Authority> newestAuthorities = getAuthorities();
        for (final Authority authorityInPerson : newestAuthorities) {
            if (authorityInPerson.getId() == authority.getId()) {
                return;
            }
        }

        authority.setAuthorityPersonId(getId());
        authority.update();
        authorities.add(authority);
    }

    /**
     * Returns date of creating for authority person.
     *
     * @return Date of creation of authority person.
     */
    public final Date getCreated() {
        Date date = null;
        try {
            final DateFormat formatter = new SimpleDateFormat(DATE_FORMAT_STRING, Locale.ENGLISH);
            date = formatter.parse(tableRow.getStringColumn("created"));
        } catch (ParseException e) {
            LOGGER.error("Could not parse date from database at authority_person_id=" + getId() + ". " + e);
        }
        return date;
    }

    /**
     * Generate UID for this authority person.
     */
    public final void generateUID() {
        final String uid = UUID.randomUUID().toString();
        tableRow.setColumn("authority_person_uid", uid);
    }
}
