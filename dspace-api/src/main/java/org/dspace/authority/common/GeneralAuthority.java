/**
 * This package contains classes for general authority module.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version $Revision$
 * @since 2015-12-03
 */
package org.dspace.authority.common;

import org.apache.log4j.Logger;
import org.dspace.content.DCPersonName;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents searching thought authority persons for general authority module.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version $Revision$
 * @since 2015-12-03
 */
public class GeneralAuthority implements ChoiceAuthority {

    /** Log4j logger instance. */
    private static final Logger LOGGER = Logger.getLogger(GeneralAuthority.class);

    /**
     * Get all values from the authority that match the preferred value. Note that the offering was entered by the user
     * and may contain mixed/incorrect case, whitespace, etc so the plugin should be careful to clean up user data
     * before making comparisons.
     * <p/>
     * Value of a "Name" field will be in canonical DSpace person name format, which is "Lastname, Firstname(s)", e.g.
     * "Smith, John Q.".
     * <p/>
     * Some authorities with a small set of values may simply return the whole set for any sample value, although it's a
     * good idea to set the defaultSelected index in the Choices instance to the choice, if any, that matches the
     * value.
     *
     * @param field      being matched for
     * @param text       user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start      choice at which to start, 0 is first.
     * @param limit      maximum number of choices to return, 0 for no limit.
     * @param locale     explicit localization key if available, or null
     *
     * @return a Choices object (never null).
     */
    @Override
    public final Choices getMatches(final String field, final String text, final int collection, final int start, final int limit, final String locale) {
        Context context = null;
        Choices choices = null;

        String name = repairText(text);

        try {
            context = new Context();
            final DCPersonName personName = new DCPersonName(name);
            final List<AuthorityPersonIterator> iterators = new ArrayList<>();

            iterators.add(AuthorityPerson.findByName(context, personName.getFirstNames(), personName.getLastName()));
            iterators.add(AuthorityPerson.findByName(context, personName.getLastName(), personName.getFirstNames()));
            iterators.add(AuthorityPerson.findLikeName(context, personName.getLastName(), personName.getFirstNames()));
            iterators.add(AuthorityPerson.findLikeName(context, personName.getFirstNames(), personName.getLastName()));
            iterators.add(AuthorityPerson.findLikeName(context, name));

            int count = 0;
            AuthorityPerson person;
            final List<Choice> choiceList = new ArrayList<>();
            final List<AuthorityPerson> personList = new ArrayList<>();
            AuthorityPersonIterator lastIterator = null;
            for (AuthorityPersonIterator personIterator : iterators) {
                while (personIterator.hasNext() && count - start < limit) {
                    person = personIterator.next();
                    if (count >= start && !personList.contains(person)) {
                        personList.add(person);
                    }
                    if (count - start >= limit) {
                        lastIterator = personIterator;
                    }
                    count++;
                }
            }

            for (AuthorityPerson authorityPerson : personList) {
                choiceList.add(new Choice(authorityPerson.getUid(), authorityPerson.getName(), authorityPerson.getName()));
            }

            int confidence;
            switch (count) {
                case 0:
                    confidence = Choices.CF_NOTFOUND;
                    break;
                case 1:
                    confidence = Choices.CF_UNCERTAIN;
                    break;
                default:
                    confidence = Choices.CF_AMBIGUOUS;
                    break;
            }

            if (lastIterator != null) {
                choices = new Choices(choiceList.toArray(new Choice[0]), start, count, confidence, lastIterator.hasNext());
            } else {
                choices = new Choices(choiceList.toArray(new Choice[0]), start, count, confidence, false);
            }
            context.complete();
        } catch (SQLException e) {
            LOGGER.error("Something get wrong while searching for matches. " + e);
        } finally {
            if (context != null && context.isValid()) {
                context.abort();
                LOGGER.error("Something get wrong. Aborting context in finally statement.");
            }
        }
        return choices;
    }

    private String repairText(String text) {
        // Author Jan and Jan Author
        if (text.split(", ").length == 1 && text.split(" ").length == 2) {
            String firstName = text.split(" ")[0].trim();
            firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1);
            String secondName = text.split(" ")[1].trim();
            secondName = secondName.substring(0, 1).toUpperCase() + secondName.substring(1);
            String name = firstName + ", " + secondName;
            return name;
        }
        // Author, Jan and Jan, Author
        else if (text.split(", ").length == 2) {
            String firstName = text.split(", ")[0].trim();
            firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1);
            String secondName = text.split(", ")[1].trim();
            secondName = secondName.substring(0, 1).toUpperCase() + secondName.substring(1);
            String name = firstName + ", " + secondName;
            return name;
        }
        return text;
    }

    /**
     * Get the single "best" match (if any) of a value in the authority to the given user value.  The "confidence"
     * element of Choices is expected to be set to a meaningful value about the circumstances of this match.
     * <p/>
     * This call is typically used in non-interactive metadata ingest where there is no interactive agent to choose from
     * among options.
     *
     * @param field      being matched for
     * @param text       user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param locale     explicit localization key if available, or null
     *
     * @return a Choices object (never null) with 1 or 0 values.
     */
    @Override
    public final Choices getBestMatch(final String field, final String text, final int collection, final String locale) {
        // TODO punt!  this is a poor implementation..
        return getMatches(field, text, collection, 0, 5, locale);
    }

    /**
     * Get the canonical user-visible "label" (i.e. short descriptive text) for a key in the authority.  Can be
     * localized given the implicit or explicit locale specification.
     * <p/>
     * This may get called many times while populating a Web page so it should be implemented as efficiently as
     * possible.
     *
     * @param field  being matched for
     * @param key    authority key known to this authority.
     * @param locale explicit localization key if available, or null
     *
     * @return descriptive label - should always return something, never null.
     */
    @Override
    public final String getLabel(final String field, final String key, final String locale) {
        Context context = null;
        String value = "";

        try {
            context = new Context();
            value = AuthorityPerson.findByUID(context, key).getName();
        } catch (SQLException e) {
            LOGGER.error("Something get wrong while searching for matches. " + e);
        } finally {
            if (context != null && context.isValid()) {
                context.abort();
                LOGGER.error("Something get wrong. Aborting context in finally statement.");
            }
        }

        return value;
    }
}
