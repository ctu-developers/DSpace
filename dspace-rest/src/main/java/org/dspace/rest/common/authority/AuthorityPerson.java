package org.dspace.rest.common.authority;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class represents person in authority module.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version 1.0
 * @since 2015-12-03
 */
@XmlRootElement
public class AuthorityPerson {

    /** Log4j for logging. */
    private final static Logger LOGGER = Logger.getLogger(AuthorityPerson.class);

    /** Generated uid from authority module. */
    private String uid;
    /** Last name of person. */
    private String lastName;
    /** First name of person. */
    private String firstName;
    /** List of authorities for person. */
    private List<Authority> authorities;
    /** Date of creation of this person in authority module. */
    private Date created;

    /** Forbidden authorities from anonymous users. */
    public static final List<String> FORBIDDEN_AUTHORITIES;

    static {
        String authorities = ConfigurationManager.getProperty("rest", "forbidden-authorities");
        FORBIDDEN_AUTHORITIES = new ArrayList<>();
        if (authorities != null) {
            String[] parsedAuthorities = authorities.split(",");
            for (String name : parsedAuthorities) {
                FORBIDDEN_AUTHORITIES.add(name.trim());
            }
        }
        LOGGER.debug("FORBIDDEN_AUTHORITIES: " + FORBIDDEN_AUTHORITIES.toString());
    }

    /** Create empty authority person. */
    public AuthorityPerson() {
         /* Constructor for rest. */
    }

    /**
     * Create authority person.
     *
     * @param person Authority person from api.
     */
    public AuthorityPerson(final org.dspace.authority.common.AuthorityPerson person, boolean anonymous) throws AuthorizeException {
        uid = person.getUid();
        lastName = person.getLastName();
        firstName = person.getFirstName();
        authorities = new ArrayList<>();
        try {
            for (org.dspace.authority.common.Authority apiAuthority : person.getAuthorities()) {
                if (!anonymous || !FORBIDDEN_AUTHORITIES.contains(apiAuthority.getName())) {
                    authorities.add(new Authority(apiAuthority));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Could not read authorities from database.");
        }
        created = person.getCreated();
    }

    /**
     * Returns uid for authority person.
     *
     * @return String with uid.
     */
    public final String getUid() {
        return uid;
    }

    /**
     * Sets uid for authority person.
     *
     * @param uid uid which will be user for person.
     */
    public final void setUid(final String uid) {
        this.uid = uid;
    }

    /**
     * Returns last name of authority person.
     *
     * @return Last name of this person.
     */
    public final String getLastName() {
        return lastName;
    }

    /**
     * Sets last name of authority person.
     *
     * @param lastName Last name which will be set.
     */
    public final void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns first name of authority person.
     *
     * @return Returns first name.
     */
    public final String getFirstName() {
        return firstName;
    }

    /**
     * Sets firstname of authority person.
     *
     * @param firstName First name for this person.
     */
    public final void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns list of authorities for authority person.
     *
     * @return List of authorities.
     */
    public final List<Authority> getAuthorities() {
        return authorities;
    }

    /**
     * Sets list of authorities for authority person.
     *
     * @param authorities List of authorities for this person.
     */
    public final void setAuthorities(final List<Authority> authorities) {
        this.authorities = authorities;
    }

    /**
     * Returns date of creating for authority person.
     *
     * @return Date of creation of authority person.
     */
    public final String getCreated() {
        final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        return formatter.format(created);
    }

    /**
     * Sets date of creating of authority person.
     *
     * @param created Date of creation.
     */
    public final void setCreated(final Date created) {
        this.created = created;
    }
}
