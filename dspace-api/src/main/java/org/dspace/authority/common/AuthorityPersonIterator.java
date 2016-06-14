/**
 * This package contains classes for general authority module.
 *
 * @since 2015-12-03
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version $Revision$
 */
package org.dspace.authority.common;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;

/**
 * Specialized iterator for authority persons. This class basically wraps
 * a TableRowIterator.
 *
 * Note that this class is not a real Iterator, as it does not implement
 * the Iterator interface
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version $Revision$
 * @since 2015-12-05
 */
public class AuthorityPersonIterator {

    /** Our context. */
    private final Context ourContext;

    /** The table row iterator of Item rows. */
    private final TableRowIterator personRows;

    /**
     * Construct an authority person iterator using a set of TableRow objects
     * from database table.
     *
     * @param context our context
     * @param rows the rows that correspond to the authority persons to be
     *             iterated over
     */
    public AuthorityPersonIterator(final Context context, final TableRowIterator rows) {
        ourContext = context;
        personRows = rows;
    }

    /**
     * Find out if there are any more items to iterate over.
     *
     * @return <code>true</code> if there are more items
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public final boolean hasNext() throws SQLException {
        boolean value = false;

        if (personRows != null) {
            value = personRows.hasNext();
        }

        return value;
    }

    /**
     * Get the next authority person in the iterator. Returns <code>null</code>
     * if there are no more items.
     *
     * @return the next authority person, or <code>null</code>
     * @throws SQLException Is thrown when was problem with reading from database.
     */
    public final AuthorityPerson next() throws SQLException {
        AuthorityPerson person = null;

        if (personRows.hasNext()) {
            final TableRow row = personRows.next();
            person = (AuthorityPerson) ourContext.fromCache(AuthorityPerson.class, row.getIntColumn("authority_person_id"));
            if (person == null) {
                person = new AuthorityPerson(ourContext, row);
            }
        }

        return person;
    }
}
